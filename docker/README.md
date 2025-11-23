# Docker Setup

This directory contains Docker Compose configuration for running IntelliGuard infrastructure services.

## Quick Start

```bash
# Start all infrastructure services
./run-dev.sh

# Or manually
docker-compose up -d
```

## Services

The following services will be started:

- **PostgreSQL** (port 5432) - Primary database
- **Redis** (port 6379) - Caching and session management
- **Kafka** (ports 9092, 29092) - Event streaming
- **Zookeeper** (port 2181) - Kafka coordination
- **Elasticsearch** (port 9200) - Search and analytics

## Scripts

- `run-dev.sh` - Start all services and create Kafka topics
- `setup-project.sh` - Pull images and start stack
- `docker-compose.yml` - Service definitions

## Environment Variables

See `.env.example` in the root directory for required environment variables.

## Stopping Services

```bash
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

## Note on extras/ Directory

The `extras/` directory is gitignored and contains experimental or alternative configurations. The canonical configuration is in this `docker/` directory.
