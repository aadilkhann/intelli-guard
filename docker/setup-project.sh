#!/bin/bash

echo "🔥 Pulling latest images as per docker/docker-compose.yml..."
docker compose -f docker/docker-compose.yml pull

echo "🚀 Starting IntelliGuard stack..."
docker compose -f docker/docker-compose.yml up -d

echo "✅ IntelliGuard infra environment is up and running!"
