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
WORKDIR /app

# Copy extracted layers
COPY --from=builder /extracted/dependencies/ ./
COPY --from=builder /extracted/spring-boot-loader/ ./
COPY --from=builder /extracted/snapshot-dependencies/ ./
COPY --from=builder /extracted/application/ ./

# JVM flags
ENV JAVA_OPTS="--enable-preview -XX:+UseZGC -XX:MaxRAMPercentage=75.0"

EXPOSE 8105

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
