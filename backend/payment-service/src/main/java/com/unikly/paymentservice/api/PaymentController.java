package com.unikly.paymentservice.api;

import com.unikly.common.security.UserContext;
import com.unikly.paymentservice.api.dto.CreatePaymentRequest;
import com.unikly.paymentservice.api.dto.CreatePaymentResponse;
import com.unikly.paymentservice.api.dto.PaymentResponse;
import com.unikly.paymentservice.application.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Payments", description = "Escrow payment lifecycle with Stripe")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a payment intent", description = "Initiates an escrow payment for a job contract via Stripe")
    @ApiResponse(responseCode = "201", description = "Payment intent created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    public CreatePaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        UUID clientId = UserContext.getUserId();
        var result = paymentService.createPaymentIntent(
                request.jobId(), clientId, request.freelancerId(),
                request.amount(), request.currency(), request.idempotencyKey()
        );
        return new CreatePaymentResponse(result.paymentId(), result.clientSecret());
    }

    @GetMapping
    @Operation(summary = "Get payments by job", description = "Returns all payments associated with a job")
    @ApiResponse(responseCode = "200", description = "Payments retrieved")
    public List<PaymentResponse> getPaymentsByJob(
            @Parameter(description = "Job UUID") @RequestParam UUID jobId) {
        return paymentService.getPaymentsByJob(jobId).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @PostMapping("/{id}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Release escrow", description = "Releases held funds to the freelancer after work completion")
    @ApiResponse(responseCode = "204", description = "Escrow released")
    @ApiResponse(responseCode = "403", description = "Forbidden — not the contract client")
    @ApiResponse(responseCode = "404", description = "Payment not found")
    public void releaseEscrow(
            @Parameter(description = "Payment UUID") @PathVariable UUID id) {
        UUID clientId = UserContext.getUserId();
        paymentService.releaseEscrow(id, clientId);
    }

    @PostMapping("/{id}/refund")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Request a refund", description = "Refunds escrowed funds back to the client")
    @ApiResponse(responseCode = "204", description = "Refund initiated")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Payment not found")
    public void requestRefund(
            @Parameter(description = "Payment UUID") @PathVariable UUID id) {
        UUID clientId = UserContext.getUserId();
        paymentService.requestRefund(id, clientId);
    }
}
