package com.unikly.jobservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.unikly.jobservice", "com.unikly.common"})
@EntityScan(basePackages = {"com.unikly.jobservice", "com.unikly.common"})
@EnableJpaRepositories(basePackages = {"com.unikly.jobservice", "com.unikly.common"})
@EnableScheduling
public class JobServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobServiceApplication.class, args);
    }
}
