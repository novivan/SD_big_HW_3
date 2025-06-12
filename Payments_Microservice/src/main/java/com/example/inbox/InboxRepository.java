package com.example.inbox;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Репозиторий для хранения входящих сообщений
 */
public class InboxRepository {
    private final Map<String, InboxMessage> messages = new ConcurrentHashMap<>();
    
    /**
     * Сохранить входящее сообщение
     */
    public void save(InboxMessage message) {
        messages.put(message.getId(), message);
    }
    
    /**
     * Найти сообщение по ID
     */
    public Optional<InboxMessage> findById(String id) {
        return Optional.ofNullable(messages.get(id));
    }
    
    /**
     * Найти сообщение по transactionId
     */
    public Optional<InboxMessage> findByTransactionId(String transactionId) {
        return messages.values().stream()
                .filter(message -> message.getTransactionId().equals(transactionId))
                .findFirst();
    }
    
    /**
     * Получить необработанные сообщения
     */
    public List<InboxMessage> findUnprocessedMessages() {
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
    
    /**
     * Проверить, было ли сообщение с данным transactionId обработано
     */
    public boolean isProcessed(String transactionId) {
        return messages.values().stream()
                .anyMatch(message -> message.getTransactionId().equals(transactionId) && message.isProcessed());
    }
}