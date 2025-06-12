package com.example.inbox;

import java.time.LocalDateTime;

/**
 * Сообщение во входящей очереди (Inbox) с поддержкой идемпотентности
 */
public class InboxMessage {
    private final String id;
    private final String messageType;
    private final String payload;
    private final String transactionId;
    private final LocalDateTime receivedAt;
    private boolean processed;
    private LocalDateTime processedAt;
    
    public InboxMessage(String id, String messageType, String payload, String transactionId) {
        this.id = id;
        this.messageType = messageType;
        this.payload = payload;
        this.transactionId = transactionId;
        this.receivedAt = LocalDateTime.now();
        this.processed = false;
    }
    
    // Геттеры
    public String getId() {
        return id;
    }
    
    public String getMessageType() {
        return messageType;
    }
    
    public String getPayload() {
        return payload;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public LocalDateTime getReceivedAt() {
        return receivedAt;
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