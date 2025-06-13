# SD_big_HW_3

---
Пока что это наиболее простая инструкция для запуска сразу всех микросервисов. В корне проекта напишите в консоль:
```
chmod +x start.sh
#это делается единожды для выдачи прав на исполнение
```
```
./start.sh
#каждый раз для запуска приложения
```
---
Это пока остается для меня.
Инструкции по запуску(для всех микросервисов одинаково):
переходим в папку микросервиса:
```
cd <microservice directory's name>
```
после:
```
mvn clean package
```
(чистим и пересобираем все цели сборки, запускаем тесты и пакуем в .jar)

```
mvn exec:java -Dexec.mainClass="com.example.App"
```
говорим maven'у, чем запускать, где искать "запускаемый" класс (у которого есть метод Main)
-Dexec передает mvn'у системаные свойства для запуска (generally)
-Dexec.mainClass="<путь до класса>" говорит, чей Main запускать в начале

## API Documentation

The system provides comprehensive API documentation through OpenAPI (Swagger) specification files:

1. **Orders Microservice API**: `/Orders_Microservice/swagger.yaml`
2. **Payments Microservice API**: `/Payments_Microservice/swagger.yaml`
3. **API Gateway Overview**: `/API_Gateway/swagger.yaml`

### Accessing Swagger UI

You can access the interactive Swagger UI at the following URLs once the services are running:

1. **API Gateway**: [http://localhost:8080/docs](http://localhost:8080/docs)
2. **Orders Microservice**: [http://localhost:8081/docs](http://localhost:8081/docs) 
3. **Payments Microservice**: [http://localhost:8082/docs](http://localhost:8082/docs)

Обратите внимание на пути: теперь используется `/swagger/` вместо предыдущего.

### Viewing the API Documentation

To view the API documentation in a user-friendly format:
1. Visit [Swagger Editor](https://editor.swagger.io/)
2. Import the corresponding YAML file
3. Review the API endpoints, schemas, and examples

### Postman Collection

A Postman collection is also provided for testing the API endpoints:
1. Import `postman_collection.json` into Postman
2. The collection contains example requests for all endpoints
3. Use these examples to interact with the microservices

### Testing the API

You can test the basic endpoints using curl:

```
curl -X GET http://localhost:8080/api/hello
```

```
curl -X GET http://localhost:8081/orders/hello
```

```
curl -X GET http://localhost:8082/payments/hello
```

For more complex endpoints that require a request body:

```
# Create an account
curl -X POST http://localhost:8082/accounts \
  -H "Content-Type: application/json" \
  -d '{"userId": 1}'

# Deposit funds
curl -X POST http://localhost:8082/accounts/1/deposit \
  -H "Content-Type: application/json" \
  -d '{"amount": 100.00}'

# Create an order
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "items": [{"name": "T-shirt", "price": 19.99, "description": "Summer collection", "quantity": 2}]}'
```