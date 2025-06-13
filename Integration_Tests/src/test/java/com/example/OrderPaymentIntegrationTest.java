package com.example;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.Assert.*;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Интеграционный тест для проверки взаимодействия между Orders и Payments микросервисами
 */
public class OrderPaymentIntegrationTest {

    private static final String API_GATEWAY_URL = "http://localhost:8080";
    private static final String ORDERS_URL = "http://localhost:8081";
    private static final String PAYMENTS_URL = "http://localhost:8082";
    
    private HttpClient httpClient;
    
    @Before
    public void setUp() {
        // Настройка HTTP клиента
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        // Проверка доступности сервисов
        assertServiceAvailable(API_GATEWAY_URL + "/api/hello");
        assertServiceAvailable(ORDERS_URL + "/orders/hello");
        assertServiceAvailable(PAYMENTS_URL + "/payments/hello");
    }
    
    @Test
    public void testCompleteOrderWorkflow() throws Exception {
        // Шаг 1: Создание аккаунта пользователя
        int userId = 999; // Уникальный ID для тестов
        JSONObject accountRequest = new JSONObject();
        accountRequest.put("userId", userId);
        
        HttpRequest createAccountRequest = HttpRequest.newBuilder()
                .uri(new URL(PAYMENTS_URL + "/accounts").toURI())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(accountRequest.toString()))
                .build();
        
        HttpResponse<String> accountResponse = httpClient.send(
                createAccountRequest, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(201, accountResponse.statusCode());
        JSONObject accountJson = new JSONObject(accountResponse.body());
        assertTrue(accountJson.has("accountId"));
        
        // Шаг 2: Пополнение счета
        JSONObject depositRequest = new JSONObject();
        depositRequest.put("amount", 500.0); // Достаточно для покупки
        
        HttpRequest depositRequest = HttpRequest.newBuilder()
                .uri(new URL(PAYMENTS_URL + "/accounts/" + userId + "/deposit").toURI())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(depositRequest.toString()))
                .build();
        
        HttpResponse<String> depositResponse = httpClient.send(
                depositRequest, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, depositResponse.statusCode());
        JSONObject depositJson = new JSONObject(depositResponse.body());
        assertEquals(500.0, depositJson.getDouble("balance"), 0.001);
        
        // Шаг 3: Создание заказа
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("userId", userId);
        
        JSONArray items = new JSONArray();
        JSONObject item = new JSONObject();
        item.put("name", "Test Product");
        item.put("price", 99.99);
        item.put("description", "Integration test product");
        item.put("quantity", 2);
        items.put(item);
        
        orderRequest.put("items", items);
        
        HttpRequest createOrderRequest = HttpRequest.newBuilder()
                .uri(new URL(ORDERS_URL + "/orders").toURI())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(orderRequest.toString()))
                .build();
        
        HttpResponse<String> orderResponse = httpClient.send(
                createOrderRequest, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(201, orderResponse.statusCode());
        JSONObject orderJson = new JSONObject(orderResponse.body());
        int orderId = orderJson.getInt("orderId");
        
        // Шаг 4: Дождаться обработки платежа (это может занять некоторое время из-за асинхронности)
        waitForOrderStatus(orderId, userId, "PAID", 10);
        
        // Шаг 5: Проверка списания средств
        HttpRequest balanceRequest = HttpRequest.newBuilder()
                .uri(new URL(PAYMENTS_URL + "/accounts/" + userId + "/balance").toURI())
                .GET()
                .build();
        
        HttpResponse<String> balanceResponse = httpClient.send(
                balanceRequest, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, balanceResponse.statusCode());
        JSONObject balanceJson = new JSONObject(balanceResponse.body());
        // Ожидаемый баланс: 500.0 - (99.99 * 2) = 300.02
        assertEquals(300.02, balanceJson.getDouble("balance"), 0.1);
    }
    
    /**
     * Ждем пока статус заказа не изменится на указанный или не истечет таймаут
     */
    private void waitForOrderStatus(int orderId, int userId, String expectedStatus, int maxAttempts) throws Exception {
        int attempts = 0;
        String status = "";
        
        while (!expectedStatus.equals(status) && attempts < maxAttempts) {
            HttpRequest orderStatusRequest = HttpRequest.newBuilder()
                    .uri(new URL(ORDERS_URL + "/orders/" + orderId + "?userId=" + userId).toURI())
                    .GET()
                    .build();
            
            HttpResponse<String> statusResponse = httpClient.send(
                    orderStatusRequest, HttpResponse.BodyHandlers.ofString());
            
            if (statusResponse.statusCode() == 200) {
                JSONObject orderJson = new JSONObject(statusResponse.body());
                status = orderJson.getString("status");
                if (expectedStatus.equals(status)) {
                    break;
                }
            }
            
            attempts++;
            Thread.sleep(1000); // Ждем 1 секунду между запросами
        }
        
        // Проверяем, что статус изменился на ожидаемый
        assertEquals("Order did not reach expected status in time", expectedStatus, status);
    }
    
    /**
     * Проверяет доступность сервиса
     */
    private void assertServiceAvailable(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            int responseCode = connection.getResponseCode();
            assertTrue("Service " + url + " not available, response code: " + responseCode, 
                    responseCode >= 200 && responseCode < 500);
        } catch (IOException e) {
            fail("Service " + url + " not available: " + e.getMessage());
        }
    }
}
