package com.example.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.example.Order;
import com.example.OrderStatus;
import com.example.service.OrderService;
import com.google.gson.Gson;

import spark.Route;

public class OrderController {
    private final OrderService orderService;
    private final Gson gson = new Gson();
    
    // Объявляем Route как поля класса
    private final Route createOrder;
    private final Route getOrder;
    private final Route getUserOrders;
    private final Route handlePaymentResult;
    
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
        
        // Инициализируем Route в конструкторе
        this.createOrder = (request, response) -> {
            try {
                Map<String, Object> requestBody = gson.fromJson(request.body(), Map.class);
                
                int userId = ((Number) requestBody.get("userId")).intValue();
                
                List<Map<String, Object>> items = (List<Map<String, Object>>) requestBody.get("items");
                
                Order order = orderService.createOrder(userId, items);
                
                response.status(201); // Created
                response.type("application/json");
                
                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("orderId", order.getId());
                responseBody.put("status", order.getStatus().toString());
                responseBody.put("totalPrice", order.getTotalPrice());
                
                return gson.toJson(responseBody);
            } catch (Exception e) {
                response.status(400); // Bad Request
                response.type("application/json");
                return gson.toJson(Map.of("error", e.getMessage()));
            }
        };
        
        this.getOrder = (request, response) -> {
            try {
                int orderId = Integer.parseInt(request.params(":orderId"));
                
                int userId = Integer.parseInt(request.queryParams("userId"));
                
                Optional<Order> orderOptional = orderService.getOrderById(orderId);
                
                if (orderOptional.isPresent()) {
                    Order order = orderOptional.get();
                    
                    if (order.getUserId() != userId) {
                        response.status(403); // Forbidden
                        return gson.toJson(Map.of("error", "You are not authorized to view this order"));
                    }
                    
                    response.status(200); // OK
                    response.type("application/json");
                    
                    Map<String, Object> orderData = new HashMap<>();
                    orderData.put("orderId", order.getId());
                    orderData.put("status", order.getStatus().toString());
                    orderData.put("totalPrice", order.getTotalPrice());
                    orderData.put("items", order.getItems());
                    
                    return gson.toJson(orderData);
                } else {
                    response.status(404); // Not Found
                    return gson.toJson(Map.of("error", "Order not found"));
                }
            } catch (NumberFormatException e) {
                response.status(400); // Bad Request
                return gson.toJson(Map.of("error", "Invalid order ID format"));
            } catch (Exception e) {
                response.status(500); // Internal Server Error
                return gson.toJson(Map.of("error", e.getMessage()));
            }
        };
        
        this.getUserOrders = (request, response) -> {
            try {
                int userId = Integer.parseInt(request.params(":userId"));
                
                List<Order> orders = orderService.getUserOrders(userId);
                
                response.status(200); // OK
                response.type("application/json");
                
                List<Map<String, Object>> ordersList = orders.stream()
                    .map(order -> {
                        Map<String, Object> orderMap = new HashMap<>();
                        orderMap.put("orderId", order.getId());
                        orderMap.put("status", order.getStatus().toString());
                        orderMap.put("totalPrice", order.getTotalPrice());
                        orderMap.put("createdAt", order.getTransactionId());
                        return orderMap;
                    })
                    .collect(Collectors.toList());
                return gson.toJson(Map.of("orders", ordersList));
            } catch (NumberFormatException e) {
                response.status(400); // Bad Request
                return gson.toJson(Map.of("error", "Invalid user ID format"));
            } catch (Exception e) {
                response.status(500); // Internal Server Error
                return gson.toJson(Map.of("error", e.getMessage()));
            }
        };
        
        this.handlePaymentResult = (request, response) -> {
            try {
                Map<String, Object> requestBody = gson.fromJson(request.body(), Map.class);
                
                int orderId = ((Number) requestBody.get("orderId")).intValue();
                boolean success = (Boolean) requestBody.get("success");
                
                OrderStatus newStatus = success ? OrderStatus.PAID : OrderStatus.FAILED;
                boolean updated = orderService.updateOrderStatus(orderId, newStatus);
                
                if (updated) {
                    response.status(200); // OK
                    return gson.toJson(Map.of("success", true));
                } else {
                    response.status(404); // Not Found
                    return gson.toJson(Map.of("error", "Order not found"));
                }
            } catch (Exception e) {
                response.status(400); // Bad Request
                return gson.toJson(Map.of("error", e.getMessage()));
            }
        };
    }
    
    // Геттеры для доступа к приватным полям маршрутов
    public Route getCreateOrder() {
        return createOrder;
    }
    
    public Route getGetOrder() {
        return getOrder;
    }
    
    public Route getGetUserOrders() {
        return getUserOrders;
    }
    
    public Route getHandlePaymentResult() {
        return handlePaymentResult;
    }
}