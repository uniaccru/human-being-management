# Решение проблем с MinIO

## Проблема: "Failed to connect to /192.168.31.47:9000"

### Причина
MinIO не может подключиться к указанному адресу. Возможные причины:
1. MinIO не запущен
2. MinIO слушает только localhost, а не все интерфейсы
3. Неправильный IP адрес
4. Файрвол блокирует соединение

### Решение 1: Проверить, что MinIO запущен

```bash
docker ps | grep minio
```

Если MinIO не запущен, запустите его:

```bash
docker run -d -p 9000:9000 -p 9001:9001 \
  --name minio \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"
```

### Решение 2: Убедиться, что MinIO слушает на всех интерфейсах

MinIO по умолчанию слушает на `0.0.0.0`, но проверьте:

```bash
docker logs minio
```

Должно быть что-то вроде:
```
API: http://0.0.0.0:9000  http://127.0.0.1:9000
Console: http://0.0.0.0:9001 http://127.0.0.1:9001
```

### Решение 3: Проверить IP адрес

Убедитесь, что IP адрес правильный:

```bash
# Mac/Linux
ifconfig | grep "inet "

# Windows
ipconfig
```

Найдите IP адрес вашей машины в локальной сети (обычно начинается с 192.168.x.x или 10.x.x.x).

### Решение 4: Проверить доступность MinIO

Попробуйте подключиться к MinIO с вашего компьютера:

```bash
curl http://192.168.31.47:9000/minio/health/live
```

Если не работает, попробуйте:
```bash
curl http://localhost:9000/minio/health/live
```

### Решение 5: Использовать localhost вместо IP (если WildFly на той же машине)

Если WildFly запущен на той же машине, что и MinIO, используйте:

```bash
export MINIO_ENDPOINT=http://localhost:9000
```

### Решение 6: Проверить файрвол

Убедитесь, что порт 9000 открыт:

```bash
# Mac
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --listapps

# Linux
sudo ufw status
sudo firewall-cmd --list-ports
```

### Решение 7: Временное решение - приложение запустится без MinIO

Я обновил код так, что приложение **не упадет**, если MinIO недоступен при старте. Оно запустится, но операции с файлами будут выдавать ошибки до тех пор, пока MinIO не станет доступен.

**Важно:** После того, как MinIO станет доступен, перезапустите WildFly, чтобы MinIOService переинициализировался.

### Проверка после исправления

1. Убедитесь, что MinIO запущен и доступен
2. Проверьте переменные окружения на WildFly сервере:
   ```bash
   echo $MINIO_ENDPOINT
   ```
3. Перезапустите WildFly
4. Проверьте логи WildFly - должно быть сообщение об успешной инициализации MinIO

### Альтернатива: Использовать MinIO через Docker network

Если и WildFly, и MinIO в Docker, используйте имя контейнера:

```bash
export MINIO_ENDPOINT=http://minio:9000
```

И добавьте в docker-compose.yml:

```yaml
services:
  minio:
    # ... конфигурация MinIO
    networks:
      - app-network
      
  wildfly:
    # ... конфигурация WildFly
    networks:
      - app-network
    environment:
      MINIO_ENDPOINT: http://minio:9000

networks:
  app-network:
    driver: bridge
```

