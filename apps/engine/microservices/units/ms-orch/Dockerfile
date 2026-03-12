# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy parent POM and modules for dependency resolution
COPY packages/java-base/pom.xml packages/java-base/pom.xml
COPY packages/java-base/java-base-grpc packages/java-base/java-base-grpc
COPY packages/java-base/java-base-dapr packages/java-base/java-base-dapr
COPY packages/java-base/java-base-observability packages/java-base/java-base-observability
COPY packages/java-base/java-base-security packages/java-base/java-base-security

# Copy service source
COPY apps/engine/microservices/units/ms-orch/pom.xml apps/engine/microservices/units/ms-orch/pom.xml
COPY apps/engine/microservices/units/ms-orch/src apps/engine/microservices/units/ms-orch/src

# Build parent modules first
RUN cd packages/java-base && \
    ./mvnw -B -ntp install -DskipTests -pl java-base-grpc,java-base-dapr,java-base-observability

# Build the service
RUN cd apps/engine/microservices/units/ms-orch && \
    mvn -B -ntp package -DskipTests

# Extract layers for optimized Docker caching
RUN java -Djarmode=layertools \
    -jar apps/engine/microservices/units/ms-orch/target/ms-orch-*.jar extract \
    --destination /extracted

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup --system appgroup && \
    adduser --system --ingroup appgroup appuser

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

ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1
