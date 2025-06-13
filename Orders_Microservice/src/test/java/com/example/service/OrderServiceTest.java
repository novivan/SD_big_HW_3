package com.example.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.example.Order;
import com.example.OrderStatus;
import com.example.outbox.OutboxMessage;
import com.example.outbox.OutboxService;
import com.example.repository.OrderRepository;

public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxService outboxService;

    private OrderService orderService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        orderService = new OrderService(orderRepository, outboxService);
    }

    @Test
    public void testCreateOrder() {
        // Подготовка данных
        int userId = 1;
        List<Map<String, Object>> items = new ArrayList<>();
        
        Map<String, Object> item1 = new HashMap<>();
        item1.put("price", 10.0);
        item1.put("name", "Test Product");
        item1.put("description", "Test Description");
        item1.put("quantity", 2);
        items.add(item1);
        
        // Настраиваем поведение мока - при сохранении любого объекта Order возвращаем тот же объект
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        
        // Вызов тестируемого метода
        Order result = orderService.createOrder(userId, items);
        
        // Проверки
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(20.0, result.getTotalPrice(), 0.001); // 10.0 * 2
        assertEquals(OrderStatus.CREATED, result.getStatus());
        
        // Проверяем вызов outboxService.saveMessage()
        verify(outboxService).saveMessage(any(OutboxMessage.class));
    }

    @Test
    public void testGetOrderById() {
        // Подготовка данных
        int orderId = 1;
        
        // Вместо установки ID через setter, который не существует,
        // мы создаем реальный объект Order и настраиваем mock для его возврата
        Order order = new Order(5); // Создаем реальный объект с userId=5
        
        // Мы настраиваем мок, чтобы он возвращал наш объект при вызове findById с orderId
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        
        // Вызов тестируемого метода
        Optional<Order> result = orderService.getOrderById(orderId);
        
        // Проверки
        assertTrue(result.isPresent());
        assertEquals(5, result.get().getUserId());
    }

    @Test
    public void testGetOrderById_NotFound() {
        // Подготовка данных
        int orderId = 999;
        
        // Настройка моков
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
        
        // Вызов тестируемого метода
        Optional<Order> result = orderService.getOrderById(orderId);
        
        // Проверки
        assertFalse(result.isPresent());
    }
    
    @Test
    public void testGetUserOrders() {
        // Подготовка данных
        int userId = 1;
        List<Order> expectedOrders = new ArrayList<>();
        
        // Создаем реальные объекты Order вместо использования сеттеров
        Order order1 = new Order(userId);
        Order order2 = new Order(userId);
        
        expectedOrders.add(order1);
        expectedOrders.add(order2);
        
        when(orderRepository.findByUserId(userId)).thenReturn(expectedOrders);
        
        // Вызов тестируемого метода
        List<Order> result = orderService.getUserOrders(userId);
        
        // Проверки
        assertEquals(2, result.size());
        assertEquals(userId, result.get(0).getUserId());
        assertEquals(userId, result.get(1).getUserId());
    }

    @Test
    public void testUpdateOrderStatus() {
        // Подготовка данных
        int orderId = 1;
        OrderStatus newStatus = OrderStatus.PAID;
        
        when(orderRepository.updateStatus(orderId, newStatus)).thenReturn(true);
        
        // Вызов тестируемого метода
        boolean result = orderService.updateOrderStatus(orderId, newStatus);
        
        // Проверки
        assertTrue(result);
        verify(orderRepository).updateStatus(orderId, newStatus);
    }

    @Test
    public void testUpdateOrderStatus_NotFound() {
        // Подготовка данных
        int orderId = 999;
        OrderStatus newStatus = OrderStatus.PAID;
        
        // Настройка моков
        when(orderRepository.updateStatus(orderId, newStatus)).thenReturn(false);
        
        // Вызов тестируемого метода
        boolean result = orderService.updateOrderStatus(orderId, newStatus);
        
        // Проверки
        assertFalse(result);
    }
    
    @Test
    public void testCalculateTotalPrice() {
        // Подготовка данных
        List<Map<String, Object>> items = new ArrayList<>();
        
        Map<String, Object> item1 = new HashMap<>();
        item1.put("price", 10.0);
        item1.put("quantity", 2);
        items.add(item1);
        
        Map<String, Object> item2 = new HashMap<>();
        item2.put("price", 15.0);
        item2.put("quantity", 3);
        items.add(item2);
        
        // Create a test order and manually add the items
        Order order = new Order(1);
        
        // Instead of using reflection to access a private method, we'll test
        // the logic through the createOrder method which internally calculates the total price
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        
        // Executing the method that will calculate the total price
        Order result = orderService.createOrder(1, items);
        
        // Verify the total price calculation
        assertEquals(65.0, result.getTotalPrice(), 0.001);
    }
}
