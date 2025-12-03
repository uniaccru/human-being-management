# Быстрое решение: Connect timed out

## Проблема
Ошибка: `Connect timed out` при импорте файла означает, что WildFly не может подключиться к MinIO.

## Решение (выполните на сервере WildFly)

### 1. Проверьте доступность MinIO

**ВАЖНО:** Выполните эту команду **на сервере, где запущен WildFly**:

```bash
curl http://192.168.31.47:9000/minio/health/live
```

**Если команда НЕ работает** (timeout или connection refused):
- MinIO недоступен с сервера WildFly
- Переходите к шагу 2

**Если команда работает** (возвращает `OK`):
- Проблема в конфигурации приложения
- Проверьте переменные окружения (шаг 3)

### 2. Если MinIO недоступен

#### Вариант A: WildFly и MinIO на одной машине

Используйте `localhost` вместо IP:

```bash
# Остановите WildFly
# Установите переменные окружения
export MINIO_ENDPOINT=http://localhost:9000
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin
export MINIO_BUCKET_NAME=import-files

# Запустите WildFly
./standalone.sh
```

#### Вариант B: WildFly и MinIO на разных машинах

1. Проверьте, что MinIO запущен:
   ```bash
   # На машине с MinIO
   docker ps | grep minio
   ```

2. Проверьте IP адрес машины с MinIO:
   ```bash
   # На машине с MinIO
   ifconfig | grep "inet "  # Mac/Linux
   ipconfig                  # Windows
   ```

3. Убедитесь, что MinIO слушает на всех интерфейсах:
   ```bash
   # На машине с MinIO
   docker logs minio | grep "API:"
   ```
   Должно быть: `API: http://0.0.0.0:9000`

4. Проверьте доступность с сервера WildFly:
   ```bash
   # На сервере WildFly
   ping <IP_АДРЕС_МИНИО>
   curl http://<IP_АДРЕС_МИНИО>:9000/minio/health/live
   ```

5. Если доступность подтверждена, используйте правильный IP:
   ```bash
   # На сервере WildFly
   export MINIO_ENDPOINT=http://<ПРАВИЛЬНЫЙ_IP>:9000
   export MINIO_ACCESS_KEY=minioadmin
   export MINIO_SECRET_KEY=minioadmin
   export MINIO_BUCKET_NAME=import-files
   ./standalone.sh
   ```

### 3. Проверьте переменные окружения

Убедитесь, что переменные установлены **ПЕРЕД запуском** WildFly:

```bash
# На сервере WildFly
env | grep MINIO
```

Должны быть:
- `MINIO_ENDPOINT=http://...`
- `MINIO_ACCESS_KEY=...`
- `MINIO_SECRET_KEY=...`
- `MINIO_BUCKET_NAME=...`

### 4. Используйте диагностический скрипт

```bash
# На сервере WildFly
export MINIO_ENDPOINT=http://192.168.31.47:9000
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin
export MINIO_BUCKET_NAME=import-files

./diagnose_minio.sh
```

Скрипт покажет, в чем именно проблема.

### 5. Проверьте эндпоинт test-minio

После перезапуска WildFly проверьте:

```bash
curl http://localhost:8080/human-being-manager/api/import/test-minio
```

Должен вернуть JSON с информацией о подключении к MinIO.

## Частые причины

1. **Неправильный IP адрес** - IP изменился или указан неверно
2. **MinIO слушает только localhost** - нужно перезапустить с правильными параметрами
3. **Файрвол блокирует соединение** - проверьте настройки файрвола
4. **MinIO не запущен** - запустите: `docker ps | grep minio`
5. **Переменные окружения не установлены** - установите перед запуском WildFly

## После исправления

1. Пересоберите проект: `mvn clean package`
2. Перезапустите WildFly с правильными переменными окружения
3. Проверьте эндпоинт test-minio
4. Попробуйте импорт снова

