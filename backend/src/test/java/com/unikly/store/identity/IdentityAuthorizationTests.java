package com.unikly.store.identity;

import com.unikly.store.identity.domain.StoreRole;
import com.unikly.store.identity.domain.StoreUser;
import com.unikly.store.identity.persistence.StoreUserRepository;
import com.unikly.store.profile.application.ProfileRequests;
import com.unikly.store.profile.persistence.CustomerProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class IdentityAuthorizationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoreUserRepository users;

    @Autowired
    private PasswordEncoder passwords;

    @Autowired
    private CustomerProfileRepository profiles;

    @Test
    void registrationLoginAndAuthorizationUseSelectedBuyerRole() throws Exception {
    String email = "buyer-" + java.util.UUID.randomUUID() + "@example.com";

    mockMvc.perform(post("/api/auth/register")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"%s","password":"correct-horse-battery-staple","displayName":"Sample Buyer","accountType":"BUYER","role":"ADMIN"}
                """.formatted(email)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.role", is("BUYER")));

    MvcResult login = mockMvc.perform(post("/api/auth/login")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"%s","password":"correct-horse-battery-staple"}
                """.formatted(email)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role", is("BUYER")))
        .andReturn();

    MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
    mockMvc.perform(get("/api/auth/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email", is(email)))
        .andExpect(jsonPath("$.permissions", hasItem("ACCOUNT_READ_SELF")))
        .andExpect(jsonPath("$.permissions", hasItem("CATALOG_READ")))
        .andExpect(jsonPath("$.permissions", hasItem("ORDER_CREATE_SELF")))
        .andExpect(jsonPath("$.permissions", not(hasItem("PLATFORM_ADMIN"))));

    mockMvc.perform(get("/api/admin/access-check").session(session))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/unmapped").session(session))
        .andExpect(status().isForbidden());

    mockMvc.perform(post("/api/auth/logout").with(csrf()).session(session))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/auth/me").session(session))
        .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationAllowsSellerAccountTypeWithoutGrantingAdminPermissions() throws Exception {
        String email = "seller-" + java.util.UUID.randomUUID() + "@example.com";

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correct-horse-battery-staple","displayName":"Sample Seller","accountType":"SELLER"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role", is("SELLER")))
                .andExpect(jsonPath("$.permissions", not(hasItem("PLATFORM_ADMIN"))));
    }

    @Test
    void adminPermissionAllowsAdminApiAndBuyerCannotAccessUnmappedApi() throws Exception {
        String email = "admin-" + java.util.UUID.randomUUID() + "@example.com";
        users.save(new StoreUser(email, passwords.encode("correct-horse-battery-staple"), "Store Admin", StoreRole.ADMIN));

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correct-horse-battery-staple"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions", hasItem("PLATFORM_REPORT_READ")))
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/api/admin/access-check").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void stateChangingRequestsRequireCsrfToken() throws Exception {
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"csrf@example.com","password":"correct-horse-battery-staple","displayName":"Sample Buyer","accountType":"BUYER"}
                """))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/auth/me"))
        .andExpect(status().isUnauthorized());
    }

        @Test
        void customerCanUpdateOnlyOwnProfileAndPasswordChangeRevokesSession() throws Exception {
        String email = "profile-" + java.util.UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"correct-horse-battery-staple","displayName":"Original Name","accountType":"BUYER"}
                    """.formatted(email)))
            .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"correct-horse-battery-staple"}
                    """.formatted(email)))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(get("/api/profile/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName", is("Original Name")));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/profile/me")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"displayName":"Attempted Anonymous Edit"}
                    """))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/profile/me")
                .with(csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"displayName":"Updated Name","phoneNumber":"+12025550123","addressLine1":"1 Market Street","city":"San Francisco","region":"CA","postalCode":"94105","countryCode":"us"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName", is("Updated Name")))
            .andExpect(jsonPath("$.countryCode", is("US")));
        org.junit.jupiter.api.Assertions.assertTrue(profiles.findAll().stream()
            .anyMatch(profile -> profile.getPhoneNumber().equals("+12025550123")));

        mockMvc.perform(post("/api/auth/password")
                .with(csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"currentPassword":"correct-horse-battery-staple","newPassword":"another-secure-password-927"}
                    """))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"another-secure-password-927"}
                    """.formatted(email)))
            .andExpect(status().isOk());
        }

        @Test
        void profileUpdateRequiresCurrentProfilePermissionAndPasswordMustBeVerified() throws Exception {
        String email = "password-check-" + java.util.UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"correct-horse-battery-staple","displayName":"Profile Owner","accountType":"BUYER"}
                    """.formatted(email)))
            .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"correct-horse-battery-staple"}
                    """.formatted(email)))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(post("/api/auth/password")
                .with(csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"currentPassword":"incorrect-current-password","newPassword":"yet-another-new-password"}
                    """))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/profile/me").session(session))
            .andExpect(status().isOk());
        }
}