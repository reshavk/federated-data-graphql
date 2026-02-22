# Stage 1: Build
FROM gradle:8.4-jdk21 AS builder

WORKDIR /app

COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src
COPY gradle ./gradle

RUN gradle fatJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/graphql-countries-service-1.0.0.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

ENV PORT=8080
ENV REST_COUNTRIES_URL=https://restcountries.com/v3.1

ENTRYPOINT ["java", "-jar", "app.jar"]
