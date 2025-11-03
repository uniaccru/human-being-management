# Инструкция по деплою на WildFly

## Предварительные требования

1. WildFly 27+ (или последняя версия)
2. PostgreSQL база данных
3. JDK 11+

## Шаги деплоя

### 1. Установка PostgreSQL драйвера в WildFly

```bash
# Создайте модуль для PostgreSQL драйвера
$WILDFLY_HOME/bin/jboss-cli.sh --connect

# В CLI выполните:
module add --name=org.postgresql --resources=/path/to/postgresql-42.7.8.jar --dependencies=jakarta.api,jakarta.transaction.api
```

Или вручную создайте структуру:
```
$WILDFLY_HOME/modules/org/postgresql/main/
  ├── module.xml
  └── postgresql-42.7.3.jar
```

Содержимое `module.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.9" name="org.postgresql">
    <resources>
        <resource-root path="postgresql-42.7.8.jar"/>
    </resources>
    <dependencies>
        <module name="jakarta.api"/>
        <module name="jakarta.transaction.api"/>
    </dependencies>
</module>
```

### 2. Настройка DataSource

#### Вариант А: Через CLI (рекомендуется)

```bash
$WILDFLY_HOME/bin/jboss-cli.sh --connect

# Добавить DataSource
/subsystem=datasources/data-source=HumanBeingDS:add(
    jndi-name=java:/jboss/datasources/HumanBeingDS,
    driver-name=postgresql,
    connection-url=jdbc:postgresql://localhost:5432/humanbeingdb,
    user-name=your_user,
    password=your_password
)

# Настроить pool
/subsystem=datasources/data-source=HumanBeingDS/pool=pool:add(
    min-pool-size=5,
    max-pool-size=20,
    prefill=true
)

# Включить DataSource
/subsystem=datasources/data-source=HumanBeingDS:enable()

# Проверить подключение
/subsystem=datasources/data-source=HumanBeingDS:test-connection-in-pool()
```

#### Вариант Б: Через standalone.xml

Добавьте в `$WILDFLY_HOME/standalone/configuration/standalone.xml`:

```xml
<subsystem xmlns="urn:jboss:domain:datasources:7.0">
    <datasources>
        <datasource jndi-name="java:/jboss/datasources/HumanBeingDS"
                    pool-name="HumanBeingDS"
                    enabled="true"
                    use-java-context="true">
            <connection-url>jdbc:postgresql://localhost:5432/humanbeingdb</connection-url>
            <driver>postgresql</driver>
            <security>
                <user-name>your_user</user-name>
                <password>your_password</password>
            </security>
            <pool>
                <min-pool-size>5</min-pool-size>
                <max-pool-size>20</max-pool-size>
            </pool>
        </datasource>
        <drivers>
            <driver name="postgresql" module="org.postgresql">
                <xa-datasource-class>org.postgresql.xa.PGXADataSource</xa-datasource-class>
            </driver>
        </drivers>
    </datasources>
</subsystem>
```

### 3. Сборка WAR файла

```bash
mvn clean package -DskipTests
```

WAR файл будет в `target/human-being-manager.war`

### 4. Деплой приложения

#### Вариант А: Автоматический деплой

```bash
# Скопируйте WAR в deployments
cp target/human-being-manager.war $WILDFLY_HOME/standalone/deployments/

# WildFly автоматически задеплоит приложение
```

#### Вариант Б: Через CLI

```bash
$WILDFLY_HOME/bin/jboss-cli.sh --connect

deploy /Users/macbookair2020/Downloads/HumanRegistry/target/human-being-manager.war
```

#### Вариант В: Через Management Console

1. Откройте http://localhost:9990
2. Перейдите в Deployments
3. Нажмите "Add" и выберите WAR файл

### 5. Проверка деплоя

1. Проверьте логи: `$WILDFLY_HOME/standalone/log/server.log`
2. Откройте приложение: http://localhost:8080/human-being-manager/
3. Проверьте API: http://localhost:8080/human-being-manager/api/humanbeings

### 6. Настройка для production

#### Изменение портов

В `standalone.xml` или через CLI:
```bash
/socket-binding-group=standard-sockets/socket-binding=http:write-attribute(name=port,value=8080)
```

#### Настройка JVM

В `standalone.conf` или `standalone.conf.bat`:
```bash
JAVA_OPTS="$JAVA_OPTS -Xms512m -Xmx1024m"
```

#### Логирование

В `standalone.xml`:
```xml
<logger category="com.humanbeingmanager">
    <level name="INFO"/>
</logger>
```

## Troubleshooting

### Ошибка: "No data source found"
- Убедитесь, что DataSource создан и включен
- Проверьте JNDI имя в persistence.xml: `java:/jboss/datasources/HumanBeingDS`

### Ошибка: "Driver not found"
- Убедитесь, что PostgreSQL модуль установлен
- Проверьте имя драйвера в DataSource конфигурации

### Ошибка: "Connection refused"
- Проверьте доступность PostgreSQL
- Проверьте настройки подключения (host, port, database, user, password)

### Ошибка: "Transaction is not active"
- Убедитесь, что persistence.xml использует `transaction-type="JTA"`
- Проверьте, что методы сервисов помечены `@Transactional`

## Полезные команды CLI

```bash
# Список деплоев
deployment-info

# Статус деплоя
deployment-info --name=human-being-manager.war

# Перезапустить деплой
deployment redeploy --name=human-being-manager.war

# Удалить деплой
deployment undeploy --name=human-being-manager.war

# Тест DataSource
/subsystem=datasources/data-source=HumanBeingDS:test-connection-in-pool()

# Статистика DataSource
/subsystem=datasources/data-source=HumanBeingDS/statistics=pool:read-resource(include-runtime=true)
```

