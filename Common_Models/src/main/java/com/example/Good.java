package com.example;


public class Good {
    private static int lastId = 0;

    private final double Price;
    private final int Id;
    private final String Name;
    private final String Description;

    private int amount_on_market;

    public Good(double price, String name, String description, int AoM) {
        this.Price = price;
        this.Id = ++lastId;
        this.Name = name;
        this.Description = description;
        this.amount_on_market = AoM;
    }

    public double getPrice() {
        return Price;
    }
    public int getId() {
        return Id;
    }
    public String getName() {
        return Name;
    }
    public String getDescription() {
        return Description;
    }
    public int getAmountOnMarket() {
        return amount_on_market;
    }
    public void DecreaseAmountOnMarket(int amount) {
        if (amount > 0 && amount <= amount_on_market) {
            amount_on_market -= amount;
            return;
        }
        throw new IllegalArgumentException("Invalid amount to decrease from market");
    }
    public void IncreaseAmountOnMarket(int amount) {
        if (amount > 0) {
            amount_on_market += amount;
            return;
        }
        throw new IllegalArgumentException("Invalid amount to increase on market");
    }
    
}
