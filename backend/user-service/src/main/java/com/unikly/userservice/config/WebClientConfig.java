package com.unikly.userservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({KeycloakAdminProperties.class, KeycloakAuthProperties.class})
public class WebClientConfig {

    @Bean
    public RestClient keycloakRestClient() {
        return RestClient.builder().build();
    }
}
