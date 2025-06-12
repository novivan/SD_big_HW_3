package com.example.inbox;

import com.example.service.PaymentService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Сервис для обработки входящих сообщений
 */
public class InboxService {
    private final InboxRepository inboxRepository;
    private final PaymentService paymentService;
    private final Gson gson = new Gson();
    
    public InboxService(InboxRepository inboxRepository, PaymentService paymentService) {
        this.inboxRepository = inboxRepository;
        this.paymentService = paymentService;
    }
    
    /**
     * Обработать входящее сообщение из очереди
     */
    public void processMessage(String payload) {
        try {
            // Парсим JSON сообщение
            JsonObject jsonObject = gson.fromJson(payload, JsonObject.class);
            
            // Извлекаем transactionId для идемпотентности
            String transactionId = jsonObject.get("transactionId").getAsString();
            
            // Проверяем, было ли это сообщение уже обработано
            if (inboxRepository.isProcessed(transactionId)) {
                System.out.println("Message with transactionId " + transactionId + " already processed. Skipping.");
                return;
            }
            
            // Сохраняем сообщение в Inbox
            InboxMessage inboxMessage = new InboxMessage("PAYMENT_REQUEST", payload, transactionId);
            inboxRepository.save(inboxMessage);
            
            // Обрабатываем платеж
            int orderId = jsonObject.get("orderId").getAsInt();
            int userId = jsonObject.get("userId").getAsInt();
            double amount = jsonObject.get("amount").getAsDouble();
            
            boolean success = paymentService.processPayment(orderId, userId, amount, transactionId);
            
            // Отмечаем сообщение как обработанное
            inboxRepository.markAsProcessed(inboxMessage.getId());
            
            System.out.println("Payment processed for order " + orderId + ", result: " + (success ? "success" : "failed"));
        } catch (Exception e) {
            System.err.println("Error processing incoming message: " + e.getMessage());
        }
    }
}