package com.example.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.example.Good;
import com.example.Order;
import com.example.OrderStatus;
import com.example.messaging.MessageSchema;
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
        System.out.println("Creating order for user " + userId + " with " + orderItems.size() + " items");
        Order order = new Order(userId);
        
        for (Map<String, Object> item : orderItems) {
            double price = ((Number) item.get("price")).doubleValue();
            String name = (String) item.get("name");
            String description = (String) item.get("description");
            int quantity = ((Number) item.get("quantity")).intValue();
            
            System.out.println("Adding item to order: " + name + ", price: " + price + ", quantity: " + quantity);
            
            Good good = new Good(price, name, description, 100); // amount_on_market=100 (условное значение)
            
            order.addItem(good, quantity);
        }
        
        orderRepository.save(order);
        System.out.println("Order saved with ID: " + order.getId() + ", total price: " + order.getTotalPrice());
        
        // Создаем запрос на оплату, используя схему сообщений
        MessageSchema.PaymentRequest paymentRequest = new MessageSchema.PaymentRequest(
            order.getId(),
            order.getUserId(),
            order.getTotalPrice(),
            order.getTransactionId()
        );
        
        // Добавляем уникальный messageId
        paymentRequest.messageId = UUID.randomUUID().toString();
        
        // Преобразуем в JSON и сохраняем в Outbox
        String payload = gson.toJson(paymentRequest);
        System.out.println("Creating payment request message: " + payload);
        
        OutboxMessage outboxMessage = new OutboxMessage(
                String.valueOf(order.getId()),
                "Payment",
                "PROCESS_PAYMENT",
                payload,
                paymentRequest.messageId
        );
        
        outboxService.saveMessage(outboxMessage);
        System.out.println("Payment request message saved to outbox");
        
        return order;
    }

    public Optional<Order> getOrderById(int id) {
        return orderRepository.findById(id);
    }
    
    public List<Order> getUserOrders(int userId) {
        return orderRepository.findByUserId(userId);
    }
    
    public boolean updateOrderStatus(int orderId, OrderStatus newStatus) {
        System.out.println("Updating order " + orderId + " status to " + newStatus);
        return orderRepository.updateStatus(orderId, newStatus);
    }
}