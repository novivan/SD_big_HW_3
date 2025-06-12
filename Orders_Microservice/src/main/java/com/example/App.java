package com.example;

import java.io.IOException;
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

import static spark.Spark.*;

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
        
        messageBroker = new MessageBroker("localhost", 5672);
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
        
        get("/orders/hello", (req, res) -> "Hello from Orders Microservice!");
        
        // API endpoints
        post("/orders", orderController.getCreateOrder());
        get("/orders/:orderId", orderController.getGetOrder());
        get("/users/:userId/orders", orderController.getGetUserOrders());
        
        post("/orders/payment-result", orderController.getHandlePaymentResult());
        
        // Добавляем эндпоинт для проверки состояния сервиса
        get("/orders/health", (req, res) -> {
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
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            outboxService.stopProcessing();
            try {
                messageBroker.close();
            } catch (IOException e) {
                System.err.println("Error closing message broker: " + e.getMessage());
            }
        }));
        
        System.out.println("Orders microservice started successfully");
    }
}