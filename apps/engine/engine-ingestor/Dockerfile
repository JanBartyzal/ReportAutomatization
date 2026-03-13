# ============================================================
# Stage 1: Build all modules
# ============================================================
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# Copy parent POM hierarchy (java-base)
COPY ../../../packages/java-base/pom.xml /build/packages/java-base/pom.xml
COPY ../../../packages/java-base/java-base-security/pom.xml /build/packages/java-base/java-base-security/pom.xml
COPY ../../../packages/java-base/java-base-dapr/pom.xml /build/packages/java-base/java-base-dapr/pom.xml
COPY ../../../packages/java-base/java-base-observability/pom.xml /build/packages/java-base/java-base-observability/pom.xml
COPY ../../../packages/java-base/java-base-grpc/pom.xml /build/packages/java-base/java-base-grpc/pom.xml

# Copy proto definitions
COPY ../../../protos /build/protos

# Copy module POMs first for dependency caching
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY ingestor/pom.xml ingestor/pom.xml
COPY scanner/pom.xml scanner/pom.xml
COPY app/pom.xml app/pom.xml

# Install parent dependencies
RUN --mount=type=cache,target=/root/.m2 \
    cd /build/packages/java-base && mvn install -N -B -q || true

# Download dependencies (cached layer)
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B -pl app -am || true

# Copy source code
COPY common/src common/src
COPY ingestor/src ingestor/src
COPY scanner/src scanner/src
COPY app/src app/src

# Build the application
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:21-jre-jammy AS runtime

# Download and install OpenTelemetry Java agent
RUN apt-get update && apt-get install -y --no-install-recommends wget curl && \
    mkdir -p /app/agent && \
    wget -q -O /app/agent/opentelemetry-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.9.0/opentelemetry-javaagent.jar && \
    chmod 644 /app/agent/opentelemetry-javaagent.jar && \
    rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -m appuser

WORKDIR /app

# Copy the fat JAR from builder
COPY --from=builder /build/app/target/*.jar app.jar

# Copy OTEL agent
COPY --from=runtime /app/agent/opentelemetry-javaagent.jar /app/agent/

# Change ownership
RUN chown -R appuser:appgroup /app

USER appuser

# JVM flags with OTEL agent
ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 --enable-preview \
    -javaagent:/app/agent/opentelemetry-javaagent.jar \
    -Dotel.service.name=engine-ingestor \
    -Dotel.exporter.otlp.endpoint=http://otel-collector:4317 \
    -Dotel.resource.attributes=service.namespace=reportplatform,deployment.environment=local"

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
