# ============================================================
# Stage 1: Build all modules
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy parent POM and module POMs first for dependency caching
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY auth/pom.xml auth/pom.xml
COPY admin/pom.xml admin/pom.xml
COPY batch/pom.xml batch/pom.xml
COPY versioning/pom.xml versioning/pom.xml
COPY audit/pom.xml audit/pom.xml
COPY app/pom.xml app/pom.xml

# Install Maven (Alpine)
RUN apk add --no-cache maven

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B -pl app -am || true

# Copy source code
COPY common/src common/src
COPY auth/src auth/src
COPY admin/src admin/src
COPY batch/src batch/src
COPY versioning/src versioning/src
COPY audit/src audit/src
COPY app/src app/src

# Build the application
RUN mvn clean package -DskipTests -B --enable-preview

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create non-root user
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D appuser

WORKDIR /app

# Copy the fat JAR from builder
COPY --from=builder /build/app/target/*.jar app.jar

# Change ownership
RUN chown -R appuser:appgroup /app

USER appuser

# JVM flags
ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 --enable-preview"

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
