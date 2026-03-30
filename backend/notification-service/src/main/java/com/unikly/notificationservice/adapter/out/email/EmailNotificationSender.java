package com.unikly.notificationservice.application.port.out;
import com.unikly.notificationservice.application.port.out.NotificationPreferenceRepository;

import com.unikly.notificationservice.domain.model.Notification;
import com.unikly.notificationservice.domain.model.NotificationPreference;
import com.unikly.notificationservice.application.port.out.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Sends email notifications when a user's preference allows it.
 * <p>
 * Defaults to Mailhog (port 1025) in dev — no auth needed.
 * Override MAIL_HOST/MAIL_PORT/MAIL_USERNAME/MAIL_PASSWORD env vars for production SMTP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationSender {

    private final JavaMailSender mailSender;
    private final NotificationPreferenceRepository preferenceRepository;

    @Value("${spring.mail.from:noreply@unikly.com}")
    private String fromAddress;

    /**
     * Attempts to send an email for the given notification.
     * Silently skips if the user has disabled email notifications or has no email on file.
     */
    @Async
    public void sendIfEnabled(Notification notification, String email) {
        if (email == null || email.isBlank()) {
            log.debug("No email address for userId={}, skipping email notification", notification.getUserId());
            return;
        }

        Optional<NotificationPreference> pref = preferenceRepository.findById(notification.getUserId());
        if (pref.isPresent() && !pref.get().isEmailEnabled()) {
            log.debug("Email notifications disabled for user: {}", notification.getUserId());
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject(getSubjectForType(notification.getType().name()));
        message.setText(notification.getBody());

        try {
            mailSender.send(message);
            log.info("Sent email notification to {}", email);
        } catch (org.springframework.mail.MailException e) {
            log.error("Failed to send email to {}", email, e);
        }
    }

    private String getSubjectForType(String type) {
        return switch (type) {
            case "JOB_CREATED" -> "Unikly — New job matches your profile";
            case "PROPOSAL_RECEIVED" -> "Unikly — You received a new proposal";
            case "PROPOSAL_ACCEPTED" -> "Unikly — Congrats, your proposal was accepted!";
            case "PAYMENT_RECEIVED" -> "Unikly — Payment received";
            case "REVIEW_RECEIVED" -> "Unikly — You received a new review";
            case "FREELANCER_INVITED" -> "Unikly — You've been invited to a job";
            case "MESSAGE_RECEIVED" -> "Unikly — New message";
            default -> "Unikly — New Notification";
        };
    }
}
