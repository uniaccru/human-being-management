# Настройка SSH туннеля для MinIO

## Проблема
WildFly на удаленном сервере не может достучаться до MinIO на вашей локальной машине, потому что `192.168.31.47` - это локальный IP в вашей домашней сети.

## Решение: SSH туннель

### Вариант 1: Проброс порта MinIO на удаленный сервер (Рекомендуется)

Создайте SSH туннель, который пробросит порт MinIO (9000) на удаленный сервер:

```bash
# На вашей локальной машине (MacBook)
ssh -R 9000:localhost:9000 user@remote-server
```

Где:
- `-R 9000:localhost:9000` - проброс порта 9000 с удаленного сервера на локальный localhost:9000
- `user@remote-server` - ваш пользователь и адрес удаленного сервера

После этого на удаленном сервере MinIO будет доступен по `localhost:9000`.

**Настройка WildFly:**
```bash
# На удаленном сервере (в SSH сессии с туннелем)
export MINIO_ENDPOINT=http://localhost:9000
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin
export MINIO_BUCKET_NAME=import-files
./standalone.sh
```

### Вариант 2: Постоянный SSH туннель в фоне

Если нужно, чтобы туннель работал постоянно:

```bash
# На вашей локальной машине
ssh -f -N -R 9000:localhost:9000 user@remote-server
```

Где:
- `-f` - запуск в фоне
- `-N` - не выполнять команды, только туннель
- `-R 9000:localhost:9000` - проброс порта

Проверка, что туннель работает:
```bash
# На удаленном сервере
curl http://localhost:9000/minio/health/live
```

### Вариант 3: Через SSH config (удобнее)

Добавьте в `~/.ssh/config` на локальной машине:

```
Host remote-wildfly
    HostName your-remote-server.com
    User your-username
    RemoteForward 9000 localhost:9000
```

Затем подключайтесь:
```bash
ssh remote-wildfly
```

## Проверка работы

1. **На локальной машине** убедитесь, что MinIO работает:
   ```bash
   curl http://localhost:9000/minio/health/live
   ```

2. **На удаленном сервере** (после создания туннеля):
   ```bash
   curl http://localhost:9000/minio/health/live
   ```
   Должен вернуть `OK`

3. **Настройте WildFly** с `MINIO_ENDPOINT=http://localhost:9000`

## Альтернативные решения

### Решение 2: Запустить MinIO на удаленном сервере

Если возможно, запустите MinIO на том же сервере, где WildFly:

```bash
# На удаленном сервере
docker run -d -p 9000:9000 -p 9001:9001 \
  --name minio \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"
```

Затем используйте:
```bash
export MINIO_ENDPOINT=http://localhost:9000
```

### Решение 3: Использовать ngrok (для тестирования)

Если нужен быстрый доступ без настройки:

1. Установите ngrok: https://ngrok.com/
2. На локальной машине:
   ```bash
   ngrok http 9000
   ```
3. Используйте публичный URL от ngrok в `MINIO_ENDPOINT`

**Внимание:** ngrok бесплатный план имеет ограничения, не используйте для продакшена.

## Рекомендация

Для разработки лучше всего использовать **SSH туннель** (Вариант 1) - это безопасно и не требует дополнительных сервисов.

