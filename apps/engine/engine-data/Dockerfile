# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

# Install Maven and protobuf
RUN apk add --no-cache maven protobuf

# Copy parent pom and base modules for dependency resolution
COPY packages/java-base/pom.xml packages/java-base/pom.xml
COPY packages/java-base/java-base-security/pom.xml packages/java-base/java-base-security/pom.xml
COPY packages/java-base/java-base-grpc/pom.xml packages/java-base/java-base-grpc/pom.xml
COPY packages/java-base/java-base-dapr/pom.xml packages/java-base/java-base-dapr/pom.xml
COPY packages/java-base/java-base-observability/pom.xml packages/java-base/java-base-observability/pom.xml

# Copy source for base modules
COPY packages/java-base/java-base-security/src packages/java-base/java-base-security/src
COPY packages/java-base/java-base-grpc/src packages/java-base/java-base-grpc/src
COPY packages/java-base/java-base-dapr/src packages/java-base/java-base-dapr/src
COPY packages/java-base/java-base-observability/src packages/java-base/java-base-observability/src

# Build parent modules first
RUN cd packages/java-base && mvn install -pl java-base-security,java-base-grpc,java-base-dapr,java-base-observability -am -DskipTests -q

# Copy protobuf definitions
COPY packages/protos packages/protos

# Copy engine-data multi-module project
COPY apps/engine/engine-data/pom.xml apps/engine/engine-data/pom.xml
COPY apps/engine/engine-data/common/pom.xml apps/engine/engine-data/common/pom.xml
COPY apps/engine/engine-data/sink-tbl/pom.xml apps/engine/engine-data/sink-tbl/pom.xml
COPY apps/engine/engine-data/sink-doc/pom.xml apps/engine/engine-data/sink-doc/pom.xml
COPY apps/engine/engine-data/sink-log/pom.xml apps/engine/engine-data/sink-log/pom.xml
COPY apps/engine/engine-data/query/pom.xml apps/engine/engine-data/query/pom.xml
COPY apps/engine/engine-data/dashboard/pom.xml apps/engine/engine-data/dashboard/pom.xml
COPY apps/engine/engine-data/search/pom.xml apps/engine/engine-data/search/pom.xml
COPY apps/engine/engine-data/template/pom.xml apps/engine/engine-data/template/pom.xml
COPY apps/engine/engine-data/app/pom.xml apps/engine/engine-data/app/pom.xml

# Copy all source code
COPY apps/engine/engine-data/common/src apps/engine/engine-data/common/src
COPY apps/engine/engine-data/sink-tbl/src apps/engine/engine-data/sink-tbl/src
COPY apps/engine/engine-data/sink-doc/src apps/engine/engine-data/sink-doc/src
COPY apps/engine/engine-data/sink-log/src apps/engine/engine-data/sink-log/src
COPY apps/engine/engine-data/query/src apps/engine/engine-data/query/src
COPY apps/engine/engine-data/dashboard/src apps/engine/engine-data/dashboard/src
COPY apps/engine/engine-data/search/src apps/engine/engine-data/search/src
COPY apps/engine/engine-data/template/src apps/engine/engine-data/template/src
COPY apps/engine/engine-data/app/src apps/engine/engine-data/app/src

# Build engine-data
RUN cd apps/engine/engine-data && mvn package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime

# Download OpenTelemetry Java agent
RUN apk add --no-cache wget curl && \
    mkdir -p /app/agent && \
    wget -q -O /app/agent/opentelemetry-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.9.0/opentelemetry-javaagent.jar && \
    chmod 644 /app/agent/opentelemetry-javaagent.jar

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=build /workspace/apps/engine/engine-data/app/target/*.jar app.jar

# Copy OTEL agent
COPY --from=runtime /app/agent/opentelemetry-javaagent.jar /app/agent/

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8100 50052

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8100/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "--enable-preview", \
    "-javaagent:/app/agent/opentelemetry-javaagent.jar", \
    "-Dotel.service.name=engine-data", \
    "-Dotel.exporter.otlp.endpoint=http://otel-collector:4317", \
    "-Dotel.resource.attributes=service.namespace=reportplatform,deployment.environment=local", \
    "-jar", "app.jar"]
