package com.unikly.paymentservice.api;

import com.unikly.common.security.UserContext;
import com.unikly.paymentservice.api.dto.CreatePaymentRequest;
import com.unikly.paymentservice.api.dto.CreatePaymentResponse;
import com.unikly.paymentservice.api.dto.PaymentResponse;
import com.unikly.paymentservice.application.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatePaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        UUID clientId = UserContext.getUserId();
        var result = paymentService.createPaymentIntent(
                request.jobId(), clientId, request.freelancerId(),
                request.amount(), request.currency(), request.idempotencyKey()
        );
        return new CreatePaymentResponse(result.paymentId(), result.clientSecret());
    }

    @GetMapping
    public List<PaymentResponse> getPaymentsByJob(@RequestParam UUID jobId) {
        return paymentService.getPaymentsByJob(jobId).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @PostMapping("/{id}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void releaseEscrow(@PathVariable UUID id) {
        UUID clientId = UserContext.getUserId();
        paymentService.releaseEscrow(id, clientId);
    }

    @PostMapping("/{id}/refund")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestRefund(@PathVariable UUID id) {
        UUID clientId = UserContext.getUserId();
        paymentService.requestRefund(id, clientId);
    }
}
