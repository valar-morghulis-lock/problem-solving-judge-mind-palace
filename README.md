# 🧠 Judge Mind Palace

> **Solve problems, learn algorithms and data structures at your own pace.**
> The home for every Software Engineer.

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.5-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)](https://www.postgresql.org/)
[![Kafka](https://img.shields.io/badge/Apache_Kafka-7.6.0-black?logo=apachekafka)](https://kafka.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen)](https://github.com/valar-morghulis-lock/problem-solving-judge-mind-palace/pulls)

---

## What is this?

Judge Mind Palace is an **open-source, self-hostable code judge platform** built for the Arab developer community and beyond. Submit your solution, get a real-time verdict, track your progress — no premium subscription, no lock-in, no data sent anywhere you don't control.

Think LeetCode — but open source, Arabic-friendly, and built by engineers who've been through the grind.

---

## Features

- **Real-time verdicts** via WebSocket (STOMP) — no polling, instant feedback
- **Sandboxed execution** — each submission runs in an isolated Docker container with memory, CPU, and process limits
- **Async judge pipeline** — Kafka-backed submission queue decouples the API from execution
- **Transactional Outbox** — guaranteed at-least-once delivery even under failure
- **JWT auth** — register, login, refresh tokens, role-based access (SOLVER / ADMIN)
- **OpenAPI / Swagger UI** — fully documented REST API at `/v1/swagger-ui.html`
- **Flyway migrations** — schema versioned and reproducible across all environments
- **ArchUnit enforcement** — architectural rules baked into the test suite

---

## Architecture

```
Client
  └─► POST /v1/submissions
        └─► Submission Service (Spring Boot)
              ├─► saves submission (PENDING) + outbox event → PostgreSQL
              └─► Outbox Relay → Kafka: submission.created
                                          └─► Judge Worker (consumes)
                                                └─► Docker sandbox (per submission)
                                                      ├─► compile
                                                      └─► run against test cases
                                                └─► Kafka: submission.result
                                                      └─► API consumes
                                                            ├─► updates submission in PostgreSQL
                                                            └─► WebSocket push → /topic/submissions/{id}
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Security | Spring Security + JWT (jjwt 0.12.5) |
| Persistence | Spring Data JPA + PostgreSQL 16 |
| Migrations | Flyway |
| Messaging | Apache Kafka (Confluent 7.6.0) |
| Real-time | Spring WebSocket + STOMP |
| Sandbox | Docker Java SDK 3.3.6 |
| API Docs | SpringDoc OpenAPI 2.5.0 |
| Architecture | ArchUnit 1.3.0 + jMolecules |
| Observability | Spring Actuator + Prometheus (planned) |

---

## Project Structure

```
judge-platform/
├── infra/
│   └── docker-compose.yml          # PostgreSQL, Kafka, Zookeeper, Kafka UI
├── src/
│   ├── main/
│   │   ├── java/dev/judge/
│   │   │   ├── auth/               # JWT auth, register, login, refresh
│   │   │   ├── problem/            # Problem CRUD, tags, examples, test cases
│   │   │   ├── submission/         # Submit code, track verdicts
│   │   │   ├── worker/             # Judge worker, Docker sandbox execution
│   │   │   ├── websocket/          # STOMP WebSocket config
│   │   │   └── shared/             # Exception handling, common DTOs, config
│   │   └── resources/
│   │       ├── db/migration/       # Flyway SQL migrations (V1__, V2__...)
│   │       ├── application.yml
│   │       └── application-dev.yml
│   └── test/
│       └── java/dev/judge/
│           └── architecture/       # ArchUnit architectural rules
└── pom.xml
```

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker Desktop

### 1. Clone the repo

```bash
git clone https://github.com/valar-morghulis-lock/problem-solving-judge-mind-palace.git
cd problem-solving-judge-mind-palace
```

### 2. Start infrastructure

```bash
docker-compose -f infra/docker-compose.yml up -d
```

This starts:

| Container | Purpose | Port |
|---|---|---|
| `judge_postgres` | PostgreSQL database | `5433` |
| `judge_zookeeper` | Kafka coordination | `2181` |
| `judge_kafka` | Message broker | `9092` |
| `judge_kafka_ui` | Kafka dashboard | `8090` |

### 3. Run the application

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

### 4. Verify

- API base: `http://localhost:8080/v1`
- Swagger UI: `http://localhost:8080/v1/swagger-ui.html`
- Kafka UI: `http://localhost:8090`
- Health check: `http://localhost:8080/v1/actuator/health`

---

## API Overview

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | Public | Register new user |
| POST | `/auth/login` | Public | Login, receive JWT |
| POST | `/auth/refresh` | Public | Refresh access token |
| GET | `/problems` | Public | List problems (paginated) |
| GET | `/problems/{slug}` | Public | Get problem detail |
| POST | `/problems` | ADMIN | Create problem |
| PATCH | `/problems/{slug}/publish` | ADMIN | Toggle visibility |
| POST | `/submissions` | SOLVER | Submit code → 202 + WebSocket |
| GET | `/submissions/{id}` | SOLVER | Poll submission result |
| GET | `/submissions` | SOLVER | Submission history |

Full OpenAPI spec available at `/v1/api-docs` or interactively at `/v1/swagger-ui.html`.

---

## WebSocket

Connect to receive real-time verdicts:

```
wss://localhost:8080/ws?token=<JWT>

Subscribe: /topic/submissions/{submissionId}
```

Verdict payload:

```json
{
  "submissionId": "uuid",
  "status": "ACCEPTED",
  "runtimeMs": 42,
  "memoryMb": 41,
  "passedCases": 57,
  "totalCases": 57,
  "errorOutput": null
}
```

Possible statuses: `PENDING` → `RUNNING` → `ACCEPTED` / `WRONG_ANSWER` / `TIME_LIMIT_EXCEEDED` / `MEMORY_LIMIT_EXCEEDED` / `COMPILE_ERROR` / `RUNTIME_ERROR`

---

## Sandbox Security

Each submission runs in a locked-down Docker container:

```bash
docker run --rm
  --network none          # no internet
  --memory 256m           # memory cap
  --cpus 0.5              # CPU cap
  --pids-limit 50         # prevent fork bombs
  --read-only             # immutable filesystem
  eclipse-temurin:21-jdk-alpine
```

---

## Architectural Rules (ArchUnit)

Enforced at test time — `mvn test` will fail if violated:

- Controllers must never access repositories directly
- `@Transactional` methods must never publish to Kafka directly (use Outbox)
- All DTOs and events must be Java records
- `ProcessBuilder` / `Runtime` usage restricted to the `sandbox` package only
- No native SQL queries in repositories
- Logger fields must be `private static final Logger log`
- No `System.out` / `System.err` usage

---

## Roadmap

- [ ] Python language support
- [ ] Leaderboard per problem
- [ ] Arabic UI
- [ ] GitHub sync (push solutions to a repo)
- [ ] Prometheus + Grafana observability dashboard
- [ ] Problem import from LeetCode (via the Mind Palace Chrome extension)
- [ ] Bulk problem export to PDF

---

## Contributing

Pull requests are welcome. For major changes please open an issue first.

```bash
# Run tests
./mvnw test

# Run with dev profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## License

MIT © [valar-morghulis-lock](https://github.com/valar-morghulis-lock)
