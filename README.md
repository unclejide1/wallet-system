# Wallet System

A simple Spring Boot wallet service that supports:

- user onboarding and authentication
- wallet account creation with generated 10-digit account numbers
- simulated wallet funding for any wallet owned by the signed-in user
- wallet-to-wallet transfers
- idempotent retries for funding and transfer requests when a client reference is supplied
- wallet statements with transaction history

## Tech Stack

- Java 17
- Spring Boot 3.5
- Spring Security with JWT
- Spring Data JPA
- H2 in-memory database
- Swagger / OpenAPI

## Setup

### Prerequisites

- Java 17 installed

### Run the application

```bash
./mvnw spring-boot:run
```

The application starts on:

- `http://localhost:9090`

### Run the test suite

```bash
./mvnw test
```

## API Docs And Local Database

- Swagger UI: `http://localhost:9090/swagger-ui.html`
- Swagger UI direct index: `http://localhost:9090/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:9090/v3/api-docs`
- H2 Console: `http://localhost:9090/h2-console`

Use these H2 settings:

- JDBC URL: `jdbc:h2:mem:walletdb`
- Username: `sa`
- Password: leave blank

## Default Seeded Admin

The app seeds an admin user at startup:

- Email: `admin@wallet.com`
- Password: `AdminPassword2026!`

This is mainly useful for admin-only inspection endpoints and statement access.

## Quick Test Flow

### 1. Onboard a user

`POST /api/v1/auth/onboard`

```json
{
  "firstName": "Ada",
  "lastName": "Lovelace",
  "otherName": "Test",
  "gender": "FEMALE",
  "address": "12 Broad Street, Lagos",
  "stateOfOrigin": "Lagos",
  "email": "ada@example.com",
  "password": "Password123!",
  "phoneNumber": "08111111111",
  "alternativePhoneNumber": "08111111112"
}
```

### 2. Login

`POST /api/v1/auth/login`

```json
{
  "email": "ada@example.com",
  "password": "Password123!"
}
```

Copy the JWT token from the response and use it as:

`Authorization: Bearer <token>`

### 3. Create a wallet

`POST /api/v1/wallets`

```json
{
  "walletType": "SAVINGS"
}
```

You can create one wallet per type:

- `SAVINGS`
- `CURRENT`
- `BUSINESS`

### 4. Fund a wallet

`POST /api/v1/wallets/fund`

```json
{
  "accountNumber": "1000000001",
  "amount": 5000.00,
  "narration": "Card top-up",
  "paymentReference": "PAY-001"
}
```

### 5. Transfer between wallets

`POST /api/v1/wallets/transfer`

```json
{
  "sourceAccountNumber": "1000000001",
  "destinationAccountNumber": "1000000002",
  "amount": 1250.00,
  "narration": "Wallet transfer",
  "clientReference": "TRF-001"
}
```

### 6. View a wallet statement

`GET /api/v1/wallets/{accountNumber}/statement`

Example:

- `GET http://localhost:9090/api/v1/wallets/1000000001/statement`

## Main Endpoints

- `POST /api/v1/auth/onboard`
- `POST /api/v1/auth/login`
- `GET /api/v1/users/me`
- `PUT /api/v1/users/me`
- `POST /api/v1/wallets`
- `GET /api/v1/wallets`
- `POST /api/v1/wallets/fund`
- `POST /api/v1/wallets/transfer`
- `GET /api/v1/wallets/{accountNumber}/statement`

## Assumptions Made

- Wallet funding is implemented as a simulated external top-up, not a live payment gateway integration.
- Funding becomes idempotent when the caller retries with the same `paymentReference`.
- Transfers become idempotent when the caller retries with the same `clientReference`.
- A newly created wallet starts with `0.00` balance.
- A user can own multiple wallets, but only one wallet per wallet type.
- Only the owner of a wallet can fund it or transfer money out of it.
- A transfer can be sent to any valid wallet in the system.
- Wallet statements are shown from the requested wallet's perspective:
  debit entries where that wallet sent funds, and credit entries where that wallet received funds.
- Account numbers are generated sequentially and formatted as 10 digits, starting from `1000000001`.
- Currency is fixed to `NGN`.
- Data is stored in an in-memory H2 database, so restarting the app resets all users, wallets, and transactions.

## Notes For Future Improvements

- integrate a real payment processor for wallet funding
- add transaction categories and richer audit metadata
- persist data with PostgreSQL or MySQL instead of in-memory H2
- add rate limiting, idempotency keys, and more transfer safeguards
- add pagination metadata DTOs instead of returning Spring page structure directly

## Verification

The project was verified with:

```bash
./mvnw test
```
