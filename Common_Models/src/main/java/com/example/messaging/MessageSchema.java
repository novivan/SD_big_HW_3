package com.example.messaging;

/**
 * Определяет схему сообщений для взаимодействия между микросервисами
 */
public class MessageSchema {
    // Названия очередей
    public static final String PAYMENT_REQUESTS_QUEUE = "payment_requests";
    public static final String ORDER_PAYMENT_RESULTS_QUEUE = "order_payment_results";
    
    // Типы событий для запросов на оплату
    public static class PaymentRequestType {
        public static final String PROCESS_PAYMENT = "PROCESS_PAYMENT";
    }
    
    // Типы событий для результатов оплаты
    public static class PaymentResultType {
        public static final String PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
        public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    }
    
    /**
     * Структура сообщения для запроса оплаты
     */
    public static class PaymentRequest {
        public String messageId;       // Уникальный ID сообщения
        public String eventType;       // Тип события (PROCESS_PAYMENT)
        public int orderId;            // ID заказа
        public int userId;             // ID пользователя
        public double amount;          // Сумма платежа
        public String transactionId;   // ID транзакции (связывает запрос и ответ)
        public long timestamp;         // Временная метка
        
        public PaymentRequest() {}
        
        public PaymentRequest(int orderId, int userId, double amount, String transactionId) {
            this.eventType = PaymentRequestType.PROCESS_PAYMENT;
            this.orderId = orderId;
            this.userId = userId;
            this.amount = amount;
            this.transactionId = transactionId;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * Структура сообщения для результата оплаты
     */
    public static class PaymentResult {
        public String messageId;       // Уникальный ID сообщения
        public String eventType;       // Тип события (PAYMENT_COMPLETED/PAYMENT_FAILED)
        public int orderId;            // ID заказа
        public double amount;          // Сумма платежа
        public boolean success;        // Успешность платежа
        public String transactionId;   // ID транзакции (связывает запрос и ответ)
        public String failureReason;   // Причина неудачи (если есть)
        public long timestamp;         // Временная метка
        
        public PaymentResult() {}
        
        public PaymentResult(int orderId, double amount, boolean success, 
                            String transactionId, String failureReason) {
            this.eventType = success ? PaymentResultType.PAYMENT_COMPLETED : PaymentResultType.PAYMENT_FAILED;
            this.orderId = orderId;
            this.amount = amount;
            this.success = success;
            this.transactionId = transactionId;
            this.failureReason = failureReason;
            this.timestamp = System.currentTimeMillis();
        }
    }
}