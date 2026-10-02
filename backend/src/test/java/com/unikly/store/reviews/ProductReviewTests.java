package com.unikly.store.reviews;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.unikly.store.catalog.domain.CatalogProduct;
import com.unikly.store.catalog.persistence.CatalogProductRepository;
import com.unikly.store.identity.domain.StoreRole;
import com.unikly.store.identity.domain.StoreUser;
import com.unikly.store.identity.persistence.StoreUserRepository;
import com.unikly.store.orders.application.CustomerOrderService;
import com.unikly.store.orders.application.OrderRequests;
import com.unikly.store.reviews.application.ProductReviewService;
import com.unikly.store.reviews.application.ReviewRequests;
import com.unikly.store.reviews.application.ReviewViews.ProductReviewSummaryView;
import com.unikly.store.reviews.application.ReviewViews.ReviewItemView;
import com.unikly.store.reviews.persistence.ProductReviewRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ProductReviewTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProductReviewService reviewService;
    @Autowired private ProductReviewRepository reviewRepository;
    @Autowired private CatalogProductRepository productRepository;
    @Autowired private StoreUserRepository userRepository;
    @Autowired private CustomerOrderService orderService;
    @Autowired private PasswordEncoder passwordEncoder;

    private String testSuffix;
    private StoreUser buyer;
    private StoreUser seller;
    private StoreUser admin;
    private CatalogProduct product;
    private final String password = "SecretPassword123!";

    @BeforeEach
    void setUp() {
        testSuffix = UUID.randomUUID().toString().substring(0, 8);
        buyer = userRepository.save(new StoreUser(
                "review-buyer-" + testSuffix + "@example.com",
                passwordEncoder.encode(password),
                "Review Buyer " + testSuffix,
                StoreRole.BUYER));
        seller = userRepository.save(new StoreUser(
                "review-seller-" + testSuffix + "@example.com",
                passwordEncoder.encode(password),
                "Review Seller " + testSuffix,
                StoreRole.SELLER));
        admin = userRepository.save(new StoreUser(
                "review-admin-" + testSuffix + "@example.com",
                passwordEncoder.encode(password),
                "Review Admin " + testSuffix,
                StoreRole.ADMIN));

        product = productRepository.save(new CatalogProduct(
                "prod-" + testSuffix,
                seller.getId(),
                "Handcrafted Mug " + testSuffix,
                "Beautiful stoneware mug",
                "Kitchen",
                new BigDecimal("24.00"),
                "https://example.com/mug.jpg",
                50));
    }

    private MockHttpSession login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void publicCanRetrieveEmptyReviewSummary() throws Exception {
        mockMvc.perform(get("/api/products/" + product.getId() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is(product.getId())))
                .andExpect(jsonPath("$.totalReviews", is(0)))
                .andExpect(jsonPath("$.averageRating", is(0.0)))
                .andExpect(jsonPath("$.reviews", hasSize(0)))
                .andExpect(jsonPath("$.currentUserCanReview", is(false)))
                .andExpect(jsonPath("$.currentUserVerifiedBuyer", is(false)));
    }

    @Test
    void buyerCanSubmitReviewAndGetVerifiedBadgeAfterPurchase() throws Exception {
        MockHttpSession buyerSession = login(buyer.getEmail());

        // 1. Submit review before purchase (unverified)
        mockMvc.perform(post("/api/products/" + product.getId() + "/reviews")
                        .session(buyerSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "rating": 5,
                                    "title": "Stunning quality!",
                                    "comment": "Exceeded all my expectations. The finish is gorgeous."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating", is(5)))
                .andExpect(jsonPath("$.title", is("Stunning quality!")))
                .andExpect(jsonPath("$.isVerifiedPurchase", is(false)));

        // 2. Fetch summary as buyer - should show currentUserReview
        mockMvc.perform(get("/api/products/" + product.getId() + "/reviews").session(buyerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReviews", is(1)))
                .andExpect(jsonPath("$.averageRating", is(5.0)))
                .andExpect(jsonPath("$.currentUserCanReview", is(true)))
                .andExpect(jsonPath("$.currentUserReview.title", is("Stunning quality!")));

        // 3. Buyer places an order for the product
        orderService.create(buyer.getEmail(), new OrderRequests.Create(
                "Jane Buyer", "jane@example.com", "555-1234",
                "123 Artisans Way", "", "Portland", "OR", "97201", "US",
                "STANDARD", List.of(new OrderRequests.Item(product.getId(), 1))));

        // 4. Update the review - should now be marked as verified purchase
        mockMvc.perform(post("/api/products/" + product.getId() + "/reviews")
                        .session(buyerSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "rating": 4,
                                    "title": "Updated: Really great after actual use",
                                    "comment": "Holds heat very well throughout the morning."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating", is(4)))
                .andExpect(jsonPath("$.isVerifiedPurchase", is(true)));

        // 5. Verify summary reflects updated rating (still 1 review total, rating 4.0)
        ProductReviewSummaryView summary = reviewService.getProductReviewSummary(product.getId(), buyer.getEmail());
        assertEquals(1, summary.totalReviews());
        assertEquals(4.0, summary.averageRating());
        assertTrue(summary.currentUserVerifiedBuyer());
        assertNotNull(summary.currentUserReview());
        assertTrue(summary.currentUserReview().isVerifiedPurchase());
    }

    @Test
    void sellerCannotSubmitReview() throws Exception {
        MockHttpSession sellerSession = login(seller.getEmail());

        mockMvc.perform(post("/api/products/" + product.getId() + "/reviews")
                        .session(sellerSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "rating": 5,
                                    "title": "Nice product",
                                    "comment": "Testing forbidden access."
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void reviewValidationRejectsInvalidRatings() throws Exception {
        MockHttpSession buyerSession = login(buyer.getEmail());

        // Rating 0 is invalid
        mockMvc.perform(post("/api/products/" + product.getId() + "/reviews")
                        .session(buyerSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "rating": 0,
                                    "title": "Zero stars",
                                    "comment": "Not allowed"
                                }
                                """))
                .andExpect(status().isBadRequest());

        // Rating 6 is invalid
        mockMvc.perform(post("/api/products/" + product.getId() + "/reviews")
                        .session(buyerSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "rating": 6,
                                    "title": "Six stars",
                                    "comment": "Not allowed"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buyerCanDeleteOwnReviewAndAdminCanModerate() throws Exception {
        MockHttpSession buyerSession = login(buyer.getEmail());
        MockHttpSession adminSession = login(admin.getEmail());

        // Submit review
        MvcResult created = mockMvc.perform(post("/api/products/" + product.getId() + "/reviews")
                        .session(buyerSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "rating": 3,
                                    "title": "Moderate feedback",
                                    "comment": "It's decent but could be improved."
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        // Delete own review
        mockMvc.perform(delete("/api/products/" + product.getId() + "/reviews/mine")
                        .session(buyerSession)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        ProductReviewSummaryView afterSelfDelete = reviewService.getProductReviewSummary(product.getId(), null);
        assertEquals(0, afterSelfDelete.totalReviews());

        // Re-submit review
        ReviewItemView newReview = reviewService.submitReview(buyer.getEmail(), product.getId(),
                new ReviewRequests.SubmitReview(5, "Second chance", "Much better this time!"));
        assertEquals(1, reviewRepository.countByProductId(product.getId()));

        // Admin moderation deletes the review
        mockMvc.perform(delete("/api/reviews/" + newReview.id())
                        .session(adminSession)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertEquals(0, reviewRepository.countByProductId(product.getId()));
    }
}
