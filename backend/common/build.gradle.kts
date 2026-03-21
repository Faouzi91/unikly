plugins {
    `java-library`
}

dependencies {
    // JPA for OutboxEvent entity
    api("org.springframework.boot:spring-boot-starter-data-jpa")

    // Kafka for outbox publisher
    api("org.springframework:spring-context")
    api("org.springframework.kafka:spring-kafka")

    // Validation
    api("jakarta.validation:jakarta.validation-api")

    // Metrics for outbox publisher
    api("io.micrometer:micrometer-core")

    // Jackson for event serialization
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.core:jackson-annotations")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Spring Security for UserContext and GlobalExceptionHandlerBase
    api("org.springframework.security:spring-security-core")
    api("org.springframework.security:spring-security-web")
    compileOnly("org.springframework.security:spring-security-oauth2-jose")
    compileOnly("org.springframework:spring-web")

    // Servlet API for HttpServletRequest
    compileOnly("jakarta.servlet:jakarta.servlet-api")
}
