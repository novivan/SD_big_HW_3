package com.example.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.Good;
import com.example.Order;
import com.example.OrderStatus;
import com.example.outbox.OutboxMessage;
import com.example.outbox.OutboxService;
import com.example.repository.OrderRepository;
import com.google.gson.Gson;

public class OrderService {
    private final OrderRepository orderRepository;
    private final OutboxService outboxService;
    private final Gson gson = new Gson();
    
    public OrderService(OrderRepository orderRepository, OutboxService outboxService) {
        this.orderRepository = orderRepository;
        this.outboxService = outboxService;
    }
    
    public Order createOrder(int userId, List<Map<String, Object>> orderItems) {
        Order order = new Order(userId);
        
        for (Map<String, Object> item : orderItems) {
            double price = ((Number) item.get("price")).doubleValue();
            String name = (String) item.get("name");
            String description = (String) item.get("description");
            int quantity = ((Number) item.get("quantity")).intValue();
            
            Good good = new Good(price, name, description, 100); // amount_on_market=100 (условное значение)
            
            order.addItem(good, quantity);
        }
        
        orderRepository.save(order);
        
        Map<String, Object> paymentRequest = Map.of(
                "orderId", order.getId(),
                "userId", order.getUserId(),
                "amount", order.getTotalPrice(),
                "transactionId", order.getTransactionId()
        );
        
        String payload = gson.toJson(paymentRequest);
        OutboxMessage outboxMessage = new OutboxMessage(
                String.valueOf(order.getId()),
                "Order",
                "ORDER_CREATED",
                payload
        );
        
        outboxService.saveMessage(outboxMessage);
        
        return order;
    }

    public Optional<Order> getOrderById(int id) {
        return orderRepository.findById(id);
    }
    
    public List<Order> getUserOrders(int userId) {
        return orderRepository.findByUserId(userId);
    }
    
    public boolean updateOrderStatus(int orderId, OrderStatus status) {
        return orderRepository.UpdateStatus(orderId, status);
    }
}