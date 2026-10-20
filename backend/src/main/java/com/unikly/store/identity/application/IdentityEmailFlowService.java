package com.unikly.store.identity.application;

import com.unikly.store.identity.domain.IdentityEmailPurpose;
import com.unikly.store.identity.domain.IdentityEmailToken;
import com.unikly.store.identity.domain.StoreRole;
import com.unikly.store.identity.domain.StoreUser;
import com.unikly.store.identity.persistence.IdentityEmailTokenRepository;
import com.unikly.store.identity.persistence.StoreUserRepository;
import com.unikly.store.identity.security.StoreUserDetailsService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class IdentityEmailFlowService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentityEmailFlowService.class);
    private static final Duration VERIFICATION_LIFETIME = Duration.ofHours(24);
    private static final Duration PASSWORD_RESET_LIFETIME = Duration.ofMinutes(30);
    private static final String GENERIC_RESPONSE = "If this address can receive email, instructions will be sent.";

    private final StoreUserRepository users;
    private final IdentityEmailTokenRepository tokens;
    private final IdentityEmailDelivery emailDelivery;
    private final IdentityRequestRateLimiter rateLimiter;
    private final PasswordEncoder passwords;
    private final StoreUserDetailsService userDetailsService;
    private final SessionRegistry sessions;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final String publicBaseUrl;

    public IdentityEmailFlowService(
            StoreUserRepository users,
            IdentityEmailTokenRepository tokens,
            IdentityEmailDelivery emailDelivery,
            IdentityRequestRateLimiter rateLimiter,
            PasswordEncoder passwords,
            StoreUserDetailsService userDetailsService,
            SessionRegistry sessions,
            Clock clock,
            @Value("${unikly.email.public-base-url}") String publicBaseUrl) {
        this.users = users;
        this.tokens = tokens;
        this.emailDelivery = emailDelivery;
        this.rateLimiter = rateLimiter;
        this.passwords = passwords;
        this.userDetailsService = userDetailsService;
        this.sessions = sessions;
        this.clock = clock;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    public String genericResponse() {
        return GENERIC_RESPONSE;
    }

    public void register(String email, String password, String displayName, StoreRole role, String clientAddress) {
        String normalizedEmail = normalize(email);
        requireRateLimit("verification", clientAddress, normalizedEmail);
        String encodedPassword = passwords.encode(password);
        String cleanDisplayName = displayName.trim();

        Optional<StoreUser> existing = users.findByEmail(normalizedEmail);
        if (existing.isPresent()) {
            StoreUser user = existing.get();
            if (!user.isEmailVerified()) {
                user.updatePasswordHash(encodedPassword);
                user.updateDisplayName(cleanDisplayName);
                user.updateRole(role);
                sendVerification(user);
            }
            return;
        }

        StoreUser user = users.save(StoreUser.unverified(
                normalizedEmail, encodedPassword, cleanDisplayName, role));
        sendVerification(user);
    }

    public void requestVerification(String email, String clientAddress) {
        String normalizedEmail = normalize(email);
        requireRateLimit("verification", clientAddress, normalizedEmail);
        users.findByEmail(normalizedEmail)
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::sendVerification);
    }

    public void requestPasswordReset(String email, String clientAddress) {
        String normalizedEmail = normalize(email);
        requireRateLimit("recovery", clientAddress, normalizedEmail);
        users.findByEmail(normalizedEmail)
                .filter(StoreUser::isEmailVerified)
                .ifPresent(this::sendPasswordReset);
    }

    private void requireRateLimit(String action, String clientAddress, String normalizedEmail) {
        if (!rateLimiter.allow(action, clientAddress, normalizedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please wait and try again.");
        }
    }

    public boolean confirmEmail(String rawToken) {
        Instant now = clock.instant();
        Optional<IdentityEmailToken> found = tokens.findForUpdate(hashToken(rawToken), IdentityEmailPurpose.VERIFY_EMAIL);
        if (found.isEmpty() || !found.get().isUsableAt(now)) return false;

        IdentityEmailToken token = found.get();
        Optional<StoreUser> foundUser = users.findById(token.getUserId());
        if (foundUser.isEmpty() || foundUser.get().isEmailVerified()) return false;

        foundUser.get().verifyEmail();
        token.consume(now);
        tokens.deleteByUserAndPurpose(token.getUserId(), IdentityEmailPurpose.VERIFY_EMAIL);
        return true;
    }

    public boolean resetPassword(String rawToken, String newPassword) {
        Instant now = clock.instant();
        Optional<IdentityEmailToken> found = tokens.findForUpdate(hashToken(rawToken), IdentityEmailPurpose.RESET_PASSWORD);
        if (found.isEmpty() || !found.get().isUsableAt(now)) return false;

        IdentityEmailToken token = found.get();
        Optional<StoreUser> foundUser = users.findById(token.getUserId());
        if (foundUser.isEmpty() || !foundUser.get().isEmailVerified()) return false;

        StoreUser user = foundUser.get();
        user.updatePasswordHash(passwords.encode(newPassword));
        token.consume(now);
        tokens.deleteByUserAndPurpose(user.getId(), IdentityEmailPurpose.RESET_PASSWORD);
        revokeSessions(user.getEmail());
        return true;
    }

    private void sendVerification(StoreUser user) {
        String token = issueToken(user, IdentityEmailPurpose.VERIFY_EMAIL, VERIFICATION_LIFETIME);
        String link = publicBaseUrl + "/verify-email#token=" + token;
        deliver(new IdentityEmailMessage(user.getEmail(), "Verify your Unikly email",
                "Confirm your email address to finish setting up your Unikly account.\n\n"
                        + link + "\n\nThis link expires in 24 hours. If you did not request this, you can ignore this message."));
    }

    private void sendPasswordReset(StoreUser user) {
        String token = issueToken(user, IdentityEmailPurpose.RESET_PASSWORD, PASSWORD_RESET_LIFETIME);
        String link = publicBaseUrl + "/reset-password#token=" + token;
        deliver(new IdentityEmailMessage(user.getEmail(), "Reset your Unikly password",
                "Use this link to choose a new password for your Unikly account.\n\n"
                        + link + "\n\nThis link expires in 30 minutes. If you did not request this, you can ignore this message."));
    }

    private String issueToken(StoreUser user, IdentityEmailPurpose purpose, Duration lifetime) {
        tokens.deleteByUserAndPurpose(user.getId(), purpose);
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        tokens.save(new IdentityEmailToken(user.getId(), hashToken(rawToken), purpose, now, now.plus(lifetime)));
        return rawToken;
    }

    private void revokeSessions(String email) {
        UserDetails principal = userDetailsService.loadUserByUsername(email);
        sessions.getAllSessions(principal, false)
                .forEach(org.springframework.security.core.session.SessionInformation::expireNow);
    }

    private void deliver(IdentityEmailMessage message) {
        try {
            emailDelivery.send(message);
        } catch (MailException exception) {
            LOGGER.warn("Account email could not be delivered; the user can request a new link.");
        }
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
