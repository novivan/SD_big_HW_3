package com.example.messaging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class MessageBroker {
    private final ConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    
    // Для отслеживания отправленных сообщений (предотвращение дубликатов)
    private final Map<String, Long> sentMessages = new ConcurrentHashMap<>();
    
    public MessageBroker() {
        this("localhost", 5672);
    }
    
    public MessageBroker(String host, int port) {
        factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
    }

    public void connect() throws IOException, TimeoutException {
        connection = factory.newConnection();
        channel = connection.createChannel();
    }
    
    /**
     * Отправка сообщения с гарантированной доставкой и идемпотентностью
     */
    public void sendMessage(String queueName, String message) throws IOException {
        // Проверяем, есть ли messageId в сообщении
        String messageId = extractMessageId(message);
        if (messageId == null) {
            // Если нет, то добавляем свой messageId
            messageId = UUID.randomUUID().toString();
            message = addMessageId(message, messageId);
        }
        
        // Проверка, не отправляли ли мы это сообщение недавно
        if (sentMessages.containsKey(messageId)) {
            long lastSent = sentMessages.get(messageId);
            if (System.currentTimeMillis() - lastSent < 60000) { // 1 минута
                System.out.println("Skipping duplicate message send: " + messageId);
                return;
            }
        }
        
        // Объявляем очередь с поддержкой долговечности
        channel.queueDeclare(queueName, true, false, false, null);
        
        // Установка свойств сообщения
        Map<String, Object> headers = new HashMap<>();
        headers.put("messageId", messageId);
        
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .deliveryMode(2) // persistent
                .messageId(messageId) // Установка messageId в свойствах сообщения
                .headers(headers)
                .build();
        
        // Отправка сообщения
        channel.basicPublish("", queueName, properties, message.getBytes(StandardCharsets.UTF_8));
        
        // Запоминаем, что это сообщение было отправлено
        sentMessages.put(messageId, System.currentTimeMillis());
        
        // Ограничиваем размер кэша
        if (sentMessages.size() > 1000) {
            cleanupSentMessages();
        }
        
        System.out.println(" [x] Sent '" + message + "' with messageId '" + messageId + "' to queue '" + queueName + "'");
    }
    
    /**
     * Получение сообщений из очереди с дедупликацией
     */
    public void receiveMessages(String queueName, Consumer<String> messageHandler) throws IOException {
        try {
            channel.queueDeclare(queueName, true, false, false, null);
            
            channel.basicQos(1);
            
            System.out.println(" [*] Waiting for messages from queue '" + queueName + "'");
            
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String messageId = delivery.getProperties().getMessageId();
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                
                try {
                    System.out.println(" [x] Received message with ID '" + messageId + "' from queue '" + queueName + "'");
                    messageHandler.accept(message);
                    
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } catch (Exception e) {
                    System.err.println("Error processing message: " + e.getMessage());
                    e.printStackTrace();
                    
                    try {
                        // Отправляем NACK с requeue=false при критической ошибке обработки
                        // чтобы избежать цикличной обработки проблемных сообщений
                        boolean requeue = !(e instanceof RuntimeException);
                        channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, requeue);
                    } catch (IOException ioException) {
                        System.err.println("Failed to nack message: " + ioException.getMessage());
                    }
                }
            };
            
            channel.basicConsume(queueName, false, deliverCallback, consumerTag -> { });
        } catch (Exception e) {
            System.err.println("Error setting up message consumer: " + e.getMessage());
            throw new IOException("Failed to set up message consumer", e);
        }
    }
    
    /**
     * Извлечь messageId из JSON сообщения
     */
    private String extractMessageId(String message) {
        try {
            JsonObject jsonObject = JsonParser.parseString(message).getAsJsonObject();
            if (jsonObject.has("messageId")) {
                return jsonObject.get("messageId").getAsString();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Добавить messageId в JSON сообщение
     */
    private String addMessageId(String message, String messageId) {
        try {
            JsonObject jsonObject = JsonParser.parseString(message).getAsJsonObject();
            jsonObject.addProperty("messageId", messageId);
            return jsonObject.toString();
        } catch (Exception e) {
            // Если не можем разобрать JSON, вернем исходное сообщение
            return message;
        }
    }
    
    /**
     * Очистка устаревших записей об отправленных сообщениях
     */
    private void cleanupSentMessages() {
        long cutoffTime = System.currentTimeMillis() - 3600000; // 1 час
        sentMessages.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
    }
    
    public void close() throws IOException {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
            System.out.println("RabbitMQ connection closed");
        } catch (TimeoutException e) {
            throw new IOException("Timeout while closing RabbitMQ connections", e);
        }
    }
}