#!/bin/bash

echo "Starting the application..."

# Сначала собираем общие модели
cd ./Common_Models && mvn clean install
echo "Common Models built successfully."
cd ..

# Эта функция запускает тесты и показывает их результаты
run_tests_and_show_results() {
    local module=$1
    echo "=========================================="
    echo "🧪 Running tests for ${module}..."
    echo "=========================================="
    
    # Переходим в каталог модуля
    cd ./${module}
    
    # Запускаем тесты
    mvn clean test
    
    # Получаем статус выполнения тестов
    TEST_STATUS=$?
    
    # Генерируем отчет JaCoCo независимо от результата тестов
    mvn jacoco:report
    
    echo ""
    echo "--- Test Summary for ${module} ---"
    
    # Подсчитываем количество тестов, ошибок и неудачных тестов
    TOTAL_TESTS=$(find target/surefire-reports -name "TEST-*.xml" -exec grep -l "<testsuite" {} \; | xargs grep -l "tests=" | xargs cat | grep -o 'tests="[0-9]*"' | cut -d'"' -f2 | awk '{sum += $1} END {print sum}')
    ERRORS=$(find target/surefire-reports -name "TEST-*.xml" -exec grep -l "<testsuite" {} \; | xargs grep -l "errors=" | xargs cat | grep -o 'errors="[0-9]*"' | cut -d'"' -f2 | awk '{sum += $1} END {print sum}')
    FAILURES=$(find target/surefire-reports -name "TEST-*.xml" -exec grep -l "<testsuite" {} \; | xargs grep -l "failures=" | xargs cat | grep -o 'failures="[0-9]*"' | cut -d'"' -f2 | awk '{sum += $1} END {print sum}')
    
    echo "Total tests: ${TOTAL_TESTS}"
    echo "Errors: ${ERRORS}"
    echo "Failures: ${FAILURES}"
    
    # Отображаем имена неудачных тестов, если таковые имеются
    if [ "$ERRORS" -gt 0 ] || [ "$FAILURES" -gt 0 ]; then
        echo ""
        echo "Failed tests:"
        grep -l "failures=\"[1-9]" target/surefire-reports/TEST-*.xml | while read file; do
            TEST_CLASS=$(echo "$file" | sed -n 's/.*TEST-\(.*\)\.xml/\1/p')
            echo "- $TEST_CLASS"
            # Показываем подробности для неудачных тестов
            cat "$file" | grep -o '<testcase.*>.*</testcase>' | grep -E 'failure|error' | sed -n 's/.*name="\([^"]*\)".*/  • \1/p'
        done
        
        echo ""
        echo "Common error details from surefire-reports:"
        grep -r "Caused by:" target/surefire-reports/*.txt | head -5
    fi
    
    # Открываем отчет JaCoCo в браузере
    REPORT_PATH="$(pwd)/target/site/jacoco/index.html"
    if [ -f "$REPORT_PATH" ]; then
        echo "Opening JaCoCo report in browser: $REPORT_PATH"
        open "$REPORT_PATH"  # для macOS
        # для Linux можно использовать: xdg-open "$REPORT_PATH"
        # для Windows можно использовать: start "" "$REPORT_PATH"
    else
        echo "JaCoCo report not found at: $REPORT_PATH"
    fi
    
    echo ""
    echo "JaCoCo report generated: target/site/jacoco/index.html"
    echo "=========================================="
    
    # Возвращаемся в родительский каталог
    cd ..
    
    return $TEST_STATUS
}

# Запускаем тесты для всех модулей
run_tests_and_show_results "API_Gateway"
run_tests_and_show_results "Orders_Microservice"
run_tests_and_show_results "Payments_Microservice"

echo "All tests completed. JaCoCo reports generated."

echo "Do you want to start the application anyway? (y/n)"
read -r answer
if [[ "$answer" != "y" && "$answer" != "Y" ]]; then
    echo "Application start cancelled."
    exit 0
fi

# Запуск RabbitMQ и микросервисов
echo "Starting RabbitMQ..."
if brew services info rabbitmq | grep "running" > /dev/null; then
    echo "RabbitMQ is already running."
else
    brew services start rabbitmq
    sleep 5
    echo "RabbitMQ started successfully."
fi

cd ./API_Gateway && mvn clean package -DskipTests
nohup mvn exec:java -Dexec.mainClass="com.example.App" > /dev/null 2>&1 &
cd ..
echo "API_Gateway started successfully."

cd ./Orders_Microservice && mvn clean package -DskipTests
nohup mvn exec:java -Dexec.mainClass="com.example.App" > /dev/null 2>&1 &
cd ..
echo "Orders Microservice started successfully."

cd ./Payments_Microservice && mvn clean package -DskipTests
nohup mvn exec:java -Dexec.mainClass="com.example.App" > /dev/null 2>&1 &
cd ..
echo "Payments Microservice started successfully."

echo "All services are now running!"