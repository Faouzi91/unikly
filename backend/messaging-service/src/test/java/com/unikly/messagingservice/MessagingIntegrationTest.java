package com.unikly.messagingservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.messagingservice.application.GetOrCreateConversationRequest;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class MessagingIntegrationTest {

    private static final UUID FIXED_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    // Token value = subject UUID so the decoder can derive the user from the token
    private static final String BEARER_TOKEN = FIXED_USER_ID.toString();

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.6.0");

    @SuppressWarnings("resource")
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        @Primary
        JwtDecoder jwtDecoder() {
            // Token value IS the subject UUID — allows tests to control which user is authenticated
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "RS256")
                    .subject(token)
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
        }

        @Bean(name = "testSecurityFilterChain")
        @Primary
        SecurityFilterChain testFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)));
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
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    void testGetConversations_returnsEmptyPage() throws Exception {
        // Fresh UUID — has no conversations
        String freshToken = UUID.randomUUID().toString();
        mockMvc.perform(get("/api/v1/messages/conversations")
                        .header("Authorization", "Bearer " + freshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void testCreateConversation_returnsOk() throws Exception {
        UUID otherUser = UUID.randomUUID();

        var request = new GetOrCreateConversationRequest(
                List.of(FIXED_USER_ID, otherUser),
                null
        );

        mockMvc.perform(post("/api/v1/messages/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + BEARER_TOKEN)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void testCreateConversation_tooFewParticipants_returns400() throws Exception {
        // participantIds must have exactly 2 elements (@Size(min = 2, max = 2))
        var request = new GetOrCreateConversationRequest(
                List.of(FIXED_USER_ID),
                null
        );

        mockMvc.perform(post("/api/v1/messages/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + BEARER_TOKEN)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateConversation_idempotent_returnsSameConversation() throws Exception {
        UUID otherUser = UUID.randomUUID();

        var request = new GetOrCreateConversationRequest(
                List.of(FIXED_USER_ID, otherUser),
                UUID.randomUUID()
        );

        // First call — creates
        String firstResponse = mockMvc.perform(post("/api/v1/messages/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + BEARER_TOKEN)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Second call with same participants — must return same conversation
        String secondResponse = mockMvc.perform(post("/api/v1/messages/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + BEARER_TOKEN)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var firstId = objectMapper.readTree(firstResponse).get("id").asText();
        var secondId = objectMapper.readTree(secondResponse).get("id").asText();
        assert firstId.equals(secondId) : "Expected same conversation ID on repeated call";
    }
}
