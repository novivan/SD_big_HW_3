package com.example.outbox;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.example.messaging.MessageBroker;
import com.example.messaging.MessageSchema;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Сервис для обработки исходящих сообщений с поддержкой messageId
 */
public class OutboxService {
    private final OutboxRepository outboxRepository;
    private final MessageBroker messageBroker;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Gson gson = new Gson();
    
    public OutboxService(OutboxRepository outboxRepository, MessageBroker messageBroker) {
        this.outboxRepository = outboxRepository;
        this.messageBroker = messageBroker;
    }
    
    /**
     * Сохранить сообщение в Outbox
     */
    public void saveMessage(OutboxMessage message) {
        outboxRepository.save(message);
    }
    
    /**
     * Запустить обработку исходящих сообщений
     */
    public void startProcessing() {
        scheduler.scheduleAtFixedRate(this::processOutboxMessages, 0, 3, TimeUnit.SECONDS);
    }
    
    /**
     * Обработать все необработанные сообщения в Outbox
     */
    private void processOutboxMessages() {
        outboxRepository.findUnprocessedMessages().forEach(message -> {
            try {
                // Определение очереди в зависимости от типа события
                String queueName = null;
                if ("PAYMENT_RESULT".equals(message.getEventType())) {
                    queueName = MessageSchema.ORDER_PAYMENT_RESULTS_QUEUE;
                }
                
                if (queueName != null) {
                    // Убеждаемся, что в payload есть messageId
                    String payload = ensureMessageIdInPayload(message);
                    
                    // Отправляем сообщение
                    messageBroker.sendMessage(queueName, payload);
                    outboxRepository.markAsProcessed(message.getId());
                    System.out.println("Successfully processed outbox message: " + message.getId() + 
                                     ", messageId: " + message.getMessageId() +
                                     ", correlationId: " + message.getCorrelationId());
                }
            } catch (IOException e) {
                System.err.println("Error processing outbox message: " + e.getMessage());
            }
        });
    }
    
    /**
     * Убедиться, что в payload есть messageId для идентификации сообщений
     */
    private String ensureMessageIdInPayload(OutboxMessage message) {
        try {
            // Парсим JSON строку
            JsonObject jsonPayload = JsonParser.parseString(message.getPayload()).getAsJsonObject();
            
            // Проверяем наличие messageId
            if (!jsonPayload.has("messageId")) {
                jsonPayload.addProperty("messageId", message.getMessageId());
            }
            
            // Проверяем наличие eventType
            if (!jsonPayload.has("eventType")) {
                // Определяем тип события в зависимости от наличия поля success
                if (jsonPayload.has("success")) {
                    boolean success = jsonPayload.get("success").getAsBoolean();
                    String eventType = success ? 
                        MessageSchema.PaymentResultType.PAYMENT_COMPLETED : 
                        MessageSchema.PaymentResultType.PAYMENT_FAILED;
                    jsonPayload.addProperty("eventType", eventType);
                }
            }
            
            return gson.toJson(jsonPayload);
        } catch (Exception e) {
            // В случае проблем с парсингом, возвращаем исходный payload
            System.err.println("Error ensuring messageId in payload: " + e.getMessage());
            return message.getPayload();
        }
    }
    
    /**
     * Остановить обработку сообщений
     */
    public void stopProcessing() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}