package com.unikly.store.identity.application;

import com.unikly.store.identity.domain.StoreRole;
import com.unikly.store.identity.domain.StoreUser;
import com.unikly.store.identity.persistence.StoreUserRepository;
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
            configureDevelopmentUser(users, passwords, customerEmail, customerPassword, "Demo Buyer", StoreRole.BUYER);
            configureDevelopmentUser(users, passwords, adminEmail, adminPassword, "Development Admin", StoreRole.ADMIN);
            configureDevelopmentUser(users, passwords, sellerEmail, sellerPassword, "Demo Seller", StoreRole.SELLER);
        };
    }

    private static void requirePassword(String password, String variableName) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Development user seeding requires " + variableName + " to be set.");
        }
    }

    private static void configureDevelopmentUser(
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
        users.save(user);
    }
}
