#!/bin/bash

if ! command -v docker &> /dev/null
then
    echo "❌ Docker could not be found. Please install Docker before running this script."
    exit
fi

echo "🔥 Pulling latest images as per docker-compose.yml..."
docker-compose up -d --build
echo "🚀 Starting IntelliGuard stack..."

echo "✅ IntelliGuard environment is up and running!"

