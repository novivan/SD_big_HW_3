package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.example.controller.AccountController;
import com.example.inbox.InboxRepository;
import com.example.inbox.InboxService;
import com.example.messaging.MessageBroker;
import com.example.messaging.MessageSchema;
import com.example.outbox.OutboxRepository;
import com.example.outbox.OutboxService;
import com.example.repository.AccountRepository;
import com.example.repository.PaymentRepository;
import com.example.service.AccountService;
import com.example.service.PaymentService;
import com.google.gson.Gson;

import static spark.Spark.awaitInitialization;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.notFound;
import static spark.Spark.port;
import static spark.Spark.post;

public class App {
    private static MessageBroker messageBroker;
    private static InboxService inboxService;
    private static OutboxService outboxService;
    private static final Gson gson = new Gson();
    
    public static void main(String[] args) {
        port(8082);
        
        System.out.println("Payments microservice is starting on port 8082");
        
        // Инициализация репозиториев
        AccountRepository accountRepository = new AccountRepository();
        PaymentRepository paymentRepository = new PaymentRepository();
        InboxRepository inboxRepository = new InboxRepository();
        OutboxRepository outboxRepository = new OutboxRepository();
        
        // Инициализация брокера сообщений
        messageBroker = new MessageBroker("localhost", 5672);
        try {
            messageBroker.connect();
        } catch (Exception e) {
            System.err.println("Failed to connect to message broker: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
        // Инициализация сервисов
        AccountService accountService = new AccountService(accountRepository);
        
        // Инициализация Outbox сервиса
        outboxService = new OutboxService(outboxRepository, messageBroker);
        outboxService.startProcessing();
        
        PaymentService paymentService = new PaymentService(paymentRepository, accountService, outboxService);
        
        // Инициализация Inbox сервиса
        inboxService = new InboxService(inboxRepository, paymentService);
        
        // Инициализация контроллеров
        AccountController accountController = new AccountController(accountService);
        
        // REST API эндпоинты
        get("/payments/hello", (req, res) -> "Hello from Payments Microservice!");
        
        // Эндпоинты для работы со счетами
        post("/accounts", accountController.getCreateAccount());
        get("/accounts/:userId/balance", accountController.getGetBalance());
        post("/accounts/:userId/deposit", accountController.getDepositFunds());
        
        // Добавляем эндпоинт для проверки состояния сервиса
        get("/payments/health", (req, res) -> {
            res.type("application/json");
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("timestamp", System.currentTimeMillis());
            return gson.toJson(health);
        });
        
        // Обработка ошибок
        exception(Exception.class, (e, request, response) -> {
            e.printStackTrace();
            response.status(500);
            response.type("application/json");
            response.body(gson.toJson(Map.of(
                "error", e.getMessage(),
                "type", e.getClass().getName(),
                "timestamp", System.currentTimeMillis()
            )));
        });
        
        notFound((req, res) -> {
            res.type("application/json");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Not found");
            errorResponse.put("path", req.pathInfo());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return gson.toJson(errorResponse);
        });
        
        // Запускаем прослушивание очереди платежных запросов с использованием константы из схемы
        try {
            messageBroker.receiveMessages(MessageSchema.PAYMENT_REQUESTS_QUEUE, message -> {
                System.out.println("Received payment request: " + message);
                inboxService.processMessage(message);
            });
            
            System.out.println("Started listening for payment requests on queue: " 
                + MessageSchema.PAYMENT_REQUESTS_QUEUE);
        } catch (IOException e) {
            System.err.println("Failed to set up message consumer: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Корректное завершение работы
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            outboxService.stopProcessing();
            try {
                messageBroker.close();
            } catch (IOException e) {
                System.err.println("Error closing message broker: " + e.getMessage());
                e.printStackTrace();
            }
        }));
        
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
                   "    <title>Payments API Documentation</title>\n" +
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

        // Редирект с корневого маршрута
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

        // Обязательно дожидаемся инициализации всех маршрутов
        awaitInitialization();
        System.out.println("Payments microservice is ready at http://localhost:8082/docs");
        
        System.out.println("Payments microservice started successfully");
    }
}