package com.example.controller;

import java.util.HashMap;
import java.util.Map;

import com.example.Account;
import com.example.service.AccountService;
import com.google.gson.Gson;

import spark.Route;

/**
 * Контроллер для работы со счетами
 */
public class AccountController {
    private final AccountService accountService;
    private final Gson gson = new Gson();
    
    // Объявляем Route как поля класса
    private final Route createAccount;
    private final Route getBalance;
    private final Route depositFunds;
    
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
        
        // Инициализируем маршруты
        this.createAccount = (request, response) -> {
            try {
                Map<String, Object> requestBody = gson.fromJson(request.body(), Map.class);
                int userId = ((Number) requestBody.get("userId")).intValue();
                
                Account account = accountService.createAccount(userId);
                
                response.status(201); // Created
                response.type("application/json");
                
                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("accountId", account.getId());
                responseBody.put("userId", account.getUserId());
                responseBody.put("balance", account.getBalance());
                
                return gson.toJson(responseBody);
            } catch (IllegalStateException e) {
                response.status(400); // Bad Request
                return gson.toJson(Map.of("error", e.getMessage()));
            } catch (Exception e) {
                response.status(500); // Internal Server Error
                return gson.toJson(Map.of("error", e.getMessage()));
            }
        };
        
        this.getBalance = (request, response) -> {
            try {
                int userId = Integer.parseInt(request.params(":userId"));
                
                double balance = accountService.getBalance(userId);
                
                response.status(200); // OK
                response.type("application/json");
                
                return gson.toJson(Map.of(
                        "userId", userId,
                        "balance", balance
                ));
            } catch (IllegalArgumentException e) {
                response.status(404); // Not Found
                return gson.toJson(Map.of("error", "Account not found"));
            } catch (Exception e) {
                response.status(500); // Internal Server Error
                return gson.toJson(Map.of("error", e.getMessage()));
            }
        };
        
        this.depositFunds = (request, response) -> {
            try {
                int userId = Integer.parseInt(request.params(":userId"));
                
                Map<String, Object> requestBody = gson.fromJson(request.body(), Map.class);
                double amount = ((Number) requestBody.get("amount")).doubleValue();
                
                if (amount <= 0) {
                    response.status(400); // Bad Request
                    return gson.toJson(Map.of("error", "Deposit amount must be positive"));
                }
                
                Account account = accountService.depositFunds(userId, amount);
                
                response.status(200); // OK
                response.type("application/json");
                
                return gson.toJson(Map.of(
                        "accountId", account.getId(),
                        "userId", account.getUserId(),
                        "balance", account.getBalance(),
                        "depositAmount", amount
                ));
            } catch (IllegalArgumentException e) {
                response.status(404); // Not Found
                return gson.toJson(Map.of("error", "Account not found"));
            } catch (Exception e) {
                response.status(500); // Internal Server Error
                return gson.toJson(Map.of("error", e.getMessage()));
            }
        };
    }
    
    // Геттеры для доступа к маршрутам
    public Route getCreateAccount() {
        return createAccount;
    }
    
    public Route getGetBalance() {
        return getBalance;
    }
    
    public Route getDepositFunds() {
        return depositFunds;
    }
}