# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy the built JAR from builder stage
COPY --from=builder /app/target/csv-exporter-1.0.0.jar app.jar

# Create exports directory
RUN mkdir -p /app/exports

# Set environment variables
ENV DB_HOST=postgres-service
ENV DB_PORT=5432
ENV DB_NAME=shipment
ENV DB_USER=shipment
ENV DB_PASSWORD=shipment
ENV OUTPUT_DIR=/app/exports
ENV EXPORT_TYPE=all

# Expose port for health checks (optional)
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
