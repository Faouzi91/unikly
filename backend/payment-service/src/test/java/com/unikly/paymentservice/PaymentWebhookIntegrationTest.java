package com.unikly.paymentservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.unikly.paymentservice.api.dto.CreatePaymentRequest;
import com.unikly.paymentservice.domain.Payment;
import com.unikly.paymentservice.domain.PaymentStatus;
import com.unikly.paymentservice.infrastructure.LedgerEntryRepository;
import com.unikly.paymentservice.infrastructure.PaymentRepository;
import com.unikly.paymentservice.infrastructure.StripeClient;
import com.unikly.paymentservice.infrastructure.WebhookEventRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class PaymentWebhookIntegrationTest {

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

        @Bean(name = "filterChain")
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @MockitoBean
    StripeClient stripeClient;

    @Autowired
    WebApplicationContext wac;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    WebhookEventRepository webhookEventRepository;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        ledgerEntryRepository.deleteAll();
        webhookEventRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    void testCreatePaymentIntent_success() throws Exception {
        // Arrange: mock Stripe SDK
        PaymentIntent mockPi = mock(PaymentIntent.class);
        when(mockPi.getId()).thenReturn("pi_test_create_001");
        when(mockPi.getClientSecret()).thenReturn("pi_test_create_001_secret");
        when(stripeClient.createPaymentIntent(anyLong(), anyString(), any(), anyString()))
                .thenReturn(mockPi);

        UUID jobId = UUID.randomUUID();
        UUID freelancerId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        var request = new CreatePaymentRequest(
                jobId,
                freelancerId,
                new BigDecimal("1500.00"),
                "USD",
                "idempotency-key-test-" + UUID.randomUUID()
        );

        // Act
        String responseBody = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", clientId.toString())
                        .header("X-User-Roles", "ROLE_CLIENT")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.clientSecret").value("pi_test_create_001_secret"))
                .andReturn().getResponse().getContentAsString();

        // Assert: payment saved with PENDING status
        UUID paymentId = UUID.fromString(
                objectMapper.readValue(responseBody, JsonNode.class).get("paymentId").asText());

        Payment saved = paymentRepository.findById(paymentId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getStripePaymentIntentId()).isEqualTo("pi_test_create_001");

        // Assert: Stripe was called once
        verify(stripeClient, times(1)).createPaymentIntent(anyLong(), anyString(), any(), anyString());
    }

    @Test
    void testWebhookProcessing_updatesToFunded() throws Exception {
        // Arrange: seed a PENDING payment
        String piId = "pi_test_webhook_001";
        Payment payment = Payment.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("1000.00"), "USD",
                "idem-key-webhook-" + UUID.randomUUID(), piId
        );
        paymentRepository.save(payment);

        // Mock constructWebhookEvent to return a payment_intent.succeeded event
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getId()).thenReturn(piId);

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(pi));

        Event stripeEvent = mock(Event.class);
        when(stripeEvent.getId()).thenReturn("evt_test_001");
        when(stripeEvent.getType()).thenReturn("payment_intent.succeeded");
        when(stripeEvent.getDataObjectDeserializer()).thenReturn(deserializer);

        when(stripeClient.constructWebhookEvent(anyString(), anyString())).thenReturn(stripeEvent);

        // Act
        mockMvc.perform(post("/webhooks/stripe")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Stripe-Signature", "t=test,v1=test")
                        .content("{\"type\":\"payment_intent.succeeded\"}"))
                .andExpect(status().isOk());

        // Assert: payment transitioned to FUNDED
        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.FUNDED);

        // Assert: ledger entry created
        var ledgerEntries = ledgerEntryRepository.findByPaymentId(payment.getId());
        assertThat(ledgerEntries).hasSize(1);
    }

    @Test
    void testWebhookIdempotency_processesOnce() throws Exception {
        // Arrange: seed a PENDING payment
        String piId = "pi_test_idempotent_001";
        Payment payment = Payment.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("800.00"), "USD",
                "idem-key-idem-" + UUID.randomUUID(), piId
        );
        paymentRepository.save(payment);

        // Mock Stripe event
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getId()).thenReturn(piId);

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(pi));

        Event stripeEvent = mock(Event.class);
        when(stripeEvent.getId()).thenReturn("evt_idempotent_001");
        when(stripeEvent.getType()).thenReturn("payment_intent.succeeded");
        when(stripeEvent.getDataObjectDeserializer()).thenReturn(deserializer);

        when(stripeClient.constructWebhookEvent(anyString(), anyString())).thenReturn(stripeEvent);

        String payload = "{\"type\":\"payment_intent.succeeded\"}";

        // Act: call webhook twice with the same event
        mockMvc.perform(post("/webhooks/stripe")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Stripe-Signature", "t=test,v1=test")
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/webhooks/stripe")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Stripe-Signature", "t=test,v1=test")
                        .content(payload))
                .andExpect(status().isOk());

        // Assert: only one ledger entry — idempotency guard prevented duplicate processing
        var ledgerEntries = ledgerEntryRepository.findByPaymentId(payment.getId());
        assertThat(ledgerEntries).hasSize(1);
    }
}
