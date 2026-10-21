package com.unikly.store.catalog;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.unikly.store.catalog.application.CatalogProductService;
import com.unikly.store.catalog.application.CatalogProductService.ProductView;
import com.unikly.store.catalog.domain.CatalogProduct;
import com.unikly.store.catalog.persistence.CatalogProductRepository;
import com.unikly.store.identity.domain.StoreRole;
import com.unikly.store.identity.domain.StoreUser;
import com.unikly.store.identity.persistence.StoreUserRepository;
import com.unikly.store.profile.application.CustomerProfileService;
import com.unikly.store.profile.application.ProfileRequests;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SellerStoreProfileTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private StoreUserRepository users;
    @Autowired private CatalogProductRepository products;
    @Autowired private CatalogProductService catalogService;
    @Autowired private CustomerProfileService customerProfiles;

    @Test
    void sellerCanUpdatePublicStoreProfileAndBuyerSeesSellerIdentityWithoutPrivateSellerData() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String sellerPrivateEmail = "private-seller-" + suffix + "@example.com";
        StoreUser seller = users.save(
                new StoreUser(sellerPrivateEmail, "hash", "Default Seller Name", StoreRole.SELLER));

        // Seller also has private personal contact/address data in CustomerProfile
        customerProfiles.update(sellerPrivateEmail, new ProfileRequests.Update(
                "Default Seller Name",
                "+12025550177",
                "742 Private Residence Ave",
                "Apt 9B",
                "Seattle",
                "WA",
                "98101",
                "US"));

        String realProductId = UUID.randomUUID().toString();
        products.save(new CatalogProduct(
                realProductId, seller.getId(), "Handcrafted Oak Tray",
                "Solid white oak catchall tray with natural oil finish.",
                "Home", new BigDecimal("42.00"),
                "https://images.unsplash.com/photo-1517256064527-09c73fc73e38", 8, false));

        String demoProductId = UUID.randomUUID().toString();
        products.save(new CatalogProduct(
                demoProductId, seller.getId(), "Seeded Demo Lamp",
                "Sample fixture listing.",
                "Home", new BigDecimal("35.00"),
                "https://images.unsplash.com/photo-1563861826100-9cb868fdbe1c", 5, true));

        // Before customization, public seller profile defaults to displayName and omits private email/address
        mockMvc.perform(get("/api/sellers/" + seller.getId() + "/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerId").value(seller.getId()))
                .andExpect(jsonPath("$.storeName").value("Default Seller Name"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.phoneNumber").doesNotExist())
                .andExpect(content().string(not(containsString(sellerPrivateEmail))))
                .andExpect(content().string(not(containsString("+12025550177"))))
                .andExpect(content().string(not(containsString("742 Private Residence Ave"))));

        // Seller updates their public store profile
        mockMvc.perform(put("/api/sellers/me/profile")
                        .with(user(sellerPrivateEmail).roles("SELLER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeName": "Atelier Nord Studio",
                                  "headline": "Small-batch woodworking & everyday homewares",
                                  "description": "Crafted in our Pacific Northwest workshop using sustainably sourced hardwood.",
                                  "location": "Portland, OR",
                                  "supportEmail": "care@ateliernord.example"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerId").value(seller.getId()))
                .andExpect(jsonPath("$.storeName").value("Atelier Nord Studio"))
                .andExpect(jsonPath("$.headline").value("Small-batch woodworking & everyday homewares"))
                .andExpect(jsonPath("$.location").value("Portland, OR"))
                .andExpect(jsonPath("$.supportEmail").value("care@ateliernord.example"));

        // Public buyer view reflects the updated store profile and keeps private seller account data hidden
        mockMvc.perform(get("/api/sellers/" + seller.getId() + "/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("Atelier Nord Studio"))
                .andExpect(jsonPath("$.headline").value("Small-batch woodworking & everyday homewares"))
                .andExpect(jsonPath("$.description").value("Crafted in our Pacific Northwest workshop using sustainably sourced hardwood."))
                .andExpect(jsonPath("$.location").value("Portland, OR"))
                .andExpect(jsonPath("$.supportEmail").value("care@ateliernord.example"))
                .andExpect(content().string(not(containsString(sellerPrivateEmail))))
                .andExpect(content().string(not(containsString("+12025550177"))))
                .andExpect(content().string(not(containsString("742 Private Residence Ave"))));

        // Public product catalog includes sellerStoreName on real seller offers, null on demo offers
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + realProductId + "')].sellerStoreName").value("Atelier Nord Studio"));

        List<ProductView> catalogViews = catalogService.list();
        ProductView realView = catalogViews.stream()
                .filter(p -> p.id().equals(realProductId))
                .findFirst()
                .orElseThrow();
        ProductView demoView = catalogViews.stream()
                .filter(p -> p.id().equals(demoProductId))
                .findFirst()
                .orElseThrow();
        assertEquals("Atelier Nord Studio", realView.sellerStoreName());
        assertNull(demoView.sellerStoreName());
    }

    @Test
    void nonSellersCannotManageStoreProfileAndNonSellerIdReturnsNotFound() throws Exception {
        String suffix = UUID.randomUUID().toString();
        StoreUser buyer = users.save(
                new StoreUser("buyer-" + suffix + "@example.com", "hash", "Regular Buyer", StoreRole.BUYER));

        mockMvc.perform(get("/api/sellers/me/profile")
                        .with(user(buyer.getEmail()).roles("BUYER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/sellers/me/profile")
                        .with(user(buyer.getEmail()).roles("BUYER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeName": "Unauthorized Store"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/sellers/" + buyer.getId() + "/profile"))
                .andExpect(status().isNotFound());
    }
}
