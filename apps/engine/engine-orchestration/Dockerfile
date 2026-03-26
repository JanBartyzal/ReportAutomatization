# Stage 1: Build
FROM eclipse-temurin:21-jdk AS builder

# Install Maven (binary) and protobuf compiler
ARG MAVEN_VERSION=3.9.6
RUN apt-get update && apt-get install -y --no-install-recommends curl protobuf-compiler && \
    rm -rf /var/lib/apt/lists/* && \
    curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz | tar xz -C /opt && \
    ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/bin/mvn

WORKDIR /build

# Copy protobuf definitions (needed by java-base-protos)
COPY packages/protos packages/protos

# Copy all java-base modules
COPY packages/java-base packages/java-base

# Build and install all parent modules
RUN cd packages/java-base && mvn -B install -DskipTests \
    -Dprotoc.executable=/usr/bin/protoc

# Copy service source
COPY apps/engine/engine-orchestration/pom.xml apps/engine/engine-orchestration/pom.xml
COPY apps/engine/engine-orchestration/src apps/engine/engine-orchestration/src

# Build the service
RUN cd apps/engine/engine-orchestration && \
    mvn -B package -DskipTests

# Extract layers for optimized Docker caching
RUN java -Djarmode=layertools \
    -jar apps/engine/engine-orchestration/target/ms-orch-*.jar extract \
    --destination /extracted

# Stage 2: Runtime
FROM eclipse-temurin:21-jre

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

# Copy extracted layers in order of change frequency (least to most)
COPY --from=builder /extracted/dependencies/ ./
COPY --from=builder /extracted/spring-boot-loader/ ./
COPY --from=builder /extracted/snapshot-dependencies/ ./
COPY --from=builder /extracted/application/ ./

RUN chown -R appuser:appgroup /app
USER appuser

# gRPC port
EXPOSE 50051
# HTTP port (actuator + Dapr subscriber)
EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError \
    -javaagent:/app/agent/opentelemetry-javaagent.jar \
    -Dotel.service.name=engine-orchestration \
    -Dotel.exporter.otlp.endpoint=http://otel-collector:4317 \
    -Dotel.resource.attributes=service.namespace=reportplatform,deployment.environment=local \
    -Dotel.javaagent.logging=application"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1
