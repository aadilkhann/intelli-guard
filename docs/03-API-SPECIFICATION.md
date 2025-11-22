# IntelliGuard - API Specification Document

**Version**: 1.0  
**Date**: November 2025  
**Base URL**: `https://api.intelliguard.io`  
**Protocol**: HTTPS only

---

## Table of Contents
1. [Authentication](#1-authentication)
2. [User Service API](#2-user-service-api)
3. [Data Ingestion API](#3-data-ingestion-api)
4. [Fraud Detection API](#4-fraud-detection-api)
5. [Alerting API](#5-alerting-api)
6. [Admin API](#6-admin-api)
7. [Error Handling](#7-error-handling)
8. [Rate Limiting](#8-rate-limiting)

---

## 1. Authentication

### 1.1 Authentication Methods

IntelliGuard supports two authentication methods:

1. **JWT Bearer Token** (for user sessions)
2. **API Key** (for service-to-service communication)

### 1.2 Obtaining JWT Token

**Endpoint**: `POST /api/v1/users/login`

**Request**:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "123",
    "email": "user@example.com",
    "role": "ANALYST"
  }
}
```

### 1.3 Using JWT Token

Include the token in the `Authorization` header:

```http
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 1.4 Refreshing Token

**Endpoint**: `POST /api/v1/users/refresh`

**Request**:
```json
{
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

---

## 2. User Service API

### 2.1 Register User

**Endpoint**: `POST /api/v1/users/register`  
**Authentication**: None  
**Rate Limit**: 10 requests/hour per IP

**Request**:
```json
{
  "email": "newuser@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response** (201 Created):
```json
{
  "id": "456",
  "email": "newuser@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "VIEWER",
  "status": "PENDING_VERIFICATION",
  "createdAt": "2025-11-22T10:30:00Z"
}
```

**Validation Rules**:
- Email: RFC 5322 compliant
- Password: Min 8 chars, 1 uppercase, 1 lowercase, 1 number, 1 special char
- First/Last Name: 2-100 characters

---

### 2.2 Get User Profile

**Endpoint**: `GET /api/v1/users/{userId}`  
**Authentication**: JWT (Bearer Token)  
**Authorization**: User can only access their own profile, or ADMIN role

**Response** (200 OK):
```json
{
  "id": "123",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "ANALYST",
  "status": "ACTIVE",
  "createdAt": "2025-01-15T08:00:00Z",
  "lastLoginAt": "2025-11-22T09:15:00Z"
}
```

---

### 2.3 Update User Profile

**Endpoint**: `PUT /api/v1/users/{userId}`  
**Authentication**: JWT (Bearer Token)  
**Authorization**: User can only update their own profile

**Request**:
```json
{
  "firstName": "Jane",
  "lastName": "Smith"
}
```

**Response** (200 OK):
```json
{
  "id": "123",
  "email": "user@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "role": "ANALYST",
  "status": "ACTIVE",
  "updatedAt": "2025-11-22T10:45:00Z"
}
```

---

### 2.4 Request Password Reset

**Endpoint**: `POST /api/v1/users/reset-password`  
**Authentication**: None  
**Rate Limit**: 3 requests/hour per email

**Request**:
```json
{
  "email": "user@example.com"
}
```

**Response** (200 OK):
```json
{
  "message": "If the email exists, a password reset link has been sent."
}
```

**Note**: Always returns 200 OK to prevent email enumeration.

---

### 2.5 Delete User

**Endpoint**: `DELETE /api/v1/users/{userId}`  
**Authentication**: JWT (Bearer Token)  
**Authorization**: ADMIN role required

**Response** (204 No Content)

**Note**: Soft delete - user data is retained for compliance but account is deactivated.

---

## 3. Data Ingestion API

### 3.1 Ingest Single Transaction

**Endpoint**: `POST /api/v1/transactions`  
**Authentication**: API Key or JWT  
**Rate Limit**: 1000 requests/minute per API key

**Request**:
```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user-123",
  "amount": 1250.50,
  "currency": "USD",
  "merchantId": "merchant-456",
  "merchantName": "Amazon",
  "paymentMethod": "CARD",
  "cardLast4": "4242",
  "timestamp": "2025-11-22T10:30:00Z",
  "ipAddress": "203.0.113.42",
  "deviceFingerprint": "fp-abc123",
  "geolocation": {
    "latitude": 37.7749,
    "longitude": -122.4194,
    "country": "US",
    "city": "San Francisco"
  },
  "metadata": {
    "userAgent": "Mozilla/5.0...",
    "referrer": "https://example.com"
  }
}
```

**Response** (202 Accepted):
```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PROCESSING",
  "message": "Transaction accepted for fraud analysis",
  "estimatedProcessingTime": "100ms"
}
```

**Field Descriptions**:
- `transactionId`: Unique UUID for idempotency
- `amount`: Decimal with 2 decimal places
- `currency`: ISO 4217 currency code
- `paymentMethod`: Enum - `CARD`, `BANK_TRANSFER`, `WALLET`, `CRYPTO`
- `timestamp`: ISO 8601 datetime
- `ipAddress`: IPv4 or IPv6
- `deviceFingerprint`: Unique device identifier

---

### 3.2 Ingest Batch Transactions

**Endpoint**: `POST /api/v1/transactions/batch`  
**Authentication**: API Key  
**Rate Limit**: 100 requests/minute per API key  
**Max Batch Size**: 1000 transactions

**Request**:
```json
{
  "transactions": [
    {
      "transactionId": "550e8400-e29b-41d4-a716-446655440000",
      "userId": "user-123",
      "amount": 1250.50,
      "currency": "USD",
      ...
    },
    {
      "transactionId": "660e8400-e29b-41d4-a716-446655440001",
      "userId": "user-456",
      "amount": 75.00,
      "currency": "EUR",
      ...
    }
  ]
}
```

**Response** (202 Accepted):
```json
{
  "batchId": "batch-789",
  "totalTransactions": 2,
  "acceptedTransactions": 2,
  "rejectedTransactions": 0,
  "status": "PROCESSING"
}
```

---

### 3.3 Get Transaction Status

**Endpoint**: `GET /api/v1/transactions/{transactionId}`  
**Authentication**: API Key or JWT

**Response** (200 OK):
```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "decision": "ALLOW",
  "riskScore": 25.5,
  "reasonCodes": ["LOW_AMOUNT", "KNOWN_DEVICE"],
  "processedAt": "2025-11-22T10:30:05Z",
  "processingTime": "85ms"
}
```

**Status Values**:
- `PROCESSING`: Transaction is being analyzed
- `COMPLETED`: Analysis complete
- `FAILED`: Processing error

**Decision Values**:
- `ALLOW`: Transaction approved
- `BLOCK`: Transaction blocked
- `REVIEW`: Manual review required

---

## 4. Fraud Detection API

### 4.1 Get Fraud Decision

**Endpoint**: `GET /api/v1/fraud/decisions/{transactionId}`  
**Authentication**: JWT (Bearer Token)  
**Authorization**: ANALYST or ADMIN role

**Response** (200 OK):
```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "riskScore": 75.5,
  "decision": "REVIEW",
  "reasonCodes": [
    "HIGH_AMOUNT",
    "NEW_DEVICE",
    "VELOCITY_EXCEEDED"
  ],
  "ruleScores": {
    "amount_threshold": 80,
    "velocity_check": 90,
    "geo_mismatch": 60
  },
  "mlScores": {
    "logistic_regression": 72,
    "random_forest": 78,
    "xgboost": 76
  },
  "features": {
    "amount": 5000.00,
    "user_transaction_count_24h": 8,
    "avg_transaction_amount": 150.00,
    "distance_from_last_txn_km": 500
  },
  "createdAt": "2025-11-22T10:30:05Z"
}
```

---

### 4.2 Override Decision

**Endpoint**: `POST /api/v1/fraud/decisions/{transactionId}/override`  
**Authentication**: JWT (Bearer Token)  
**Authorization**: ADMIN role required

**Request**:
```json
{
  "decision": "ALLOW",
  "reason": "Customer verified via phone call",
  "overriddenBy": "admin-user-123"
}
```

**Response** (200 OK):
```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "originalDecision": "BLOCK",
  "newDecision": "ALLOW",
  "overriddenBy": "admin-user-123",
  "overriddenAt": "2025-11-22T11:00:00Z",
  "reason": "Customer verified via phone call"
}
```

---

### 4.3 List Fraud Rules

**Endpoint**: `GET /api/v1/fraud/rules`  
**Authentication**: JWT (Bearer Token)  
**Authorization**: ANALYST or ADMIN role

**Query Parameters**:
- `page`: Page number (default: 0)
- `size`: Page size (default: 20, max: 100)
- `isActive`: Filter by active status (true/false)
- `ruleType`: Filter by rule type

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": "rule-001",
      "name": "High Amount Threshold",
      "description": "Flag transactions over $10,000",
      "ruleType": "THRESHOLD",
      "conditions": {
        "field": "amount",
        "operator": "GREATER_THAN",
        "value": 10000
      },
      "action": "REVIEW",
      "priority": 10,
      "isActive": true,
      "createdAt": "2025-01-01T00:00:00Z",
      "updatedAt": "2025-11-01T12:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 45,
  "totalPages": 3
}
```

---

### 4.4 Create Fraud Rule

**Endpoint**: `POST /api/v1/fraud/rules`  
**Authentication**: JWT (Bearer Token)  
**Authorization**: ADMIN role required

**Request**:
```json
{
  "name": "Velocity Check - 5 transactions per hour",
  "description": "Flag users with more than 5 transactions in 1 hour",
  "ruleType": "VELOCITY",
  "conditions": {
    "field": "transaction_count",
    "timeWindow": "1h",
    "threshold": 5
  },
  "action": "REVIEW",
  "priority": 20
}
```

**Response** (201 Created):
```json
{
  "id": "rule-046",
  "name": "Velocity Check - 5 transactions per hour",
  "description": "Flag users with more than 5 transactions in 1 hour",
  "ruleType": "VELOCITY",
  "conditions": {
    "field": "transaction_count",
    "timeWindow": "1h",
    "threshold": 5
  },
  "action": "REVIEW",
  "priority": 20,
  "isActive": true,
  "createdAt": "2025-11-22T11:15:00Z"
}
```

---

### 4.5 Update Fraud Rule

**Endpoint**: `PUT /api/v1/fraud/rules/{ruleId}`  
**Authentication**: JWT (Bearer Token)  
**Authorization**: ADMIN role required

**Request**:
```json
{
  "name": "Updated Rule Name",
  "priority": 15,
  "isActive": false
}
```

**Response** (200 OK):
```json
{
  "id": "rule-046",
  "name": "Updated Rule Name",
  "priority": 15,
  "isActive": false,
  "updatedAt": "2025-11-22T11:30:00Z"
}
```

---

### 4.6 Delete Fraud Rule

**Endpoint**: `DELETE /api/v1/fraud/rules/{ruleId}`  
**Authentication**: JWT (Bearer Token)  
**Authorization**: ADMIN role required

**Response** (204 No Content)

---

## 5. Alerting API

### 5.1 List Alerts

**Endpoint**: `GET /api/v1/alerts`  
**Authentication**: JWT (Bearer Token)  
**Authorization**: ANALYST or ADMIN role

**Query Parameters**:
- `page`: Page number (default: 0)
- `size`: Page size (default: 20, max: 100)
- `severity`: Filter by severity (CRITICAL, HIGH, MEDIUM, LOW)
- `status`: Filter by status (OPEN, ACKNOWLEDGED, RESOLVED)
- `startDate`: Filter by creation date (ISO 8601)
- `endDate`: Filter by creation date (ISO 8601)

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": "alert-001",
      "transactionId": "550e8400-e29b-41d4-a716-446655440000",
      "severity": "HIGH",
      "title": "High-risk transaction detected",
      "description": "Transaction amount $15,000 from new device",
      "status": "OPEN",
      "riskScore": 85.5,
      "createdAt": "2025-11-22T10:30:05Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

---

### 5.2 Get Alert Details

**Endpoint**: `GET /api/v1/alerts/{alertId}`  
**Authentication**: JWT (Bearer Token)  
**Authorization**: ANALYST or ADMIN role

**Response** (200 OK):
```json
{
  "id": "alert-001",
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "severity": "HIGH",
  "title": "High-risk transaction detected",
  "description": "Transaction amount $15,000 from new device",
  "status": "OPEN",
  "riskScore": 85.5,
  "reasonCodes": ["HIGH_AMOUNT", "NEW_DEVICE"],
  "transaction": {
    "userId": "user-123",
    "amount": 15000.00,
    "currency": "USD",
    "merchantName": "Luxury Goods Store"
  },
  "createdAt": "2025-11-22T10:30:05Z"
}
```

---

### 5.3 Acknowledge Alert

**Endpoint**: `PUT /api/v1/alerts/{alertId}/acknowledge`  
**Authentication**: JWT (Bearer Token)  
**Authorization**: ANALYST or ADMIN role

**Response** (200 OK):
```json
{
  "id": "alert-001",
  "status": "ACKNOWLEDGED",
  "acknowledgedBy": "analyst-user-456",
  "acknowledgedAt": "2025-11-22T11:00:00Z"
}
```

---

### 5.4 Resolve Alert

**Endpoint**: `PUT /api/v1/alerts/{alertId}/resolve`  
**Authentication**: JWT (Bearer Token)  
**Authorization**: ANALYST or ADMIN role

**Request**:
```json
{
  "resolution": "False positive - customer verified",
  "notes": "Contacted customer via phone, confirmed legitimate purchase"
}
```

**Response** (200 OK):
```json
{
  "id": "alert-001",
  "status": "RESOLVED",
  "resolution": "False positive - customer verified",
  "notes": "Contacted customer via phone, confirmed legitimate purchase",
  "resolvedBy": "analyst-user-456",
  "resolvedAt": "2025-11-22T11:30:00Z"
}
```

---

## 6. Admin API

### 6.1 Get System Metrics

**Endpoint**: `GET /api/v1/admin/metrics`  
**Authentication**: JWT (Bearer Token)  
**Authorization**: ADMIN role required

**Response** (200 OK):
```json
{
  "timestamp": "2025-11-22T12:00:00Z",
  "transactionMetrics": {
    "totalTransactions24h": 125000,
    "avgTransactionsPerSecond": 1.45,
    "peakTransactionsPerSecond": 8.2
  },
  "fraudMetrics": {
    "fraudDetectionRate": 0.95,
    "falsePositiveRate": 0.04,
    "avgRiskScore": 32.5,
    "blockedTransactions24h": 450,
    "reviewTransactions24h": 1200
  },
  "systemHealth": {
    "services": {
      "gateway": "UP",
      "userService": "UP",
      "fraudDetection": "UP",
      "dataIngestion": "UP",
      "alerting": "UP"
    },
    "kafka": {
      "status": "UP",
      "lag": 125
    },
    "database": {
      "status": "UP",
      "connections": 45
    }
  }
}
```

---

### 6.2 List All Users

**Endpoint**: `GET /api/v1/admin/users`  
**Authentication**: JWT (Bearer Token)  
**Authorization**: ADMIN role required

**Query Parameters**:
- `page`: Page number (default: 0)
- `size`: Page size (default: 20, max: 100)
- `role`: Filter by role
- `status`: Filter by status

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": "123",
      "email": "user@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "role": "ANALYST",
      "status": "ACTIVE",
      "createdAt": "2025-01-15T08:00:00Z",
      "lastLoginAt": "2025-11-22T09:15:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 50,
  "totalPages": 3
}
```

---

### 6.3 Update User Role

**Endpoint**: `PUT /api/v1/admin/users/{userId}/role`  
**Authentication**: JWT (Bearer Token)  
**Authorization**: ADMIN role required

**Request**:
```json
{
  "role": "ADMIN"
}
```

**Response** (200 OK):
```json
{
  "id": "123",
  "email": "user@example.com",
  "role": "ADMIN",
  "updatedAt": "2025-11-22T12:00:00Z"
}
```

---

## 7. Error Handling

### 7.1 Error Response Format

All errors follow RFC 7807 Problem Details format:

```json
{
  "type": "https://api.intelliguard.io/errors/validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Invalid transaction amount",
  "instance": "/api/v1/transactions",
  "timestamp": "2025-11-22T12:00:00Z",
  "errors": [
    {
      "field": "amount",
      "message": "Amount must be greater than 0"
    }
  ]
}
```

### 7.2 HTTP Status Codes

| Code | Meaning | Usage |
|------|---------|-------|
| 200 | OK | Successful GET, PUT |
| 201 | Created | Successful POST (resource created) |
| 202 | Accepted | Async operation accepted |
| 204 | No Content | Successful DELETE |
| 400 | Bad Request | Invalid request body/parameters |
| 401 | Unauthorized | Missing or invalid authentication |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Duplicate resource |
| 422 | Unprocessable Entity | Validation error |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |
| 503 | Service Unavailable | Service temporarily down |

### 7.3 Common Error Types

**Validation Error** (400):
```json
{
  "type": "https://api.intelliguard.io/errors/validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Request validation failed"
}
```

**Authentication Error** (401):
```json
{
  "type": "https://api.intelliguard.io/errors/authentication-error",
  "title": "Authentication Required",
  "status": 401,
  "detail": "Valid JWT token required"
}
```

**Authorization Error** (403):
```json
{
  "type": "https://api.intelliguard.io/errors/authorization-error",
  "title": "Forbidden",
  "status": 403,
  "detail": "Insufficient permissions to access this resource"
}
```

**Rate Limit Error** (429):
```json
{
  "type": "https://api.intelliguard.io/errors/rate-limit-error",
  "title": "Rate Limit Exceeded",
  "status": 429,
  "detail": "Too many requests. Please try again later.",
  "retryAfter": 60
}
```

---

## 8. Rate Limiting

### 8.1 Rate Limit Headers

All responses include rate limit headers:

```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 950
X-RateLimit-Reset: 1700000000
```

### 8.2 Rate Limits by Endpoint

| Endpoint | Limit | Window |
|----------|-------|--------|
| `/api/v1/users/register` | 10 | 1 hour |
| `/api/v1/users/login` | 20 | 1 hour |
| `/api/v1/users/reset-password` | 3 | 1 hour |
| `/api/v1/transactions` | 1000 | 1 minute |
| `/api/v1/transactions/batch` | 100 | 1 minute |
| All other endpoints | 1000 | 1 minute |

---

## 9. Webhooks

### 9.1 Webhook Events

IntelliGuard can send webhook notifications for the following events:

- `transaction.processed` - Transaction analysis completed
- `transaction.blocked` - High-risk transaction blocked
- `alert.created` - New fraud alert created
- `alert.resolved` - Alert resolved

### 9.2 Webhook Payload

**Event**: `transaction.blocked`

```json
{
  "event": "transaction.blocked",
  "timestamp": "2025-11-22T12:00:00Z",
  "data": {
    "transactionId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "user-123",
    "amount": 15000.00,
    "currency": "USD",
    "riskScore": 95.5,
    "decision": "BLOCK",
    "reasonCodes": ["HIGH_AMOUNT", "NEW_DEVICE", "VELOCITY_EXCEEDED"]
  }
}
```

### 9.3 Webhook Security

- Webhooks are signed using HMAC-SHA256
- Signature included in `X-IntelliGuard-Signature` header
- Verify signature before processing webhook

---

**Document Control**

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | Nov 2025 | API Team | Initial draft |
