package com.unikly.store.identity;

import com.unikly.store.identity.domain.StoreRole;
import com.unikly.store.identity.persistence.StoreUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

@SpringBootTest
@ActiveProfiles({"test", "dev"})
@TestPropertySource(properties = "unikly.dev-users.enabled=true")
@AutoConfigureMockMvc
class DevelopmentUserSeederTests {
    private static final String CUSTOMER_PASSWORD = testPassword();
    private static final String ADMIN_PASSWORD = testPassword();
    private static final String SELLER_PASSWORD = testPassword();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoreUserRepository users;

    @Autowired
    private PasswordEncoder passwords;

    @Autowired
    private ApplicationRunner developmentSeeder;

    @DynamicPropertySource
    static void developmentPasswords(DynamicPropertyRegistry properties) {
        properties.add("unikly.dev-users.customer.password", () -> CUSTOMER_PASSWORD);
        properties.add("unikly.dev-users.admin.password", () -> ADMIN_PASSWORD);
        properties.add("unikly.dev-users.seller.password", () -> SELLER_PASSWORD);
    }

    @Test
    void seedsDevelopmentAccountsWithEncodedPasswordsAndAllowsLogin() throws Exception {
        assertSeededLogin("customer@unikly.local", CUSTOMER_PASSWORD, StoreRole.BUYER);
        assertSeededLogin("admin@unikly.local", ADMIN_PASSWORD, StoreRole.ADMIN);
        assertSeededLogin("seller@unikly.local", SELLER_PASSWORD, StoreRole.SELLER);
    }


    @Test
    void repairsAnExistingDevelopmentAccountToConfiguredRoleAndPassword() throws Exception {
        var seller = users.findByEmail("seller@unikly.local").orElseThrow();
        seller.updatePasswordHash(passwords.encode("IncorrectOldPassword123!"));
        seller.updateRole(StoreRole.BUYER);
        users.save(seller);

        developmentSeeder.run(new DefaultApplicationArguments(new String[0]));

        assertSeededLogin("seller@unikly.local", SELLER_PASSWORD, StoreRole.SELLER);
    }

    private static String testPassword() {
        return "SeederTest-" + UUID.randomUUID() + "Aa1!";
    }

    private void assertSeededLogin(String email, String password, StoreRole role) throws Exception {
        var user = users.findByEmail(email).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(role, user.getRole());
        org.junit.jupiter.api.Assertions.assertNotEquals(password, user.getPasswordHash());
        org.junit.jupiter.api.Assertions.assertTrue(passwords.matches(password, user.getPasswordHash()));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is(role.name())));
    }
}
