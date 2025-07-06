#!/bin/bash

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

echo "🚀 Starting IntelliGuard full development stack..."
docker-compose up -d --build

# Dynamically detect the Kafka container name
KAFKA_CONTAINER=$(docker ps --filter "ancestor=confluentinc/cp-kafka:7.3.0" --format "{{.Names}}" | head -n 1)

if [ -z "$KAFKA_CONTAINER" ]; then
    echo "❌ Could not find a running Kafka container based on the image 'confluentinc/cp-kafka:7.3.0'"
    docker ps
    exit 1
fi

echo "🔎 Kafka container detected: $KAFKA_CONTAINER"

# Wait for Kafka to become available
BOOTSTRAP_SERVER="localhost:29092"
MAX_RETRIES=20
COUNTER=0
KAFKA_TOPIC="pending-payment-pool"

echo "⏳ Waiting for Kafka to be ready at $BOOTSTRAP_SERVER..."
until docker exec "$KAFKA_CONTAINER" kafka-topics --bootstrap-server "$BOOTSTRAP_SERVER" --list &> /dev/null
do
    sleep 3
    ((COUNTER++))
    echo "🔄 Attempt $COUNTER of $MAX_RETRIES..."
    if [ "$COUNTER" -ge "$MAX_RETRIES" ]; then
        echo "❌ Kafka did not become ready in time."
        echo "📋 Last 30 lines of Kafka logs:"
        docker logs "$KAFKA_CONTAINER" | tail -n 30
        exit 1
    fi
done

# Create topic
echo "📌 Creating Kafka topic: pending-payment-pool"
docker exec "$KAFKA_CONTAINER" kafka-topics \
  --create \
  --topic "$KAFKA_TOPIC" \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --partitions 1 \
  --replication-factor 1 \
  --if-not-exists

# List topics
echo "📋 Listing Kafka topics:"
docker exec "$KAFKA_CONTAINER" kafka-topics \
  --list \
  --bootstrap-server "$BOOTSTRAP_SERVER"

# Describe a topic
echo "🔍 Describing Kafka topic:"
docker exec "$KAFKA_CONTAINER" kafka-topics \
  --describe \
  --topic "$KAFKA_TOPIC" \
  --bootstrap-server "$BOOTSTRAP_SERVER"
