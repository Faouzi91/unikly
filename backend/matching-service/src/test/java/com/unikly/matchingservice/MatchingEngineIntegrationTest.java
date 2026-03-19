package com.unikly.matchingservice;

import com.unikly.common.dto.Money;
import com.unikly.common.events.JobCreatedEvent;
import com.unikly.matchingservice.application.AiMatchingClient;
import com.unikly.matchingservice.application.MatchingService;
import com.unikly.matchingservice.domain.FreelancerSkillCache;
import com.unikly.matchingservice.domain.MatchResult;
import com.unikly.matchingservice.infrastructure.FreelancerSkillCacheRepository;
import com.unikly.matchingservice.infrastructure.JobEventConsumer;
import com.unikly.matchingservice.infrastructure.MatchResultRepository;
import com.unikly.matchingservice.infrastructure.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class MatchingEngineIntegrationTest {

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

    // Mock the AI client so matching works offline (simulates circuit-open fallback)
    @MockitoBean
    AiMatchingClient aiMatchingClient;

    @Autowired
    MatchingService matchingService;

    @Autowired
    JobEventConsumer jobEventConsumer;

    @Autowired
    FreelancerSkillCacheRepository freelancerSkillCacheRepository;

    @Autowired
    MatchResultRepository matchResultRepository;

    @Autowired
    ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void setUp() {
        matchResultRepository.deleteAll();
        processedEventRepository.deleteAll();
        freelancerSkillCacheRepository.deleteAll();

        // AI circuit is "open": returns empty → 100% rule-based fallback
        when(aiMatchingClient.computeMatches(any(), anyString(), anyString(), anyList(), any()))
                .thenReturn(Collections.emptyList());
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    void testRuleBasedMatching_producesScores() {
        // Arrange: 5 freelancers with varying skill overlap
        UUID jobId = UUID.randomUUID();
        List<String> requiredSkills = List.of("Java", "Spring", "PostgreSQL");

        freelancerSkillCacheRepository.saveAll(List.of(
                freelancer(List.of("Java", "Spring", "PostgreSQL"), new BigDecimal("50.00"), new BigDecimal("4.5")),
                freelancer(List.of("Java", "Spring"), new BigDecimal("40.00"), new BigDecimal("4.0")),
                freelancer(List.of("Java"), new BigDecimal("30.00"), new BigDecimal("3.5")),
                freelancer(List.of("React", "TypeScript"), new BigDecimal("60.00"), new BigDecimal("4.8")),
                freelancer(List.of("Java", "PostgreSQL", "Docker"), new BigDecimal("45.00"), new BigDecimal("4.2"))
        ));

        var event = jobCreatedEvent(jobId, requiredSkills, new BigDecimal("8000.00"));

        // Act
        matchingService.processJobForMatching(event);

        // Assert: results exist, sorted by score DESC, scores clamped to [0.0, 1.0]
        List<MatchResult> results = matchResultRepository.findByJobIdOrderByScoreDesc(
                jobId, PageRequest.of(0, 20));

        assertThat(results).isNotEmpty();

        // All matching freelancers must have at least one overlapping skill
        assertThat(results).allSatisfy(r -> {
            assertThat(r.getScore()).isBetween(BigDecimal.ZERO, BigDecimal.ONE);
            assertThat(r.getMatchedSkills()).isNotEmpty();
        });

        // Scores must be in descending order
        for (int i = 0; i < results.size() - 1; i++) {
            assertThat(results.get(i).getScore())
                    .isGreaterThanOrEqualTo(results.get(i + 1).getScore());
        }
    }

    @Test
    void testIdempotentConsumer_skipsDuplicate() {
        // Arrange: seed one freelancer with matching skills
        UUID jobId = UUID.randomUUID();
        List<String> requiredSkills = List.of("Python", "Django");

        freelancerSkillCacheRepository.save(
                freelancer(List.of("Python", "Django", "PostgreSQL"), new BigDecimal("35.00"), new BigDecimal("4.7"))
        );

        var event = jobCreatedEvent(jobId, requiredSkills, new BigDecimal("5000.00"));

        // Act: process the same event twice
        jobEventConsumer.handleJobCreated(event);
        jobEventConsumer.handleJobCreated(event);   // must be skipped (duplicate)

        // Assert: exactly one set of match results (not doubled)
        List<MatchResult> results = matchResultRepository.findByJobIdOrderByScoreDesc(
                jobId, PageRequest.of(0, 20));

        assertThat(results).hasSize(1);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private FreelancerSkillCache freelancer(List<String> skills, BigDecimal hourlyRate, BigDecimal rating) {
        return FreelancerSkillCache.builder()
                .userId(UUID.randomUUID())
                .skills(skills)
                .hourlyRate(hourlyRate)
                .averageRating(rating)
                .updatedAt(Instant.now())
                .build();
    }

    private JobCreatedEvent jobCreatedEvent(UUID jobId, List<String> skills, BigDecimal budget) {
        return new JobCreatedEvent(
                UUID.randomUUID(),
                "JobCreated",
                Instant.now(),
                jobId,
                UUID.randomUUID(),
                "Test Job",
                "Integration test job description.",
                skills,
                new Money(budget, "USD"),
                "OPEN"
        );
    }
}
