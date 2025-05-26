#!/bin/bash

# Build containers
docker-compose up -d --build

# Initialize databases
docker-compose exec postgres psql -U $POSTGRES_USER -d $POSTGRES_DB -f /scripts/schema.sql

# Create Kafka topics
docker-compose exec kafka kafka-topics \
  --create \
  --bootstrap-server kafka:9092 \
  --topic transactions \
  --partitions 3 \
  --replication-factor 1
