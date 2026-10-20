package com.unikly.store.identity.application;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class IdentityEmailConfiguration {
    @Bean
    Clock identityClock() {
        return Clock.systemUTC();
    }
}
