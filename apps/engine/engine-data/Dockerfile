# Stage 1: Build
FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

# Install Maven (binary) and protobuf compiler
ARG MAVEN_VERSION=3.9.6
RUN apt-get update && apt-get install -y --no-install-recommends curl protobuf-compiler && \
    rm -rf /var/lib/apt/lists/* && \
    curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz | tar xz -C /opt && \
    ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/bin/mvn

# Copy protobuf definitions (needed by java-base-protos)
COPY packages/protos packages/protos

# Copy all java-base modules (including java-base-protos)
COPY packages/java-base packages/java-base

# Build and install all parent modules
RUN cd packages/java-base && mvn install -DskipTests -B \
    -Dprotoc.executable=/usr/bin/protoc

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
RUN cd apps/engine/engine-data && mvn package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:25-jre

# Download OpenTelemetry Java agent
RUN apt-get update && apt-get install -y --no-install-recommends wget && \
    mkdir -p /app/agent && \
    wget -q -O /app/agent/opentelemetry-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.9.0/opentelemetry-javaagent.jar && \
    chmod 644 /app/agent/opentelemetry-javaagent.jar && \
    rm -rf /var/lib/apt/lists/*

RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -m appuser

WORKDIR /app

COPY --from=build /workspace/apps/engine/engine-data/app/target/*.jar app.jar

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
