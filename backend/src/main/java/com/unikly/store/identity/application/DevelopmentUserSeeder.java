package com.unikly.store.identity.application;

import com.unikly.store.catalog.domain.CatalogProduct;
import com.unikly.store.catalog.persistence.CatalogProductRepository;
import com.unikly.store.identity.domain.StoreRole;
import com.unikly.store.identity.domain.StoreUser;
import com.unikly.store.identity.persistence.StoreUserRepository;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
@ConditionalOnProperty(name = "unikly.dev-users.enabled", havingValue = "true")
class DevelopmentUserSeeder {
    @Bean
    ApplicationRunner seedDevelopmentUsers(
            StoreUserRepository users,
            PasswordEncoder passwords,
            CatalogProductRepository products,
            com.unikly.store.reviews.persistence.ProductReviewRepository reviews,
            @Value("${unikly.dev-users.customer.email}") String customerEmail,
            @Value("${unikly.dev-users.customer.password}") String customerPassword,
            @Value("${unikly.dev-users.admin.email}") String adminEmail,
            @Value("${unikly.dev-users.admin.password}") String adminPassword,
            @Value("${unikly.dev-users.seller.email}") String sellerEmail,
            @Value("${unikly.dev-users.seller.password}") String sellerPassword) {
        return args -> {
            requirePassword(customerPassword, "DEV_CUSTOMER_PASSWORD");
            requirePassword(adminPassword, "DEV_ADMIN_PASSWORD");
            requirePassword(sellerPassword, "DEV_SELLER_PASSWORD");
            StoreUser buyer = configureDevelopmentUser(users, passwords, customerEmail, customerPassword, "Demo Buyer", StoreRole.BUYER);
            configureDevelopmentUser(users, passwords, adminEmail, adminPassword, "Development Admin", StoreRole.ADMIN);
            StoreUser seller = configureDevelopmentUser(users, passwords, sellerEmail, sellerPassword, "Demo Seller", StoreRole.SELLER);

            for (CatalogProduct p : products.findAll()) {
                if (p.getSellerId() == null) {
                    p.assignSeller(seller.getId());
                    products.save(p);
                }
            }

            seedProduct(products, seller.getId(), "ceramic-pour-over",
                    "Ceramic Pour-Over Coffee Dripper",
                    "A handcrafted ceramic coffee dripper designed for precise water flow and even extraction.",
                    "Kitchen", new BigDecimal("32.00"),
                    "https://images.unsplash.com/photo-1517256064527-09c73fc73e38?auto=format&fit=crop&w=720&q=85",
                    20);
            seedProduct(products, seller.getId(), "matte-desk-clock",
                    "Matte Black Minimalist Desk Clock",
                    "A silent sweep movement clock with a matte powder-coated steel casing and clean dial.",
                    "Home", new BigDecimal("45.00"),
                    "https://images.unsplash.com/photo-1563861826100-9cb868fdbe1c?auto=format&fit=crop&w=720&q=85",
                    15);
            seedProduct(products, seller.getId(), "waffle-bath-towel",
                    "Waffle Weave Organic Cotton Bath Towel",
                    "Ultra-absorbent, fast-drying 100% organic cotton bath towel with textured honeycomb weave.",
                    "Home", new BigDecimal("38.50"),
                    "https://images.unsplash.com/photo-1616627547584-bf28cee262db?auto=format&fit=crop&w=720&q=85",
                    25);
            seedProduct(products, seller.getId(), "walnut-cutting-board",
                    "Handcrafted Walnut Cutting Board",
                    "Solid American black walnut cutting board with beveled edge grips and food-safe mineral oil finish.",
                    "Kitchen", new BigDecimal("54.00"),
                    "https://images.unsplash.com/photo-1590794056226-79ef3a8147e1?auto=format&fit=crop&w=720&q=85",
                    12);
            seedProduct(products, seller.getId(), "trailhead-duffle",
                    "Trailhead Canvas & Leather Duffle",
                    "Heavyweight waxed canvas weekender duffle bag with vegetable-tanned leather straps and brass hardware.",
                    "Outdoor", new BigDecimal("88.00"),
                    "https://images.unsplash.com/photo-1547949003-9792a18a2601?auto=format&fit=crop&w=720&q=85",
                    10);
            seedProduct(products, seller.getId(), "aluminum-pencil-set",
                    "Anodized Aluminum Mechanical Pencil Set",
                    "Precision-machined matte aluminum drafting pencils with knurled grip and balanced weight.",
                    "Home", new BigDecimal("29.00"),
                    "https://images.unsplash.com/photo-1583485088034-697b5bc54ccd?auto=format&fit=crop&w=720&q=85",
                    30);

            seedReview(reviews, "aluminum-pencil-set", buyer.getId(), buyer.getDisplayName(), 5,
                    "Incredible weight and precision",
                    "The balance and grip knurling are exceptional. It feels indestructible and writes like a dream.",
                    true);
            seedReview(reviews, "ceramic-pour-over", buyer.getId(), buyer.getDisplayName(), 5,
                    "Smooth brewing experience",
                    "The interior ribs guide the water flow evenly. A gorgeous centerpiece for my morning coffee.",
                    true);
            seedReview(reviews, "trailhead-duffle", buyer.getId(), buyer.getDisplayName(), 4,
                    "Rugged and spacious weekender",
                    "Quality canvas and solid brass hardware. Fits easily into overhead bins with room to spare.",
                    false);
        };
    }

    private static void seedReview(
            com.unikly.store.reviews.persistence.ProductReviewRepository reviews,
            String productId,
            Long buyerId,
            String authorName,
            int rating,
            String title,
            String comment,
            boolean isVerifiedPurchase) {
        if (!reviews.existsByProductIdAndBuyerId(productId, buyerId)) {
            reviews.save(new com.unikly.store.reviews.domain.ProductReview(
                    productId, buyerId, authorName, rating, title, comment, isVerifiedPurchase));
        }
    }

    private static void seedProduct(
            CatalogProductRepository products,
            Long sellerId,
            String id,
            String name,
            String description,
            String category,
            BigDecimal price,
            String image,
            int stockQuantity) {
        if (products.findById(id).isEmpty()) {
            products.save(new CatalogProduct(id, sellerId, name, description, category, price, image, stockQuantity));
        }
    }

    private static void requirePassword(String password, String variableName) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Development user seeding requires " + variableName + " to be set.");
        }
    }

    private static StoreUser configureDevelopmentUser(
            StoreUserRepository users,
            PasswordEncoder passwords,
            String email,
            String password,
            String displayName,
            StoreRole role) {
        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        StoreUser user = users.findByEmail(normalizedEmail).orElseGet(
                () -> new StoreUser(normalizedEmail, passwords.encode(password), displayName, role));
        user.updatePasswordHash(passwords.encode(password));
        user.updateDisplayName(displayName);
        user.updateRole(role);
        return users.save(user);
    }
}
