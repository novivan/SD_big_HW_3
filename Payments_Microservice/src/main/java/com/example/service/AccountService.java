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
        Optional<Account> existingAccount = accountRepository.findByUserId(userId);
        if (existingAccount.isPresent()) {
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
        
        // Сохраняем обновленный счет
        return accountRepository.save(account);
    }
    
    /**
     * Списать средства со счета пользователя
     */
    public boolean withdrawFunds(int userId, double amount) {
        System.out.println("Attempting to withdraw " + amount + " from user " + userId + "'s account");
        Account account = getAccountByUserId(userId);
        System.out.println("Current balance before withdrawal: " + account.getBalance());
        
        // Атомарное списание средств с проверкой достаточности
        boolean success = account.withdraw(amount);
        
        // Если списание успешно, сохраняем обновление
        if (success) {
            System.out.println("Withdrawal successful. New balance: " + account.getBalance());
            accountRepository.save(account);
        } else {
            System.out.println("Withdrawal failed. Insufficient funds. Current balance: " + account.getBalance());
        }
        
        return success;
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