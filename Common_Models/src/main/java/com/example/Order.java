package com.example;

import java.util.ArrayList;

public class Order {
    private static int lastId = 0;

    private final int id;
    private final Account account;
    
    private ArrayList<Good> goods;
    private ArrayList<Integer> amounts;
    private double totalPrice;

    public Order(Account account) {
        this.id = ++lastId;
        this.account = account;
        this.goods = new ArrayList<>();
        this.totalPrice = 0.0;
        this.amounts = new ArrayList<>();
    }

    void addGood(Good good, int amount) {
        if (amount > 0 && good.getAmountOnMarket() >= amount) {
            goods.add(good);
            good.DecreaseAmountOnMarket(amount);
            totalPrice += good.getPrice() * amount;
            amounts.add(amount);
        } else {
            throw new IllegalArgumentException("Invalid amount or insufficient goods on market");
        }
    }
}
