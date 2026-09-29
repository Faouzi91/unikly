# syntax=docker/dockerfile:1.7
FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,id=unikly-maven,target=/root/.m2 \
    mvn -B -ntp \
      -Daether.transport.http.requestTimeout=180000 \
      -Daether.transport.http.retryHandler.count=5 \
      -DskipTests package

FROM eclipse-temurin:26.0.2.1_1-jre
WORKDIR /app
RUN useradd --system --uid 10001 appuser
COPY --from=build /workspace/target/unikly-api-0.0.1-SNAPSHOT.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]