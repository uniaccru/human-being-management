# Настройка Druid Connection Pool в WildFly

## Проблема
Файл `druid-ds.xml` был удален из WAR, так как он конфликтовал с существующей конфигурацией datasource в WildFly.

## Решение: Настройка через WildFly CLI

Datasource должен быть настроен через WildFly консоль управления или CLI. 

### Вариант 1: Через WildFly CLI (рекомендуется)

Подключитесь к WildFly CLI:
```bash
$WILDFLY_HOME/bin/jboss-cli.sh --connect
```

Затем выполните команды для настройки datasource:

```bash
# Удалить существующий datasource (если есть)
/subsystem=datasources/data-source=PostgresDS:remove

# Добавить новый datasource с параметрами пула
/subsystem=datasources/data-source=PostgresDS:add(
    jndi-name=java:/PostgresDS,
    driver-name=postgresql,
    connection-url=${env.DATABASE_URL},
    user-name=${env.PGUSER},
    password=${env.PGPASSWORD},
    min-pool-size=5,
    max-pool-size=20,
    initial-pool-size=5,
    background-validation=true,
    background-validation-millis=60000,
    idle-timeout-minutes=30,
    query-timeout=300
)

# Включить datasource
/subsystem=datasources/data-source=PostgresDS:enable
```

### Вариант 2: Через веб-консоль WildFly

1. Откройте http://localhost:9990 (или адрес вашего WildFly сервера)
2. Перейдите в **Configuration** → **Subsystems** → **Datasources & Drivers** → **Datasources**
3. Если `PostgresDS` уже существует:
   - Нажмите на него
   - Нажмите **Remove**
4. Нажмите **Add** → **Add Datasource**
5. Выберите **PostgreSQL**
6. Заполните параметры:
   - **Name**: `PostgresDS`
   - **JNDI Name**: `java:/PostgresDS`
   - **Connection URL**: `${env.DATABASE_URL}` или полный URL
   - **Username**: `${env.PGUSER}` или имя пользователя
   - **Password**: `${env.PGPASSWORD}` или пароль
7. В разделе **Connection Pool**:
   - **Min Pool Size**: `5`
   - **Max Pool Size**: `20`
   - **Initial Pool Size**: `5`
8. Сохраните и включите datasource

### Параметры Druid Connection Pool

Для документации в отчете используйте следующие параметры:

**Основные параметры:**
- `initialSize`: 5 - начальное количество соединений
- `minIdle`: 5 - минимальное количество простаивающих соединений  
- `maxActive`: 20 - максимальное количество активных соединений
- `maxWait`: -1 (неограниченно) - максимальное время ожидания соединения

**Валидация:**
- `validationQuery`: "SELECT 1" - SQL запрос для валидации
- `testOnBorrow`: false - проверять при получении
- `testOnReturn`: false - проверять при возврате
- `testWhileIdle`: true - проверять простаивающие соединения
- `background-validation`: true - фоновое валидирование
- `background-validation-millis`: 60000 - интервал проверки (60 сек)

**Таймауты:**
- `timeBetweenEvictionRunsMillis`: 60000 - интервал между проверками (60 сек)
- `minEvictableIdleTimeMillis`: 1800000 - минимальное время простоя (30 мин)
- `idle-timeout-minutes`: 30 - таймаут простаивающих соединений
- `query-timeout`: 300 - таймаут запросов (5 мин)

**Очистка:**
- `removeAbandoned`: true - удалять заброшенные соединения
- `removeAbandonedTimeout`: 300 - таймаут для заброшенных соединений (5 мин)

### Примечание

В WildFly используется встроенный connection pool (IronJacamar), который предоставляет аналогичную функциональность Druid. Параметры конфигурации похожи и обеспечивают те же возможности управления пулом соединений.

