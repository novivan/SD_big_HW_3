package com.example.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.Payment;
import com.example.PaymentStatus;
import com.example.messaging.MessageSchema;
import com.example.outbox.OutboxMessage;
import com.example.outbox.OutboxService;
import com.example.repository.PaymentRepository;
import com.google.gson.Gson;

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
            
            String failureReason = null;
            boolean success = false;
            
            // Проверяем наличие счета у пользователя
            if (!accountService.hasAccount(userId)) {
                // Счет не найден, помечаем платеж как неуспешный
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                failureReason = "Account not found for user " + userId;
            } else {
                // Пытаемся списать средства со счета
                success = accountService.withdrawFunds(userId, amount);
                
                if (success) {
                    // Успешное списание средств
                    payment.setStatus(PaymentStatus.COMPLETED);
                    paymentRepository.save(payment);
                } else {
                    // Недостаточно средств или другая ошибка списания
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                    failureReason = "Insufficient funds";
                }
            }
            
            // Отправляем результат в Order Service через Outbox
            sendPaymentResultMessage(payment, success, failureReason);
            
            return success;
        } catch (Exception e) {
            System.err.println("Error processing payment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Отправить сообщение о результате платежа через Outbox
     */
    private void sendPaymentResultMessage(Payment payment, boolean success, String failureReason) {
        // Создаем объект результата платежа по схеме
        MessageSchema.PaymentResult paymentResult = new MessageSchema.PaymentResult(
            payment.getOrderId(),
            payment.getAmount(),
            success,
            payment.getTransactionId(),
            failureReason
        );
        
        // Добавляем уникальный messageId
        paymentResult.messageId = UUID.randomUUID().toString();
        
        // Сериализуем в JSON
        String payload = gson.toJson(paymentResult);
        
        // Сохраняем сообщение в Outbox
        OutboxMessage outboxMessage = new OutboxMessage(
                "Payment",
                "PAYMENT_RESULT",
                payload,
                payment.getTransactionId(),
                paymentResult.messageId
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