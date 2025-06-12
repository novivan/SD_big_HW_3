package com.example.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.example.Order;

/**
 * In-memory orders' storage.
 */

public class OrderRepository {
    private final Map<Integer, Order> orders = new ConcurrentHashMap<>();

    //Сохранить заказ
    public Order save(Order order) {
        orders.put(order.getId(), order);
        return order;
    }

    //Найти заказ по ID
    public Optional<Order> findById(int id) {
        return Optional.ofNullable(orders.get(id));
    }

    public List<Order> findAll() {
        return new ArrayList<>(orders.values());
    }

    public List<Order> findByUserId(int userId) {
        return orders.values().stream() 
                .filter(order -> order.getUserId() == userId)
                .collect(Collectors.toList());
    }

    public boolean UpdateStatus(int orderId, com.example.OrderStatus newStatus) {
        if (orders.containsKey(orderId)) {
            orders.get(orderId).setStatus(newStatus);
            return true;
        }
        return false;
    } 
}
