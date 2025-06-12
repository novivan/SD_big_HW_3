package com.example.outbox;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Репозиторий для хранения исходящих сообщений
 */
public class OutboxRepository {
    private final Map<String, OutboxMessage> messages = new ConcurrentHashMap<>();
    
    /**
     * Сохранить исходящее сообщение
     */
    public void save(OutboxMessage message) {
        messages.put(message.getId(), message);
    }
    
    /**
     * Найти все необработанные сообщения
     */
    public List<OutboxMessage> findUnprocessedMessages() {
        return messages.values().stream()
                .filter(message -> !message.isProcessed())
                .collect(Collectors.toList());
    }
    
    /**
     * Отметить сообщение как обработанное
     */
    public void markAsProcessed(String messageId) {
        if (messages.containsKey(messageId)) {
            messages.get(messageId).markAsProcessed();
        }
    }
}