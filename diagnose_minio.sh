#!/bin/bash

# Скрипт для диагностики проблем с подключением к MinIO

echo "=========================================="
echo "MinIO Connection Diagnostic Script"
echo "=========================================="
echo ""

# 1. Проверка переменных окружения
echo "1. Checking environment variables:"
echo "-----------------------------------"
if [ -z "$MINIO_ENDPOINT" ]; then
    echo "❌ MINIO_ENDPOINT is NOT SET"
else
    echo "✅ MINIO_ENDPOINT=$MINIO_ENDPOINT"
fi

if [ -z "$MINIO_ACCESS_KEY" ]; then
    echo "❌ MINIO_ACCESS_KEY is NOT SET"
else
    echo "✅ MINIO_ACCESS_KEY is SET (length: ${#MINIO_ACCESS_KEY})"
fi

if [ -z "$MINIO_SECRET_KEY" ]; then
    echo "❌ MINIO_SECRET_KEY is NOT SET"
else
    echo "✅ MINIO_SECRET_KEY is SET (length: ${#MINIO_SECRET_KEY})"
fi

if [ -z "$MINIO_BUCKET_NAME" ]; then
    echo "❌ MINIO_BUCKET_NAME is NOT SET"
else
    echo "✅ MINIO_BUCKET_NAME=$MINIO_BUCKET_NAME"
fi
echo ""

# 2. Извлечение хоста и порта из MINIO_ENDPOINT
if [ ! -z "$MINIO_ENDPOINT" ]; then
    # Удаляем http:// или https://
    HOST_PORT=$(echo $MINIO_ENDPOINT | sed 's|https\?://||')
    HOST=$(echo $HOST_PORT | cut -d: -f1)
    PORT=$(echo $HOST_PORT | cut -d: -f2)
    
    echo "2. Testing network connectivity:"
    echo "-----------------------------------"
    echo "Host: $HOST"
    echo "Port: $PORT"
    echo ""
    
    # 3. Проверка доступности порта
    echo "3. Testing port connectivity:"
    echo "-----------------------------------"
    if command -v nc &> /dev/null; then
        if nc -z -w 5 $HOST $PORT 2>/dev/null; then
            echo "✅ Port $PORT is reachable on $HOST"
        else
            echo "❌ Port $PORT is NOT reachable on $HOST"
            echo "   This means WildFly cannot connect to MinIO!"
        fi
    else
        echo "⚠️  'nc' (netcat) not found, skipping port test"
    fi
    echo ""
    
    # 4. Проверка HTTP доступности
    echo "4. Testing HTTP connectivity:"
    echo "-----------------------------------"
    if command -v curl &> /dev/null; then
        echo "Testing: curl $MINIO_ENDPOINT/minio/health/live"
        HTTP_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" --max-time 10 "$MINIO_ENDPOINT/minio/health/live" 2>&1)
        HTTP_CODE=$(echo "$HTTP_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
        HTTP_BODY=$(echo "$HTTP_RESPONSE" | grep -v "HTTP_CODE")
        
        if [ "$HTTP_CODE" = "200" ]; then
            echo "✅ MinIO is accessible via HTTP"
            echo "   Response: $HTTP_BODY"
        else
            echo "❌ MinIO is NOT accessible via HTTP"
            echo "   HTTP Code: $HTTP_CODE"
            echo "   Response: $HTTP_BODY"
            echo ""
            echo "   Possible causes:"
            echo "   - MinIO is not running"
            echo "   - Wrong IP address or port"
            echo "   - Firewall blocking connection"
            echo "   - MinIO not listening on all interfaces (0.0.0.0)"
        fi
    else
        echo "⚠️  'curl' not found, skipping HTTP test"
    fi
    echo ""
    
    # 5. Проверка Docker контейнера MinIO
    echo "5. Checking MinIO Docker container:"
    echo "-----------------------------------"
    if command -v docker &> /dev/null; then
        MINIO_CONTAINER=$(docker ps | grep minio | head -1)
        if [ ! -z "$MINIO_CONTAINER" ]; then
            echo "✅ MinIO container is running:"
            echo "$MINIO_CONTAINER"
            echo ""
            echo "Checking MinIO logs for listening address:"
            docker logs minio 2>&1 | grep -i "API:" | head -1
        else
            echo "❌ MinIO container is NOT running"
            echo "   Start it with:"
            echo "   docker run -d -p 9000:9000 -p 9001:9001 \\"
            echo "     --name minio \\"
            echo "     -e MINIO_ROOT_USER=minioadmin \\"
            echo "     -e MINIO_ROOT_PASSWORD=minioadmin \\"
            echo "     minio/minio server /data --console-address \":9001\""
        fi
    else
        echo "⚠️  'docker' not found, skipping container check"
    fi
    echo ""
fi

# 6. Рекомендации
echo "6. Recommendations:"
echo "-----------------------------------"
if [ -z "$MINIO_ENDPOINT" ]; then
    echo "❌ Set MINIO_ENDPOINT environment variable"
    echo "   Example: export MINIO_ENDPOINT=http://192.168.31.47:9000"
elif [ ! -z "$HOST" ] && [ "$HOST" != "localhost" ] && [ "$HOST" != "127.0.0.1" ]; then
    echo "ℹ️  You're using IP address: $HOST"
    echo "   Make sure:"
    echo "   1. MinIO is running and accessible from this machine"
    echo "   2. MinIO is listening on 0.0.0.0 (all interfaces)"
    echo "   3. Firewall allows connections on port $PORT"
    echo "   4. IP address $HOST is correct for your network"
    echo ""
    echo "   Test from this machine:"
    echo "   curl $MINIO_ENDPOINT/minio/health/live"
    echo ""
    echo "   If that works but WildFly can't connect, check:"
    echo "   - Is WildFly on a different machine? Use the correct IP"
    echo "   - Is there a firewall between WildFly and MinIO?"
    echo "   - Can WildFly server reach $HOST:$PORT?"
else
    echo "ℹ️  Using localhost - MinIO should be on the same machine as WildFly"
fi
echo ""

echo "=========================================="
echo "Diagnostic complete"
echo "=========================================="

