package com.example.service;

import java.util.Optional;

import com.example.Account;
import com.example.repository.AccountRepository;

/**
 * Сервис для работы со счетами
 */
public class AccountService {
    private final AccountRepository accountRepository;
    
    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }
    
    /**
     * Создать новый счет для пользователя
     */
    public Account createAccount(int userId) {
        // Проверка, что у пользователя еще нет счета
        if (accountRepository.existsByUserId(userId)) {
            throw new IllegalStateException("Account for user " + userId + " already exists");
        }
        
        // Создаем новый счет
        Account account = new Account(userId);
        return accountRepository.save(account);
    }
    
    /**
     * Пополнить счет пользователя
     */
    public Account depositFunds(int userId, double amount) {
        Account account = getAccountByUserId(userId);
        
        // Атомарное пополнение счета
        account.deposit(amount);
        
        return account;
    }
    
    /**
     * Списать средства со счета пользователя
     */
    public boolean withdrawFunds(int userId, double amount) {
        Account account = getAccountByUserId(userId);
        
        // Атомарное списание средств с проверкой достаточности
        return account.withdraw(amount);
    }
    
    /**
     * Получить баланс счета пользователя
     */
    public double getBalance(int userId) {
        Account account = getAccountByUserId(userId);
        return account.getBalance();
    }
    
    /**
     * Получить счет по ID пользователя
     */
    public Account getAccountByUserId(int userId) {
        Optional<Account> accountOptional = accountRepository.findByUserId(userId);
        if (!accountOptional.isPresent()) {
            throw new IllegalArgumentException("Account for user " + userId + " not found");
        }
        return accountOptional.get();
    }
    
    /**
     * Проверить наличие счета у пользователя
     */
    public boolean hasAccount(int userId) {
        return accountRepository.existsByUserId(userId);
    }
}