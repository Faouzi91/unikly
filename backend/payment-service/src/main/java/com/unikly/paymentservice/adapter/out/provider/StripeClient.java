package com.unikly.paymentservice.adapter.out.provider;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.Webhook;
import com.unikly.paymentservice.domain.exception.PaymentProviderUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class StripeClient {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @CircuitBreaker(name = "stripe", fallbackMethod = "createPaymentIntentFallback")
    @Retry(name = "stripe")
    public PaymentIntent createPaymentIntent(long amountInCents, String currency,
                                              UUID jobId, String idempotencyKey) {
        try {
            com.stripe.Stripe.apiKey = secretKey;
            Map<String, Object> params = new HashMap<>();
            params.put("amount", amountInCents);
            params.put("currency", currency.toLowerCase());
            params.put("metadata", Map.of("jobId", jobId.toString()));
            params.put("automatic_payment_methods", Map.of("enabled", true));

            com.stripe.net.RequestOptions options = com.stripe.net.RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey)
                    .setReadTimeout(5000)
                    .setConnectTimeout(5000)
                    .build();

            return PaymentIntent.create(params, options);
        } catch (StripeException e) {
            log.error("Stripe createPaymentIntent failed: {}", e.getMessage());
            throw new PaymentProviderUnavailableException("Stripe error: " + e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private PaymentIntent createPaymentIntentFallback(long amountInCents, String currency,
                                                       UUID jobId, String idempotencyKey,
                                                       Throwable t) {
        log.error("Circuit breaker open for createPaymentIntent", t);
        throw new PaymentProviderUnavailableException("Stripe temporarily unavailable");
    }

    /**
     * Retrieves the current status of a PaymentIntent from Stripe.
     */
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) {
        try {
            com.stripe.Stripe.apiKey = secretKey;
            return PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException e) {
            log.error("Stripe retrievePaymentIntent failed: {}", e.getMessage());
            throw new PaymentProviderUnavailableException("Stripe error: " + e.getMessage());
        }
    }

    @CircuitBreaker(name = "stripe", fallbackMethod = "createRefundFallback")
    @Retry(name = "stripe")
    public Refund createRefund(String paymentIntentId) {
        try {
            com.stripe.Stripe.apiKey = secretKey;
            Map<String, Object> params = new HashMap<>();
            params.put("payment_intent", paymentIntentId);
            return Refund.create(params);
        } catch (StripeException e) {
            log.error("Stripe createRefund failed: {}", e.getMessage());
            throw new PaymentProviderUnavailableException("Stripe error: " + e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private Refund createRefundFallback(String paymentIntentId, Throwable t) {
        log.error("Circuit breaker open for createRefund", t);
        throw new PaymentProviderUnavailableException("Stripe temporarily unavailable");
    }

    /**
     * Constructs and verifies a Stripe webhook event from the raw payload and signature header.
     * This method is NOT wrapped in Resilience4j — signature verification is a local operation.
     */
    public Event constructWebhookEvent(String payload, String sigHeader) {
        try {
            return Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid Stripe webhook signature");
        }
    }
}
