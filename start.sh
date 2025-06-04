#!/bin/bash

echo "Starting the application..."

cd ./API_Gateway && mvn clean package 
#nohup чтобы запустить процесс в фоновом режиме
#> /dev/null 2>&1 & перенаправляет лог из nohup.out в мусор (чтоб не создавать лишних логов)
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