package com.example.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.example.Order;
import com.example.OrderStatus;

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

    // Обновлено: переименовано с UpdateStatus на updateStatus (camelCase)
    public boolean updateStatus(int orderId, OrderStatus newStatus) {
        if (orders.containsKey(orderId)) {
            orders.get(orderId).setStatus(newStatus);
            return true;
        }
        return false;
    }
    
    // Для обратной совместимости оставим старый метод, 
    // который вызывает новый (для случая если где-то используется старое название)
    public boolean UpdateStatus(int orderId, OrderStatus newStatus) {
        return updateStatus(orderId, newStatus);
    }
}