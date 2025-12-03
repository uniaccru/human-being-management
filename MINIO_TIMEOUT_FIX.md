# Исправление проблемы с таймаутами MinIO

## Проблема
Импорт из JSON файла выдавал ошибку "timeout of 60000ms exceeded", а эндпоинт `/import/test-minio` не возвращал ответ.

## Причина
MinIO Java клиент не имел настроенных таймаутов, что приводило к зависанию при попытке подключения к недоступному MinIO серверу.

## Исправления

### 1. Добавлены таймауты в MinIO клиент
- **Connection timeout**: 10 секунд - время ожидания установки соединения
- **Write timeout**: 30 секунд - время ожидания записи данных
- **Read timeout**: 30 секунд - время ожидания чтения данных

Теперь при недоступности MinIO запросы будут завершаться с ошибкой через 10 секунд вместо зависания на неопределенное время.

### 2. Улучшен эндпоинт `/import/test-minio`
- Теперь использует легковесную операцию `fileExists()` вместо загрузки тестового файла
- Добавлена обработка специфичных исключений (TimeoutException, ConnectException)
- Более информативные сообщения об ошибках

### 3. Улучшена обработка ошибок
- Все методы MinIOService теперь различают типы ошибок (timeout, connection refused, etc.)
- Более детальные сообщения об ошибках в логах
- Информативные исключения с указанием endpoint и причины ошибки

## Что нужно сделать

### 1. Пересобрать проект
```bash
mvn clean package
```

### 2. Перезапустить WildFly
После пересборки перезапустите WildFly с установленными переменными окружения:
```bash
export MINIO_ENDPOINT=http://192.168.31.47:9000
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin
export MINIO_BUCKET_NAME=import-files
./standalone.sh
```

### 3. Проверить доступность MinIO
Перед запуском WildFly убедитесь, что MinIO доступен:
```bash
# Проверка с вашего компьютера
curl http://192.168.31.47:9000/minio/health/live

# Должен вернуть: OK
```

### 4. Проверить эндпоинт test-minio
После запуска WildFly проверьте эндпоинт:
```bash
curl http://localhost:8080/human-being-manager/api/import/test-minio
```

Теперь он должен быстро вернуть ответ (в течение 10-15 секунд) даже если MinIO недоступен.

### 5. Проверить логи WildFly
Проверьте логи на наличие сообщений об инициализации MinIO:
```bash
tail -f $WILDFLY_HOME/standalone/log/server.log | grep -i minio
```

Должно быть сообщение:
- `MinIO service initialized successfully` - если MinIO доступен
- `MinIO connection test failed` - если MinIO недоступен (но приложение все равно запустится)

## Диагностика проблем

### Использование диагностического скрипта

Запустите скрипт диагностики на сервере WildFly:
```bash
# Установите переменные окружения
export MINIO_ENDPOINT=http://192.168.31.47:9000
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin
export MINIO_BUCKET_NAME=import-files

# Запустите диагностику
./diagnose_minio.sh
```

Скрипт проверит:
- ✅ Переменные окружения
- ✅ Доступность порта
- ✅ HTTP доступность MinIO
- ✅ Статус Docker контейнера MinIO

### Если MinIO недоступен (Connect timed out)

**Ошибка:** `Connect timed out` или `SocketTimeoutException`

Это означает, что **WildFly не может достучаться до MinIO** по указанному адресу.

#### Шаг 1: Проверьте доступность MinIO с сервера WildFly

**ВАЖНО:** Выполните это на сервере, где запущен WildFly:

```bash
# На сервере WildFly выполните:
curl http://192.168.31.47:9000/minio/health/live
```

**Если команда не работает:**
- MinIO недоступен с сервера WildFly
- Проверьте IP адрес - возможно он изменился
- Проверьте, что MinIO запущен: `docker ps | grep minio`

#### Шаг 2: Проверьте, что MinIO слушает на всех интерфейсах

```bash
docker logs minio | grep "API:"
```

Должно быть: `API: http://0.0.0.0:9000` (не только `127.0.0.1`)

Если MinIO слушает только на `127.0.0.1`, перезапустите его:
```bash
docker stop minio
docker rm minio
docker run -d -p 9000:9000 -p 9001:9001 \
  --name minio \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"
```

#### Шаг 3: Проверьте правильность IP адреса

Если WildFly и MinIO на **одной машине**, используйте `localhost`:
```bash
export MINIO_ENDPOINT=http://localhost:9000
```

Если WildFly и MinIO на **разных машинах**:
1. Узнайте IP адрес машины с MinIO:
   ```bash
   # На машине с MinIO
   ifconfig | grep "inet "  # Linux/Mac
   ipconfig                  # Windows
   ```

2. Убедитесь, что IP правильный и машины в одной сети

3. Проверьте доступность с WildFly сервера:
   ```bash
   # На сервере WildFly
   ping 192.168.31.47
   curl http://192.168.31.47:9000/minio/health/live
   ```

#### Шаг 4: Проверьте файрвол

Убедитесь, что порт 9000 открыт:
```bash
# Mac
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --listapps

# Linux
sudo ufw status
sudo firewall-cmd --list-ports
```

#### Шаг 5: Проверьте переменные окружения

```bash
# На сервере WildFly
env | grep MINIO
```

Убедитесь, что переменные установлены **перед запуском** WildFly.

### Если импорт все еще не работает
1. Проверьте логи WildFly во время импорта
2. Проверьте, что MinIO доступен с сервера WildFly
3. Убедитесь, что bucket `import-files` существует в MinIO
4. Проверьте права доступа (access key и secret key)

## Ожидаемое поведение после исправления

- **Если MinIO доступен**: импорт работает нормально, файлы сохраняются в MinIO
- **Если MinIO недоступен**: импорт завершается с ошибкой через ~10 секунд с понятным сообщением вместо зависания на 60 секунд
- **Эндпоинт test-minio**: всегда возвращает ответ в течение 10-15 секунд

