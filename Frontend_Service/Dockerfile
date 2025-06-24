FROM maven:3.8.4-openjdk-11 AS builder

WORKDIR /app

# Copy all project files first
COPY . /app/

# Build Common_Models first
RUN cd /app/Common_Models && mvn clean install -DskipTests

# Build the Frontend Service
WORKDIR /app/Frontend_Service
RUN mvn clean package -DskipTests

# Runtime image
FROM openjdk:11-jre-slim

WORKDIR /app

# Copy the built artifacts
COPY --from=builder /app/Frontend_Service/target/*.jar /app/frontend-service.jar

EXPOSE 8083

CMD ["java", "-jar", "/app/frontend-service.jar"]
