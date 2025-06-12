package com.example.outbox;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.example.messaging.MessageBroker;
import com.google.gson.Gson;

public class OutboxService {
    private static final String PAYMENT_QUEUE = "payment_requests";

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
        scheduler.scheduleAtFixedRate(this::processOutboxMessages, 0, 5, TimeUnit.SECONDS);
    }

    private void processOutboxMessages() {
        outboxRepository.findUnprocessedMessages().forEach(message -> {
            try {
                if ("ORDER_CREATED".equals(message.getEventType())) {
                    messageBroker.sendMessage(PAYMENT_QUEUE, message.getPayload());

                    outboxRepository.markAsProcessed(message.getId());
                    System.out.println("Successfully processed outbox message: " + message.getId());
                }
            } catch (IOException e) {
                System.err.println("Error processing outbox message: " + e.getMessage());
            }
        });
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