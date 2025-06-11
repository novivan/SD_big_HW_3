#!/bin/bash

echo "Stopping all microservices..."

# Функция для остановки сервиса по имени и порту
stop_service() {
    local service_name=$1
    local port=$2

    echo "Stopping $service_name on port $port..."

    # Находим PID процесса, который слушает указанный порт
    pid=$(lsof -ti:$port)

    if [ -n "$pid" ]; then
        echo "Found $service_name process with PID: $pid. Stopping it..."
        kill $pid
        echo "$service_name stopped successfully."
    else
        echo "No $service_name process found on port $port."
    fi
}

# Останавливаем каждый сервис
stop_service "API Gateway" 8080
stop_service "Orders Microservice" 8081
stop_service "Payments Microservice" 8082

echo "All microservices stopped successfully."