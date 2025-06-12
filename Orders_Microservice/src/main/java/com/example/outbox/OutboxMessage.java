package com.example.outbox;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сообщение в очереди Outbox.
 */
public class OutboxMessage {
    private final String id;
    private final String aggregateId;
    private final String aggregateType;
    private final String eventType;
    private final String payload;
    private final LocalDateTime createdAt;
    private boolean processed;
    private LocalDateTime processedAt;

    public OutboxMessage(String aggregateId, String aggregateType, String eventType, String payload) {
        this.id = UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = LocalDateTime.now();
        this.processed = false;
    }

    public String getId() {
        return id;
    }

    public String getAggregateIg() {
        return aggregateId;
    }

    public String getAggregatedType() {
        return aggregateType;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
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

    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
    }
}
