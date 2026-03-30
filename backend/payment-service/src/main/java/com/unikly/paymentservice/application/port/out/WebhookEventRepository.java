package com.unikly.paymentservice.application.port.out;

import com.unikly.paymentservice.domain.model.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, String> {
}
