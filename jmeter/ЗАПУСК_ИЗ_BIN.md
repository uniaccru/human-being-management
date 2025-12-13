# Запуск теста из директории bin JMeter

## Файлы скопированы в:
```
~/Downloads/apache-jmeter-5.6.3/bin/
```

## Запуск теста:

```bash
cd ~/Downloads/apache-jmeter-5.6.3/bin

./jmeter -n -t distributed-transaction-test.jmx \
         -JSERVER_HOST=localhost \
         -JSERVER_PORT=24180 \
         -JAPI_PATH=/human-being-manager/api/import/humanbeings/file \
         -l Log_distributed_transaction.jtl \
         -e -o Results_distributed_transaction && \
         open Results_distributed_transaction/index.html
```

**Примечание:** Параметры `-J` опциональны, так как в тесте уже установлены дефолтные значения (localhost:24180, путь `/human-being-manager/api/import/humanbeings/file`). Но их можно переопределить при необходимости.

## Просмотр результатов:

```bash
open Results_distributed_transaction/index.html
```

## Что делает тест:

- Запускает **5 параллельных потоков** (пользователей)
- Каждый поток отправляет **POST запрос** на `/api/import/humanbeings/file`
- Каждый запрос загружает **разный JSON файл** (test1.json, test2.json, test3.json)
- Все запросы выполняются **одновременно** для проверки распределенных транзакций
- Порт сервера: **24180**

## Структура файлов в bin:

```
~/Downloads/apache-jmeter-5.6.3/bin/
├── distributed-transaction-test.jmx  # Тест-план
├── test_files.csv                    # Список JSON файлов
├── test1.json                        # Тестовые данные 1
├── test2.json                        # Тестовые данные 2
├── test3.json                        # Тестовые данные 3
├── Log_distributed_transaction.jtl   # Результаты (создается после запуска)
└── Results_distributed_transaction/  # HTML отчет (создается после запуска)
```

