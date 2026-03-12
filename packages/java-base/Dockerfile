# =============================================================================
# Report Platform - Java Microservice Base Dockerfile
# Multi-stage build: Maven build -> JRE 21 slim runtime
# =============================================================================
# Usage:
#   docker build --build-arg SERVICE_MODULE=my-service -t my-service:latest .
#   docker build --build-arg SERVICE_MODULE=my-service --build-arg OTEL_AGENT=true -t my-service:latest .
# =============================================================================

# ---------------------------------------------------------------------------
# Stage 1: Build with Maven
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS builder

ARG SERVICE_MODULE
ARG MAVEN_OPTS="-Xmx1024m"

WORKDIR /build

# Cache Maven dependencies first
COPY pom.xml ./
COPY java-base-security/pom.xml ./java-base-security/
COPY java-base-grpc/pom.xml ./java-base-grpc/
COPY java-base-dapr/pom.xml ./java-base-dapr/
COPY java-base-observability/pom.xml ./java-base-observability/

# Copy Maven wrapper if present, otherwise use system Maven
COPY .mvn/ ./.mvn/ 2>/dev/null || true
COPY mvnw* ./  2>/dev/null || true

RUN --mount=type=cache,target=/root/.m2/repository \
    if [ -f mvnw ]; then chmod +x mvnw && ./mvnw dependency:go-offline -B; \
    else apt-get update && apt-get install -y maven && mvn dependency:go-offline -B; fi

# Copy source and build
COPY . .

RUN --mount=type=cache,target=/root/.m2/repository \
    if [ -f mvnw ]; then ./mvnw clean package -pl ${SERVICE_MODULE} -am -DskipTests -B; \
    else mvn clean package -pl ${SERVICE_MODULE} -am -DskipTests -B; fi

# ---------------------------------------------------------------------------
# Stage 2: Download OpenTelemetry Java Agent (optional)
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS otel-downloader

ARG OTEL_AGENT_VERSION=2.9.0
ARG OTEL_AGENT=true

RUN if [ "${OTEL_AGENT}" = "true" ]; then \
        curl -fsSL -o /tmp/opentelemetry-javaagent.jar \
        "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar"; \
    else \
        touch /tmp/opentelemetry-javaagent.jar; \
    fi

# ---------------------------------------------------------------------------
# Stage 3: Runtime
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime

ARG SERVICE_MODULE
ARG OTEL_AGENT=true

LABEL maintainer="Report Platform Team"
LABEL org.opencontainers.image.source="https://github.com/reportplatform"
LABEL org.opencontainers.image.description="Report Platform Java Microservice"

# Security: run as non-root
RUN groupadd --gid 1001 appgroup && \
    useradd --uid 1001 --gid appgroup --shell /bin/false --create-home appuser

WORKDIR /app

# Copy the built JAR
COPY --from=builder /build/${SERVICE_MODULE}/target/*.jar app.jar

# Copy OpenTelemetry agent
COPY --from=otel-downloader /tmp/opentelemetry-javaagent.jar /app/otel-agent.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Switch to non-root user
USER appuser

# JVM configuration for containers and virtual threads
ENV JAVA_OPTS="\
    -XX:+UseZGC \
    -XX:+ZGenerational \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseContainerSupport \
    -Djdk.virtualThreadScheduler.parallelism=0 \
    -Djdk.virtualThreadScheduler.maxPoolSize=256 \
    --enable-preview"

# Dapr sidecar ports
EXPOSE 8080 3500 50001

# Entrypoint with conditional OpenTelemetry agent
ENTRYPOINT ["sh", "-c", "\
    AGENT_FLAG=''; \
    if [ -s /app/otel-agent.jar ]; then \
        AGENT_FLAG='-javaagent:/app/otel-agent.jar'; \
    fi; \
    exec java ${JAVA_OPTS} ${AGENT_FLAG} -jar /app/app.jar \
"]

# =============================================================================
# GraalVM Native Image Build (uncomment to enable)
# =============================================================================
# FROM ghcr.io/graalvm/native-image-community:21 AS native-builder
#
# ARG SERVICE_MODULE
# WORKDIR /build
#
# COPY --from=builder /build/${SERVICE_MODULE}/target/*.jar app.jar
#
# RUN native-image \
#     --no-fallback \
#     --enable-preview \
#     -H:+ReportExceptionStackTraces \
#     -H:Name=app \
#     -jar app.jar
#
# FROM debian:bookworm-slim AS native-runtime
#
# RUN groupadd --gid 1001 appgroup && \
#     useradd --uid 1001 --gid appgroup --shell /bin/false --create-home appuser
#
# WORKDIR /app
# COPY --from=native-builder /build/app /app/app
#
# HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=10s \
#     CMD curl -f http://localhost:8080/actuator/health || exit 1
#
# USER appuser
# EXPOSE 8080 3500 50001
# ENTRYPOINT ["/app/app"]
