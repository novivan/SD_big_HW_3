package com.example.messaging;

import com.example.OrderStatus;
import com.example.messaging.MessageSchema.PaymentResult;
import com.example.service.OrderService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Обработчик входящих сообщений для Orders Microservice
 */
public class MessageHandler {
    private final OrderService orderService;
    private final Gson gson = new Gson();
    
    public MessageHandler(OrderService orderService) {
        this.orderService = orderService;
    }
    
    /**
     * Обрабатывает результат платежа, полученный из очереди
     */
    public void handlePaymentResult(String message) {
        try {
            // Парсим и проверяем структуру сообщения
            JsonObject jsonObject = JsonParser.parseString(message).getAsJsonObject();
            
            // Проверяем наличие необходимых полей
            if (!jsonObject.has("orderId") || !jsonObject.has("transactionId") || !jsonObject.has("success")) {
                System.err.println("Invalid payment result message format: " + message);
                return;
            }
            
            // Преобразуем в объект PaymentResult
            PaymentResult paymentResult = gson.fromJson(message, PaymentResult.class);
            
            // Логируем полученный результат
            System.out.println("Received payment result for order " + paymentResult.orderId + 
                ": " + (paymentResult.success ? "SUCCESS" : "FAILURE"));
            
            if (paymentResult.failureReason != null && !paymentResult.failureReason.isEmpty()) {
                System.out.println("Failure reason: " + paymentResult.failureReason);
            }
            
            // Обновляем статус заказа
            OrderStatus newStatus = paymentResult.success ? OrderStatus.PAID : OrderStatus.FAILED;
            boolean updated = orderService.updateOrderStatus(paymentResult.orderId, newStatus);
            
            if (updated) {
                System.out.println("Order status updated successfully for order ID: " + paymentResult.orderId);
                
                // Здесь можно добавить код для уведомления пользователя об изменении статуса заказа
                // например, через WebSocket или другие механизмы уведомлений
            } else {
                System.err.println("Failed to update order status for order ID: " + paymentResult.orderId);
            }
        } catch (Exception e) {
            System.err.println("Error handling payment result: " + e.getMessage());
            e.printStackTrace();
        }
    }
}