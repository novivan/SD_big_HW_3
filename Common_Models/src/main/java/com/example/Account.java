package com.example;

public class Account {
    private static int lastId = 0;

    private final int id;
    private double balance;
    
    public Account() {
        this.id = ++lastId;
        balance = 0.0;
    }

    public int hetId() {
        return id; 
    // тк конструктор возвращает объект класса, а не int,
    // то мы позже сделаем фабрику, и при создании будем брать id
    }

    public double getBalance() {
        return balance;
    }

    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
            return;
        }
        throw new IllegalArgumentException("Deposit amount must be positive");
    }
}
