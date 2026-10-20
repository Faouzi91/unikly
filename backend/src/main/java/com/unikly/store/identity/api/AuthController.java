package com.unikly.store.identity.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import com.unikly.store.identity.application.IdentityAccountService;
import com.unikly.store.identity.application.IdentityAccountService.AuthenticatedUserView;
import com.unikly.store.identity.application.IdentityEmailFlowService;
import com.unikly.store.identity.application.IdentityRequestRateLimiter;
import org.hibernate.exception.ConstraintViolationException;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;
    private final IdentityAccountService identity;
    private final IdentityEmailFlowService emailFlows;
    private final IdentityRequestRateLimiter rateLimiter;
    private final SessionRegistry sessions;

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository,
            IdentityAccountService identity,
            IdentityEmailFlowService emailFlows,
            IdentityRequestRateLimiter rateLimiter,
            SessionRegistry sessions) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
        this.identity = identity;
        this.emailFlows = emailFlows;
        this.rateLimiter = rateLimiter;
        this.sessions = sessions;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody AuthRequests.Register request,
            HttpServletRequest servletRequest) {
        try {
            emailFlows.register(request.email(), request.password(), request.displayName(),
                    request.accountType().role(), servletRequest.getRemoteAddr());
        } catch (DataIntegrityViolationException exception) {
            if (!isEmailUniquenessConflict(exception)) throw exception;
        }
        return ResponseEntity.accepted().body(Map.of("message", emailFlows.genericResponse()));
    }

    private static boolean isEmailUniquenessConflict(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && "uk_store_users_email".equals(violation.getConstraintName())) return true;
            cause = cause.getCause();
        }
        return false;
    }

    @PostMapping("/login")
    public AuthenticatedUserView login(
            @Valid @RequestBody AuthRequests.Login request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (!rateLimiter.allow("login", servletRequest.getRemoteAddr(), normalizedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS, "Too many sign-in attempts. Please wait and try again.");
        }
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        normalizedEmail, request.password()));
        if (sessions.getAllSessions(authentication.getPrincipal(), false).size() >= 5) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many active sessions");
        }
        HttpSession existingSession = servletRequest.getSession(false);
        if (existingSession != null) {
            servletRequest.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        sessions.registerNewSession(servletRequest.getSession().getId(), authentication.getPrincipal());
        csrfTokenRepository.saveToken(null, servletRequest, servletResponse);
        return identity.requireAuthenticatedUser(authentication.getName());
    }

    @GetMapping("/me")
    public AuthenticatedUserView currentUser(Authentication authentication) {
        return identity.requireAuthenticatedUser(authentication.getName());
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            Authentication authentication,
            @Valid @RequestBody AuthRequests.ChangePassword request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        identity.changePassword(authentication.getName(), request.currentPassword(), request.newPassword());
        sessions.getAllSessions(authentication.getPrincipal(), false)
            .forEach(org.springframework.security.core.session.SessionInformation::expireNow);
        HttpSession session = servletRequest.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        csrfTokenRepository.saveToken(null, servletRequest, servletResponse);
    }
}
