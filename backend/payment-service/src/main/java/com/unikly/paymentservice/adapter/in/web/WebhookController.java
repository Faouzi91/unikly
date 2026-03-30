package com.unikly.paymentservice.adapter.in.web;

import com.unikly.paymentservice.application.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final PaymentService paymentService;

    /**
     * Stripe webhook endpoint — NO JWT authentication.
     * Security is provided solely by Stripe HMAC signature verification.
     */
    @PostMapping("/stripe")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        paymentService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }
}
