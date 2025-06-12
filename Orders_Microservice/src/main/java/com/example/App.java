package com.example;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.example.controller.OrderController;
import com.example.messaging.MessageBroker;
import com.example.outbox.OutboxRepository;
import com.example.outbox.OutboxService;
import com.example.repository.OrderRepository;
import com.example.service.OrderService;
import com.google.gson.Gson;

import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.notFound;
import static spark.Spark.port;
import static spark.Spark.post;

public class App {
    private static MessageBroker messageBroker;
    private static OutboxService outboxService;
    private static final Gson gson = new Gson();

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
            return;
        }
        
        outboxService = new OutboxService(outboxRepository, messageBroker);
        outboxService.startProcessing();
        
        OrderService orderService = new OrderService(orderRepository, outboxService);
        
        OrderController orderController = new OrderController(orderService);
        
        get("/orders/hello", (req, res) -> "Hello from Orders Microservice!");
        
        // Используем геттеры для получения маршрутов
        post("/orders", orderController.getCreateOrder());
        get("/orders/:orderId", orderController.getGetOrder());
        get("/users/:userId/orders", orderController.getGetUserOrders());
        
        post("/orders/payment-result", orderController.getHandlePaymentResult());
        
        exception(Exception.class, (e, request, response) -> {
            response.status(500);
            response.type("application/json");
            response.body(gson.toJson(Map.of("error", e.getMessage())));
        });
        
        notFound((req, res) -> {
            res.type("application/json");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Not found");
            errorResponse.put("path", req.pathInfo());
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