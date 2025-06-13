package com.example.messaging;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class MessageBrokerTest {

    @Mock
    private ConnectionFactory connectionFactory;
    
    @Mock
    private Connection connection;
    
    @Mock
    private Channel channel;
    
    private MessageBroker messageBroker;
    
    @Before
    public void setUp() throws IOException, TimeoutException {
        MockitoAnnotations.initMocks(this);
        
        // Mock the connection creation
        when(connectionFactory.newConnection()).thenReturn(connection);
        when(connection.createChannel()).thenReturn(channel);
        
        // Create message broker with mocked factory
        messageBroker = new MessageBroker();
        
        // Inject the mocked connection factory
        try {
            java.lang.reflect.Field factoryField = MessageBroker.class.getDeclaredField("factory");
            factoryField.setAccessible(true);
            factoryField.set(messageBroker, connectionFactory);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to inject mocked connection factory: " + e.getMessage());
        }
        
        // Вызов connect(), чтобы канал не был null
        try {
            messageBroker.connect();
        } catch (Exception e) {
            fail("Failed to connect: " + e.getMessage());
        }
        
        // Mock channel.queueDeclare and channel.basicPublish
        doAnswer(invocation -> null).when(channel).queueDeclare(anyString(), anyBoolean(), anyBoolean(), anyBoolean(), anyMap());
        doAnswer(invocation -> null).when(channel).basicPublish(anyString(), anyString(), any(BasicProperties.class), any(byte[].class));
    }
    
    @Test
    public void testSendMessage() throws IOException {
        // Test data
        String queueName = "test-queue";
        String message = "{\"key\":\"value\"}";
        
        // Invoke the method
        messageBroker.sendMessage(queueName, message);
        
        // Verify queue was declared and message was published
        verify(channel).queueDeclare(eq(queueName), eq(true), eq(false), eq(false), isNull());
        verify(channel).basicPublish(anyString(), eq(queueName), any(BasicProperties.class), any(byte[].class));
    }
}
