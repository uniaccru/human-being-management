# Конфигурация проекта

## 1. Druid Connection Pool

### Параметры конфигурации

Druid Connection Pool настроен через WildFly datasource. Основные параметры:

- **initialSize**: 5 - начальное количество соединений
- **minIdle**: 5 - минимальное количество простаивающих соединений
- **maxActive**: 20 - максимальное количество активных соединений
- **maxWait**: -1 (неограниченно) - максимальное время ожидания соединения
- **validationQuery**: "SELECT 1" - SQL запрос для валидации соединений
- **testOnBorrow**: false - проверять соединение при получении
- **testOnReturn**: false - проверять соединение при возврате
- **testWhileIdle**: true - проверять простаивающие соединения
- **timeBetweenEvictionRunsMillis**: 60000 - интервал между проверками (60 секунд)
- **minEvictableIdleTimeMillis**: 1800000 - минимальное время простоя перед удалением (30 минут)
- **removeAbandoned**: true - удалять заброшенные соединения
- **removeAbandonedTimeout**: 300 - таймаут для заброшенных соединений (5 минут)

### Настройка в WildFly

Datasource настраивается через WildFly CLI или консоль управления. Файл `druid-ds.xml` служит справочной документацией.

## 2. L2 JPA Cache (Ehcache)

### Параметры конфигурации

Конфигурация находится в `src/main/resources/ehcache.xml`:

- **maxElementsInMemory**: 1000 - максимальное количество элементов в памяти
- **maxElementsOnDisk**: неограниченно - максимальное количество элементов на диске
- **eternal**: false - элементы не вечны
- **timeToLiveSeconds**: 3600 (1 час) - время жизни элемента
- **timeToIdleSeconds**: 1800 (30 минут) - время простоя перед удалением
- **overflowToDisk**: true - переполнение на диск
- **diskPersistent**: true - персистентность на диске
- **memoryStoreEvictionPolicy**: LRU - политика вытеснения (Least Recently Used)

### Включение/выключение логирования статистики кэша

Логирование статистики L2 JPA Cache можно включить/выключить через системное свойство:

```bash
-Dcache.statistics.enabled=true
```

Или через переменную окружения:
```bash
export CACHE_STATISTICS_ENABLED=true
```

По умолчанию логирование выключено (`false`).

### Использование

CDI Interceptor `@CacheStatisticsLogging` автоматически логирует статистику для методов, помеченных этой аннотацией. В текущей реализации логируется время выполнения методов как индикатор производительности кэша.

## 3. MinIO Configuration

### Переменные окружения

MinIO настраивается через переменные окружения:

- **MINIO_ENDPOINT**: URL локального MinIO (по умолчанию: `http://localhost:9000`)
- **MINIO_ACCESS_KEY**: Access key для MinIO (по умолчанию: `minioadmin`)
- **MINIO_SECRET_KEY**: Secret key для MinIO (по умолчанию: `minioadmin`)
- **MINIO_BUCKET_NAME**: Имя bucket для хранения файлов (по умолчанию: `import-files`)

### Запуск MinIO локально

```bash
# Запуск MinIO через Docker
docker run -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"
```

Или через docker-compose (добавить в docker-compose.yml):

```yaml
minio:
  image: minio/minio
  ports:
    - "9000:9000"
    - "9001:9001"
  environment:
    MINIO_ROOT_USER: minioadmin
    MINIO_ROOT_PASSWORD: minioadmin
  command: server /data --console-address ":9001"
  volumes:
    - minio_data:/data
```

### Настройка доступа с удаленного сервера

Для доступа к MinIO с удаленного WildFly сервера:

1. Убедитесь, что MinIO доступен по сети (не только localhost)
2. Настройте `MINIO_ENDPOINT` на URL, доступный с сервера WildFly
3. Если MinIO запущен локально, используйте IP-адрес машины вместо localhost

Пример:
```bash
export MINIO_ENDPOINT=http://192.168.1.100:9000
```

## 4. Распределенные транзакции

### Двухфазный коммит

Реализован собственный механизм двухфазного коммита на уровне бизнес-логики:

**Phase 1 (Prepare)**:
- Файл загружается в MinIO с временным ключом (`temp/`)
- Данные валидируются и подготавливаются для БД

**Phase 2 (Commit)**:
- Если все успешно: файл переименовывается из `temp/` в финальное местоположение
- Данные фиксируются в БД

**Rollback**:
- При ошибке БД: файл удаляется из MinIO
- При ошибке MinIO: транзакция БД откатывается
- При ошибке бизнес-логики: обе операции откатываются

### Обработка ошибок

Система обрабатывает следующие сценарии:

1. **Отказ MinIO** (БД работает): транзакция БД откатывается
2. **Отказ БД** (MinIO работает): файл удаляется из MinIO
3. **RuntimeException в бизнес-логике**: обе операции откатываются
4. **Параллельные запросы**: обрабатываются thread-safe с использованием уникальных transaction ID

## 5. Остановка приложения

После демонстрации необходимо остановить:

1. **WildFly сервер**: 
   ```bash
   ./stop.sh
   # или
   $WILDFLY_HOME/bin/jboss-cli.sh --connect command=:shutdown
   ```

2. **MinIO**:
   ```bash
   docker stop <minio-container-id>
   # или через docker-compose
   docker-compose down
   ```

3. **База данных** (если запущена локально):
   ```bash
   docker-compose down
   ```



