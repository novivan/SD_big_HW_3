package com.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


public class Order {
    private static int lastId = 0;

    private final int id;
    private final int userId;
    private final String transactionId;
    private OrderStatus status;

    private final List<OrderItem> items;
    private double totalPrice;

    public Order(int userId) {
        this.id = ++lastId;
        this.userId = userId;
        this.transactionId = UUID.randomUUID().toString();
        this.status = OrderStatus.CREATED;
        this.items = new ArrayList<>();
        this.totalPrice = 0.0;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void addItem(Good good, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity mist be positive");
        }
        if (good == null) {
            throw new IllegalArgumentException("Good cannot be null");
        }

        // Проверяем, мб товар уже в заказе
        for (OrderItem item: items) {
            if (item.getGood().getId() == good.getId()) {
                item.increaseQuantity(quantity);
                totalPrice += good.getPrice() * quantity;
                return;
            }
        }
        // Add new item if not found
        OrderItem newItem = new OrderItem(good, quantity);
        items.add(newItem);
        totalPrice += good.getPrice() * quantity;
    }
}
