package com.example.inbox;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.example.messaging.MessageSchema;
import com.example.service.PaymentService;
import com.google.gson.JsonObject;

public class InboxServiceTest {

    @Mock
    private InboxRepository inboxRepository;
    
    @Mock
    private PaymentService paymentService;
    
    private InboxService inboxService;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        inboxService = new InboxService(inboxRepository, paymentService);
    }
    
    @Test
    public void testProcessPaymentRequest_Valid() {
        // Создаем тестовый JSON с запросом на оплату
        String messageId = "msg-123";
        String transactionId = "tx-456";
        String validJson = createValidPaymentRequestJson(transactionId);
        
        // Настраиваем моки для репозитория
        when(inboxRepository.existsById(messageId)).thenReturn(false);
        when(inboxRepository.isProcessed(transactionId)).thenReturn(false);
        when(paymentService.processPayment(anyInt(), anyInt(), anyDouble(), anyString())).thenReturn(true);
        
        // Напрямую вызываем метод processMessage, а не через рефлексию
        inboxService.processMessage(validJson);
        
        // Проверяем вызовы
        verify(paymentService).processPayment(eq(1), eq(1), eq(100.0), eq(transactionId));
    }
    
    @Test
    public void testProcessInvalidJson() {
        // Настройка невалидного JSON
        String invalidJson = "not a valid json";
        
        // Напрямую вызываем метод processMessage
        inboxService.processMessage(invalidJson);
        
        // Проверяем, что обработки не было
        verify(paymentService, never()).processPayment(anyInt(), anyInt(), anyDouble(), anyString());
    }
    
    @Test
    public void testProcessAlreadyProcessedMessage() {
        // Настройка сообщения, которое уже обработано
        String messageId = "msg-123";
        String transactionId = "tx-456";
        String validJson = createValidPaymentRequestJson(transactionId);
        
        // Настраиваем мок для existsById чтобы вернул true
        when(inboxRepository.existsById(anyString())).thenReturn(true);
        
        // Напрямую вызываем метод processMessage
        inboxService.processMessage(validJson);
        
        // Проверяем, что платеж не обрабатывался
        verify(paymentService, never()).processPayment(anyInt(), anyInt(), anyDouble(), anyString());
    }
    
    // Вспомогательный метод для создания валидного JSON
    private String createValidPaymentRequestJson(String transactionId) {
        JsonObject json = new JsonObject();
        json.addProperty("eventType", MessageSchema.PaymentRequestType.PROCESS_PAYMENT);
        json.addProperty("orderId", 1);
        json.addProperty("userId", 1);
        json.addProperty("amount", 100.0);
        json.addProperty("transactionId", transactionId);
        json.addProperty("messageId", "msg-123"); // Добавляем messageId для теста
        return json.toString();
    }
}
