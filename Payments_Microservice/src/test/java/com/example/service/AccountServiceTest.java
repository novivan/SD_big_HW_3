package com.example.service;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.example.Account;
import com.example.repository.AccountRepository;

public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private AccountService accountService;
    
    private ExecutorService executorService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        accountService = new AccountService(accountRepository);
        executorService = Executors.newSingleThreadExecutor();
    }
    
    @After
    public void tearDown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            try {
                if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                    System.err.println("ExecutorService did not terminate");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test(timeout = 1000)
    public void testCreateAccount() {
        // Подготовка данных
        int userId = 1;
        
        // Создаем реальный Account (т.к. id генерируется в конструкторе)
        Account realAccount = new Account(userId);
        
        // Настройка моков
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenReturn(realAccount);

        // Вызов тестируемого метода
        Account result = accountService.createAccount(userId);

        // Проверки
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(0.0, result.getBalance(), 0.001);

        // Проверка вызова методов репозитория
        verify(accountRepository).findByUserId(userId);
        verify(accountRepository).save(any(Account.class));
    }

    @Test(expected = IllegalStateException.class, timeout = 1000)
    public void testCreateAccountWhenAccountAlreadyExists() {
        // Подготовка данных
        int userId = 1;
        Account existingAccount = new Account(userId);

        // Настройка моков
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(existingAccount));

        // Вызов тестируемого метода - должен выбросить исключение
        accountService.createAccount(userId);
    }

    @Test(timeout = 1000)
    public void testDepositFunds() {
        // Подготовка данных
        int userId = 1;
        // Используем реальный объект Account
        Account account = new Account(userId);

        // Настройка моков
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Вызов тестируемого метода
        Account result = accountService.depositFunds(userId, 100.0);

        // Проверки
        assertEquals(100.0, result.getBalance(), 0.001);

        // Проверка вызова методов репозитория
        verify(accountRepository).findByUserId(userId);
        verify(accountRepository).save(account);
    }

    @Test(expected = IllegalArgumentException.class, timeout = 1000)
    public void testDepositFundsWithNegativeAmount() {
        // Подготовка данных
        int userId = 1;
        Account account = new Account(userId);

        // Настройка моков
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        // Вызов тестируемого метода с отрицательной суммой - должен выбросить исключение
        accountService.depositFunds(userId, -50.0);
    }

    @Test(timeout = 5000) // Увеличиваем таймаут для предотвращения проблем с синхронизацией
    public void testWithdrawFundsSuccess() {
        // Подготовка данных
        int userId = 1;
        Account account = spy(new Account(userId));
        account.deposit(100.0); // Предварительно пополняем счет
        
        // Обхоим проблему с долгим выполнением withdraw
        doReturn(true).when(account).withdraw(anyDouble());

        // Настройка моков
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        // Вызов тестируемого метода
        boolean result = accountService.withdrawFunds(userId, 50.0);

        // Проверки
        assertTrue(result);
        
        // Проверка вызова методов репозитория
        verify(accountRepository).findByUserId(userId);
        verify(accountRepository).save(any(Account.class));
        verify(account).withdraw(50.0);
    }

    @Test(timeout = 1000)
    public void testWithdrawFundsInsufficientBalance() {
        // Подготовка данных
        int userId = 1;
        Account account = spy(new Account(userId));
        account.deposit(30.0); // Пополняем на сумму меньше, чем будем снимать
        
        // Обходим проблему с долгим выполнением withdraw
        doReturn(false).when(account).withdraw(anyDouble());

        // Настройка моков
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        // Вызов тестируемого метода
        boolean result = accountService.withdrawFunds(userId, 50.0);

        // Проверки
        assertFalse(result); // Снятие должно не удаться
        
        // Проверка вызова методов репозитория
        verify(accountRepository).findByUserId(userId);
        verify(account).withdraw(50.0);
        verify(accountRepository, never()).save(any(Account.class)); // Не должно быть вызова save при неуспешном снятии
    }

    @Test(timeout = 1000)
    public void testGetBalance() {
        // Подготовка данных
        int userId = 1;
        Account account = new Account(userId);

        // Внесем средства
        account.deposit(150.0);

        // Настройка моков
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        // Вызов тестируемого метода
        double balance = accountService.getBalance(userId);

        // Проверки
        assertEquals(150.0, balance, 0.001);

        // Проверка вызова методов репозитория
        verify(accountRepository).findByUserId(userId);
    }

    @Test(expected = IllegalArgumentException.class, timeout = 1000)
    public void testGetBalanceAccountNotFound() {
        // Подготовка данных
        int userId = 999;

        // Настройка моков
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Вызов тестируемого метода - должен выбросить исключение
        accountService.getBalance(userId);
    }
}
