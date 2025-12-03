#!/bin/bash

# Скрипт для настройки SSH туннеля для MinIO

echo "=========================================="
echo "MinIO SSH Tunnel Setup"
echo "=========================================="
echo ""

# Проверка, что MinIO работает локально
echo "1. Checking local MinIO..."
if curl -s http://localhost:9000/minio/health/live > /dev/null 2>&1; then
    echo "✅ MinIO is running locally on port 9000"
else
    echo "❌ MinIO is NOT running locally"
    echo "   Start it with:"
    echo "   docker run -d -p 9000:9000 -p 9001:9001 \\"
    echo "     --name minio \\"
    echo "     -e MINIO_ROOT_USER=minioadmin \\"
    echo "     -e MINIO_ROOT_PASSWORD=minioadmin \\"
    echo "     minio/minio server /data --console-address \":9001\""
    exit 1
fi
echo ""

# Запрос данных для подключения
if [ -z "$1" ]; then
    echo "Usage: $0 user@remote-server"
    echo ""
    echo "Example:"
    echo "  $0 myuser@192.168.1.100"
    echo "  $0 myuser@example.com"
    exit 1
fi

REMOTE_SERVER=$1

echo "2. Setting up SSH tunnel to $REMOTE_SERVER"
echo "-----------------------------------"
echo ""
echo "This will create a reverse tunnel:"
echo "  Remote server:localhost:9000 -> Local:localhost:9000"
echo ""
echo "Press Ctrl+C to stop the tunnel"
echo ""

# Создание туннеля
ssh -R 9000:localhost:9000 $REMOTE_SERVER

echo ""
echo "Tunnel closed."

