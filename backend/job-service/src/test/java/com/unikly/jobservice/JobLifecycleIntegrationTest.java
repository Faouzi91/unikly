package com.unikly.jobservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.jobservice.api.dto.CreateJobRequest;
import com.unikly.jobservice.api.dto.StatusTransitionRequest;
import com.unikly.jobservice.api.dto.SubmitProposalRequest;
import com.unikly.jobservice.domain.JobStatus;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@Testcontainers
@ActiveProfiles("test")
class JobLifecycleIntegrationTest {

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

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    void testCreateJob_returnsCreated() throws Exception {
        var request = new CreateJobRequest(
                "Build Angular Dashboard",
                "Need an Angular developer to build a responsive admin dashboard.",
                new BigDecimal("1500.00"),
                "USD",
                List.of("Angular", "TypeScript")
        );

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Roles", "ROLE_CLIENT")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void testCreateJob_invalidTitle_returns400() throws Exception {
        var request = new CreateJobRequest(
                "",           // blank — @NotBlank must reject
                "Valid description.",
                new BigDecimal("1000.00"),
                "USD",
                List.of("Java")
        );

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Roles", "ROLE_CLIENT")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("title"));
    }

    @Test
    void testFullJobLifecycle() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID freelancerId = UUID.randomUUID();

        // 1. Create job (DRAFT)
        var createRequest = new CreateJobRequest(
                "Full Stack Developer Needed",
                "Looking for an experienced full stack developer.",
                new BigDecimal("3000.00"),
                "USD",
                List.of("Java", "Angular", "PostgreSQL")
        );

        String createBody = mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", clientId.toString())
                        .header("X-User-Roles", "ROLE_CLIENT")
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        String jobId = objectMapper.readValue(createBody, JsonNode.class).get("id").asText();

        // 2. Transition DRAFT → OPEN
        mockMvc.perform(patch("/api/v1/jobs/{id}/status", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", clientId.toString())
                        .header("X-User-Roles", "ROLE_CLIENT")
                        .content(objectMapper.writeValueAsString(new StatusTransitionRequest(JobStatus.OPEN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));

        // 3. Submit proposal (freelancer)
        var proposalRequest = new SubmitProposalRequest(
                new BigDecimal("2800.00"),
                "I have 5 years experience with this stack and can deliver on time."
        );

        String proposalBody = mockMvc.perform(post("/api/v1/jobs/{jobId}/proposals", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", freelancerId.toString())
                        .header("X-User-Roles", "ROLE_FREELANCER")
                        .content(objectMapper.writeValueAsString(proposalRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String proposalId = objectMapper.readValue(proposalBody, JsonNode.class).get("id").asText();

        // 4. Accept proposal (client) — creates contract
        mockMvc.perform(patch("/api/v1/jobs/{jobId}/proposals/{proposalId}/accept", jobId, proposalId)
                        .header("X-User-Id", clientId.toString())
                        .header("X-User-Roles", "ROLE_CLIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void testInvalidStatusTransition_returns409() throws Exception {
        UUID clientId = UUID.randomUUID();

        // Create job (DRAFT)
        var createRequest = new CreateJobRequest(
                "Test Invalid Transition Job",
                "Testing that DRAFT to COMPLETED is rejected.",
                new BigDecimal("500.00"),
                "USD",
                List.of("Python")
        );

        String createBody = mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", clientId.toString())
                        .header("X-User-Roles", "ROLE_CLIENT")
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String jobId = objectMapper.readValue(createBody, JsonNode.class).get("id").asText();

        // Try DRAFT → COMPLETED (invalid — machine only allows DRAFT → OPEN)
        mockMvc.perform(patch("/api/v1/jobs/{id}/status", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", clientId.toString())
                        .header("X-User-Roles", "ROLE_CLIENT")
                        .content(objectMapper.writeValueAsString(new StatusTransitionRequest(JobStatus.COMPLETED))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }
}
