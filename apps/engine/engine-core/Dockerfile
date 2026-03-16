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
COPY apps/engine/engine-core/pom.xml apps/engine/engine-core/pom.xml
COPY apps/engine/engine-core/common/pom.xml apps/engine/engine-core/common/pom.xml
COPY apps/engine/engine-core/auth/pom.xml apps/engine/engine-core/auth/pom.xml
COPY apps/engine/engine-core/admin/pom.xml apps/engine/engine-core/admin/pom.xml
COPY apps/engine/engine-core/batch/pom.xml apps/engine/engine-core/batch/pom.xml
COPY apps/engine/engine-core/versioning/pom.xml apps/engine/engine-core/versioning/pom.xml
COPY apps/engine/engine-core/audit/pom.xml apps/engine/engine-core/audit/pom.xml
COPY apps/engine/engine-core/app/pom.xml apps/engine/engine-core/app/pom.xml

# Download dependencies (cached layer)
RUN cd apps/engine/engine-core && mvn dependency:go-offline -B -pl app -am || true

# Copy source code
COPY apps/engine/engine-core/common/src apps/engine/engine-core/common/src
COPY apps/engine/engine-core/auth/src apps/engine/engine-core/auth/src
COPY apps/engine/engine-core/admin/src apps/engine/engine-core/admin/src
COPY apps/engine/engine-core/batch/src apps/engine/engine-core/batch/src
COPY apps/engine/engine-core/versioning/src apps/engine/engine-core/versioning/src
COPY apps/engine/engine-core/audit/src apps/engine/engine-core/audit/src
COPY apps/engine/engine-core/app/src apps/engine/engine-core/app/src

# Build the application
RUN cd apps/engine/engine-core && mvn clean package -DskipTests -Dmaven.test.skip=true -B

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
COPY --from=builder /build/apps/engine/engine-core/app/target/*.jar app.jar

# Change ownership
RUN chown -R appuser:appgroup /app

USER appuser

# JVM flags with OTEL agent
ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 --enable-preview \
    -javaagent:/app/agent/opentelemetry-javaagent.jar \
    -Dotel.service.name=engine-core \
    -Dotel.exporter.otlp.endpoint=http://otel-collector:4317 \
    -Dotel.resource.attributes=service.namespace=reportplatform,deployment.environment=local"

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
