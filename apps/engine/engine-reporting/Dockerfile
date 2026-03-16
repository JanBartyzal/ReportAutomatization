# Stage 1: Build
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
COPY apps/engine/engine-reporting/pom.xml apps/engine/engine-reporting/pom.xml
COPY apps/engine/engine-reporting/common/pom.xml apps/engine/engine-reporting/common/
COPY apps/engine/engine-reporting/lifecycle/pom.xml apps/engine/engine-reporting/lifecycle/
COPY apps/engine/engine-reporting/period/pom.xml apps/engine/engine-reporting/period/
COPY apps/engine/engine-reporting/form/pom.xml apps/engine/engine-reporting/form/
COPY apps/engine/engine-reporting/pptx-template/pom.xml apps/engine/engine-reporting/pptx-template/
COPY apps/engine/engine-reporting/notification/pom.xml apps/engine/engine-reporting/notification/
COPY apps/engine/engine-reporting/app/pom.xml apps/engine/engine-reporting/app/

# Copy source code
COPY apps/engine/engine-reporting/common/src apps/engine/engine-reporting/common/src
COPY apps/engine/engine-reporting/lifecycle/src apps/engine/engine-reporting/lifecycle/src
COPY apps/engine/engine-reporting/period/src apps/engine/engine-reporting/period/src
COPY apps/engine/engine-reporting/form/src apps/engine/engine-reporting/form/src
COPY apps/engine/engine-reporting/pptx-template/src apps/engine/engine-reporting/pptx-template/src
COPY apps/engine/engine-reporting/notification/src apps/engine/engine-reporting/notification/src
COPY apps/engine/engine-reporting/app/src apps/engine/engine-reporting/app/src

# Build (skip tests for Docker image)
RUN cd apps/engine/engine-reporting && mvn -B -DskipTests package

# Extract layers for optimized caching
RUN java -Djarmode=layertools \
    -jar apps/engine/engine-reporting/app/target/*.jar extract \
    --destination /extracted

# Stage 2: Runtime
FROM eclipse-temurin:25-jre

# Download and install OpenTelemetry Java agent
RUN apt-get update && apt-get install -y --no-install-recommends wget && \
    mkdir -p /app/agent && \
    wget -q -O /app/agent/opentelemetry-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.9.0/opentelemetry-javaagent.jar && \
    chmod 644 /app/agent/opentelemetry-javaagent.jar && \
    rm -rf /var/lib/apt/lists/*

RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -m appuser

WORKDIR /app

# Copy extracted layers
COPY --from=builder /extracted/dependencies/ ./
COPY --from=builder /extracted/spring-boot-loader/ ./
COPY --from=builder /extracted/snapshot-dependencies/ ./
COPY --from=builder /extracted/application/ ./

RUN chown -R appuser:appgroup /app
USER appuser

# JVM flags with OTEL agent
ENV JAVA_OPTS="--enable-preview -XX:+UseZGC -XX:MaxRAMPercentage=75.0 \
    -javaagent:/app/agent/opentelemetry-javaagent.jar \
    -Dotel.service.name=engine-reporting \
    -Dotel.exporter.otlp.endpoint=http://otel-collector:4317 \
    -Dotel.resource.attributes=service.namespace=reportplatform,deployment.environment=local"

EXPOSE 8105

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8105/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
