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

    // Jackson for event serialization
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.core:jackson-annotations")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Spring Security for UserContext
    compileOnly("org.springframework.security:spring-security-core")
    compileOnly("org.springframework.security:spring-security-web")
    compileOnly("org.springframework:spring-web")
}
