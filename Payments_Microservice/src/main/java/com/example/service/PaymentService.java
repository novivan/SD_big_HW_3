package com.example.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.Payment;
import com.example.PaymentStatus;
import com.example.outbox.OutboxMessage;
import com.example.outbox.OutboxService;
import com.example.repository.PaymentRepository;
import com.google.gson.Gson;

/**
 * Сервис для обработки платежей
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
     * Обработать платеж
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
            return existingPayment.get().getStatus() == PaymentStatus.COMPLETED;
        }
        
        try {
            // Создаем запись о платеже в статусе PENDING
            Payment payment = new Payment(orderId, amount);
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
            return false;
        }
    }
    
    /**
     * Отправить сообщение о результате платежа через Outbox
     */
    private void sendPaymentResultMessage(Payment payment, boolean success) {
        Map<String, Object> paymentResult = Map.of(
                "orderId", payment.getOrderId(),
                "amount", payment.getAmount(),
                "transactionId", payment.getTransactionId(),
                "success", success
        );
        
        String payload = gson.toJson(paymentResult);
        
        OutboxMessage outboxMessage = new OutboxMessage(
                "Payment",
                "PAYMENT_RESULT",
                payload,
                payment.getTransactionId()
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