# LLM Gateway

A production-grade, multi-tenant API gateway for Large Language Models built with Spring Boot 3.3.5. Route requests across Groq and OpenAI with API-key authentication, per-tenant budget enforcement, semantic caching, circuit-breaking, and Prometheus/Grafana observability — all behind a single unified endpoint.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          LLM Gateway                                │
│                                                                     │
│  Client Request                                                     │
│      │                                                              │
│      ▼                                                              │
│  ┌──────────────┐                                                   │
│  │  Auth Filter │  Validates API key, resolves tenant               │
│  │  (ApiKeyFilter)                                                  │
│  └──────┬───────┘                                                   │
│         │                                                           │
│         ▼                                                           │
│  ┌──────────────┐                                                   │
│  │ Budget Check │  Rejects if spentUsd ≥ limitUsd                  │
│  │(BudgetService)                                                   │
│  └──────┬───────┘                                                   │
│         │                                                           │
│         ▼                                                           │
│  ┌──────────────┐                                                   │
│  │ Rate Limiter │  Per-tenant request rate enforcement              │
│  │(Resilience4j)                                                    │
│  └──────┬───────┘                                                   │
│         │                                                           │
│         ▼                                                           │
│  ┌──────────────┐   Hit?                                            │
│  │    Cache     │ ──────► Return cached ChatResponse                │
│  │(Redis SHA256)│                                                   │
│  └──────┬───────┘                                                   │
│         │ Miss                                                      │
│         ▼                                                           │
│  ┌──────────────┐                                                   │
│  │    Router    │  PRIORITY / COST / LATENCY strategy               │
│  │(RoutingService)                                                  │
│  └──────┬───────┘                                                   │
│         │                                                           │
│    ┌────┴─────┐                                                     │
│    ▼          ▼                                                     │
│ ┌──────┐  ┌────────┐                                                │
│ │ Groq │  │ OpenAI │  Circuit-breaker + Retry per provider          │
│ └──────┘  └────────┘                                                │
│    │          │                                                     │
│    └────┬─────┘                                                     │
│         │                                                           │
│         ▼                                                           │
│  ┌──────────────┐                                                   │
│  │   Response   │  Enriched with GatewayMeta (provider, latency,    │
│  │  + Logging   │  cacheHit). Usage logged async to PostgreSQL.     │
│  └──────────────┘                                                   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer              | Technology                                      |
|--------------------|-------------------------------------------------|
| Runtime            | Java 17, Spring Boot 3.3.5                      |
| Web                | Spring MVC + WebFlux (reactive streaming)       |
| Security           | Spring Security 6 (API-key filter chain)        |
| Persistence        | Spring Data JPA, Hibernate 6, PostgreSQL 16     |
| Schema Migrations  | Flyway 10                                       |
| Caching            | Redis 7 via Spring Data Redis                   |
| Resilience         | Resilience4j 2.2 (circuit-breaker, retry, rate-limiter) |
| Observability      | Micrometer + Prometheus + Grafana               |
| API Docs           | SpringDoc OpenAPI 2.6 / Swagger UI              |
| Build              | Maven 3, Lombok 1.18, MapStruct 1.5             |
| Containerisation   | Docker Compose (Postgres, Redis, Prometheus, Grafana) |

---

## Prerequisites

- **Java 17** — [Adoptium Temurin](https://adoptium.net/)
- **Maven 3.9+**
- **Docker Desktop** — for running Postgres, Redis, Prometheus, Grafana
- **Groq API Key** — free at [console.groq.com](https://console.groq.com)
- **OpenAI API Key** (optional) — [platform.openai.com](https://platform.openai.com)

---

## Local Setup

### 1. Clone the repository

```bash
git clone https://github.com/yashpratap/llm-gateway.git
cd llm-gateway
```

### 2. Configure environment variables

```bash
cp .env.example .env
# Edit .env and set GROQ_API_KEY (required) and OPENAI_API_KEY (optional)
```

### 3. Start infrastructure services

```bash
docker compose up -d postgres redis prometheus grafana
```

### 4. Run the application

```bash
# Export env vars (Windows PowerShell)
$env:GROQ_API_KEY = "your_groq_api_key"
$env:OPENAI_API_KEY = "your_openai_api_key"

mvn spring-boot:run
```

The gateway starts on **http://localhost:8080**.

---

## Docker Commands

```bash
# Start all infrastructure services
docker compose up -d

# Stop all services
docker compose down

# Tail logs for a specific service
docker compose logs -f postgres

# Reset volumes (clears all data)
docker compose down -v
```

---

## Environment Variables

| Variable        | Default                                      | Required | Description                     |
|-----------------|----------------------------------------------|----------|---------------------------------|
| `GROQ_API_KEY`  | —                                            | Yes      | Groq cloud API key              |
| `OPENAI_API_KEY`| *(empty)*                                    | No       | OpenAI API key                  |
| `DB_URL`        | `jdbc:postgresql://localhost:5432/llmgateway`| No       | PostgreSQL JDBC URL             |
| `DB_USERNAME`   | `llmgateway`                                 | No       | PostgreSQL username             |
| `DB_PASSWORD`   | `llmgateway`                                 | No       | PostgreSQL password             |
| `REDIS_HOST`    | `localhost`                                  | No       | Redis hostname                  |
| `REDIS_PORT`    | `6379`                                       | No       | Redis port                      |
| `REDIS_PASSWORD`| *(empty)*                                    | No       | Redis password                  |
| `SERVER_PORT`   | `8080`                                       | No       | HTTP port for the gateway       |

---

## API Endpoints

| Method | Path                              | Description                              | Auth     |
|--------|-----------------------------------|------------------------------------------|----------|
| POST   | `/api/v1/chat/completions`        | OpenAI-compatible chat completion        | API Key  |
| GET    | `/api/v1/health/providers`        | Provider health status map               | None     |
| GET    | `/api/v1/admin/tenants`           | List all tenants                         | Admin    |
| POST   | `/api/v1/admin/tenants`           | Create a new tenant                      | Admin    |
| GET    | `/api/v1/admin/analytics`         | Usage analytics summary                  | Admin    |
| GET    | `/swagger-ui.html`                | Interactive API documentation            | None     |
| GET    | `/api-docs`                       | Raw OpenAPI 3 JSON spec                  | None     |
| GET    | `/actuator/health`                | Spring Boot health check                 | None     |
| GET    | `/actuator/prometheus`            | Prometheus metrics scrape endpoint       | None     |

---

## Project Structure

```
llm-gateway/
├── docker-compose.yml
├── prometheus.yml
├── .env.example
└── src/
    └── main/
        ├── java/dev/yashpratap/llmgateway/
        │   ├── LlmGatewayApplication.java
        │   ├── security/
        │   │   ├── ApiKeyFilter.java
        │   │   ├── ApiKeyAuthenticationToken.java
        │   │   ├── ApiKeyAuthenticationProvider.java
        │   │   └── SecurityConfig.java
        │   ├── domain/
        │   │   ├── Tenant.java
        │   │   ├── ApiKey.java
        │   │   ├── Budget.java
        │   │   ├── RoutingPolicy.java
        │   │   ├── UsageLog.java
        │   │   └── ModelPricing.java
        │   ├── tenant/
        │   │   ├── TenantRepository.java
        │   │   ├── TenantService.java
        │   │   └── TenantContext.java
        │   ├── provider/
        │   │   ├── LLMProvider.java
        │   │   ├── ChatRequest.java
        │   │   ├── ChatResponse.java
        │   │   ├── Message.java / Choice.java / Usage.java
        │   │   ├── GatewayMeta.java / ChatChunk.java
        │   │   ├── ProviderName.java / ProviderException.java
        │   │   ├── groq/
        │   │   │   ├── GroqProvider.java
        │   │   │   └── GroqProperties.java
        │   │   └── openai/
        │   │       ├── OpenAIProvider.java
        │   │       └── OpenAIProperties.java
        │   ├── routing/
        │   │   ├── Router.java / RoutingStrategy.java
        │   │   ├── CostRouter.java / LatencyRouter.java / PriorityRouter.java
        │   │   └── RoutingService.java
        │   ├── cache/
        │   │   ├── CacheKeyGenerator.java
        │   │   └── RedisCacheService.java
        │   ├── billing/
        │   │   ├── BudgetService.java
        │   │   ├── UsageLogger.java
        │   │   └── UsageLogRepository.java
        │   ├── analytics/
        │   │   ├── AnalyticsService.java
        │   │   └── AnalyticsController.java
        │   ├── streaming/
        │   │   └── StreamingHandler.java
        │   ├── config/
        │   │   ├── AppConfig.java
        │   │   ├── RedisConfig.java
        │   │   ├── WebClientConfig.java
        │   │   ├── AsyncConfig.java
        │   │   └── PropertiesConfig.java
        │   ├── common/
        │   │   ├── GlobalExceptionHandler.java
        │   │   ├── ApiResponse.java / Constants.java
        │   │   ├── RateLimitException.java
        │   │   └── BudgetExceededException.java
        │   └── controller/
        │       ├── ChatController.java
        │       ├── AdminController.java
        │       └── HealthController.java
        └── resources/
            ├── application.yml
            └── db/migration/
                └── V1__init.sql
```

---

## Roadmap

### Phase 1 — Foundation (M1–M5)
- [x] **M1** — Project scaffold: entities, config, package structure
- [ ] **M2** — API-key authentication + tenant management REST API
- [ ] **M3** — Groq & OpenAI provider integration (circuit-breaker, retry)
- [ ] **M4** — Streaming SSE support + routing strategy selection
- [ ] **M5** — Redis semantic cache (SHA-256 exact match)

### Phase 2 — Production Hardening (M6–M13)
- [ ] **M6** — Per-tenant budget enforcement
- [ ] **M7** — Resilience4j rate limiter per tenant/plan
- [ ] **M8** — Analytics dashboard (cost, tokens, provider split)
- [ ] **M9** — Prometheus metrics + Grafana dashboards
- [ ] **M10** — Admin UI (Next.js)
- [ ] **M11** — Testcontainers integration test suite
- [ ] **M12** — Docker image + Kubernetes Helm chart
- [ ] **M13** — Semantic cache (embedding-based similarity search)

---

## License

MIT License — see [LICENSE](LICENSE) for details.
