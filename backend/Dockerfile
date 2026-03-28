# Multi-stage build for optimized image size
FROM maven:3.9.5-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (for better Docker layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Create non-root user for security
RUN addgroup -g 1000 iam && \
    adduser -u 1000 -G iam -s /bin/sh -D iam

# Set working directory
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/iam-server-*.jar app.jar

# Change ownership to non-root user
RUN chown -R iam:iam /app

# Switch to non-root user
USER iam

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Labels for better image management
LABEL maintainer="IAM Server Team <support@vibes.com>"
LABEL version="1.0.0"
LABEL description="Identity and Access Management Server"
LABEL org.opencontainers.image.source="https://github.com/vibes/iam-server"