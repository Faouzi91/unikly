package com.unikly.store.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.unikly.store.catalog.domain.CatalogProduct;
import com.unikly.store.catalog.persistence.CatalogProductRepository;
import com.unikly.store.identity.domain.StoreRole;
import com.unikly.store.identity.domain.StoreUser;
import com.unikly.store.identity.persistence.StoreUserRepository;
import com.unikly.store.orders.application.CustomerOrderService;
import com.unikly.store.orders.application.OrderRequests;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
class CustomerOrderDeliveryTests {
    @Autowired private CustomerOrderService orders;
    @Autowired private StoreUserRepository users;
    @Autowired private CatalogProductRepository products;

    @Test
    void standardDeliveryUnderThresholdCalculatesFiveDollarFee() {
        String suffix = UUID.randomUUID().toString();
        String buyerEmail = "buyer-" + suffix + "@example.com";
        users.save(new StoreUser(buyerEmail, "hash", "Buyer", StoreRole.BUYER));
        StoreUser seller = users.save(new StoreUser("seller-" + suffix + "@example.com", "hash", "Seller", StoreRole.SELLER));
        String productId = UUID.randomUUID().toString();
        products.save(new CatalogProduct(productId, seller.getId(), "Mug", "Ceramic mug", "Kitchen",
                new BigDecimal("20.00"), "https://example.com/mug.jpg", 10));

        var req = new OrderRequests.Create("Buyer Name", buyerEmail, "+12025550123", "1 Market St",
                null, "SF", "CA", "94105", "US", "STANDARD",
                List.of(new OrderRequests.Item(productId, 1)));

        CustomerOrderService.OrderView order = orders.create(buyerEmail, req);

        assertEquals("STANDARD", order.deliveryMethod());
        assertEquals(new BigDecimal("5.00"), order.deliveryFee());
        assertEquals(new BigDecimal("25.00"), order.total());
    }

    @Test
    void standardDeliveryAtOrAboveThresholdIsFree() {
        String suffix = UUID.randomUUID().toString();
        String buyerEmail = "buyer-" + suffix + "@example.com";
        users.save(new StoreUser(buyerEmail, "hash", "Buyer", StoreRole.BUYER));
        StoreUser seller = users.save(new StoreUser("seller-" + suffix + "@example.com", "hash", "Seller", StoreRole.SELLER));
        String productId = UUID.randomUUID().toString();
        products.save(new CatalogProduct(productId, seller.getId(), "Keyboard", "Mechanical", "Electronics",
                new BigDecimal("60.00"), "https://example.com/kb.jpg", 10));

        var req = new OrderRequests.Create("Buyer Name", buyerEmail, "+12025550123", "1 Market St",
                null, "SF", "CA", "94105", "US", "STANDARD",
                List.of(new OrderRequests.Item(productId, 1)));

        CustomerOrderService.OrderView order = orders.create(buyerEmail, req);

        assertEquals("STANDARD", order.deliveryMethod());
        assertEquals(new BigDecimal("0.00"), order.deliveryFee());
        assertEquals(new BigDecimal("60.00"), order.total());
    }

    @Test
    void expressDeliveryCalculatesFifteenDollarFee() {
        String suffix = UUID.randomUUID().toString();
        String buyerEmail = "buyer-" + suffix + "@example.com";
        users.save(new StoreUser(buyerEmail, "hash", "Buyer", StoreRole.BUYER));
        StoreUser seller = users.save(new StoreUser("seller-" + suffix + "@example.com", "hash", "Seller", StoreRole.SELLER));
        String productId = UUID.randomUUID().toString();
        products.save(new CatalogProduct(productId, seller.getId(), "Headphones", "Wireless", "Electronics",
                new BigDecimal("100.00"), "https://example.com/hp.jpg", 10));

        var req = new OrderRequests.Create("Buyer Name", buyerEmail, "+12025550123", "1 Market St",
                null, "SF", "CA", "94105", "US", "EXPRESS",
                List.of(new OrderRequests.Item(productId, 1)));

        CustomerOrderService.OrderView order = orders.create(buyerEmail, req);

        assertEquals("EXPRESS", order.deliveryMethod());
        assertEquals(new BigDecimal("15.00"), order.deliveryFee());
        assertEquals(new BigDecimal("115.00"), order.total());
    }

    @Test
    void unsupportedDeliveryMethodThrowsBadRequest() {
        String suffix = UUID.randomUUID().toString();
        String buyerEmail = "buyer-" + suffix + "@example.com";
        users.save(new StoreUser(buyerEmail, "hash", "Buyer", StoreRole.BUYER));
        StoreUser seller = users.save(new StoreUser("seller-" + suffix + "@example.com", "hash", "Seller", StoreRole.SELLER));
        String productId = UUID.randomUUID().toString();
        products.save(new CatalogProduct(productId, seller.getId(), "Shirt", "Cotton", "Apparel",
                new BigDecimal("30.00"), "https://example.com/shirt.jpg", 10));

        var req = new OrderRequests.Create("Buyer Name", buyerEmail, "+12025550123", "1 Market St",
                null, "SF", "CA", "94105", "US", "DRONE_AIRDROP",
                List.of(new OrderRequests.Item(productId, 1)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> orders.create(buyerEmail, req));
        assertEquals(400, ex.getStatusCode().value());
    }
}
