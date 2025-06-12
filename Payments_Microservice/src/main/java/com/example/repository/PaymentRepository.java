package com.example.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.example.Payment;
import com.example.PaymentStatus;

/**
 * In-memory хранилище платежей
 */
public class PaymentRepository {
    private final Map<Integer, Payment> payments = new ConcurrentHashMap<>();
    
    /**
     * Сохранить платеж
     */
    public Payment save(Payment payment) {
        payments.put(payment.getId(), payment);
        return payment;
    }
    
    /**
     * Найти платеж по ID
     */
    public Optional<Payment> findById(int id) {
        return Optional.ofNullable(payments.get(id));
    }
    
    /**
     * Найти все платежи по ID заказа
     */
    public List<Payment> findByOrderId(int orderId) {
        return payments.values().stream()
                .filter(payment -> payment.getOrderId() == orderId)
                .collect(Collectors.toList());
    }
    
    /**
     * Найти платеж по transactionId
     */
    public Optional<Payment> findByTransactionId(String transactionId) {
        return payments.values().stream()
                .filter(payment -> payment.getTransactionId().equals(transactionId))
                .findFirst();
    }
    
    /**
     * Получить список всех платежей
     */
    public List<Payment> findAll() {
        return new ArrayList<>(payments.values());
    }
    
    /**
     * Обновить статус платежа
     */
    public boolean updateStatus(int paymentId, PaymentStatus status) {
        if (payments.containsKey(paymentId)) {
            payments.get(paymentId).setStatus(status);
            return true;
        }
        return false;
    }
}