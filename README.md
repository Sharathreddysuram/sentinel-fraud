# sentinel-fraud — Real-Time Fraud Detection Platform

An event-driven **microservices** platform that scores bank transactions for fraud in real time.
Transaction events flow through Kafka into a rules engine (velocity, amount-spike, and
impossible-travel checks); suspicious ones become analyst-reviewable alerts.

This is the companion project to **paycore-payments-engine** (the payments API) — a transfer
there can be published as a `TransactionEvent` here and scored automatically.

---

## Architecture

```
                    ┌───────────────────┐   Kafka topic      ┌────────────────────┐   Kafka topic      ┌──────────────────┐
                    │ ingestion-service │  ──► "transactions" ──► │ detection-service  │ ──► "fraud-alerts" ──► │  alert-service   │
 Client ──REST──►   │   :8091           │                    │   :8092              │                    │   :8093          │
  (bank core /      │ validate + publish│                    │ rules engine → score │                    │ read-model DB    │
   demo script)     └───────────────────┘                    │ own alerts DB (H2)   │                    │ analyst REST API │
                                                             └────────────────────┘                    └──────────────────┘
```

**Data flow, step by step**

1. A transaction (from the bank's core system, or your `curl`) hits
   `POST /api/transactions` on **ingestion-service** (`:8091`).
2. It is validated and published to the Kafka topic **`transactions`**, keyed by
   `accountId` so one account's events stay ordered on a single partition.
3. **detection-service** (`:8092`) consumes each event and runs it through the
   rules engine:
   - **VELOCITY** — 5+ transactions in 10 minutes (card-testing / account-takeover bursts), weight 40
   - **AMOUNT** — ≥ $10,000 absolute, or ≥ 3× the account's recent average, weight 35
   - **GEO** — country change within 60 minutes (impossible travel), weight 25
4. The weights of fired rules are summed into a **risk score**. Score ≥ 50 →
   a fraud alert is persisted in detection's own database **and** published to
   the Kafka topic **`fraud-alerts`**.
5. **alert-service** (`:8093`) consumes `fraud-alerts` and projects each alert
   into its own read-model database (CQRS-lite — each service owns its data).
   Analysts work the queue via its REST API: list, fetch, mark reviewed.

**Why three services?** Each has one job and its own database. Ingestion stays
thin and horizontally scalable under burst load; detection owns the scoring
brain and its thresholds; alerting owns the analyst workflow. Kafka is the
only coupling between them — no service calls another over HTTP.

---

## Services

| Service | Port | Role | Database | Kafka |
|---|---|---|---|---|
| `ingestion-service` | 8091 | REST edge: validate + publish transaction events | none (stateless) | produces `transactions` |
| `detection-service` | 8092 | Rules engine: score events, persist + publish alerts | H2 (own `fraud_alerts` table) | consumes `transactions`, produces `fraud-alerts` |
| `alert-service` | 8093 | Analyst read model: project alerts, review API | H2 (own `alert_view` table) | consumes `fraud-alerts` |

---

## Tech stack

| Layer | Technology |
|---|---|
| Language / framework | Java 17, Spring Boot 3.2.5 |
| Messaging | Apache Kafka 3.8 (KRaft mode), Spring Kafka |
| Persistence | Spring Data JPA, H2 (per-service database) |
| Validation | Jakarta Bean Validation |
| API docs | SpringDoc OpenAPI (Swagger UI per service) |
| Tests | JUnit 5, AssertJ |
| Build | Maven multi-module (`sentinel-fraud` parent pom) |
| Infra (local) | Docker Compose — single-node Kafka |

---

## Project structure

```
sentinel-fraud/
├── pom.xml                          # parent pom (packaging pom, 3 modules)
├── docker-compose.yml               # Kafka 3.8 KRaft, auto-creates topics
├── ingestion-service/               # :8091 — REST in, Kafka out
│   └── src/main/java/.../ingestion/
│       ├── api/TransactionController.java      # POST /api/transactions -> 202
│       ├── api/dto/PublishTransactionRequest.java
│       ├── events/TransactionEvent.java        # Kafka payload (Jackson-friendly)
│       ├── config/KafkaProducerConfig.java
│       └── common/ (ApiError, GlobalExceptionHandler)
├── detection-service/               # :8092 — the fraud brain
│   └── src/main/java/.../detection/
│       ├── consumer/TransactionConsumer.java   # @KafkaListener("transactions")
│       ├── rules/                              # strategy-pattern rules engine
│       │   ├── FraudRule.java / RuleResult.java / RulesEngine.java
│       │   ├── VelocityRule.java / AmountThresholdRule.java / GeoAnomalyRule.java
│       │   ├── TransactionHistoryStore.java / AccountHistory.java
│       ├── config/RuleProperties.java          # fraud.rules.* thresholds
│       ├── alert/ (FraudAlert, AlertService, FraudAlertMessage, producer)
│       ├── producer/AlertProducer.java         # -> "fraud-alerts"
│       ├── api/SimulateController.java         # POST /simulate, GET+PUT /rules
│       └── common/ (ApiError, GlobalExceptionHandler, ResourceNotFoundException)
└── alert-service/                   # :8093 — analyst read model
    └── src/main/java/.../alerts/
        ├── consumer/AlertConsumer.java         # @KafkaListener("fraud-alerts")
        ├── view/ (AlertView, AlertViewRepository, AlertViewService)
        ├── api/AlertController.java            # GET /alerts, review, rules info
        └── common/ (...)
```

---

## How to run

Prerequisites: **Java 17**, **Maven 3.9+**, **Docker** (for Kafka).

```bash
cd sentinel-fraud
mvn -q -DskipTests package   # builds all three services
```

### Option A — zero-infrastructure demo (fastest)

Run only the detection-service and score transactions synchronously. No Kafka needed.

```bash
cd detection-service
mvn spring-boot:run          # listens on :8092
```

Swagger UI: http://localhost:8092/swagger-ui.html

### Option B — full pipeline

```bash
# 1. start Kafka (topics auto-created on first publish)
docker compose up -d

# 2. in three terminals, one service each
cd ingestion-service && mvn spring-boot:run   # :8091
cd detection-service && mvn spring-boot:run   # :8092
cd alert-service     && mvn spring-boot:run   # :8093
```

### Run the tests

```bash
mvn test   # rules-engine unit tests (detection-service); no Kafka broker needed
```

---

## API examples

### 1. Score a clean transaction (detection-service, no Kafka)

```bash
curl -s -X POST localhost:8092/api/simulate \
  -H 'Content-Type: application/json' -d '{
    "transactionId": "tx-clean-001",
    "accountId": "acc-1001",
    "amount": 42.50,
    "currency": "USD",
    "merchant": "Whole Foods",
    "merchantCountry": "US",
    "channel": "POS"
  }' | python3 -m json.tool
```

Response — `fraud: false`, `riskScore: 0`, no alert persisted:

```json
{
  "transactionId": "tx-clean-001",
  "riskScore": 0,
  "fraud": false,
  "triggeredRules": [],
  "alertId": null
}
```

### 2. Score a fraudulent transaction (impossible travel + burst)

First, seed 4 normal US transactions for the account, then fire one from Nigeria:

```bash
for i in 1 2 3 4; do
  curl -s -X POST localhost:8092/api/simulate \
    -H 'Content-Type: application/json' -d "{
      \"transactionId\": \"tx-seed-$i\",
      \"accountId\": \"acc-2002\",
      \"amount\": 30.00,
      \"currency\": \"USD\",
      \"merchant\": \"Starbucks\",
      \"merchantCountry\": \"US\",
      \"channel\": \"POS\"
    }" > /dev/null
done

curl -s -X POST localhost:8092/api/simulate \
  -H 'Content-Type: application/json' -d '{
    "transactionId": "tx-suspicious-001",
    "accountId": "acc-2002",
    "amount": 45.00,
    "currency": "USD",
    "merchant": "Unknown ATM",
    "merchantCountry": "NG",
    "channel": "ATM"
  }' | python3 -m json.tool
```

Response — `fraud: true`, score **65** (VELOCITY 40 + GEO 25), alert persisted:

```json
{
  "transactionId": "tx-suspicious-001",
  "riskScore": 65,
  "fraud": true,
  "triggeredRules": [
    {"ruleName": "VELOCITY", "detail": "Account acc-2002 made 5 transactions within 10 minutes (limit 5)", "weight": 40},
    {"ruleName": "GEO", "detail": "Country changed from US to NG within 60 minutes", "weight": 25}
  ],
  "alertId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```

### 3. Publish through the full Kafka pipeline (Option B running)

```bash
# publish a transaction at the edge
curl -s -X POST localhost:8091/api/transactions \
  -H 'Content-Type: application/json' -d '{
    "transactionId": "tx-live-001",
    "accountId": "acc-3003",
    "amount": 15000.00,
    "currency": "USD",
    "merchant": "Jewelry Store",
    "merchantCountry": "US",
    "channel": "ONLINE"
  }'

# a few seconds later, the alert shows up in the analyst queue
curl -s localhost:8093/api/alerts | python3 -m json.tool
```

### 4. Work the analyst queue (alert-service)

```bash
# list newest-first (paged)
curl -s 'localhost:8093/api/alerts?size=5' | python3 -m json.tool

# mark one reviewed
curl -s -X POST localhost:8093/api/alerts/<alertId>/review | python3 -m json.tool
```

### 5. Tune the rules at runtime (detection-service)

```bash
# view current thresholds
curl -s localhost:8092/api/rules | python3 -m json.tool

# tighten velocity to 3-in-10-minutes — takes effect on the next evaluation, no restart
curl -s -X PUT localhost:8092/api/rules \
  -H 'Content-Type: application/json' -d '{
    "velocity": {"maxTransactions": 3, "windowMinutes": 10, "weight": 40},
    "amount": {"absoluteThreshold": 10000, "multiplierVsAverage": 3.0, "weight": 35},
    "geo": {"windowMinutes": 60, "weight": 25},
    "alertThresholdScore": 50
  }'
```

---

## Key design decisions

1. **Strategy pattern for rules.** Each rule implements `FraudRule` and registers as a
   Spring `@Component`. Adding a rule (e.g. `DeviceFingerprintRule`) means writing one
   class — the `RulesEngine` picks it up automatically. No wiring changes.
2. **Evaluate-before-record.** The engine snapshots the account's history *before*
   the current event is recorded, so a transaction is never scored against itself.
   The event is recorded afterwards, so the *next* evaluation sees it.
3. **Scoring, not binary.** Rules contribute weights to a 0–100 risk score; the alert
   threshold decides. One strong signal (AMOUNT alone = 35) stays below the bar;
   two weak signals together (VELOCITY + GEO = 65) cross it — which matches how
   real fraud desks think.
4. **In-memory time-windowed history.** A `ConcurrentHashMap` of per-account deques
   with 24h eviction keeps hot lookups off the database. The `AccountHistory`
   abstraction means this can be swapped for Redis or a Kafka state store later
   without touching any rule.
5. **CQRS-lite: each service owns its database.** Detection persists its write-model
   alerts; alert-service builds a separate read model from the `fraud-alerts` topic.
   Kafka is the only coupling — alert-service never calls detection over HTTP.
   Projection is idempotent (alert id = primary key), so at-least-once delivery is safe.
6. **Synchronous `/simulate` for demos.** The exact same scoring path as the Kafka
   consumer, minus the infrastructure — the fastest way to show the system working.
7. **Per-service event schemas.** `TransactionEvent` / `FraudAlertMessage` are
   duplicated per service instead of shared via a common jar, so services evolve
   and deploy independently. In production this would be governed by a schema registry.
8. **Poison-record safety.** Both Kafka listeners wrap processing in try/catch and
   validate payloads — one bad record is logged and skipped, never kills the consumer.

---

## How it connects to paycore-payments-engine

The sibling `paycore-payments-engine` project is the payments API (accounts, transfers,
double-entry ledger). Its `TransferService` is the natural producer for this pipeline:
after a transfer completes, publish a `TransactionEvent` to the `transactions` topic —

```java
// inside paycore's TransferService, after a COMPLETED transfer:
kafkaTemplate.send("transactions", transfer.getFromAccountId().toString(),
    new TransactionEvent(transfer.getId().toString(), /* ...fields... */));
```

— and sentinel-fraud scores it in real time, raising an analyst alert when the
pattern looks wrong. Payments in, fraud signals out.

---

## Sample fraud scenario walkthrough

Analyst story: account `acc-2002` normally buys coffee in Ohio. At 14:02 it makes
four $30 POS purchases in the US; at 14:09 a $45 ATM withdrawal posts from Nigeria.

| Step | What happens | Score |
|---|---|---|
| 4× $30 US POS | Each scores 0 — normal pattern, history builds up | 0 |
| $45 NG ATM | 5th tx in 10 min → VELOCITY (40); US→NG in 60 min → GEO (25) | **65 → FRAUD** |
| Alert | Persisted in detection DB + published to `fraud-alerts` | — |
| Read model | alert-service projects it; analyst sees it at `GET /api/alerts` | — |
| Review | Analyst marks REVIEWED after confirming account takeover | — |

Try it yourself with the curl sequence in [API examples](#2-score-a-fraudulent-transaction-impossible-travel--burst).

---

## Future improvements

- **Schema Registry (Avro/Protobuf)** for `transactions` / `fraud-alerts` — contract
  evolution without breaking consumers.
- **Redis-backed history store** so detection scales horizontally (the in-memory
  store is per-instance today).
- **ML scoring alongside rules** — blend a model score with rule weights.
- **Exactly-once semantics** — transactional Kafka producer + idempotent alert writes.
- **Notification fan-out** — SMS/email/push on high-severity alerts via a fourth service.
- **Postgres + Flyway** per service instead of H2 for a production profile.
- **Spring Cloud Config / service discovery** if the platform grows past three services.

---

## Quick reference

| What | Where |
|---|---|
| Ingest a transaction | `POST localhost:8091/api/transactions` → 202 |
| Score synchronously | `POST localhost:8092/api/simulate` |
| View / tune rules | `GET` / `PUT localhost:8092/api/rules` |
| Analyst queue | `GET localhost:8093/api/alerts` |
| Review an alert | `POST localhost:8093/api/alerts/{alertId}/review` |
| Swagger UI | `:8091`, `:8092`, `:8093` → `/swagger-ui.html` |
| Start Kafka | `docker compose up -d` (from repo root) |
| Build everything | `mvn -q -DskipTests package` |
| Run tests | `mvn test` |
