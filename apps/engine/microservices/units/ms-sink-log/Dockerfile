# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY packages/java-base/pom.xml packages/java-base/pom.xml
COPY packages/java-base/java-base-grpc/pom.xml packages/java-base/java-base-grpc/pom.xml
COPY packages/java-base/java-base-observability/pom.xml packages/java-base/java-base-observability/pom.xml
COPY packages/java-base/java-base-grpc/src packages/java-base/java-base-grpc/src
COPY packages/java-base/java-base-observability/src packages/java-base/java-base-observability/src
COPY apps/engine/microservices/units/ms-sink-log/pom.xml apps/engine/microservices/units/ms-sink-log/pom.xml
COPY apps/engine/microservices/units/ms-sink-log/src apps/engine/microservices/units/ms-sink-log/src
COPY protos protos
RUN apk add --no-cache maven protobuf
RUN cd packages/java-base && mvn install -pl java-base-grpc,java-base-observability -am -DskipTests -q
RUN cd apps/engine/microservices/units/ms-sink-log && mvn package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=build /workspace/apps/engine/microservices/units/ms-sink-log/target/*.jar app.jar
RUN chown -R appuser:appgroup /app
USER appuser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "--enable-preview", "-jar", "app.jar"]
