package com.example.service;

import com.example.Payment;
import com.example.PaymentStatus;
import com.example.outbox.OutboxMessage;
import com.example.outbox.OutboxService;
import com.example.repository.PaymentRepository;
import com.google.gson.Gson;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервис для обработки платежей с гарантией exactly-once семантики
 */
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final AccountService accountService;
    private final OutboxService outboxService;
    private final Gson gson = new Gson();
    
    public PaymentService(
            PaymentRepository paymentRepository,
            AccountService accountService,
            OutboxService outboxService) {
        this.paymentRepository = paymentRepository;
        this.accountService = accountService;
        this.outboxService = outboxService;
    }
    
    /**
     * Обработать платеж с гарантией идемпотентности
     * @param orderId ID заказа
     * @param userId ID пользователя
     * @param amount Сумма платежа
     * @param transactionId Уникальный идентификатор транзакции
     * @return true если платеж успешен, false если платеж не удался
     */
    public boolean processPayment(int orderId, int userId, double amount, String transactionId) {
        // Проверка на повторную обработку платежа (идемпотентность)
        Optional<Payment> existingPayment = paymentRepository.findByTransactionId(transactionId);
        if (existingPayment.isPresent()) {
            // Если платеж уже обрабатывался, возвращаем результат предыдущей обработки
            Payment payment = existingPayment.get();
            System.out.println("Payment with transactionId " + transactionId + " already processed. Status: " + payment.getStatus());
            return payment.getStatus() == PaymentStatus.COMPLETED;
        }
        
        try {
            // Создаем запись о платеже в статусе PENDING
            Payment payment = new Payment(orderId, amount);
            payment.setTransactionId(transactionId); // Устанавливаем переданный transactionId вместо генерации нового
            paymentRepository.save(payment);
            
            // Проверяем наличие счета у пользователя
            if (!accountService.hasAccount(userId)) {
                // Счет не найден, помечаем платеж как неуспешный
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                
                // Отправляем результат в Order Service через Outbox
                sendPaymentResultMessage(payment, false);
                
                return false;
            }
            
            // Пытаемся списать средства со счета
            boolean withdrawSuccess = accountService.withdrawFunds(userId, amount);
            
            if (withdrawSuccess) {
                // Успешное списание средств
                payment.setStatus(PaymentStatus.COMPLETED);
                paymentRepository.save(payment);
                
                // Отправляем результат в Order Service через Outbox
                sendPaymentResultMessage(payment, true);
                
                return true;
            } else {
                // Недостаточно средств или другая ошибка списания
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                
                // Отправляем результат в Order Service через Outbox
                sendPaymentResultMessage(payment, false);
                
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error processing payment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Отправить сообщение о результате платежа через Outbox
     */
    private void sendPaymentResultMessage(Payment payment, boolean success) {
        // Создаем уникальный messageId для исходящего сообщения
        String messageId = UUID.randomUUID().toString();
        
        Map<String, Object> paymentResult = Map.of(
                "messageId", messageId, // Добавляем messageId в сообщение
                "orderId", payment.getOrderId(),
                "amount", payment.getAmount(),
                "transactionId", payment.getTransactionId(),
                "success", success,
                "timestamp", System.currentTimeMillis()
        );
        
        String payload = gson.toJson(paymentResult);
        
        OutboxMessage outboxMessage = new OutboxMessage(
                "Payment",
                "PAYMENT_RESULT",
                payload,
                payment.getTransactionId(),
                messageId // Устанавливаем messageId
        );
        
        outboxService.saveMessage(outboxMessage);
    }
    
    /**
     * Получить историю платежей для заказа
     */
    public List<Payment> getPaymentsForOrder(int orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
}