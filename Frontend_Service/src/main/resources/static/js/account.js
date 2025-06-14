document.addEventListener('DOMContentLoaded', function() {
    const API_BASE_URL = 'http://localhost:8080/api';

    // Создание счета
    document.getElementById('createAccountForm').addEventListener('submit', async function(e) {
        e.preventDefault();
        const userId = document.getElementById('userId').value;

        if (!userId) {
            alert('Введите ID пользователя');
            return;
        }

        try {
            const response = await fetch(`${API_BASE_URL}/accounts`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ userId: parseInt(userId) })
            });

            if (response.ok) {
                alert('Счет успешно создан!');
                document.getElementById('createAccountForm').reset();
            } else {
                const error = await response.json();
                alert(`Ошибка: ${error.error || 'Не удалось создать счет'}`);
            }
        } catch (error) {
            alert('Ошибка при создании счета: ' + error.message);
        }
    });

    // Пополнение счета
    document.getElementById('depositForm').addEventListener('submit', async function(e) {
        e.preventDefault();
        const userId = document.getElementById('depositUserId').value;
        const amount = document.getElementById('amount').value;

        if (!userId || !amount) {
            alert('Заполните все поля');
            return;
        }

        if (parseFloat(amount) <= 0) {
            alert('Сумма должна быть больше 0');
            return;
        }

        try {
            const response = await fetch(`${API_BASE_URL}/accounts/${userId}/deposit`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ amount: parseFloat(amount) })
            });

            if (response.ok) {
                const result = await response.json();
                alert(`Счет успешно пополнен! Новый баланс: ${result.balance} руб.`);
                document.getElementById('depositForm').reset();
            } else {
                const error = await response.json();
                alert(`Ошибка: ${error.error || 'Не удалось пополнить счет'}`);
            }
        } catch (error) {
            alert('Ошибка при пополнении счета: ' + error.message);
        }
    });

    // Проверка баланса
    document.getElementById('balanceForm').addEventListener('submit', async function(e) {
        e.preventDefault();
        const userId = document.getElementById('balanceUserId').value;
        const resultDiv = document.getElementById('balanceResult');

        if (!userId) {
            alert('Введите ID пользователя');
            return;
        }

        try {
            const response = await fetch(`${API_BASE_URL}/accounts/${userId}/balance`);
            
            if (response.ok) {
                const data = await response.json();
                resultDiv.innerHTML = `
                    <div class="alert alert-success">
                        <h6>Баланс счета:</h6>
                        <p class="mb-0">${data.balance} руб.</p>
                    </div>
                `;
            } else {
                const error = await response.json();
                resultDiv.innerHTML = `
                    <div class="alert alert-danger">
                        <p class="mb-0">Ошибка: ${error.error || 'Не удалось получить баланс'}</p>
                    </div>
                `;
            }
        } catch (error) {
            resultDiv.innerHTML = `
                <div class="alert alert-danger">
                    <p class="mb-0">Ошибка при получении баланса: ${error.message}</p>
                </div>
            `;
        }
    });
}); 