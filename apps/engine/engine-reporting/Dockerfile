# Stage 1: Build
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build

# Copy Maven wrapper and POMs first for dependency caching
COPY pom.xml .
COPY common/pom.xml common/
COPY lifecycle/pom.xml lifecycle/
COPY period/pom.xml period/
COPY form/pom.xml form/
COPY pptx-template/pom.xml pptx-template/
COPY notification/pom.xml notification/
COPY app/pom.xml app/

# Copy parent POM for resolution
COPY --from=parent ../../../packages/java-base/pom.xml /packages/java-base/pom.xml

# Copy source code
COPY common/src common/src
COPY lifecycle/src lifecycle/src
COPY period/src period/src
COPY form/src form/src
COPY pptx-template/src pptx-template/src
COPY notification/src notification/src
COPY app/src app/src

# Build (skip tests for Docker image)
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -DskipTests package

# Extract layers for optimized caching
WORKDIR /build/app/target
RUN java -Djarmode=layertools -jar *.jar extract --destination /extracted

# Stage 2: Runtime
FROM eclipse-temurin:21-jre

# Download and install OpenTelemetry Java agent
RUN apt-get update && apt-get install -y --no-install-recommends wget curl && \
    mkdir -p /app/agent && \
    wget -q -O /app/agent/opentelemetry-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.9.0/opentelemetry-javaagent.jar && \
    chmod 644 /app/agent/opentelemetry-javaagent.jar && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy extracted layers
COPY --from=builder /extracted/dependencies/ ./
COPY --from=builder /extracted/spring-boot-loader/ ./
COPY --from=builder /extracted/snapshot-dependencies/ ./
COPY --from=builder /extracted/application/ ./

# Copy OTEL agent
COPY --from=runtime /app/agent/opentelemetry-javaagent.jar /app/agent/

# JVM flags with OTEL agent
ENV JAVA_OPTS="--enable-preview -XX:+UseZGC -XX:MaxRAMPercentage=75.0 \
    -javaagent:/app/agent/opentelemetry-javaagent.jar \
    -Dotel.service.name=engine-reporting \
    -Dotel.exporter.otlp.endpoint=http://otel-collector:4317 \
    -Dotel.resource.attributes=service.namespace=reportplatform,deployment.environment=local"

EXPOSE 8105

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
