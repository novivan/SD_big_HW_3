package com.example;

import java.util.concurrent.atomic.AtomicReference;

public class Account {
    private static int lastId = 0;

    private final int id;
    private final int userId;
    private final AtomicReference<Double> balance;
    
    public Account(int userId) {
        this.id = ++lastId;
        this.userId = userId;
        this.balance = new AtomicReference<>(0.0);
    }

    public int getId() {
        return id; 
    // тк конструктор возвращает объект класса, а не int,
    // то мы позже сделаем фабрику, и при создании будем брать id
    }

    public int getUserId() {
        return userId;
    }

    public double getBalance() {
        return balance.get();
    }

    /**
     * Атомарное пополнение счета.
     * @param amount Сумма для пополнения
     * @return Актуальный баланс после пополенния
     */
    public void deposit(double amount) {
        if (amount > 0) {
            balance.updateAndGet(currentBalance -> currentBalance + amount);
            return;
        }
        throw new IllegalArgumentException("Deposit amount must be positive");
    }

    /**
     * Атомарное списание средств с проверкой на достаточность
     * @param amount Сумма для списания
     * @return true если списание успешно, иначе false, если недостаточно средств
     */
    public boolean withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        double currentBalance;
        do {
            currentBalance = balance.get();
            if (currentBalance < amount) {
                return false; 
            }
        } while (!balance.compareAndSet(currentBalance, currentBalance - amount));
        return true; // Списание успешно
    }
}
