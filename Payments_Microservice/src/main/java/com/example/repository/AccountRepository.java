package com.example.repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.example.Account;

/**
 * In-memory хранилище счетов
 */
public class AccountRepository {
    private final Map<Integer, Account> accounts = new ConcurrentHashMap<>();
    
    /**
     * Сохранить счет
     */
    public Account save(Account account) {
        accounts.put(account.getId(), account);
        return account;
    }
    
    /**
     * Найти счет по ID
     */
    public Optional<Account> findById(int id) {
        return Optional.ofNullable(accounts.get(id));
    }
    
    /**
     * Найти счет по ID пользователя
     */
    public Optional<Account> findByUserId(int userId) {
        return accounts.values().stream()
                .filter(account -> account.getUserId() == userId)
                .findFirst();
    }
    
    /**
     * Проверить существование счета для пользователя
     */
    public boolean existsByUserId(int userId) {
        return accounts.values().stream()
                .anyMatch(account -> account.getUserId() == userId);
    }
}