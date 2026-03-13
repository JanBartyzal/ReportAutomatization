# ============================================================
# Stage 1: Build all modules
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy parent POM and module POMs first for dependency caching
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY servicenow/pom.xml servicenow/pom.xml
COPY app/pom.xml app/pom.xml

# Install Maven (Alpine)
RUN apk add --no-cache maven

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B -pl app -am || true

# Copy source code
COPY common/src common/src
COPY servicenow/src servicenow/src
COPY app/src app/src

# Build the application
RUN mvn clean package -DskipTests -B --enable-preview

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Download and install OpenTelemetry Java agent
RUN apk add --no-cache wget curl && \
    mkdir -p /app/agent && \
    wget -q -O /app/agent/opentelemetry-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.9.0/opentelemetry-javaagent.jar && \
    chmod 644 /app/agent/opentelemetry-javaagent.jar

# Create non-root user
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D appuser

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
    -Dotel.service.name=engine-integrations \
    -Dotel.exporter.otlp.endpoint=http://otel-collector:4317 \
    -Dotel.resource.attributes=service.namespace=reportplatform,deployment.environment=local"

EXPOSE 8106

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8106/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
