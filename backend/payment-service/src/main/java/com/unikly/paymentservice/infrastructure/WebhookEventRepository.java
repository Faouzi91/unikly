package com.unikly.paymentservice.infrastructure;

import com.unikly.paymentservice.domain.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, String> {
}
