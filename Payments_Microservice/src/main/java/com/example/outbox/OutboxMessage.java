package com.example.outbox;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сообщение в очереди Outbox с поддержкой уникальных идентификаторов
 */
public class OutboxMessage {
    private final String id; // Внутренний ID для хранения
    private final String aggregateType;
    private final String eventType;
    private final String payload;
    private final String correlationId; // TransactionId
    private final String messageId; // Уникальный ID сообщения
    private final LocalDateTime createdAt;
    private boolean processed;
    private LocalDateTime processedAt;
    
    public OutboxMessage(String aggregateType, String eventType, String payload, String correlationId) {
        this.id = UUID.randomUUID().toString();
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.payload = payload;
        this.correlationId = correlationId;
        this.messageId = UUID.randomUUID().toString(); // Генерируем новый messageId
        this.createdAt = LocalDateTime.now();
        this.processed = false;
    }
    
    public OutboxMessage(String aggregateType, String eventType, String payload, String correlationId, String messageId) {
        this.id = UUID.randomUUID().toString();
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.payload = payload;
        this.correlationId = correlationId;
        this.messageId = messageId; // Используем предоставленный messageId
        this.createdAt = LocalDateTime.now();
        this.processed = false;
    }
    
    // Геттеры
    public String getId() {
        return id;
    }
    
    public String getAggregateType() {
        return aggregateType;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public String getPayload() {
        return payload;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public boolean isProcessed() {
        return processed;
    }
    
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    
    // Метод для изменения статуса обработки
    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
    }
}