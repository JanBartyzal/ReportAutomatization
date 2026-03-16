# ============================================================
# Stage 1: Build all modules
# ============================================================
FROM eclipse-temurin:25-jdk AS builder

# Install Maven (binary) and protobuf compiler
ARG MAVEN_VERSION=3.9.6
RUN apt-get update && apt-get install -y --no-install-recommends curl protobuf-compiler && \
    rm -rf /var/lib/apt/lists/* && \
    curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz | tar xz -C /opt && \
    ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/bin/mvn

WORKDIR /build

# Copy and build parent modules (java-base)
COPY packages/protos packages/protos
COPY packages/java-base packages/java-base
RUN cd packages/java-base && mvn install -DskipTests -B \
    -Dprotoc.executable=/usr/bin/protoc

# Copy module POMs first for dependency caching
COPY apps/engine/engine-ingestor/pom.xml apps/engine/engine-ingestor/pom.xml
COPY apps/engine/engine-ingestor/common/pom.xml apps/engine/engine-ingestor/common/pom.xml
COPY apps/engine/engine-ingestor/ingestor/pom.xml apps/engine/engine-ingestor/ingestor/pom.xml
COPY apps/engine/engine-ingestor/scanner/pom.xml apps/engine/engine-ingestor/scanner/pom.xml
COPY apps/engine/engine-ingestor/app/pom.xml apps/engine/engine-ingestor/app/pom.xml

# Download dependencies (cached layer)
RUN cd apps/engine/engine-ingestor && mvn dependency:go-offline -B -pl app -am || true

# Copy source code
COPY apps/engine/engine-ingestor/common/src apps/engine/engine-ingestor/common/src
COPY apps/engine/engine-ingestor/ingestor/src apps/engine/engine-ingestor/ingestor/src
COPY apps/engine/engine-ingestor/scanner/src apps/engine/engine-ingestor/scanner/src
COPY apps/engine/engine-ingestor/app/src apps/engine/engine-ingestor/app/src

# Build the application
RUN cd apps/engine/engine-ingestor && mvn clean package -DskipTests -Dmaven.test.skip=true -U -B

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:25-jre

# Download and install OpenTelemetry Java agent
RUN apt-get update && apt-get install -y --no-install-recommends wget && \
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
COPY --from=builder /build/apps/engine/engine-ingestor/app/target/*.jar app.jar

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
