#!/bin/bash

# Clone repo
git clone https://github.com/your-team/intelli-guard.git
cd intelli-guard

# Create .env from template
cp .env.template .env

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
