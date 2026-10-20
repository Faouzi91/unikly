package com.unikly.store.identity.api;

import com.unikly.store.identity.application.IdentityEmailFlowService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class IdentityEmailController {
    private final IdentityEmailFlowService emailFlows;

    public IdentityEmailController(IdentityEmailFlowService emailFlows) {
        this.emailFlows = emailFlows;
    }

    @PostMapping("/verification/request")
    public ResponseEntity<Map<String, String>> requestVerification(
            @Valid @RequestBody IdentityEmailRequests.EmailAddress request,
            HttpServletRequest servletRequest) {
        emailFlows.requestVerification(request.email(), servletRequest.getRemoteAddr());
        return genericAccepted();
    }

    @PostMapping("/verification/confirm")
    public ResponseEntity<Map<String, String>> confirmEmail(
            @Valid @RequestBody IdentityEmailRequests.Token request) {
        if (!emailFlows.confirmEmail(request.token())) {
            return ResponseEntity.badRequest().body(Map.of("message", "This link is invalid or has expired."));
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-recovery/request")
    public ResponseEntity<Map<String, String>> requestPasswordReset(
            @Valid @RequestBody IdentityEmailRequests.EmailAddress request,
            HttpServletRequest servletRequest) {
        emailFlows.requestPasswordReset(request.email(), servletRequest.getRemoteAddr());
        return genericAccepted();
    }

    @PostMapping("/password-recovery/confirm")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody IdentityEmailRequests.PasswordReset request) {
        if (!emailFlows.resetPassword(request.token(), request.newPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "This link is invalid or has expired."));
        }
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Map<String, String>> genericAccepted() {
        return ResponseEntity.accepted().body(Map.of("message", emailFlows.genericResponse()));
    }
}
