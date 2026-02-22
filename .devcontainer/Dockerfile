# Development Container - Kotlin/JVM GraphQL Service
# Base: Eclipse Temurin Java 21 JDK on Alpine Linux
FROM eclipse-temurin:21-jdk-alpine

# Install essential development tools
RUN apk add --no-cache \
    bash \
    curl \
    git \
    wget \
    ca-certificates \
    unzip

# Install Gradle 8.4
ENV GRADLE_VERSION=8.4 \
    GRADLE_HOME=/opt/gradle \
    PATH=$PATH:/opt/gradle/bin

RUN wget -q https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip && \
    unzip -q gradle-${GRADLE_VERSION}-bin.zip -d /opt && \
    mv /opt/gradle-${GRADLE_VERSION} /opt/gradle && \
    rm gradle-${GRADLE_VERSION}-bin.zip && \
    chmod +x /opt/gradle/bin/gradle

# Create non-root vscode user for development
RUN addgroup -S vscode && \
    adduser -S -G vscode -s /bin/bash vscode && \
    mkdir -p /home/vscode && \
    mkdir -p /workspace && \
    chown -R vscode:vscode /home/vscode /workspace /opt/gradle

# Set gradle user home for vscode user
ENV GRADLE_USER_HOME=/home/vscode/.gradle

# Switch to vscode user
USER vscode
WORKDIR /workspace
