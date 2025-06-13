package com.example.repository;

import com.example.Order;
import com.example.OrderStatus;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class OrderRepositoryTest {

    private OrderRepository orderRepository;

    @Before
    public void setUp() {
        orderRepository = new OrderRepository();
    }

    @Test
    public void testSaveAndFindById() {
        // Создание тестового заказа
        Order order = new Order(1);
        order = orderRepository.save(order);
        int orderId = order.getId();

        // Поиск по ID
        Optional<Order> foundOrder = orderRepository.findById(orderId);
        
        // Проверки
        assertTrue(foundOrder.isPresent());
        assertEquals(orderId, foundOrder.get().getId());
        assertEquals(1, foundOrder.get().getUserId());
        assertEquals(OrderStatus.CREATED, foundOrder.get().getStatus());
    }

    @Test
    public void testFindByUserIdWhenUserHasOrders() {
        // Создание тестовых заказов
        int userId = 1;
        Order order1 = new Order(userId);
        Order order2 = new Order(userId);
        Order order3 = new Order(2); // другой пользователь
        
        orderRepository.save(order1);
        orderRepository.save(order2);
        orderRepository.save(order3);

        // Поиск по userId
        List<Order> userOrders = orderRepository.findByUserId(userId);
        
        // Проверки
        assertEquals(2, userOrders.size());
        assertTrue(userOrders.stream().allMatch(order -> order.getUserId() == userId));
    }

    @Test
    public void testFindByUserIdWhenUserHasNoOrders() {
        // Создание тестового заказа для другого пользователя
        Order order = new Order(1);
        orderRepository.save(order);

        // Поиск по несуществующему userId
        List<Order> userOrders = orderRepository.findByUserId(2);
        
        // Проверка
        assertTrue(userOrders.isEmpty());
    }

    @Test
    public void testUpdateStatus() {
        // Создание тестового заказа
        Order order = new Order(1);
        order = orderRepository.save(order);
        int orderId = order.getId();

        // Обновление статуса
        boolean updated = orderRepository.updateStatus(orderId, OrderStatus.PAID);
        
        // Проверки
        assertTrue(updated);
        
        // Проверяем, что статус действительно изменился
        Optional<Order> updatedOrder = orderRepository.findById(orderId);
        assertTrue(updatedOrder.isPresent());
        assertEquals(OrderStatus.PAID, updatedOrder.get().getStatus());
    }

    @Test
    public void testUpdateStatusForNonExistentOrder() {
        // Попытка обновить несуществующий заказ
        boolean updated = orderRepository.updateStatus(999, OrderStatus.PAID);
        
        // Проверка
        assertFalse(updated);
    }
}
