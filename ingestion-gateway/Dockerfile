# ==============================================================================
# Multi-stage Dockerfile for the Ingestion & Aggregation Gateway
# Stage 1: Build with Maven
# Stage 2: Run on JRE 17 slim
# ==============================================================================

# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy POM first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src/ src/
RUN mvn clean package -DskipTests -B

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:17-jre-jammy

LABEL maintainer="log-aggregator-team"
LABEL description="Distributed Log Aggregator - Ingestion Gateway"

# Create a non-root user for security
RUN groupadd -r gateway && useradd -r -g gateway gateway

WORKDIR /app

# Copy the fat JAR from the build stage
COPY --from=builder /build/target/ingestion-gateway-1.0.0.jar /app/gateway.jar

# Copy default configuration
COPY src/main/resources/gateway.yaml /app/config/gateway.yaml

# Create log directory
RUN mkdir -p /app/logs && chown -R gateway:gateway /app

USER gateway

# Expose the gateway TCP port
EXPOSE 9090

# Health check - verifies the JVM process is running
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD pgrep -f "gateway.jar" || exit 1

# JVM tuning for containerized environments
ENV JAVA_OPTS="-XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseStringDeduplication \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heap_dump.hprof \
    -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/gateway.jar --config /app/config/gateway.yaml"]
