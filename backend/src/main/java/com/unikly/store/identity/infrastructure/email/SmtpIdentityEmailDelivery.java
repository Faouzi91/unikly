package com.unikly.store.identity.infrastructure.email;

import com.unikly.store.identity.application.IdentityEmailDelivery;
import com.unikly.store.identity.application.IdentityEmailMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpIdentityEmailDelivery implements IdentityEmailDelivery {
    private final JavaMailSender mailSender;
    private final String senderAddress;

    public SmtpIdentityEmailDelivery(
            JavaMailSender mailSender,
            @Value("${unikly.email.from}") String senderAddress) {
        this.mailSender = mailSender;
        this.senderAddress = senderAddress;
    }

    @Override
    public void send(IdentityEmailMessage message) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(senderAddress);
        email.setTo(message.recipient());
        email.setSubject(message.subject());
        email.setText(message.text());
        mailSender.send(email);
    }
}
