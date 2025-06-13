package com.example.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.example.Payment;
import com.example.PaymentStatus;
import com.example.TestUtils;
import com.example.outbox.OutboxMessage;
import com.example.outbox.OutboxService;
import com.example.repository.PaymentRepository;

public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private OutboxService outboxService;

    private PaymentService paymentService;
    
    private ExecutorService executorService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        paymentService = new PaymentService(paymentRepository, accountService, outboxService);
        
        // Initialize executor service
        executorService = Executors.newSingleThreadExecutor();
    }
    
    @After
    public void tearDown() {
        // Use the utility class for cleanup
        TestUtils.shutdownExecutor(executorService, 2);
    }

    @Test
    public void testProcessPaymentSuccessful() {
        // Подготовка данных
        int orderId = 1;
        int userId = 1;
        double amount = 100.0;
        String transactionId = "tx-123";

        // Настройка моков
        when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());
        when(accountService.hasAccount(userId)).thenReturn(true);
        when(accountService.withdrawFunds(userId, amount)).thenReturn(true);
        
        // Используем реальный объект Payment
        Payment payment = new Payment(orderId, amount);
        payment.setTransactionId(transactionId);
        
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // Вызов тестируемого метода
        boolean result = paymentService.processPayment(orderId, userId, amount, transactionId);

        // Проверки
        assertTrue(result);

        // Проверка вызова методов
        verify(paymentRepository).findByTransactionId(transactionId);
        verify(accountService).hasAccount(userId);
        verify(accountService).withdrawFunds(userId, amount);
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(outboxService).saveMessage(any(OutboxMessage.class));
    }

    @Test
    public void testProcessPaymentAlreadyProcessed() {
        // Подготовка данных
        int orderId = 1;
        int userId = 1;
        double amount = 100.0;
        String transactionId = "tx-123";

        // Создаем платеж со статусом COMPLETED
        Payment existingPayment = new Payment(orderId, amount);
        existingPayment.setStatus(PaymentStatus.COMPLETED);
        existingPayment.setTransactionId(transactionId);

        // Настройка моков
        when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(existingPayment));

        // Вызов тестируемого метода
        boolean result = paymentService.processPayment(orderId, userId, amount, transactionId);

        // Проверки
        assertTrue(result); // Должен вернуть true, так как платеж уже завершен

        // Проверка, что методы не вызывались
        verify(paymentRepository).findByTransactionId(transactionId);
        verify(accountService, never()).withdrawFunds(anyInt(), anyDouble());
        verify(outboxService, never()).saveMessage(any(OutboxMessage.class));
    }

    @Test
    public void testProcessPaymentNoAccount() {
        // Подготовка данных
        int orderId = 1;
        int userId = 1;
        double amount = 100.0;
        String transactionId = "tx-123";

        // Создаем платеж для имитации сохранения
        Payment payment = new Payment(orderId, amount);
        payment.setTransactionId(transactionId);

        // Настройка моков
        when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());
        when(accountService.hasAccount(userId)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // Вызов тестируемого метода
        boolean result = paymentService.processPayment(orderId, userId, amount, transactionId);

        // Проверки
        assertFalse(result);

        // Проверка вызова методов
        verify(paymentRepository).findByTransactionId(transactionId);
        verify(accountService).hasAccount(userId);
        verify(accountService, never()).withdrawFunds(anyInt(), anyDouble());
        verify(paymentRepository, times(2)).save(any(Payment.class)); // Один раз для создания, один раз для обновления статуса
        verify(outboxService).saveMessage(any(OutboxMessage.class));
    }

    @Test
    public void testProcessPaymentInsufficientFunds() {
        // Подготовка данных
        int orderId = 1;
        int userId = 1;
        double amount = 100.0;
        String transactionId = "tx-123";

        // Создаем платеж для имитации сохранения
        Payment payment = new Payment(orderId, amount);
        payment.setTransactionId(transactionId);

        // Настройка моков
        when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());
        when(accountService.hasAccount(userId)).thenReturn(true);
        when(accountService.withdrawFunds(userId, amount)).thenReturn(false); // Недостаточно средств
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // Вызов тестируемого метода
        boolean result = paymentService.processPayment(orderId, userId, amount, transactionId);

        // Проверки
        assertFalse(result);

        // Проверка вызова методов
        verify(paymentRepository).findByTransactionId(transactionId);
        verify(accountService).hasAccount(userId);
        verify(accountService).withdrawFunds(userId, amount);
        verify(paymentRepository, times(2)).save(any(Payment.class)); // Один раз для создания, один раз для обновления статуса
        verify(outboxService).saveMessage(any(OutboxMessage.class));
    }

    @Test
    public void testGetPaymentsForOrder() {
        // Подготовка данных
        int orderId = 1;
        List<Payment> mockPayments = new ArrayList<>();
        
        // Создаем реальные объекты Payment
        Payment payment1 = new Payment(orderId, 50.0);
        Payment payment2 = new Payment(orderId, 25.0);
        mockPayments.add(payment1);
        mockPayments.add(payment2);

        // Настройка моков
        when(paymentRepository.findByOrderId(orderId)).thenReturn(mockPayments);

        // Вызов тестируемого метода
        List<Payment> result = paymentService.getPaymentsForOrder(orderId);

        // Проверки
        assertEquals(2, result.size());
        assertEquals(50.0, result.get(0).getAmount(), 0.001);
        assertEquals(25.0, result.get(1).getAmount(), 0.001);

        // Проверка вызова методов
        verify(paymentRepository).findByOrderId(orderId);
    }
}
