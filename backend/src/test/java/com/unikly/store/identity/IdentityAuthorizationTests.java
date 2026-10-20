package com.unikly.store.identity;

import com.unikly.store.identity.application.IdentityEmailDelivery;
import com.unikly.store.identity.application.IdentityEmailMessage;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.mockito.ArgumentCaptor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
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

    @MockitoBean
    private IdentityEmailDelivery emailDelivery;

    @Test
    void registrationLoginAndAuthorizationUseSelectedBuyerRole() throws Exception {
        String email = "buyer-" + java.util.UUID.randomUUID() + "@example.com";
        String token = registerForVerification(email, "Sample Buyer", "BUYER", true);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correct-horse-battery-staple"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized());

        confirmEmail(token).andExpect(status().isNoContent());
        confirmEmail(token).andExpect(status().isBadRequest());

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
        String initialToken = registerForVerification(email, "Initial Buyer", "BUYER", false);
        String updatedToken = registerForVerification(email, "Sample Seller", "SELLER", false);

        confirmEmail(initialToken).andExpect(status().isBadRequest());
        StoreUser seller = users.findByEmail(email).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(StoreRole.SELLER, seller.getRole());
        org.junit.jupiter.api.Assertions.assertEquals("Sample Seller", seller.getDisplayName());
        org.junit.jupiter.api.Assertions.assertFalse(seller.isEmailVerified());

        confirmEmail(updatedToken).andExpect(status().isNoContent());
        org.junit.jupiter.api.Assertions.assertTrue(users.findByEmail(email).orElseThrow().isEmailVerified());
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
                .andExpect(jsonPath("$.permissions", not(hasItem("REVIEW_CREATE_SELF"))))
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
        confirmEmail(registerForVerification(email, "Original Name", "BUYER", false))
                .andExpect(status().isNoContent());

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
        confirmEmail(registerForVerification(email, "Profile Owner", "BUYER", false))
                .andExpect(status().isNoContent());

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

    @Test
    void passwordRecoveryIsNonEnumeratingSingleUseAndRevokesActiveSessions() throws Exception {
        String email = "recovery-" + java.util.UUID.randomUUID() + "@example.com";
        users.save(new StoreUser(email, passwords.encode("correct-horse-battery-staple"), "Recovery User", StoreRole.BUYER));
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correct-horse-battery-staple"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        clearInvocations(emailDelivery);
        String knownResponse = requestPasswordReset(email);
        String token = capturedToken("/reset-password#token=");
        String unknownResponse = requestPasswordReset("missing-" + java.util.UUID.randomUUID() + "@example.com");
        org.junit.jupiter.api.Assertions.assertEquals(knownResponse, unknownResponse);

        mockMvc.perform(post("/api/auth/password-recovery/confirm")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"new-secure-password-927"}
                                """.formatted(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/password-recovery/confirm")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"another-new-password-927"}
                                """.formatted(token)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correct-horse-battery-staple"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"new-secure-password-927"}
                                """.formatted(email)))
                .andExpect(status().isOk());
    }

    private String registerForVerification(String email, String displayName, String accountType, boolean attemptAdminRole)
            throws Exception {
        clearInvocations(emailDelivery);
        String roleField = attemptAdminRole ? ",\"role\":\"ADMIN\"" : "";
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correct-horse-battery-staple","displayName":"%s","accountType":"%s"%s}
                                """.formatted(email, displayName, accountType, roleField)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message", is("If this address can receive email, instructions will be sent.")));
        return capturedToken("/verify-email#token=");
    }

    private org.springframework.test.web.servlet.ResultActions confirmEmail(String token) throws Exception {
        return mockMvc.perform(post("/api/auth/verification/confirm")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token":"%s"}
                        """.formatted(token)));
    }

    private String requestPasswordReset(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/password-recovery/request")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isAccepted())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private String capturedToken(String linkPrefix) {
        ArgumentCaptor<IdentityEmailMessage> message = ArgumentCaptor.forClass(IdentityEmailMessage.class);
        verify(emailDelivery).send(message.capture());
        Matcher matcher = Pattern.compile(Pattern.quote(linkPrefix) + "([A-Za-z0-9_-]+)")
                .matcher(message.getValue().text());
        if (!matcher.find()) throw new AssertionError("Expected an email link with the requested token purpose");
        return matcher.group(1);
    }
}
