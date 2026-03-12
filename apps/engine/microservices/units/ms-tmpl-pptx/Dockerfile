# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

# Copy parent pom and modules for dependency resolution
COPY packages/java-base/pom.xml packages/java-base/pom.xml
COPY packages/java-base/java-base-dapr/pom.xml packages/java-base/java-base-dapr/pom.xml
COPY packages/java-base/java-base-security/pom.xml packages/java-base/java-base-security/pom.xml
COPY packages/java-base/java-base-observability/pom.xml packages/java-base/java-base-observability/pom.xml

# Copy source for base modules
COPY packages/java-base/java-base-dapr/src packages/java-base/java-base-dapr/src
COPY packages/java-base/java-base-security/src packages/java-base/java-base-security/src
COPY packages/java-base/java-base-observability/src packages/java-base/java-base-observability/src

# Copy ms-tmpl-pptx source
COPY apps/engine/microservices/units/ms-tmpl-pptx/pom.xml apps/engine/microservices/units/ms-tmpl-pptx/pom.xml
COPY apps/engine/microservices/units/ms-tmpl-pptx/src apps/engine/microservices/units/ms-tmpl-pptx/src

# Install Maven
RUN apk add --no-cache maven

# Build parent modules first
RUN cd packages/java-base && mvn install -pl java-base-dapr,java-base-security,java-base-observability -am -DskipTests -q

# Build ms-tmpl-pptx
RUN cd apps/engine/microservices/units/ms-tmpl-pptx && mvn package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=build /workspace/apps/engine/microservices/units/ms-tmpl-pptx/target/*.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "--enable-preview", \
    "-jar", "app.jar"]
