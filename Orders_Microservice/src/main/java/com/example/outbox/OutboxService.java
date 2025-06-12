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

public class OutboxService {
    private final OutboxRepository outboxRepository;
    private final MessageBroker messageBroker;
    private final Gson gson = new Gson();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public OutboxService(OutboxRepository outboxRepository, MessageBroker messageBroker) {
        this.outboxRepository = outboxRepository;
        this.messageBroker = messageBroker;
    }

    public void saveMessage(OutboxMessage message) {
        outboxRepository.save(message);
    }

    public void startProcessing() {
        scheduler.scheduleAtFixedRate(this::processOutboxMessages, 0, 3, TimeUnit.SECONDS);
    }

    private void processOutboxMessages() {
        outboxRepository.findUnprocessedMessages().forEach(message -> {
            try {
                if ("ORDER_CREATED".equals(message.getEventType())) {
                    // Убедимся, что в payload есть messageId
                    String payload = ensureMessageIdInPayload(message);
                    
                    // Отправляем сообщение в очередь запросов на оплату
                    messageBroker.sendMessage(MessageSchema.PAYMENT_REQUESTS_QUEUE, payload);
                    outboxRepository.markAsProcessed(message.getId());
                    System.out.println("Successfully processed outbox message: " + message.getId() + 
                                     ", messageId: " + message.getMessageId());
                }
            } catch (IOException e) {
                System.err.println("Error processing outbox message: " + e.getMessage());
                e.printStackTrace();
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
            
            return gson.toJson(jsonPayload);
        } catch (Exception e) {
            // В случае проблем с парсингом, возвращаем исходный payload
            System.err.println("Error ensuring messageId in payload: " + e.getMessage());
            return message.getPayload();
        }
    }

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