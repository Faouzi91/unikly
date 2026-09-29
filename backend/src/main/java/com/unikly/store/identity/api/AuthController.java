package com.unikly.store.identity.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import com.unikly.store.identity.domain.StoreRole;
import com.unikly.store.identity.domain.StoreUser;
import com.unikly.store.identity.application.IdentityAccountService;
import com.unikly.store.identity.persistence.StoreUserRepository;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
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
    private final StoreUserRepository users;
    private final org.springframework.security.crypto.password.PasswordEncoder passwords;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;
    private final IdentityAccountService identity;
    private final SessionRegistry sessions;

    public AuthController(
            StoreUserRepository users,
            org.springframework.security.crypto.password.PasswordEncoder passwords,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository,
            IdentityAccountService identity,
            SessionRegistry sessions) {
        this.users = users;
        this.passwords = passwords;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
        this.identity = identity;
        this.sessions = sessions;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody AuthRequests.Register request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        StoreUser user = users.save(new StoreUser(
                email,
                passwords.encode(request.password()),
                request.displayName().trim(),
                request.accountType().role()));
        return ResponseEntity.status(HttpStatus.CREATED).body(publicUser(user));
    }

    @PostMapping("/login")
    public Map<String, Object> login(
            @Valid @RequestBody AuthRequests.Login request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.email().trim().toLowerCase(Locale.ROOT), request.password()));
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
        StoreUser user = users.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return publicUser(user);
    }

    @GetMapping("/me")
    public Map<String, Object> currentUser(Authentication authentication) {
        StoreUser user = users.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return publicUser(user);
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

    private Map<String, Object> publicUser(StoreUser user) {
        return Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "displayName", user.getDisplayName(),
                "role", user.getRole().name(),
                "permissions", user.getRole().permissions().stream()
                        .map(Enum::name)
                        .sorted()
                        .toList());
    }
}