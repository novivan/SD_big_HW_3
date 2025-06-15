package com.example.messaging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class MessageBroker {
    private final ConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    
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
    
    public void sendMessage(String queueName, String message) throws IOException {
        System.out.println("Declaring queue: " + queueName);
        channel.queueDeclare(queueName, true, false, false, null);
        
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .deliveryMode(2) // persistent
                .build();
        
        System.out.println("Sending message to queue " + queueName + ": " + message);
        channel.basicPublish("", queueName, properties, message.getBytes(StandardCharsets.UTF_8));
        System.out.println(" [x] Sent '" + message + "' to queue '" + queueName + "'");
    }

    public void receiveMessages(String queueName, Consumer<String> messageHandler) throws IOException {
        try {
            System.out.println("Declaring queue for receiving: " + queueName);
            channel.queueDeclare(queueName, true, false, false, null);
            
            channel.basicQos(1);
            
            System.out.println(" [*] Waiting for messages from queue '" + queueName + "'");
            
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                
                try {
                    System.out.println(" [x] Received '" + message + "' from queue '" + queueName + "'");
                    messageHandler.accept(message);
                    
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    System.out.println("Message acknowledged");
                } catch (Exception e) {
                    System.err.println("Error processing message: " + e.getMessage());
                    
                    try {
                        channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
                        System.out.println("Message nacked and requeued");
                    } catch (IOException ioException) {
                        System.err.println("Failed to nack message: " + ioException.getMessage());
                    }
                }
            };
            
            channel.basicConsume(queueName, false, deliverCallback, consumerTag -> { });
            System.out.println("Started consuming messages from queue: " + queueName);
        } catch (Exception e) {
            System.err.println("Error setting up message consumer: " + e.getMessage());
            throw new IOException("Failed to set up message consumer", e);
        }
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