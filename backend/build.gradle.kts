plugins {
    java
    id("org.springframework.boot") version "4.0.3" apply false
}

allprojects {
    group = "com.unikly"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    dependencies {
        "implementation"(platform("org.springframework.boot:spring-boot-dependencies:4.0.3"))
        "annotationProcessor"(platform("org.springframework.boot:spring-boot-dependencies:4.0.3"))
        "implementation"(platform("org.springframework.cloud:spring-cloud-dependencies:2024.0.1"))

        "compileOnly"("org.projectlombok:lombok")
        "annotationProcessor"("org.projectlombok:lombok")

        "implementation"("org.slf4j:slf4j-api")
        "implementation"("com.fasterxml.jackson.core:jackson-databind")
        "implementation"("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters"))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
