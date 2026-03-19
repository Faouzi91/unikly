package com.unikly.common.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Registers a Jackson 2.x ObjectMapper bean for backward-compatibility with services
 * that use com.fasterxml.jackson.databind.ObjectMapper (Spring Boot 4 auto-configures
 * Jackson 3.x tools.jackson.databind.json.JsonMapper by default).
 */
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
public class Jackson2CompatAutoConfiguration {

    @Bean("jackson2ObjectMapper")
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
