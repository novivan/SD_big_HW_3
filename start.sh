#!/bin/bash

echo "Starting the application..."

#Проверка и запуск RabbitMQ
echo "Starting RAbbitMQ..."
if brew services info rabbitmq | grep "running" > /dev/null; then
    echo "RabbitMQ is already runnning."
else
    brew services start rabbitmq
    #Даем время для запуска
    sleep 5
    echo "RabbitMQ started successfully."
fi

#тут install нужен, чтобы mvn добавил модели в локальный репозиторий
cd ./Common_Models && mvn clean install
echo "Common Models built successfully."
cd ..

cd ./API_Gateway && mvn clean package 
# & (в конце строки) чтобы запустить процесс в фоновом режиме
# nohup позволяет процессу продолжать работать даже после закрытия терминала
# > /dev/null 2>&1 перенаправляет лог из nohup.out в мусор (чтоб не создавать лишних логов)
# а также из файла с дескриптором 2 в файл с дескриптором 1 (поток stderr в stdout) (который в данном случае перенаправлен в /dev/null)
nohup mvn exec:java -Dexec.mainClass="com.example.App" > /dev/null 2>&1 &
cd ..
echo "API_Gateway started successfully."

cd ./Orders_Microservice && mvn clean package 
nohup mvn exec:java -Dexec.mainClass="com.example.App" > /dev/null 2>&1 &
cd ..
echo "Orders Microservice started successfully."

cd ./Payments_Microservice && mvn clean package 
nohup mvn exec:java -Dexec.mainClass="com.example.App" > /dev/null 2>&1 &
cd ..
echo "Payments Microservice started successfully."

echo "All services are now running!"