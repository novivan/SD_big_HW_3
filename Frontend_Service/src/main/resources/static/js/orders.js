document.addEventListener('DOMContentLoaded', function() {
    const API_BASE_URL = 'http://localhost:8080/api';

    // Добавление нового товара в форму
    function addItemBlock() {
        const container = document.getElementById('itemsContainer');
        const newItem = document.createElement('div');
        newItem.className = 'item-entry mb-3';
        newItem.innerHTML = `
            <div class="row">
                <div class="col-md-3">
                    <input type="text" class="form-control" placeholder="Название" required>
                </div>
                <div class="col-md-3">
                    <input type="number" class="form-control" placeholder="Цена" step="0.01" required>
                </div>
                <div class="col-md-3">
                    <input type="text" class="form-control" placeholder="Описание" required>
                </div>
                <div class="col-md-2">
                    <input type="number" class="form-control" placeholder="Количество" required>
                </div>
                <div class="col-md-1">
                    <button type="button" class="btn btn-danger remove-item">×</button>
                </div>
            </div>
        `;
        container.appendChild(newItem);
    }

    // Обработчик кнопки "Добавить товар"
    document.getElementById('addItem').addEventListener('click', function() {
        addItemBlock();
    });

    // Удаление товара из формы
    document.getElementById('itemsContainer').addEventListener('click', function(e) {
        if (e.target.classList.contains('remove-item')) {
            e.target.closest('.item-entry').remove();
            // Если не осталось ни одного товара, добавляем новый
            if (document.querySelectorAll('.item-entry').length === 0) {
                addItemBlock();
            }
        }
    });

    // Создание заказа
    document.getElementById('createOrderForm').addEventListener('submit', async function(e) {
        e.preventDefault();
        const userId = document.getElementById('orderUserId').value;
        const resultDiv = document.getElementById('createOrderResult');
        
        if (!userId) {
            resultDiv.innerHTML = `
                <div class="alert alert-danger">
                    <p class="mb-0">Введите ID пользователя</p>
                </div>
            `;
            return;
        }
        
        // Проверяем, что есть хотя бы один товар
        const itemBlocks = Array.from(document.querySelectorAll('.item-entry'));
        if (itemBlocks.length === 0) {
            resultDiv.innerHTML = `
                <div class="alert alert-danger">
                    <p class="mb-0">Добавьте хотя бы один товар в заказ</p>
                </div>
            `;
            return;
        }
        
        let items = [];
        try {
            items = itemBlocks.map(entry => {
                const inputs = entry.querySelectorAll('input');
                const name = inputs[0].value.trim();
                const price = parseFloat(inputs[1].value);
                const description = inputs[2].value.trim();
                const quantity = parseInt(inputs[3].value);
                
                if (!name || isNaN(price) || !description || isNaN(quantity)) {
                    throw new Error('Все поля должны быть заполнены корректно');
                }
                
                return {
                    name: name,
                    price: price,
                    description: description,
                    quantity: quantity
                };
            });
        } catch (err) {
            resultDiv.innerHTML = `
                <div class="alert alert-danger">
                    <p class="mb-0">${err.message}</p>
                </div>
            `;
            return;
        }
        
        if (items.length === 0) {
            resultDiv.innerHTML = `
                <div class="alert alert-danger">
                    <p class="mb-0">Добавьте хотя бы один товар в заказ</p>
                </div>
            `;
            return;
        }
        
        try {
            const response = await fetch(`${API_BASE_URL}/orders`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    userId: parseInt(userId),
                    items: items
                })
            });
            
            if (response.ok) {
                const result = await response.json();
                resultDiv.innerHTML = `
                    <div class="alert alert-success">
                        <p class="mb-0">Заказ успешно создан! ID заказа: ${result.orderId}</p>
                    </div>
                `;
                
                // Очищаем форму
                document.getElementById('createOrderForm').reset();
                document.getElementById('itemsContainer').innerHTML = '';
                addItemBlock();
            } else {
                const error = await response.json();
                resultDiv.innerHTML = `
                    <div class="alert alert-danger">
                        <p class="mb-0">Ошибка: ${error.error || 'Не удалось создать заказ'}</p>
                    </div>
                `;
            }
        } catch (error) {
            resultDiv.innerHTML = `
                <div class="alert alert-danger">
                    <p class="mb-0">Ошибка при создании заказа: ${error.message}</p>
                </div>
            `;
        }
    });

    // Получение списка заказов
    document.getElementById('listOrdersForm').addEventListener('submit', async function(e) {
        e.preventDefault();
        const userId = document.getElementById('listOrdersUserId').value;
        const ordersList = document.getElementById('ordersList');

        if (!userId) {
            ordersList.innerHTML = `
                <div class="alert alert-danger">
                    <p class="mb-0">Введите ID пользователя</p>
                </div>
            `;
            return;
        }

        try {
            const response = await fetch(`${API_BASE_URL}/users/${userId}/orders`);
            
            if (response.ok) {
                const data = await response.json();

                if (data.orders && data.orders.length > 0) {
                    ordersList.innerHTML = `
                        <div class="table-responsive">
                            <table class="table">
                                <thead>
                                    <tr>
                                        <th>ID заказа</th>
                                        <th>Статус</th>
                                        <th>Сумма</th>
                                        <th>Дата создания</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${data.orders.map(order => `
                                        <tr>
                                            <td>${order.orderId}</td>
                                            <td>${order.status}</td>
                                            <td>${order.totalPrice} руб.</td>
                                            <td>${new Date(order.createdAt).toLocaleDateString('ru-RU')}</td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                        </div>
                    `;
                } else {
                    ordersList.innerHTML = `
                        <div class="alert alert-info">
                            <p class="mb-0">У вас пока нет заказов</p>
                        </div>
                    `;
                }
            } else {
                const error = await response.json();
                ordersList.innerHTML = `
                    <div class="alert alert-danger">
                        <p class="mb-0">Ошибка: ${error.error || 'Не удалось получить список заказов'}</p>
                    </div>
                `;
            }
        } catch (error) {
            ordersList.innerHTML = `
                <div class="alert alert-danger">
                    <p class="mb-0">Ошибка при получении списка заказов: ${error.message}</p>
                </div>
            `;
        }
    });

    // Проверка статуса заказа
    document.getElementById('orderStatusForm').addEventListener('submit', async function(e) {
        e.preventDefault();
        const orderId = document.getElementById('orderId').value;
        const userId = document.getElementById('statusUserId').value;
        const orderStatus = document.getElementById('orderStatus');

        if (!orderId || !userId) {
            orderStatus.innerHTML = `
                <div class="alert alert-danger">
                    <p class="mb-0">Заполните все поля</p>
                </div>
            `;
            return;
        }

        try {
            const response = await fetch(`${API_BASE_URL}/orders/${orderId}?userId=${userId}`);
            
            if (response.ok) {
                const data = await response.json();
                orderStatus.innerHTML = `
                    <div class="alert alert-info">
                        <h6>Статус заказа #${data.orderId}:</h6>
                        <p class="mb-0">Статус: ${data.status}</p>
                        <p class="mb-0">Сумма: ${data.totalPrice} руб.</p>
                        <p class="mb-0">Дата создания: ${new Date(data.createdAt).toLocaleDateString('ru-RU')}</p>
                    </div>
                `;
            } else {
                const error = await response.json();
                orderStatus.innerHTML = `
                    <div class="alert alert-danger">
                        <p class="mb-0">Ошибка: ${error.error || 'Не удалось получить статус заказа'}</p>
                    </div>
                `;
            }
        } catch (error) {
            orderStatus.innerHTML = `
                <div class="alert alert-danger">
                    <p class="mb-0">Ошибка при получении статуса заказа: ${error.message}</p>
                </div>
            `;
        }
    });
}); 