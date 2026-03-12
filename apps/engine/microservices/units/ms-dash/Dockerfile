###############################################################################
# Stage 1: Build
###############################################################################
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# Copy parent POM and internal modules for dependency resolution
COPY packages/java-base/pom.xml packages/java-base/pom.xml
COPY packages/java-base/java-base-security packages/java-base/java-base-security
COPY packages/java-base/java-base-grpc packages/java-base/java-base-grpc
COPY packages/java-base/java-base-dapr packages/java-base/java-base-dapr
COPY packages/java-base/java-base-observability packages/java-base/java-base-observability

# Install parent POM and modules
RUN cd packages/java-base && mvn install -DskipTests -q

# Copy ms-dash source
COPY apps/engine/microservices/units/ms-dash/pom.xml pom.xml
COPY apps/engine/microservices/units/ms-dash/src src

# Build the application with layered jar
RUN mvn package -DskipTests -q && \
    java -Djarmode=layertools -jar target/*.jar extract --destination extracted

###############################################################################
# Stage 2: Runtime
###############################################################################
FROM eclipse-temurin:21-jre-jammy

RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

WORKDIR /app

# Copy layers in order of change frequency (least to most)
COPY --from=builder /build/extracted/dependencies/ ./
COPY --from=builder /build/extracted/spring-boot-loader/ ./
COPY --from=builder /build/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/extracted/application/ ./

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "--enable-preview", "org.springframework.boot.loader.launch.JarLauncher"]
