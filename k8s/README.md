# Kubernetes Manifests

Kubernetes deployment configurations for IntelliGuard.

## Directory Structure

```
k8s/
├── production/        # Production environment
│   ├── deployments/
│   ├── services/
│   ├── configmaps/
│   └── secrets/
└── staging/           # Staging environment
    ├── deployments/
    ├── services/
    ├── configmaps/
    └── secrets/
```

## Deployment

```bash
# Deploy to staging
kubectl apply -f k8s/staging/

# Deploy to production
kubectl apply -f k8s/production/
```

Coming soon...
