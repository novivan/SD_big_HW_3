package com.example;

public class OrderItem {
    private final Good good;
    private int quantity;
    
    public OrderItem(Good good, int quantity) {
        if (good == null) {
            throw new IllegalArgumentException("Good cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        this.good = good;
        this.quantity = quantity;
    }

    public Good getGood() {
        return good;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.quantity = quantity;
    }

    public void increaseQuantity(int additionalQuantity) {
        if (additionalQuantity <= 0) {
            throw new IllegalArgumentException("Additional quantity must be positive");
        }
        this.quantity += additionalQuantity;
    }

    public double getSubtotal() {
        return good.getPrice() * quantity;
    }
}
