# LLM Gateway

Production-grade multi-tenant LLM Gateway with API-key auth, per-tenant routing policies (COST/LATENCY/PRIORITY), Redis caching, rate limiting, budget enforcement, cost tracking, and Prometheus/Grafana observability.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         LLM Gateway v1.0                                │
│                                                                         │
│  Client Request                                                         │
│       │                                                                 │
│       ▼                                                                 │
│  [Auth Filter] → API Key validation → Tenant resolution                 │
│       │                                                                 │
│       ▼                                                                 │
│  [Rate Limiter] → Redis fixed-window counter (10/60/300 req/min)        │
│       │                                                                 │
│       ▼                                                                 │
│  [Budget Check] → PostgreSQL spend vs limit                             │
│       │                                                                 │
│       ▼                                                                 │
│  [Cache Lookup] → Redis exact-match (tenant-isolated, 1hr TTL)          │
│       │ miss                                                            │
│       ▼                                                                 │
│  [Router] → Tenant policy → COST / LATENCY / PRIORITY                  │
│       │                                                                 │
│       ▼                                                                 │
│  [Provider] → Groq / OpenAI                                             │
│       │                                                                 │
│       ▼                                                                 │
│  [Cost Calc] → pricing engine → cost calculation                        │
│       │                                                                 │
│       ▼                                                                 │
│  [Async Pipeline] → Cache write + Usage log + Budget deduct             │
│       │                                                                 │
│       ▼                                                                 │
│  Response + X-RateLimit-Remaining header                                │
└─────────────────────────────────────────────────────────────────────────┘
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

| Method | Path                                                    | Description                              | Auth     |
|--------|---------------------------------------------------------|------------------------------------------|----------|
| POST   | `/api/v1/chat/completions`                              | OpenAI-compatible chat completion        | API Key  |
| GET    | `/api/v1/health/providers`                              | Provider health status map               | None     |
| POST   | `/api/v1/admin/tenants`                                 | Create a new tenant                      | API Key  |
| GET    | `/api/v1/admin/tenants`                                 | List all tenants                         | API Key  |
| POST   | `/api/v1/admin/tenants/{tenantId}/keys`                 | Generate an API key for a tenant         | API Key  |
| GET    | `/api/v1/admin/tenants/{tenantId}/keys`                 | List API keys for a tenant               | API Key  |
| DELETE | `/api/v1/admin/keys/{keyId}`                            | Revoke an API key                        | API Key  |
| GET    | `/api/v1/admin/tenants/{tenantId}/routing-policy`       | Get tenant routing policy                | API Key  |
| PUT    | `/api/v1/admin/tenants/{tenantId}/routing-policy`       | Update tenant routing policy             | API Key  |
| GET    | `/api/v1/admin/analytics/usage`                         | Usage logs for a tenant (date range)     | API Key  |
| GET    | `/api/v1/admin/analytics/by-provider`                   | Aggregated cost by provider              | API Key  |
| GET    | `/api/v1/admin/analytics/budget/{tenantId}`             | Budget status for a tenant               | API Key  |
| GET    | `/swagger-ui.html`                                      | Interactive API documentation            | None     |
| GET    | `/api-docs`                                             | Raw OpenAPI 3 JSON spec                  | None     |
| GET    | `/actuator/health`                                      | Spring Boot health check                 | None     |
| GET    | `/actuator/prometheus`                                  | Prometheus metrics scrape endpoint       | None     |

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

### Phase 1 — Complete ✅

| Module | Feature | Status |
|--------|---------|--------|
| M1 | Foundation & Infrastructure | ✅ Complete |
| M2 | Tenant & API Key Authentication | ✅ Complete |
| M3 | LLM Provider Abstraction (Groq + OpenAI) | ✅ Complete |
| M4 | Full Gateway Pipeline | ✅ Complete |
| M5 | Routing Engine | ✅ Complete |

### Phase 2 — Planned

| Module | Feature | Status |
|--------|---------|--------|
| Playground | UI/console — prompt, stream, cost display | 🔄 Planned |
| Admin Dashboard | Metrics, cost charts, provider usage | 🔄 Planned |
| Semantic Cache | pgvector + embedding similarity | 🔄 Planned |
| Anthropic Provider | Claude integration | 🔄 Planned |
| Dynamic Routing | Real latency-based auto-switching | 🔄 Planned |
| Kafka Analytics | Event-driven usage pipeline | 🔄 Planned |

---

## Known Limitations

| Limitation | Details | Planned Fix |
|-----------|---------|-------------|
| Streaming token tracking | Token usage and cost are not tracked for streamed requests | Planned in later module |

---

## License

MIT License — see [LICENSE](LICENSE) for details.
