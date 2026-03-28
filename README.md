# LendIQ — Intelligent Credit Scoring & Loan Routing Engine

> A production-grade fintech backend combining custom DSA implementations, streaming ML pipelines, and regulatory-grade audit infrastructure.

---

## Table of Contents

1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [Tech Stack](#tech-stack)
4. [DSA Implementations](#dsa-implementations)
5. [ML Pipeline](#ml-pipeline)
6. [Database Schema](#database-schema)
7. [API Reference](#api-reference)
8. [Project Structure](#project-structure)
9. [Configuration](#configuration)
10. [Getting Started](#getting-started)
11. [Non-Functional Requirements](#non-functional-requirements)

---

## Overview

**LendIQ** solves the problem of scoring thousands of loan applications per second, detecting fraud in real time, and routing each application to the best-fit lender — all in under 200ms — with full regulatory auditability.

| Property | Value |
|---|---|
| Throughput | 10,000 applications / minute |
| p99 Latency | < 200ms end-to-end |
| ML Models | LightGBM + GNN + AIF360 |
| Target Users | Fintech companies, NBFCs, digital lending startups, bank API integrations |

### Core Value Proposition

- **Sub-200ms** end-to-end application scoring and lender routing
- **Custom DSA** — Interval Tree, Red-Black Tree, Decision Tree, Sliding Window — all implemented from scratch in Java, not library wrappers
- **Three complementary ML models**: LightGBM (credit scoring), PyTorch GNN (fraud ring detection), AIF360 (fairness correction)
- **Immutable audit trail** with SHAP explainability for regulatory compliance (RBI / GDPR / FCRA)
- **Horizontally scalable** via Kafka partitioning and Kubernetes HPA

---

## System Architecture

A loan application travels through four layers before a decision is returned:

```
[Client] → [Java API Gateway] → [Kafka: loan-apps]
                                      ↓
                      ┌───────────────┼───────────────┐
              [Decision Tree]  [ML Scoring (gRPC)]  [GNN Fraud]
                      └───────────────┼───────────────┘
                                      ↓
                             [Score Aggregator]
                                      ↓
                        [Interval Tree Lender Match]
                                      ↓
                          [Red-Black Tree Ranked Queue]
                                      ↓
               [PostgreSQL Audit] + [Webhook → Lender]
```

### Component Responsibilities

| Component | Language | Responsibility |
|---|---|---|
| Java API Gateway | Java 21 | Spring Boot REST, JWT auth, Bucket4j rate limiting, Kafka producer |
| Redis Fraud Pre-check | Java | Sliding window velocity — flag if >5 apps from same IP/device in 60 min |
| Kafka (`loan-apps`) | Kafka | Partitioned by `applicant_id` — same applicant always hits same consumer |
| Decision Tree Worker | Java | Custom Gini-split tree, SHAP values at leaves, serialised to JSON |
| ML Scoring Service | Python | LightGBM via gRPC, ONNX in-process inference, p99 < 8ms |
| GNN Fraud Detector | Python | PyTorch GNN on applicant-device-address graph, async join within 50ms |
| Score Aggregator | Java | Weighted blend (40% DT + 35% ML + 25% Fairness), triggers routing |
| Interval Tree Matcher | Java | In-memory augmented BST of 1000+ lender rules, O(log n + k) query |
| Red-Black Tree Queue | Java | Keeps matched lenders sorted by match score, O(log n) insert/max |
| Webhook Publisher | Java | Async WebClient POSTs decision to winning lender's webhook URL |
| Airflow Pipeline | Python | Weekly drift check → retrain → shadow test → champion/challenger promote |
| PostgreSQL | — | Append-only decisions, applicants, lenders, fraud_events — full audit trail |
| S3 | — | Document storage (ID, income proof), model artifact versioning |
| Redis | — | Velocity counters (sliding window), lender rule cache, session tokens |

### Key Design Decisions

**Kafka partitioned by `applicant_id`** — All messages from the same applicant always hit the same partition and consumer. Stateful stream processing (sliding window deque) never needs distributed coordination.

**ONNX in-process inference** — The LightGBM model is exported to ONNX and loaded directly into the Python gRPC service's memory. No secondary network hop — this is what makes p99 < 8ms achievable.

**Async GNN fraud check** — The GNN runs on a separate consumer group and publishes to a `fraud-results` topic. The Score Aggregator waits up to 50ms — GNN detection never adds more than 50ms to the critical path.

**Immutable audit log** — The `decisions` table is append-only. No UPDATE or DELETE is ever issued. Every credit decision, including model version, input features, SHAP values, and assigned lender, is permanently recorded.

---

## Tech Stack

### Backend (Java)
- **Java 21** + **Spring Boot 3.2.5**
- **Apache Kafka** (Spring Kafka 3.1.4) — event streaming
- **Redis** (Lettuce) — rate limiting & caching
- **PostgreSQL** — primary database (JPA / Hibernate)
- **Bucket4j 8.10.1** — API rate limiting
- **gRPC** (net.devh starter) — ML service communication
- **AWS SDK S3 2.25.0** — document storage
- **JJWT 0.12.5** — JWT authentication
- **MapStruct 1.5.5** — DTO mapping
- **Micrometer + OpenTelemetry** — observability
- **Lombok** — boilerplate reduction

### ML Service (Python)
- **Python 3.11**
- **LightGBM** — gradient boosted credit scoring
- **PyTorch** — GNN fraud ring detection
- **AIF360** — fairness-aware score correction
- **ONNX Runtime** — in-process model inference
- **gRPC / Protobuf** — service interface

### Infrastructure
- **Apache Airflow** — ML retraining DAGs
- **Docker** + **Kubernetes** (HPA)
- **Prometheus + Grafana** — metrics
- **ELK Stack** — structured JSON logging

### Frontend
- **React + TypeScript + Tailwind CSS**
- **React Query** — server state management
- **Recharts** — data visualisation
- **AG Grid** — admin data tables

---

## DSA Implementations

> All five data structures are written from scratch in Java. No `TreeMap`, `SortedSet`, or any `java.util` substitute.

### Interval Tree — `O(log n + k)`
**Used for:** Lender eligibility matching  
**Why:** Handles overlapping income/age/score ranges. Only structure that can answer "which intervals contain point X" efficiently. Used to query 1,000+ lender rules simultaneously from a single applicant profile.

### Red-Black Tree — `O(log n)`
**Used for:** Sorted lender queue by match score  
**Why:** Self-balancing guarantees worst-case O(log n) — plain BST degrades to O(n) on sorted insertions. Lenders are inserted after each Interval Tree query and the max (best match) is extracted in O(log n).

### Decision Tree — `O(depth)`
**Used for:** Deterministic credit scoring + SHAP explainability  
**Why:** Custom Gini-split tree trained on historical loan data. Generates a human-readable decision path (e.g. `"DTI 0.32 < 0.40 → APPROVE"`) stored for every decision. Regulators can audit exactly why an application was approved or declined.

### Sliding Window (Deque) — `O(1)` amortised
**Used for:** Fraud velocity detection  
**Why:** Counts applications from the same IP/device in a rolling 60-minute window without a database call. Lives in Kafka consumer memory — works because Kafka partitioning by `applicant_id` ensures statefulness.

### Min-Heap — `O(log n)`
**Used for:** Kafka consumer lag monitoring  
**Why:** Tracks the top-k slowest partitions by consumer lag. Used in the operations dashboard to surface bottlenecks before they cause SLA breaches.

---

## ML Pipeline

### 1. LightGBM Credit Scorer
- Trained on: income, DTI, employment history, bureau score, loan purpose, tenure
- Inference: ONNX export → loaded in-process into gRPC service → p99 < 8ms
- Output: probability (0–1) → scaled to 0–1000 score

### 2. PyTorch GNN Fraud Detector
- Graph: nodes = applicants, devices, addresses; edges = shared attributes
- Detects: fraud rings where multiple applicants share devices or addresses
- Runs async; result joins the Score Aggregator within 50ms timeout

### 3. AIF360 Fairness Corrector
- Monitors demographic parity across gender, age, and income cohorts
- Applies post-processing correction to equalise approval rates
- Output stored in `fairness_score` column alongside raw scores

### Score Aggregation
```
final_score = (dt_score × 0.40) + (ml_score × 0.35) + (fairness_score × 0.25)

if fraud_prob > 0.75  → DECLINE (hard block, regardless of score)
if final_score >= 700 → APPROVE
if final_score >= 550 → REFER
else                  → DECLINE
```

### Retraining (Airflow DAG — weekly)
1. Extract last 30 days of labelled decisions from PostgreSQL
2. Compute PSI (Population Stability Index) per feature vs baseline
3. If PSI > 0.2 on any feature → trigger full retrain
4. Shadow-test new model on live traffic for 24 hours
5. Compare champion vs challenger on AUC, KS, fairness metrics
6. Promote challenger if it wins on all three — rollback otherwise

---

## Database Schema

### `applicants`
| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| full_name | TEXT | Legal name |
| pan_hash | TEXT | SHA-256 of PAN/SSN — raw value never stored |
| income | NUMERIC(12,2) | Monthly gross income |
| age | INT | CHECK age > 17 |
| employment_months | INT | Total months of employment history |
| existing_debt | NUMERIC(12,2) | Sum of monthly debt obligations |
| credit_bureau_score | INT | CHECK 300..900 |
| device_fp | TEXT | Device fingerprint hash (indexed) |
| ip_hash | TEXT | Hashed IP at time of application (indexed) |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() |

### `applications`
| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| applicant_id | UUID | FK → applicants.id |
| amount | NUMERIC(12,2) | Requested loan amount |
| term_months | INT | Requested tenure |
| purpose | TEXT | home / vehicle / personal / business |
| status | TEXT | pending / approved / declined / referred |
| source_channel | TEXT | api / web / mobile / partner |
| kafka_offset | BIGINT | For exactly-once deduplication (indexed) |
| created_at | TIMESTAMPTZ | Submission timestamp |

### `decisions` _(append-only — no UPDATE or DELETE ever)_
| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| application_id | UUID | FK → applications.id |
| dt_score | NUMERIC(6,2) | Custom Decision Tree score (0–1000) |
| ml_score | NUMERIC(6,2) | LightGBM score (0–1000) |
| fairness_score | NUMERIC(6,2) | Fairness-adjusted score (0–1000) |
| final_score | NUMERIC(6,2) | Weighted aggregate |
| fraud_prob | NUMERIC(5,4) | GNN fraud probability (null if timeout) |
| outcome | TEXT | APPROVE / DECLINE / REFER |
| lender_id | UUID | Assigned lender (null if declined) |
| model_version | TEXT | Semver of model bundle used |
| shap_json | JSONB | SHAP values per feature |
| decision_path | TEXT[] | Human-readable Decision Tree path |
| processing_ms | INT | End-to-end latency |
| decided_at | TIMESTAMPTZ | Immutable decision timestamp |

### `lenders`
Stores lender eligibility rules: `income_min/max`, `age_min/max`, `score_threshold`, `max_loan_amount`, `webhook_url`, `active`.

### `fraud_events`
Tracks velocity flags and GNN ring detections: `event_type` (velocity / gnn_ring / manual_flag), `window_count`, `ring_id`, `fraud_prob`, `resolved`.

---

## API Reference

All endpoints are prefixed with `/api/v1`. Rate limits: **1,000 req/min** (standard), **10,000 req/min** (enterprise).

### Application Endpoints

| Method | Endpoint | Description | Auth | Latency |
|---|---|---|---|---|
| POST | `/applications` | Submit a new loan application | JWT Bearer | < 200ms |
| GET | `/applications/:id` | Fetch application status and decision | JWT Bearer | < 30ms |
| GET | `/applications` | List applications with filters | JWT Bearer | < 50ms |
| GET | `/applications/:id/decision` | Full decision with SHAP values | JWT Bearer | < 30ms |
| POST | `/applications/:id/documents` | Upload supporting documents to S3 | JWT Bearer | < 2s |
| PATCH | `/applications/:id/withdraw` | Withdraw a pending application | JWT Bearer | < 30ms |

**POST /applications — Request**
```json
{
  "applicant_id": "uuid",
  "amount": 250000,
  "term_months": 36,
  "purpose": "vehicle",
  "device_fingerprint": "abc123hash",
  "consent_timestamp": "2026-03-20T10:00:00Z"
}
```

**POST /applications — Response (200 OK)**
```json
{
  "application_id": "uuid",
  "status": "approved",
  "final_score": 742,
  "outcome": "APPROVE",
  "lender": { "id": "uuid", "name": "FinQuick Capital" },
  "decision_path": ["DTI 0.32 < 0.40", "employment 36mo >= 24mo", "APPROVE"],
  "processing_ms": 147
}
```

### Lender Endpoints

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/lenders` | Onboard a new lender | API Key + Admin |
| GET | `/lenders` | List all active lenders | API Key |
| GET | `/lenders/:id` | Get lender detail and acceptance rate | API Key |
| PUT | `/lenders/:id/rules` | Update eligibility ranges | API Key + Admin |
| PATCH | `/lenders/:id/pause` | Pause lender from referrals | API Key + Admin |
| GET | `/lenders/:id/stats` | Referral count, acceptance rate, avg score (30d) | API Key |

### Scoring & ML Endpoints

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/score/explain` | Score with full SHAP breakdown | JWT Bearer |
| POST | `/score/batch` | Batch score up to 100 applications (async + webhook) | API Key |
| GET | `/models/current` | Active model versions and last retrain date | Admin |
| POST | `/models/shadow` | Promote shadow model to champion | Admin |
| GET | `/models/drift` | Current PSI scores per feature vs baseline | Admin |
| GET | `/models/fairness` | Demographic parity metrics per cohort | Admin |

### Fraud & Compliance Endpoints

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/fraud/flags` | List active fraud flags | Admin |
| GET | `/fraud/flags/:id` | Detail of a specific flag with evidence | Admin |
| PATCH | `/fraud/flags/:id/resolve` | Mark flag reviewed and cleared | Admin |
| GET | `/audit/:application_id` | Full immutable audit trail | Admin + Regulator |
| GET | `/audit/export` | Export decisions CSV for regulatory submission | Admin |
| GET | `/health` | Service liveness + readiness | None |

---

## Project Structure

```
lendiq/
├── api-gateway/
│   ├── src/main/java/com/lendiq/
│   │   ├── controller/          # REST controllers
│   │   ├── service/             # Interfaces + Impl classes
│   │   ├── repository/          # JPA repositories + Specification classes
│   │   ├── dto/
│   │   │   ├── request/         # Validated request DTOs
│   │   │   └── response/        # Response DTOs + PagedResponse
│   │   ├── mapper/              # MapStruct mappers
│   │   ├── kafka/
│   │   │   ├── event/           # Kafka event schemas
│   │   │   ├── producer/        # Kafka producers
│   │   │   └── consumer/        # Kafka consumers
│   │   ├── exception/           # Exception hierarchy + global handler
│   │   ├── config/              # @ConfigurationProperties + beans
│   │   ├── security/            # JWT, API key, IP allowlist filters
│   │   ├── dsa/                 # IntervalTree, RBTree, DecisionTree, SlidingWindow
│   │   └── entity/              # JPA entities
│   ├── src/main/resources/
│   │   └── application.yml      # Full configuration
│   └── build.gradle             # Dependencies
├── ml-service/                  # Python 3.11 gRPC scoring service
├── retraining/                  # Airflow DAGs (weekly drift + retrain)
├── frontend/                    # React + TypeScript applicant/lender/admin UIs
└── infra/                       # Docker + Kubernetes manifests
```

---

## Configuration

Key configuration properties (`application.yml`):

```yaml
lendiq:
  scoring:
    dt-weight: 0.40
    ml-weight: 0.35
    fairness-weight: 0.25
    approve-threshold: 700.0
    refer-threshold: 550.0
    fraud-hard-block: 0.75      # fraud_prob above this → DECLINE always

  fraud:
    velocity-threshold: 5       # max apps per window
    velocity-window-secs: 3600
    gnn-timeout-ms: 50          # max wait for GNN before proceeding

  ml:
    service-host: ${ML_SERVICE_HOST:localhost:50051}
    grpc-timeout-ms: 10
    model-bucket: ${AWS_S3_BUCKET:lendiq-models}
    champion-path: champion/lgbm.onnx

  security:
    jwt-expiry-hours: 1
    refresh-expiry-days: 7
    admin-cidrs:
      - 10.0.0.0/8
      - 172.16.0.0/12
```

### Required Environment Variables

| Variable | Description |
|---|---|
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL password |
| `JWT_SECRET` | Secret key for JWT signing |
| `ML_SERVICE_HOST` | gRPC host:port for ML scoring service |
| `AWS_S3_BUCKET` | S3 bucket for model artifacts and documents |

---

## Getting Started

### Prerequisites
- Java 21
- Python 3.11
- Docker + Docker Compose
- PostgreSQL 15+
- Redis 7+
- Apache Kafka 3.x

### Local Development

**1. Start infrastructure:**
```bash
docker compose up -d postgres redis kafka
```

**2. Run the API Gateway:**
```bash
cd api-gateway
./gradlew bootRun
```

**3. Run the ML Service:**
```bash
cd ml-service
pip install -r requirements.txt
python server.py
```

**4. Start the frontend:**
```bash
cd frontend
npm install && npm run dev
```

### Running Tests
```bash
# API Gateway (uses Testcontainers for PostgreSQL + Kafka)
./gradlew test

# ML Service
cd ml-service && pytest
```

---

## Non-Functional Requirements

| Requirement | Target | Mechanism |
|---|---|---|
| Throughput | 10,000 applications/min | Kafka partitioning + horizontal scaling |
| End-to-end latency (p99) | < 200ms | ONNX in-process inference, Redis pre-checks, async GNN |
| ML inference latency (p99) | < 8ms | ONNX in-process, no network hop |
| Availability | 99.9% | Kubernetes HPA, Kafka replication factor 3 |
| Audit retention | 7 years | Append-only PostgreSQL `decisions` table |
| Fraud detection | < 50ms overhead | Async GNN with 50ms join timeout |
| Rate limiting | 1K/10K req/min | Bucket4j + Redis |
| Regulatory compliance | RBI / GDPR / FCRA | SHAP explainability, immutable audit trail, PAN hashing |

---

*LendIQ — Complete Project Document · v2.0 · 2026 · Portfolio Use*
