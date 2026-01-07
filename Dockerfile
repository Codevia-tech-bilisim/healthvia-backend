# ========================================
# HEALTHVIA PLATFORM - PRODUCTION DOCKERFILE
# Multi-stage build for optimized image size
# ========================================

# ----------------------------------------
# STAGE 1: BUILD
# ----------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml for dependency caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make Maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application (skip tests for faster build)
RUN ./mvnw clean package -DskipTests -Pprod -B

# ----------------------------------------
# STAGE 2: RUNTIME
# ----------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: Create non-root user
RUN addgroup -g 1001 -S healthvia && \
    adduser -u 1001 -S healthvia -G healthvia

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership to non-root user
RUN chown -R healthvia:healthvia /app

# Switch to non-root user
USER healthvia

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM optimizations for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom"

# Run with production profile
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=prod -jar app.jar"]
