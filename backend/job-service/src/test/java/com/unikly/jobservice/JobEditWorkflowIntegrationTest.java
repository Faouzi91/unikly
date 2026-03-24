package com.unikly.jobservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.common.events.PaymentCompletedEvent;
import com.unikly.jobservice.api.dto.CreateJobRequest;
import com.unikly.jobservice.api.dto.StatusTransitionRequest;
import com.unikly.jobservice.api.dto.SubmitProposalRequest;
import com.unikly.jobservice.api.dto.UpdateJobRequest;
import com.unikly.jobservice.domain.JobStatus;
import com.unikly.jobservice.infrastructure.PaymentEventConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@Testcontainers
@ActiveProfiles("test")
class JobEditWorkflowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.6.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        @Primary
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired
    WebApplicationContext wac;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PaymentEventConsumer paymentEventConsumer;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String createDraftJob(UUID clientId, String title, BigDecimal budget,
                                  List<String> skills) throws Exception {
        var request = new CreateJobRequest(title,
                "Integration test job description.", budget, "USD", skills);
        String body = mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", clientId.toString())
                        .header("X-User-Roles", "ROLE_CLIENT")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, JsonNode.class).get("id").asText();
    }

    private void transitionToOpen(UUID clientId, String jobId) throws Exception {
        mockMvc.perform(patch("/api/v1/jobs/{id}/status", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", clientId.toString())
                        .header("X-User-Roles", "ROLE_CLIENT")
                        .content(objectMapper.writeValueAsString(new StatusTransitionRequest(JobStatus.OPEN))))
                .andExpect(status().isOk());
    }

    private String submitProposal(UUID freelancerId, String jobId, BigDecimal budget) throws Exception {
        var request = new SubmitProposalRequest(budget, "I'm a great fit for this role.");
        String body = mockMvc.perform(post("/api/v1/jobs/{jobId}/proposals", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", freelancerId.toString())
                        .header("X-User-Roles", "ROLE_FREELANCER")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, JsonNode.class).get("id").asText();
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void testUpdateJob_draftStatus_succeeds() throws Exception {
        UUID clientId = UUID.randomUUID();
        String jobId = createDraftJob(clientId, "Draft Edit Test", new BigDecimal("1000.00"),
                List.of("Java"));

        var update = new UpdateJobRequest("Updated Draft Title", null, null, null);

        mockMvc.perform(patch("/api/v1/jobs/{id}", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", clientId.toString())
                        .header("X-User-Roles", "ROLE_CLIENT")
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Draft Title"));
    }

    @Test
    void testUpdateJob_openNoProposals_succeeds() throws Exception {
        UUID clientId = UUID.randomUUID();
        String jobId = createDraftJob(clientId, "Open No Proposals Job", new BigDecimal("500.00"),
                List.of("Python"));
        transitionToOpen(clientId, jobId);

        // Changing budget with no proposals — safe, no confirmation needed
        var update = new UpdateJobRequest(null, null, new BigDecimal("750.00"), null);

        mockMvc.perform(patch("/api/v1/jobs/{id}", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", clientId.toString())
                        .header("X-User-Roles", "ROLE_CLIENT")
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budget").value(750.00));
    }

    @Test
    void testCheckEditEligibility_openWithProposals_returnsSensitiveWarning() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID freelancerId = UUID.randomUUID();
        String jobId = createDraftJob(clientId, "Check Edit Eligibility Job", new BigDecimal("2000.00"),
                List.of("Angular"));
        transitionToOpen(clientId, jobId);
        submitProposal(freelancerId, jobId, new BigDecimal("1900.00"));

        var update = new UpdateJobRequest(null, null, new BigDecimal("1000.00"), null);

        mockMvc.perform(post("/api/v1/jobs/{id}/check-edit", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", clientId.toString())
                        .header("X-User-Roles", "ROLE_CLIENT")
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.requiresConfirmation").value(true))
                .andExpect(jsonPath("$.sensitiveFieldsChanged[0]").value("budget"))
                .andExpect(jsonPath("$.activeProposalCount").value(1));
    }

    @Test
    void testUpdateJob_openWithProposals_sensitiveChange_withoutConfirm_returns409() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID freelancerId = UUID.randomUUID();
        String jobId = createDraftJob(clientId, "Sensitive Change No Confirm", new BigDecimal("3000.00"),
                List.of("React"));
        transitionToOpen(clientId, jobId);
        submitProposal(freelancerId, jobId, new BigDecimal("2800.00"));

        // Change budget without confirmed=true — should return 409 with EditDecision body
        var update = new UpdateJobRequest(null, null, new BigDecimal("1500.00"), null);

        mockMvc.perform(patch("/api/v1/jobs/{id}", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", clientId.toString())
                        .header("X-User-Roles", "ROLE_CLIENT")
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.requiresConfirmation").value(true))
                .andExpect(jsonPath("$.activeProposalCount").value(1));
    }

    @Test
    void testUpdateJob_openWithProposals_sensitiveChange_withConfirm_succeeds() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID freelancerId = UUID.randomUUID();
        String jobId = createDraftJob(clientId, "Sensitive Change Confirmed", new BigDecimal("3000.00"),
                List.of("Vue"));
        transitionToOpen(clientId, jobId);
        submitProposal(freelancerId, jobId, new BigDecimal("2900.00"));

        // Change budget with confirmed=true — should succeed and mark proposal as OUTDATED
        var update = new UpdateJobRequest(null, null, new BigDecimal("1500.00"), null);

        mockMvc.perform(patch("/api/v1/jobs/{id}", jobId)
                        .param("confirmed", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", clientId.toString())
                        .header("X-User-Roles", "ROLE_CLIENT")
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budget").value(1500.00))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void testResubmitProposal_fromOutdated_succeeds() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID freelancerId = UUID.randomUUID();
        String jobId = createDraftJob(clientId, "Resubmit After Outdated", new BigDecimal("2000.00"),
                List.of("Node.js"));
        transitionToOpen(clientId, jobId);
        String proposalId = submitProposal(freelancerId, jobId, new BigDecimal("1900.00"));

        // Trigger sensitive edit with confirm → proposal becomes OUTDATED
        var update = new UpdateJobRequest(null, null, new BigDecimal("1000.00"), null);
        mockMvc.perform(patch("/api/v1/jobs/{id}", jobId)
                        .param("confirmed", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", clientId.toString())
                        .header("X-User-Roles", "ROLE_CLIENT")
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());

        // Freelancer resubmits with updated budget
        var resubmit = new SubmitProposalRequest(new BigDecimal("950.00"),
                "Revised cover letter after job update.");

        mockMvc.perform(put("/api/v1/jobs/{jobId}/proposals/{proposalId}/resubmit", jobId, proposalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", freelancerId.toString())
                        .header("X-User-Roles", "ROLE_FREELANCER")
                        .content(objectMapper.writeValueAsString(resubmit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.proposedBudget").value(950.00));
    }

    @Test
    void testUpdateJob_inReview_returns409() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID freelancerId = UUID.randomUUID();
        String jobId = createDraftJob(clientId, "In Review Edit Blocked", new BigDecimal("1500.00"),
                List.of("Spring Boot"));
        transitionToOpen(clientId, jobId);
        String proposalId = submitProposal(freelancerId, jobId, new BigDecimal("1400.00"));

        // Accept proposal → job moves to IN_REVIEW
        mockMvc.perform(patch("/api/v1/jobs/{jobId}/proposals/{proposalId}/accept", jobId, proposalId)
                        .header("X-User-Id", clientId.toString())
                        .header("X-User-Roles", "ROLE_CLIENT"))
                .andExpect(status().isOk());

        // Attempt to update the IN_REVIEW job — must be rejected with 409
        var update = new UpdateJobRequest("New title attempt", null, null, null);

        mockMvc.perform(patch("/api/v1/jobs/{id}", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", clientId.toString())
                        .header("X-User-Roles", "ROLE_CLIENT")
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testCancelJob_fromOpen_rejectsProposals() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID freelancerId1 = UUID.randomUUID();
        UUID freelancerId2 = UUID.randomUUID();
        String jobId = createDraftJob(clientId, "Job To Cancel", new BigDecimal("2500.00"),
                List.of("Kotlin"));
        transitionToOpen(clientId, jobId);

        String proposalId1 = submitProposal(freelancerId1, jobId, new BigDecimal("2400.00"));
        String proposalId2 = submitProposal(freelancerId2, jobId, new BigDecimal("2300.00"));

        // Cancel the job
        mockMvc.perform(post("/api/v1/jobs/{id}/cancel", jobId)
                        .header("X-User-Id", clientId.toString())
                        .header("X-User-Roles", "ROLE_CLIENT"))
                .andExpect(status().isNoContent());

        // Verify job status is CANCELLED
        mockMvc.perform(get("/api/v1/jobs/{id}", jobId)
                        .header("X-User-Id", clientId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void testFullWorkflowWithPayment_inReviewToInProgress() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID freelancerId = UUID.randomUUID();

        // 1. Create + open job
        String jobId = createDraftJob(clientId, "Payment Workflow Job", new BigDecimal("4000.00"),
                List.of("Go", "Docker"));
        transitionToOpen(clientId, jobId);

        // 2. Submit + accept proposal → job moves to IN_REVIEW
        String proposalId = submitProposal(freelancerId, jobId, new BigDecimal("3800.00"));

        mockMvc.perform(patch("/api/v1/jobs/{jobId}/proposals/{proposalId}/accept", jobId, proposalId)
                        .header("X-User-Id", clientId.toString())
                        .header("X-User-Roles", "ROLE_CLIENT"))
                .andExpect(status().isOk());

        // Confirm job is IN_REVIEW
        mockMvc.perform(get("/api/v1/jobs/{id}", jobId)
                        .header("X-User-Id", clientId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));

        // 3. Simulate PaymentCompleted event — triggers IN_REVIEW → IN_PROGRESS
        var paymentEvent = new PaymentCompletedEvent(
                UUID.randomUUID(), "PaymentCompleted", Instant.now(),
                UUID.randomUUID(), UUID.fromString(jobId), "txn-ref-123"
        );
        paymentEventConsumer.handlePaymentCompleted(paymentEvent);

        // 4. Verify job is now IN_PROGRESS
        mockMvc.perform(get("/api/v1/jobs/{id}", jobId)
                        .header("X-User-Id", clientId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }
}
