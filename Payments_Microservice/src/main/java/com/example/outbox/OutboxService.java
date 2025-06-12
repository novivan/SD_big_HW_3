package com.example.outbox;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.example.messaging.MessageBroker;

/**
 * Сервис для обработки исходящих сообщений
 */
public class OutboxService {
    private static final String ORDER_PAYMENT_RESULT_QUEUE = "order_payment_results";
    
    private final OutboxRepository outboxRepository;
    private final MessageBroker messageBroker;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
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
        scheduler.scheduleAtFixedRate(this::processOutboxMessages, 0, 5, TimeUnit.SECONDS);
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
                    queueName = ORDER_PAYMENT_RESULT_QUEUE;
                }
                
                if (queueName != null) {
                    messageBroker.sendMessage(queueName, message.getPayload());
                    outboxRepository.markAsProcessed(message.getId());
                    System.out.println("Successfully processed outbox message: " + message.getId());
                }
            } catch (IOException e) {
                System.err.println("Error processing outbox message: " + e.getMessage());
            }
        });
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