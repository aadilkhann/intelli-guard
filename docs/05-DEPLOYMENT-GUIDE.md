# IntelliGuard - Deployment & DevOps Guide

**Version**: 1.0  
**Date**: November 2025  
**Target Environments**: Development, Staging, Production

---

## Table of Contents
1. [Development Setup](#1-development-setup)
2. [Docker Deployment](#2-docker-deployment)
3. [Kubernetes Deployment](#3-kubernetes-deployment)
4. [CI/CD Pipeline](#4-cicd-pipeline)
5. [Monitoring & Observability](#5-monitoring--observability)
6. [Troubleshooting](#6-troubleshooting)

---

## 1. Development Setup

### 1.1 Prerequisites

**Required Software**:
- Java 17 or higher
- Maven 3.8+
- Docker 24.0+
- Docker Compose 2.0+
- Node.js 18+ (for frontend)
- PostgreSQL 15+ (optional, can use Docker)
- Redis 7+ (optional, can use Docker)

**Recommended Tools**:
- IntelliJ IDEA / VS Code
- Postman (API testing)
- DBeaver (database management)
- k9s (Kubernetes management)

### 1.2 Clone Repository

```bash
git clone https://github.com/your-org/intelli-guard.git
cd intelli-guard
```

### 1.3 Environment Configuration

Create `.env` file in the root directory:

```bash
# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=intelli_guard
POSTGRES_USER=admin
POSTGRES_PASSWORD=admin

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Elasticsearch
ELASTICSEARCH_URL=http://localhost:9200

# JWT
JWT_SECRET=your-secret-key-change-in-production
JWT_EXPIRATION=3600

# External Services
SENDGRID_API_KEY=your-sendgrid-api-key
TWILIO_ACCOUNT_SID=your-twilio-sid
TWILIO_AUTH_TOKEN=your-twilio-token
MAXMIND_LICENSE_KEY=your-maxmind-key
```

### 1.4 Start Infrastructure Services

```bash
cd docker
./run-dev.sh
```

This starts:
- PostgreSQL (port 5432)
- Redis (port 6379)
- Kafka + Zookeeper (ports 9092, 2181)
- Elasticsearch (port 9200)

### 1.5 Build Services

```bash
# Build all services
cd services

# User Service
cd user-service
mvn clean install
mvn spring-boot:run

# Fraud Detection Service
cd ../fraud-scoring-service
mvn clean install
mvn spring-boot:run

# Gateway Service
cd ../Intelli-Guard
mvn clean install
mvn spring-boot:run

# Eureka Server
cd ../eureka-server
mvn clean install
mvn spring-boot:run
```

### 1.6 Run Frontend

```bash
cd admin-dashboard
npm install
npm run dev
```

Access at: `http://localhost:5173`

---

## 2. Docker Deployment

### 2.1 Docker Compose Architecture

```yaml
version: '3.8'

services:
  # Infrastructure
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: intelli_guard
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: admin
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U admin"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  zookeeper:
    image: confluentinc/cp-zookeeper:7.3.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.3.0
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:29092,PLAINTEXT_HOST://0.0.0.0:9092
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"
      - "29092:29092"
    healthcheck:
      test: ["CMD-SHELL", "kafka-topics --bootstrap-server localhost:29092 --list"]
      interval: 30s
      timeout: 10s
      retries: 5

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.7.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - ES_JAVA_OPTS=-Xms512m -Xmx512m
    ports:
      - "9200:9200"
    volumes:
      - es_data:/usr/share/elasticsearch/data

  # Application Services
  eureka-server:
    build: ./services/eureka-server
    ports:
      - "8761:8761"
    environment:
      SPRING_PROFILES_ACTIVE: docker
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5

  gateway:
    build: ./services/Intelli-Guard
    ports:
      - "8080:8080"
    depends_on:
      - eureka-server
      - redis
    environment:
      SPRING_PROFILES_ACTIVE: docker
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka
      SPRING_REDIS_HOST: redis
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5

  user-service:
    build: ./services/user-service
    ports:
      - "8081:8081"
    depends_on:
      - postgres
      - eureka-server
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/intelli_guard
      SPRING_DATASOURCE_USERNAME: admin
      SPRING_DATASOURCE_PASSWORD: admin
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka

  fraud-detection:
    build: ./services/fraud-scoring-service
    ports:
      - "8082:8082"
    depends_on:
      - postgres
      - redis
      - kafka
      - eureka-server
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/intelli_guard
      SPRING_REDIS_HOST: redis
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka

volumes:
  postgres_data:
  redis_data:
  es_data:
```

### 2.2 Build and Run

```bash
# Build all images
docker-compose build

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### 2.3 Multi-Stage Docker Build

**Example**: User Service Dockerfile

```dockerfile
# Stage 1: Build
FROM maven:3.8-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Add non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 3. Kubernetes Deployment

### 3.1 Cluster Setup

**Minikube** (Local Development):
```bash
minikube start --cpus=4 --memory=8192
minikube addons enable ingress
```

**EKS** (AWS Production):
```bash
eksctl create cluster \
  --name intelliguard-prod \
  --region us-west-2 \
  --nodegroup-name standard-workers \
  --node-type t3.xlarge \
  --nodes 3 \
  --nodes-min 3 \
  --nodes-max 10 \
  --managed
```

### 3.2 Namespace Setup

```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: intelliguard-prod
  labels:
    name: intelliguard-prod
```

```bash
kubectl apply -f namespace.yaml
```

### 3.3 ConfigMap & Secrets

**ConfigMap**:
```yaml
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: intelliguard-config
  namespace: intelliguard-prod
data:
  POSTGRES_HOST: postgres-service
  POSTGRES_PORT: "5432"
  POSTGRES_DB: intelli_guard
  REDIS_HOST: redis-service
  REDIS_PORT: "6379"
  KAFKA_BOOTSTRAP_SERVERS: kafka-service:9092
```

**Secrets**:
```yaml
# secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: intelliguard-secrets
  namespace: intelliguard-prod
type: Opaque
stringData:
  POSTGRES_PASSWORD: admin
  JWT_SECRET: your-jwt-secret
  SENDGRID_API_KEY: your-api-key
```

```bash
kubectl apply -f configmap.yaml
kubectl apply -f secrets.yaml
```

### 3.4 Deployment Example

**User Service Deployment**:
```yaml
# user-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  namespace: intelliguard-prod
spec:
  replicas: 3
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      containers:
      - name: user-service
        image: intelliguard/user-service:1.0.0
        ports:
        - containerPort: 8081
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: POSTGRES_HOST
          valueFrom:
            configMapKeyRef:
              name: intelliguard-config
              key: POSTGRES_HOST
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: intelliguard-secrets
              key: POSTGRES_PASSWORD
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8081
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: user-service
  namespace: intelliguard-prod
spec:
  selector:
    app: user-service
  ports:
  - protocol: TCP
    port: 8081
    targetPort: 8081
  type: ClusterIP
```

### 3.5 Horizontal Pod Autoscaler

```yaml
# hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: user-service-hpa
  namespace: intelliguard-prod
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: user-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### 3.6 Ingress

```yaml
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: intelliguard-ingress
  namespace: intelliguard-prod
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - api.intelliguard.io
    secretName: intelliguard-tls
  rules:
  - host: api.intelliguard.io
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: gateway-service
            port:
              number: 8080
```

---

## 4. CI/CD Pipeline

### 4.1 GitHub Actions Workflow

**.github/workflows/ci-cd.yml**:
```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven
    
    - name: Build with Maven
      run: mvn clean install -DskipTests
    
    - name: Run unit tests
      run: mvn test
    
    - name: Run integration tests
      run: mvn verify -P integration-tests
    
    - name: SonarQube Scan
      env:
        SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
      run: mvn sonar:sonar
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3

  security-scan:
    runs-on: ubuntu-latest
    needs: build-and-test
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v3
    
    - name: Run Snyk Security Scan
      uses: snyk/actions/maven@master
      env:
        SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
    
    - name: Run Trivy vulnerability scanner
      uses: aquasecurity/trivy-action@master
      with:
        scan-type: 'fs'
        scan-ref: '.'

  build-docker:
    runs-on: ubuntu-latest
    needs: security-scan
    if: github.ref == 'refs/heads/main'
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v3
    
    - name: Log in to Container Registry
      uses: docker/login-action@v2
      with:
        registry: ${{ env.REGISTRY }}
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}
    
    - name: Build and push Docker image
      uses: docker/build-push-action@v4
      with:
        context: ./services/user-service
        push: true
        tags: |
          ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/user-service:latest
          ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/user-service:${{ github.sha }}

  deploy-staging:
    runs-on: ubuntu-latest
    needs: build-docker
    if: github.ref == 'refs/heads/develop'
    
    steps:
    - name: Deploy to Staging
      uses: azure/k8s-deploy@v4
      with:
        manifests: |
          k8s/staging/deployment.yaml
        images: |
          ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/user-service:${{ github.sha }}
        namespace: intelliguard-staging

  deploy-production:
    runs-on: ubuntu-latest
    needs: build-docker
    if: github.ref == 'refs/heads/main'
    environment: production
    
    steps:
    - name: Deploy to Production
      uses: azure/k8s-deploy@v4
      with:
        manifests: |
          k8s/production/deployment.yaml
        images: |
          ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/user-service:${{ github.sha }}
        namespace: intelliguard-prod
```

---

## 5. Monitoring & Observability

### 5.1 Prometheus Setup

```yaml
# prometheus-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
    
    scrape_configs:
      - job_name: 'spring-boot'
        metrics_path: '/actuator/prometheus'
        kubernetes_sd_configs:
          - role: pod
        relabel_configs:
          - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
            action: keep
            regex: true
```

### 5.2 Grafana Dashboards

Import dashboards:
- **JVM Metrics**: Dashboard ID 4701
- **Spring Boot**: Dashboard ID 12900
- **Kafka**: Dashboard ID 7589

### 5.3 ELK Stack

**Filebeat Configuration**:
```yaml
filebeat.inputs:
- type: container
  paths:
    - /var/log/containers/*.log

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  index: "intelliguard-%{+yyyy.MM.dd}"
```

---

## 6. Troubleshooting

### 6.1 Common Issues

**Issue**: Service not registering with Eureka
```bash
# Check Eureka server logs
kubectl logs -n intelliguard-prod deployment/eureka-server

# Verify service can reach Eureka
kubectl exec -it pod/user-service-xxx -- curl http://eureka-server:8761/eureka/apps
```

**Issue**: Database connection failures
```bash
# Check PostgreSQL pod
kubectl get pods -n intelliguard-prod | grep postgres
kubectl logs -n intelliguard-prod postgres-0

# Test connection
kubectl exec -it pod/user-service-xxx -- nc -zv postgres-service 5432
```

**Issue**: Kafka consumer lag
```bash
# Check consumer group lag
docker exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:29092 \
  --describe \
  --group fraud-group
```

---

**Document Control**

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | Nov 2025 | DevOps Team | Initial guide |
