package com.example.inbox;

import com.example.messaging.MessageSchema;
import com.example.service.PaymentService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
            JsonObject jsonObject = JsonParser.parseString(payload).getAsJsonObject();
            
            // Проверяем, соответствует ли сообщение схеме
            if (!validateMessageSchema(jsonObject)) {
                System.err.println("Invalid message format: " + payload);
                return;
            }
            
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
            InboxMessage inboxMessage = new InboxMessage(messageId, 
                                                       jsonObject.get("eventType").getAsString(), 
                                                       payload, 
                                                       transactionId);
            inboxRepository.save(inboxMessage);
            
            // Обрабатываем платеж в зависимости от типа события
            String eventType = jsonObject.get("eventType").getAsString();
            
            if (MessageSchema.PaymentRequestType.PROCESS_PAYMENT.equals(eventType)) {
                // Извлекаем данные для платежа
                int orderId = jsonObject.get("orderId").getAsInt();
                int userId = jsonObject.get("userId").getAsInt();
                double amount = jsonObject.get("amount").getAsDouble();
                
                // Идемпотентная обработка
                boolean success = paymentService.processPayment(orderId, userId, amount, transactionId);
                
                System.out.println("Payment processed for order " + orderId + 
                                  ", result: " + (success ? "success" : "failed"));
            } else {
                System.out.println("Unknown event type: " + eventType + ". Skipping processing.");
            }
            
            // Отмечаем сообщение как обработанное
            inboxRepository.markAsProcessed(inboxMessage.getId());
            
        } catch (Exception e) {
            System.err.println("Error processing incoming message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Проверка соответствия сообщения схеме
     */
    private boolean validateMessageSchema(JsonObject jsonObject) {
        // Проверяем обязательные поля для запроса на оплату
        return jsonObject.has("eventType") && 
               jsonObject.has("orderId") && 
               jsonObject.has("userId") && 
               jsonObject.has("amount") && 
               jsonObject.has("transactionId");
    }
}