#!/bin/bash

if ! command -v docker &> /dev/null
then
    echo "❌ Docker could not be found. Please install Docker before running this script."
    exit
fi

echo "🚀 Starting IntelliGuard full development stack..."
docker-compose up -d --build
