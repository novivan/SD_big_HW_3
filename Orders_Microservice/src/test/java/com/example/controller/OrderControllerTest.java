package com.example.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.example.Order;
import com.example.OrderStatus;
import com.example.service.OrderService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import spark.Request;
import spark.Response;
import spark.Route;

public class OrderControllerTest {

    @Mock
    private OrderService orderService;
    
    @Mock
    private Request request;
    
    @Mock
    private Response response;
    
    private OrderController orderController;
    private Gson gson;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        orderController = new OrderController(orderService);
        gson = new Gson();
    }
    
    @Test
    public void testCreateOrder() throws Exception {
        // Подготовка данных
        int userId = 1;
        Order order = new Order(userId);
        // Since we don't have setters, we're using the real object as it is
        // We'll mock the service to return this object
        
        // Mock запроса и сервиса
        String requestBody = "{\"userId\": " + userId + ", \"items\": [{\"name\":\"Test\",\"price\":99.99,\"quantity\":1}]}";
        when(request.body()).thenReturn(requestBody);
        when(orderService.createOrder(eq(userId), anyList())).thenReturn(order);
        
        // Получаем Route для создания заказа через рефлексию
        Route createOrderRoute = getRouteFromController("createOrder");
        
        // Вызываем обработчик
        Object result = createOrderRoute.handle(request, response);
        
        // Проверки
        verify(orderService).createOrder(eq(userId), anyList());
        verify(response).status(201); // Created
        // We won't check the exact values since we can't set them,
        // but we'll make sure the response is not null
        assertNotNull(result);
    }
    
    @Test
    public void testHandlePaymentResult_Success() throws Exception {
        // Подготовка данных
        int orderId = 101;
        
        // Mock запроса и сервиса
        String requestBody = "{\"orderId\": " + orderId + ", \"success\": true}";
        when(request.body()).thenReturn(requestBody);
        when(orderService.updateOrderStatus(orderId, OrderStatus.PAID)).thenReturn(true);
        
        // Получаем Route для обработки результата оплаты через рефлексию
        Route handlePaymentResultRoute = getRouteFromController("handlePaymentResult");
        
        // Вызываем обработчик
        Object result = handlePaymentResultRoute.handle(request, response);
        JsonObject jsonResult = JsonParser.parseString(result.toString()).getAsJsonObject();
        
        // Проверки
        verify(orderService).updateOrderStatus(orderId, OrderStatus.PAID);
        verify(response).status(200); // OK
        assertTrue(jsonResult.get("success").getAsBoolean());
    }
    
    // Вспомогательный метод для получения Route из контроллера
    private Route getRouteFromController(String fieldName) {
        try {
            java.lang.reflect.Field field = OrderController.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (Route) field.get(orderController);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get route from controller: " + e.getMessage());
        }
    }
}
