package com.unikly.userservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.userservice.api.dto.UserProfileRequest;
import com.unikly.userservice.domain.UserRole;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class UserProfileIntegrationTest {

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

        @Bean(name = "testSecurityFilterChain")
        @Primary
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
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
    void testCreateProfile_returnsOk() throws Exception {
        UUID userId = UUID.randomUUID();

        var request = new UserProfileRequest(
                "Jane Freelancer",
                "Full-stack developer with 8 years of experience.",
                null,
                UserRole.FREELANCER,
                List.of("Java", "Spring Boot", "Angular"),
                new BigDecimal("75.00"),
                "USD",
                "Paris, France",
                List.of("https://github.com/jane")
        );

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", userId.toString())
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Jane Freelancer"))
                .andExpect(jsonPath("$.role").value("FREELANCER"))
                .andExpect(jsonPath("$.hourlyRate").value(75.00));
    }

    @Test
    void testGetMyProfile_afterCreate_returnsProfile() throws Exception {
        UUID userId = UUID.randomUUID();

        var request = new UserProfileRequest(
                "Bob Client",
                "Looking for skilled developers.",
                null,
                UserRole.CLIENT,
                List.of(),
                null,
                "USD",
                "New York, USA",
                List.of()
        );

        // Create profile
        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", userId.toString())
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Retrieve profile
        mockMvc.perform(get("/api/users/me")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Bob Client"))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    void testGetMyProfile_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/users/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateProfile_blankDisplayName_returns400() throws Exception {
        var request = new UserProfileRequest(
                "",   // @NotBlank — must be rejected
                "bio",
                null,
                UserRole.FREELANCER,
                List.of(),
                null,
                "USD",
                null,
                List.of()
        );

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("displayName"));
    }

    @Test
    void testSearchFreelancers_returnsPage() throws Exception {
        UUID userId = UUID.randomUUID();

        // Create a freelancer profile
        var request = new UserProfileRequest(
                "Alice Angular",
                "Angular specialist.",
                null,
                UserRole.FREELANCER,
                List.of("Angular", "TypeScript"),
                new BigDecimal("80.00"),
                "USD",
                "Berlin, Germany",
                List.of()
        );

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", userId.toString())
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Search freelancers
        mockMvc.perform(get("/api/users")
                        .param("skill", "Angular")
                        .header("X-User-Id", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
