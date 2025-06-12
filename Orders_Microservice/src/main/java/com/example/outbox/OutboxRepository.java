package com.example.outbox;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OutboxRepository {
    private final Map<String, OutboxMessage> messages = new ConcurrentHashMap<>();


    public void save(OutboxMessage message) {
        messages.put(message.getId(), message);
    }

    public List<OutboxMessage>  findUnprocessedMessages() {
        return messages.values().stream()
                .filter(message -> !message.isProcessed())
                .collect(Collectors.toList());
    }


    public void markAsProcessed(String messageId) {
        if (messages.containsKey(messageId)) {
            messages.get(messageId).markAsProcessed();
        }
    }
}
