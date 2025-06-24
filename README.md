# Микросервисное приложение интернет-магазина

## Требования

Для запуска приложения вам необходимы:
- Docker
- Docker Compose

## Запуск приложения с использованием Docker

1. **Клонировать репозиторий:**
```
git clone <url-репозитория>
cd SD_big_HW_3
```

2. **Собрать и запустить приложение:**
```
docker-compose build
docker-compose up -d
```

3. **Проверить статус сервисов:**
```
docker-compose ps
```

4. **Доступ к сервисам:**
- Frontend: http://localhost:8083
- API Gateway: http://localhost:8080
- Orders Service: http://localhost:8081
- Payments Service: http://localhost:8082
- RabbitMQ Management: http://localhost:15672 (guest/guest)

## Остановка приложения

```
docker-compose down
```

## Полная пересборка приложения

Если вам нужно полностью пересобрать приложение:
```
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

## Просмотр логов

Просмотр логов отдельного сервиса:
```
docker-compose logs [service-name]
```

Например:
```
docker-compose logs api-gateway
```

Постоянный просмотр логов:
```
docker-compose logs -f [service-name]
```

## Устранение неполадок

1. **Проблема:** Сервисы перезапускаются или не запускаются
   **Решение:** Проверьте логи сервисов:
   ```
   docker-compose logs [service-name]
   ```

2. **Проблема:** Ошибки подключения между сервисами
   **Решение:** Убедитесь, что RabbitMQ запущен и контейнеры могут взаимодействовать в сети:
   ```
   docker-compose logs rabbitmq
   ```

3. **Проблема:** Фронтенд не может подключиться к бэкенду
   **Решение:** Проверьте логи API Gateway:
   ```
   docker-compose logs api-gateway
   ```

4. **Для полной перезагрузки системы:**
   ```
   docker-compose down
   docker system prune -f
   docker-compose build --no-cache
   docker-compose up -d
   ```

## Архитектура приложения

Приложение состоит из следующих компонентов:
- Frontend Service: Пользовательский интерфейс
- API Gateway: Маршрутизация запросов
- Orders Microservice: Управление заказами
- Payments Microservice: Обработка платежей
- RabbitMQ: Обмен сообщениями между микросервисами

