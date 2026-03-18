package com.unikly.paymentservice.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.unikly.common.dto.Money;
import com.unikly.common.events.EscrowReleasedEvent;
import com.unikly.common.events.PaymentCompletedEvent;
import com.unikly.common.events.PaymentInitiatedEvent;
import com.unikly.common.outbox.OutboxEvent;
import com.unikly.common.outbox.OutboxEventRepository;
import com.unikly.paymentservice.application.exception.DuplicateIdempotencyKeyException;
import com.unikly.paymentservice.application.exception.InvalidPaymentStateException;
import com.unikly.paymentservice.application.exception.PaymentAccessDeniedException;
import com.unikly.paymentservice.application.exception.PaymentNotFoundException;
import com.unikly.paymentservice.domain.LedgerEntry;
import com.unikly.paymentservice.domain.LedgerEntryType;
import com.unikly.paymentservice.domain.Payment;
import com.unikly.paymentservice.domain.PaymentStatus;
import com.unikly.paymentservice.domain.WebhookEvent;
import com.unikly.paymentservice.infrastructure.LedgerEntryRepository;
import com.unikly.paymentservice.infrastructure.PaymentRepository;
import com.unikly.paymentservice.infrastructure.StripeClient;
import com.unikly.paymentservice.infrastructure.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final StripeClient stripeClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentIntentResult createPaymentIntent(UUID jobId, UUID clientId, UUID freelancerId,
                                                    BigDecimal amount, String currency,
                                                    String idempotencyKey) {
        if (paymentRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new DuplicateIdempotencyKeyException(idempotencyKey);
        }

        long amountInCents = amount.movePointRight(2).longValueExact();
        PaymentIntent pi = stripeClient.createPaymentIntent(amountInCents, currency, jobId, idempotencyKey);

        Payment payment = Payment.create(jobId, clientId, freelancerId, amount, currency,
                idempotencyKey, pi.getId());
        paymentRepository.save(payment);

        publishEvent(new PaymentInitiatedEvent(
                UUID.randomUUID(), "PaymentInitiated", Instant.now(),
                payment.getId(), jobId, clientId,
                new Money(amount, currency), pi.getId()
        ));

        return new PaymentIntentResult(payment.getId(), pi.getClientSecret());
    }

    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        Event event = stripeClient.constructWebhookEvent(payload, sigHeader);

        if (webhookEventRepository.existsById(event.getId())) {
            log.info("Skipping already-processed Stripe event: {}", event.getId());
            return;
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" -> handlePaymentSucceeded(event);
            case "payment_intent.payment_failed" -> handlePaymentFailed(event);
            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }

        webhookEventRepository.save(WebhookEvent.of(event.getId(), event.getType()));
    }

    private void handlePaymentSucceeded(Event event) {
        String piId = extractPaymentIntentId(event);
        Payment payment = paymentRepository.findByStripePaymentIntentId(piId)
                .orElseThrow(() -> new PaymentNotFoundException(null));

        payment.transitionTo(PaymentStatus.FUNDED);
        ledgerEntryRepository.save(LedgerEntry.of(
                payment.getId(), LedgerEntryType.ESCROW_FUND,
                payment.getAmount(), payment.getCurrency(),
                "Escrow funded via Stripe PaymentIntent " + piId
        ));

        publishEvent(new PaymentCompletedEvent(
                UUID.randomUUID(), "PaymentCompleted", Instant.now(),
                payment.getId(), payment.getJobId(), piId
        ));
        log.info("Payment {} transitioned to FUNDED", payment.getId());
    }

    private void handlePaymentFailed(Event event) {
        String piId = extractPaymentIntentId(event);
        paymentRepository.findByStripePaymentIntentId(piId).ifPresent(payment -> {
            payment.transitionTo(PaymentStatus.FAILED);
            log.warn("Payment {} transitioned to FAILED", payment.getId());
        });
    }

    @Transactional
    public void releaseEscrow(UUID paymentId, UUID clientId) {
        Payment payment = findAndVerifyOwner(paymentId, clientId);

        if (payment.getStatus() != PaymentStatus.FUNDED) {
            throw new InvalidPaymentStateException(
                    "Can only release escrow from FUNDED state, current: " + payment.getStatus());
        }

        // TODO: Call Stripe Transfer.create once freelancer Stripe accounts are onboarded
        payment.transitionTo(PaymentStatus.RELEASED);
        payment.transitionTo(PaymentStatus.COMPLETED);

        ledgerEntryRepository.save(LedgerEntry.of(
                payment.getId(), LedgerEntryType.ESCROW_RELEASE,
                payment.getAmount(), payment.getCurrency(),
                "Escrow released to freelancer " + payment.getFreelancerId()
        ));

        publishEvent(new EscrowReleasedEvent(
                UUID.randomUUID(), "EscrowReleased", Instant.now(),
                payment.getId(), payment.getJobId(), payment.getFreelancerId(),
                new Money(payment.getAmount(), payment.getCurrency())
        ));
        log.info("Escrow released for payment {}", paymentId);
    }

    @Transactional
    public void requestRefund(UUID paymentId, UUID clientId) {
        Payment payment = findAndVerifyOwner(paymentId, clientId);

        if (payment.getStatus() != PaymentStatus.FUNDED) {
            throw new InvalidPaymentStateException(
                    "Can only refund from FUNDED state, current: " + payment.getStatus());
        }

        stripeClient.createRefund(payment.getStripePaymentIntentId());

        payment.transitionTo(PaymentStatus.REFUNDED);
        ledgerEntryRepository.save(LedgerEntry.of(
                payment.getId(), LedgerEntryType.REFUND,
                payment.getAmount(), payment.getCurrency(),
                "Refund issued for PaymentIntent " + payment.getStripePaymentIntentId()
        ));
        log.info("Refund issued for payment {}", paymentId);
    }

    public List<Payment> getPaymentsByJob(UUID jobId) {
        return paymentRepository.findByJobId(jobId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Payment findAndVerifyOwner(UUID paymentId, UUID clientId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        if (!payment.getClientId().equals(clientId)) {
            throw new PaymentAccessDeniedException();
        }
        return payment;
    }

    private String extractPaymentIntentId(Event event) {
        return event.getDataObjectDeserializer()
                .getObject()
                .map(obj -> ((PaymentIntent) obj).getId())
                .orElseThrow(() -> new IllegalStateException("Cannot deserialize Stripe event data"));
    }

    private void publishEvent(Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String eventType = event.getClass().getSimpleName();
            outboxEventRepository.save(new OutboxEvent(eventType, payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event", e);
        }
    }

    public record PaymentIntentResult(UUID paymentId, String clientSecret) {}
}
