FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy parent POMs
COPY packages/java-base/pom.xml /app/packages/java-base/
COPY packages/java-base/java-base-security/pom.xml /app/packages/java-base/java-base-security/
COPY packages/java-base/java-base-dapr/pom.xml /app/packages/java-base/java-base-dapr/
COPY packages/java-base/java-base-grpc/pom.xml /app/packages/java-base/java-base-grpc/
COPY packages/java-base/java-base-observability/pom.xml /app/packages/java-base/java-base-observability/

# Copy project POM
COPY apps/engine/microservices/units/ms-ext-snow/pom.xml /app/apps/engine/microservices/units/ms-ext-snow/

# Build parent modules first
WORKDIR /app/packages/java-base
RUN mvn install -N -DskipTests

WORKDIR /app/packages/java-base/java-base-security
RUN mvn install -N -DskipTests

WORKDIR /app/packages/java-base/java-base-dapr
RUN mvn install -N -DskipTests

WORKDIR /app/packages/java-base/java-base-grpc
RUN mvn install -N -DskipTests

WORKDIR /app/packages/java-base/java-base-observability
RUN mvn install -N -DskipTests

# Build the application
WORKDIR /app/apps/engine/microservices/units/ms-ext-snow
RUN mvn clean package -DskipTests

# Runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the JAR file
COPY --from=builder /app/apps/engine/microservices/units/ms-ext-snow/target/ms-ext-snow-1.0.0-SNAPSHOT.jar /app/app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
