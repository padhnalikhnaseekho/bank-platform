# API Specification

This file describes the first-pass REST API. Final implementation should publish OpenAPI specs from each service.

## User Service

### Register

`POST /api/v1/users/register`

Request:

```json
{
  "email": "user@example.com",
  "phone": "+919999999999",
  "fullName": "Example User",
  "password": "StrongPassword123!"
}
```

Response: `201 Created`

### Login

`POST /api/v1/auth/login`

Response includes access token, refresh token, expiry, and user profile summary.

### Refresh Token

`POST /api/v1/auth/refresh`

### Current User

`GET /api/v1/users/me`

## Account Service

### Open Account

`POST /api/v1/accounts`

Request:

```json
{
  "type": "SAVINGS",
  "currency": "INR"
}
```

### Get Account

`GET /api/v1/accounts/{accountId}`

### List Accounts

`GET /api/v1/accounts?status=ACTIVE&page=0&size=20`

### Freeze Account

`POST /api/v1/accounts/{accountId}/freeze`

Admin only.

## Transaction Service

### Deposit

`POST /api/v1/transactions/deposits`

Headers:

- `Idempotency-Key`

### Withdraw

`POST /api/v1/transactions/withdrawals`

Headers:

- `Idempotency-Key`

### Transfer

`POST /api/v1/transactions/transfers`

Headers:

- `Idempotency-Key`

Request:

```json
{
  "sourceAccountId": "uuid",
  "targetAccountId": "uuid",
  "amount": 1000.00,
  "currency": "INR",
  "description": "Rent"
}
```

Response: `202 Accepted`

### Transaction Status

`GET /api/v1/transactions/{transactionId}`

## Payment Service

### Create Scheduled Payment

`POST /api/v1/payments/scheduled`

### Create Recurring Payment

`POST /api/v1/payments/recurring`

### Cancel Payment

`POST /api/v1/payments/{paymentId}/cancel`

## Reporting Service

### Generate Statement

`POST /api/v1/reports/statements`

### Download Statement Metadata

`GET /api/v1/reports/statements/{statementId}`

## Audit Service

### Search Audit Events

`GET /api/v1/audit/events?aggregateId={id}&eventType={type}&from={instant}&to={instant}`

Admin only.

## Error Model

```json
{
  "timestamp": "2026-07-18T13:00:00Z",
  "status": 400,
  "errorCode": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "correlationId": "uuid",
  "details": [
    {
      "field": "amount",
      "message": "must be greater than zero"
    }
  ]
}
```

