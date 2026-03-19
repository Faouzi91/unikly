package com.unikly.paymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.unikly.paymentservice", "com.unikly.common"})
@EntityScan(basePackages = {"com.unikly.paymentservice", "com.unikly.common"})
@EnableJpaRepositories(basePackages = {"com.unikly.paymentservice", "com.unikly.common"})
@EnableScheduling
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
