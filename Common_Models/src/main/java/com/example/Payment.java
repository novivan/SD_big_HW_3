package com.example;

import java.time.LocalDateTime;
import java.util.UUID;

public class Payment {
    private static int lastId = 0;

    private final int id;
    private final int orderId;
    private String transactionId; // Изменено с final на изменяемое
    private final double amount;
    private PaymentStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime processedAt;

    public Payment(int orderId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        this.id = ++lastId;
        this.orderId = orderId;
        this.transactionId = UUID.randomUUID().toString(); // Генерируем по умолчанию
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public int getOrderId() {
        return orderId;
    }

    public String getTransactionId() {
        return transactionId;
    }
    
    // Добавлен setter для transactionId
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public double getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
        if (status == PaymentStatus.COMPLETED || status == PaymentStatus.FAILED) {
            this.processedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

}