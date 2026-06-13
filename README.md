# Wallet System

A simple Spring Boot wallet service that supports:

- user onboarding and authentication
- wallet account creation with generated 10-digit account numbers
- simulated wallet funding for any wallet owned by the signed-in user
- wallet-to-wallet transfers
- idempotent retries for funding and transfer requests when a client reference is supplied
- wallet statements with transaction history
- concurrent-safe balance updates using database row locking

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

## Endpoint Notes And Improvements Applied

- onboarding now returns a `Location` header pointing to `/api/v1/users/me`, which is a user-facing resource instead of an admin-only route
- wallet statement and admin list endpoints validate pagination inputs and reject negative pages or invalid sizes
- wallet creation is serialized per user by locking the user row before checking wallet type uniqueness
- funding and transfer idempotency checks are performed both before work starts and again after account locks are acquired, which makes retry behavior safer under concurrent requests
- alternative phone numbers are now protected by a database unique constraint, not only an application-level check

## Multithreading And Concurrency Model

This application follows the standard Spring Boot request model:

- each HTTP request is processed on a separate server thread from the embedded servlet container
- controllers are stateless and delegate all business rules to services
- concurrency-sensitive wallet operations rely on database transactions and row locks, not Java in-memory locks

### How transfers stay safe under concurrent load

- `transferFunds` runs inside a transaction
- both the source and destination account rows are locked with `PESSIMISTIC_WRITE`
- the account numbers are sorted before locking so concurrent transfers acquire locks in the same order and avoid deadlocks
- the balance debit and credit happen only after both locks are held
- if a retry with the same `clientReference` arrives while another request is in flight, the second check after lock acquisition allows the service to return the already-created transaction instead of applying the balance movement twice

### How wallet funding stays safe

- `fundWallet` locks the destination wallet row before crediting balance
- the service performs a second idempotency lookup after the lock is acquired
- this prevents same-wallet parallel retries from crediting the account twice

### How wallet creation stays safe

- wallet creation locks the owning user row first
- while that lock is held, the service checks whether the user already has a wallet of that type
- the database also enforces a unique `(user_id, wallet_type)` constraint as a final safety net

## Database Interaction Design

The application uses Spring Data JPA with transactional services:

- read operations use `@Transactional(readOnly = true)` where appropriate
- money-moving operations use `@Transactional` so balance updates and transaction records commit or roll back together
- persistence relies on JPA dirty checking for loaded entities, which avoids unnecessary explicit `save()` calls on already managed account rows
- idempotency is backed by a unique transaction `externalReference`, which prevents duplicate persisted transfer or funding records

### Indexes And Constraints

The schema includes indexes and constraints aimed at the main access patterns:

- unique indexes on `users.email`, `users.phone_number`, `users.alternative_phone_number`, `users.user_ref`
- unique index on `accounts.account_number`
- unique constraint on `(accounts.user_id, accounts.wallet_type)`
- composite indexes on `transactions.source_account_number, timestamp` and `transactions.destination_account_number, timestamp` for statement queries
- unique index on `transactions.external_reference` for idempotency lookups

## Performance And Optimization Choices

The current implementation keeps the hot path intentionally small:

- controllers stay thin and avoid business logic
- transactional sections are short and only include validation, locking, balance movement, and transaction persistence
- expensive or slow external calls are not part of the transfer path
- statement queries are paginated and capped at a maximum page size of `50`
- duplicate-request detection happens early to reduce unnecessary writes

### What makes it fast today

- row-level locking is applied only where money movement requires strict consistency
- repeated reads are minimized by reusing managed entities inside the transaction
- statement indexes are aligned with the way statements are queried
- the default in-memory H2 database keeps local development and testing fast

### What to do in production

- move from H2 to PostgreSQL or MySQL for real production-grade concurrency behavior
- tune the servlet thread pool and Hikari connection pool together so request throughput does not exceed database capacity
- add rate limiting to login, funding, and transfer endpoints
- add request correlation IDs for easier tracing of retries and concurrent activity
- consider moving notifications, audit exports, and other non-critical side effects to asynchronous processing

## Java Best Practices Used

- layered architecture with controllers, services, repositories, DTOs, and entities separated by responsibility
- constructor injection via Lombok `@RequiredArgsConstructor`
- Bean Validation on request bodies and endpoint query parameters
- centralized exception handling through `@RestControllerAdvice`
- email and phone normalization before lookup or persistence
- explicit transactional boundaries around business operations
- defensive database constraints in addition to application-level validations
- idempotent request handling for retry-safe financial operations
- focused unit tests and end-to-end integration tests for the core wallet flow

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
- The app is designed for correctness first on balance-changing operations, so transfer/funding throughput is intentionally serialized per locked wallet row when contention occurs.

## Notes For Future Improvements

- integrate a real payment processor for wallet funding
- add transaction categories and richer audit metadata
- persist data with PostgreSQL or MySQL instead of in-memory H2
- add explicit idempotency-key headers backed by a dedicated request ledger table
- add rate limiting and more transfer safeguards
- add pagination metadata DTOs instead of returning Spring page structure directly

## Verification

The project was verified with:

```bash
./mvnw test
```
