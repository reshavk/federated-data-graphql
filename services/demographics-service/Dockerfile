# Multi-stage build
# Stage 1: Build
FROM gradle:8.4-jdk21-alpine AS builder

WORKDIR /app
COPY services/demographics-service .

RUN gradle clean build --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
