package com.example.inbox;

import com.example.service.PaymentService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Сервис для обработки входящих сообщений с поддержкой идемпотентности
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
     * Обработать входящее сообщение из очереди с гарантией идемпотентности
     */
    public void processMessage(String payload) {
        try {
            // Парсим JSON сообщение
            JsonObject jsonObject = gson.fromJson(payload, JsonObject.class);
            
            // Извлекаем messageId для дедупликации
            String messageId;
            if (jsonObject.has("messageId")) {
                messageId = jsonObject.get("messageId").getAsString();
            } else {
                // Если messageId не предоставлен, используем transactionId как запасной вариант
                messageId = jsonObject.get("transactionId").getAsString();
            }
            
            // Проверяем, было ли это сообщение уже обработано (дедупликация)
            if (inboxRepository.existsById(messageId)) {
                System.out.println("Message with ID " + messageId + " already processed. Skipping.");
                return;
            }
            
            // Извлекаем transactionId для идемпотентного обновления
            String transactionId = jsonObject.get("transactionId").getAsString();
            
            // Сохраняем сообщение в Inbox перед обработкой
            InboxMessage inboxMessage = new InboxMessage(messageId, "PAYMENT_REQUEST", payload, transactionId);
            inboxRepository.save(inboxMessage);
            
            // Обрабатываем платеж
            int orderId = jsonObject.get("orderId").getAsInt();
            int userId = jsonObject.get("userId").getAsInt();
            double amount = jsonObject.get("amount").getAsDouble();
            
            // Идемпотентная обработка
            boolean success = paymentService.processPayment(orderId, userId, amount, transactionId);
            
            // Отмечаем сообщение как обработанное
            inboxRepository.markAsProcessed(inboxMessage.getId());
            
            System.out.println("Payment processed for order " + orderId + ", result: " + (success ? "success" : "failed"));
        } catch (Exception e) {
            System.err.println("Error processing incoming message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}