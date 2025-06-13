package com.example.outbox;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.example.messaging.MessageBroker;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class OutboxServiceTest {

    @Mock
    private OutboxRepository outboxRepository;
    
    @Mock
    private MessageBroker messageBroker;
    
    private OutboxService outboxService;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        outboxService = new OutboxService(outboxRepository, messageBroker);
    }
    
    @Test
    public void testSaveMessage() {
        // Create an OutboxMessage directly
        OutboxMessage message = mock(OutboxMessage.class);
        
        // Call the saveMessage method
        outboxService.saveMessage(message);
        
        // Verify the repository was called
        verify(outboxRepository).save(message);
    }
    
    @Test
    public void testProcessOutboxMessages() throws Exception {
        // Create a list of unprocessed messages
        List<OutboxMessage> messages = new ArrayList<>();
        OutboxMessage message = mock(OutboxMessage.class);
        when(message.getId()).thenReturn("msg-123");
        
        // Instead of getQueueName, we'll mock getEventType
        // In OutboxService, "PAYMENT_RESULT" eventType uses ORDER_PAYMENT_RESULTS_QUEUE
        when(message.getEventType()).thenReturn("PAYMENT_RESULT");
        when(message.getPayload()).thenReturn("{\"key\":\"value\"}");
        when(message.getMessageId()).thenReturn(UUID.randomUUID().toString());
        
        messages.add(message);
        
        // Mock repository to return our test messages
        when(outboxRepository.findUnprocessedMessages()).thenReturn(messages);
        
        // Use reflection to access the private method
        java.lang.reflect.Method method = OutboxService.class.getDeclaredMethod("processOutboxMessages");
        method.setAccessible(true);
        method.invoke(outboxService);
        
        // Verify interactions - messageBroker.sendMessage is called with the right parameters
        verify(messageBroker).sendMessage(anyString(), anyString());
        verify(outboxRepository).markAsProcessed(anyString());
    }
    
    @Test
    public void testEnsureMessageIdInJson() {
        // Create a simple JSON payload
        String payload = "{\"data\":\"test\"}";
        
        // Create a mocked OutboxMessage with this payload
        OutboxMessage message = mock(OutboxMessage.class);
        String messageId = "test-message-id";
        when(message.getPayload()).thenReturn(payload);
        when(message.getMessageId()).thenReturn(messageId);
        
        // Use reflection to access the private method
        try {
            java.lang.reflect.Method method = OutboxService.class.getDeclaredMethod("ensureMessageIdInPayload", OutboxMessage.class);
            method.setAccessible(true);
            String resultPayload = (String) method.invoke(outboxService, message);
            
            // Parse the result JSON
            JsonObject jsonResult = JsonParser.parseString(resultPayload).getAsJsonObject();
            
            // Verify messageId was added
            assertTrue(jsonResult.has("messageId"));
            assertEquals(messageId, jsonResult.get("messageId").getAsString());
            
        } catch (Exception e) {
            fail("Failed to test ensureMessageIdInPayload: " + e.getMessage());
        }
    }
}
