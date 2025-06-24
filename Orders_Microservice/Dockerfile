FROM maven:3.8.4-openjdk-11 AS builder

WORKDIR /app

# Copy all project files first
COPY . /app/

# Build Common_Models first
RUN cd /app/Common_Models && mvn clean install -DskipTests

# Build the Orders Microservice with shade plugin
WORKDIR /app/Orders_Microservice
RUN mvn clean package -DskipTests

# Runtime image
FROM openjdk:11-jre-slim

WORKDIR /app

# Install curl for health checks
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Copy the executable jar file
COPY --from=builder /app/Orders_Microservice/target/Orders_Microservice-1.0-SNAPSHOT.jar /app/orders-service.jar
RUN touch /app/swagger.yaml

EXPOSE 8081

# Simple command to run the jar
CMD ["java", "-jar", "/app/orders-service.jar"]
