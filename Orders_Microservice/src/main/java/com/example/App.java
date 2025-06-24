package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.example.controller.OrderController;
import com.example.messaging.MessageBroker;
import com.example.messaging.MessageHandler;
import com.example.messaging.MessageSchema;
import com.example.outbox.OutboxRepository;
import com.example.outbox.OutboxService;
import com.example.repository.OrderRepository;
import com.example.service.OrderService;
import com.google.gson.Gson;

import static spark.Spark.awaitInitialization;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.notFound;
import static spark.Spark.port;
import static spark.Spark.post;

public class App {
    private static MessageBroker messageBroker;
    private static OutboxService outboxService;
    private static final Gson gson = new Gson();
    private static MessageHandler messageHandler;

    public static void main(String[] args) {
        port(8081);
        
        System.out.println("Orders microservice is starting on port 8081");

        OrderRepository orderRepository = new OrderRepository();
        OutboxRepository outboxRepository = new OutboxRepository();
        
        String rabbitMQHost = System.getenv("RABBITMQ_HOST") != null ? 
                             System.getenv("RABBITMQ_HOST") : "localhost";
        int rabbitMQPort = System.getenv("RABBITMQ_PORT") != null ? 
                           Integer.parseInt(System.getenv("RABBITMQ_PORT")) : 5672;
        
        messageBroker = new MessageBroker(rabbitMQHost, rabbitMQPort);
        try {
            messageBroker.connect();
        } catch (Exception e) {
            System.err.println("Failed to connect to message broker: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
        outboxService = new OutboxService(outboxRepository, messageBroker);
        outboxService.startProcessing();
        
        OrderService orderService = new OrderService(orderRepository, outboxService);
        
        // Инициализируем обработчик для входящих сообщений
        messageHandler = new MessageHandler(orderService);
        
        OrderController orderController = new OrderController(orderService);
        
        // Запускаем слушателя для результатов оплаты
        try {
            messageBroker.receiveMessages(MessageSchema.ORDER_PAYMENT_RESULTS_QUEUE, 
                messageHandler::handlePaymentResult);
            System.out.println("Started listening for payment results on queue: " 
                + MessageSchema.ORDER_PAYMENT_RESULTS_QUEUE);
        } catch (IOException e) {
            System.err.println("Failed to set up message consumer for payment results: " + e.getMessage());
            e.printStackTrace();
        }
        
        get("/api/orders/hello", (req, res) -> "Hello from Orders Microservice!");
        
        // API endpoints
        post("/api/orders", orderController.getCreateOrder());
        get("/api/orders/:orderId", orderController.getGetOrder());
        get("/api/users/:userId/orders", orderController.getGetUserOrders());
        
        post("/api/orders/payment-result", orderController.getHandlePaymentResult());
        
        // Добавляем эндпоинт для проверки состояния сервиса
        get("/api/orders/health", (req, res) -> {
            res.type("application/json");
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("timestamp", System.currentTimeMillis());
            return gson.toJson(health);
        });
        
        exception(Exception.class, (e, request, response) -> {
            e.printStackTrace();
            response.status(500);
            response.type("application/json");
            response.body(gson.toJson(Map.of("error", e.getMessage())));
        });
        
        notFound((req, res) -> {
            res.type("application/json");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Not found");
            errorResponse.put("path", req.pathInfo());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return gson.toJson(errorResponse);
        });
        
        // Создадим директорию для статических файлов, если она не существует
        try {
            Files.createDirectories(Paths.get("src/main/resources/static/swagger-ui"));
        } catch (IOException e) {
            System.err.println("Failed to create static directories: " + e.getMessage());
        }
        
        // Предоставление swagger.yaml
        get("/swagger.yaml", (req, res) -> {
            res.type("application/yaml");
            try {
                String yamlContent = Files.readString(Paths.get("swagger.yaml"));
                System.out.println("Successfully loaded swagger.yaml");
                return yamlContent;
            } catch (IOException e) {
                System.err.println("Failed to load swagger.yaml: " + e.getMessage());
                e.printStackTrace();
                res.status(404);
                return "Swagger YAML file not found";
            }
        });

        // Страница Swagger UI
        get("/docs", (req, res) -> {
            res.type("text/html");
            String html = "<!DOCTYPE html>\n" +
                   "<html lang=\"en\">\n" +
                   "<head>\n" +
                   "    <meta charset=\"UTF-8\">\n" +
                   "    <title>Orders API Documentation</title>\n" +
                   "    <link rel=\"stylesheet\" type=\"text/css\" href=\"https://unpkg.com/swagger-ui-dist@4.15.5/swagger-ui.css\">\n" +
                   "    <style>\n" +
                   "        body { margin: 0; padding: 0; }\n" +
                   "        #swagger-ui { max-width: 1200px; margin: 0 auto; }\n" +
                   "    </style>\n" +
                   "</head>\n" +
                   "<body>\n" +
                   "    <div id=\"swagger-ui\"></div>\n" +
                   "    <script src=\"https://unpkg.com/swagger-ui-dist@4.15.5/swagger-ui-bundle.js\"></script>\n" +
                   "    <script src=\"https://unpkg.com/swagger-ui-dist@4.15.5/swagger-ui-standalone-preset.js\"></script>\n" +
                   "    <script>\n" +
                   "        window.onload = function() {\n" +
                   "            console.log('Initializing Swagger UI...');\n" +
                   "            window.ui = SwaggerUIBundle({\n" +
                   "                url: window.location.origin + '/swagger.yaml',\n" +
                   "                dom_id: '#swagger-ui',\n" +
                   "                deepLinking: true,\n" +
                   "                presets: [\n" +
                   "                    SwaggerUIBundle.presets.apis,\n" +
                   "                    SwaggerUIStandalonePreset\n" +
                   "                ],\n" +
                   "                plugins: [\n" +
                   "                    SwaggerUIBundle.plugins.DownloadUrl\n" +
                   "                ],\n" +
                   "                layout: \"StandaloneLayout\",\n" +
                   "                onComplete: function() {\n" +
                   "                    console.log('Swagger UI initialization complete');\n" +
                   "                }\n" +
                   "            });\n" +
                   "        };\n" +
                   "    </script>\n" +
                   "</body>\n" +
                   "</html>";
            System.out.println("Serving Swagger UI HTML");
            return html;
        });

        // Редирект с корневого маршрута на Swagger UI
        get("/", (req, res) -> {
            res.redirect("/docs");
            return null;
        });

        // Альтернативный путь к Swagger UI
        get("/swagger-ui", (req, res) -> {
            res.redirect("/docs");
            return null;
        });

        // Альтернативный путь к Swagger UI
        get("/swagger", (req, res) -> {
            res.redirect("/docs");
            return null;
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            outboxService.stopProcessing();
            try {
                messageBroker.close();
            } catch (IOException e) {
                System.err.println("Error closing message broker: " + e.getMessage());
            }
        }));
        
        // Обязательно дожидаемся инициализации всех маршрутов
        awaitInitialization();
        System.out.println("Orders microservice is ready at http://localhost:8081/docs");
        
        System.out.println("Orders microservice started successfully");
    }
}