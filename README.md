# Микросервисное приложение интернет-магазина

## Запуск проекта

1. Убедитесь, что у вас установлены:
   - Java 11 или выше
   - Maven
   - RabbitMQ

2. Запустите все сервисы с помощью скрипта:
```bash
./start.sh
```

## Завершение работы

Для корректного завершения работы всех сервисов используйте скрипт:
```bash
./stop.sh
```

Этот скрипт:
- Остановит все запущенные микросервисы
- Остановит RabbitMQ
- Очистит временные файлы

## Доступные эндпоинты

### API Gateway (порт 8080)
- Swagger UI: http://localhost:8080/docs
- Основной API: http://localhost:8080/api/*

### Orders Service (порт 8081)
- Swagger UI: http://localhost:8081/docs
- API: http://localhost:8081/orders/*

### Payments Service (порт 8082)
- Swagger UI: http://localhost:8082/docs
- API: http://localhost:8082/payments/*

### Frontend Service (порт 8083)
- Веб-интерфейс: http://localhost:8083

## Соответствие критериям

### 1. Основные требования
- ✅ Payments Service:
  - Создание счета
  - Пополнение счета
  - Просмотр баланса
- ✅ Orders Service:
  - Создание заказа
  - Просмотр списка заказов
  - Просмотр статуса заказа

### 2. Архитектурное проектирование
- ✅ Четкое разделение на сервисы:
  - API Gateway (роутинг)
  - Orders Service (заказы)
  - Payments Service (платежи)
- ✅ Использование очередей сообщений (RabbitMQ)
- ✅ Паттерны:
  - Transactional Outbox в Orders Service
  - Transactional Inbox и Outbox в Payments Service
  - Exactly once семантика при списании денег

### 3. Postman/Swagger
- ✅ Swagger UI для всех сервисов
- ✅ Postman коллекция с примерами запросов

### 4. Тесты
- ✅ Покрытие кода тестами >15%
- ✅ Интеграционные тесты

### 6. Фронтенд
- ✅ Отдельный сервис
- ✅ REST API взаимодействие
- ✅ Минимальный веб-интерфейс
- ✅ Отображение статусов заказов
