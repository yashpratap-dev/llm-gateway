<div align="center">

<img src="https://img.shields.io/badge/FRIDAY-LLM%20GATEWAY-19D3FF?style=for-the-badge&labelColor=050505&color=19D3FF" alt="FRIDAY LLM Gateway" />

<br/>
<br/>

```
███████ ██████  ██ ██████   █████  ██    ██
██      ██   ██ ██ ██   ██ ██   ██  ██  ██
█████   ██████  ██ ██   ██ ███████   ████
██      ██   ██ ██ ██   ██ ██   ██    ██
██      ██   ██ ██ ██████  ██   ██    ██
```

### **Production-grade Multi-Tenant LLM Gateway**
*Route · Cache · Observe · Control — every LLM call, from one place*

<br/>

![Java](https://img.shields.io/badge/Java%2017-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot%203.3.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL%2016-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis%207-DC382D?style=flat-square&logo=redis&logoColor=white)
![React](https://img.shields.io/badge/React%2019-61DAFB?style=flat-square&logo=react&logoColor=black)
![Three.js](https://img.shields.io/badge/Three.js-000000?style=flat-square&logo=threedotjs&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)
![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=flat-square&logo=prometheus&logoColor=white)

<br/>

[**Live Playground →**](#quick-start) · [**API Docs →**](#api-reference) · [**Architecture →**](#architecture)

</div>

---

## The Problem

Every team integrating LLMs into production hits the same wall:

| Problem | Without LLM Gateway |
|---|---|
| Provider outage | Your app goes down with it |
| Duplicate prompts | You pay the LLM API every single time |
| No spend control | One bad actor burns your entire API budget |
| Zero visibility | You have no idea what's slow, what's expensive, or what's failing |
| Multi-tenant complexity | You're re-implementing auth + isolation in every service |

**LLM Gateway solves all of this in one place** — a production-grade reverse proxy that sits between your application and every LLM provider.

---

## What's Inside

<table>
<tr>
<td width="50%">

### 🔀 Multi-Provider Routing
Unified API across **OpenAI · Groq · Claude · Gemini**.  
Per-tenant routing policies: `COST` / `LATENCY` / `PRIORITY`.  
Auto-failover when a provider circuit opens.

### ⚡ Two-Layer Caching
**Exact cache** — Redis SHA-256 keyed, O(1) lookup, zero embedding cost.  
**Semantic cache** — pgvector cosine similarity (threshold 0.92). Catches paraphrased prompts. Embeddings themselves are Redis-cached to eliminate redundant API calls.

### 🛡️ Resilience
Resilience4j circuit breakers per provider.  
Configurable failure threshold, half-open recovery window, and automatic fallback routing.

</td>
<td width="50%">

### 🏢 Multi-Tenancy
Complete per-tenant isolation — API keys, budgets, routing policies, rate limits, and cache namespacing. One gateway, infinite tenants.

### 📊 Full Observability
Prometheus metrics + pre-built Grafana dashboard.  
Request volume, p95 latency, cost per provider, cache hit ratio, circuit breaker state transitions — all live.

### 🎨 FRIDAY UI
JARVIS-style enterprise playground built in React 19 + Three.js.  
Real-time telemetry rail. SSE streaming. Provider selector. Everything a developer needs to inspect gateway behavior.

</td>
</tr>
</table>

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           LLM GATEWAY — Request Pipeline                     │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Incoming Request                                                           │
│         │                                                                    │
│         ▼                                                                    │
│   ┌─────────────┐   SHA-256 hash lookup   ┌──────────────┐                  │
│   │  API Key    │ ──────────────────────► │  PostgreSQL  │                  │
│   │  Auth       │   Tenant resolved        │  (api_keys)  │                  │
│   └─────────────┘                         └──────────────┘                  │
│         │                                                                    │
│         ▼                                                                    │
│   ┌─────────────┐   fixed-window counter  ┌──────────────┐                  │
│   │  Rate       │ ──────────────────────► │    Redis     │                  │
│   │  Limiter    │   per-tenant, per-min    │  (counters)  │                  │
│   └─────────────┘                         └──────────────┘                  │
│         │                                                                    │
│         ▼                                                                    │
│   ┌─────────────┐   spend vs limit        ┌──────────────┐                  │
│   │  Budget     │ ──────────────────────► │  PostgreSQL  │                  │
│   │  Enforcer   │   hard stop on breach    │  (budgets)   │                  │
│   └─────────────┘                         └──────────────┘                  │
│         │                                                                    │
│         ▼                                                                    │
│   ┌─────────────┐   SHA-256 key           ┌──────────────┐                  │
│   │  Exact      │ ──────────────────────► │    Redis     │ ── HIT ──► ✓    │
│   │  Cache      │   tenant-isolated, 1hr   │  (cache)     │                  │
│   └─────────────┘                         └──────────────┘                  │
│         │ MISS                                                               │
│         ▼                                                                    │
│   ┌─────────────┐   cosine similarity     ┌──────────────┐                  │
│   │  Semantic   │ ──────────────────────► │  PostgreSQL  │ ── HIT ──► ✓    │
│   │  Cache      │   pgvector, ≥ 0.92       │  (pgvector)  │                  │
│   └─────────────┘                         └──────────────┘                  │
│         │ MISS                                                               │
│         ▼                                                                    │
│   ┌─────────────┐   COST / LATENCY        ┌──────────────┐                  │
│   │  Router     │   PRIORITY policy       │  Provider    │                  │
│   │             │ ──────────────────────► │  Registry    │                  │
│   └─────────────┘                         └──────────────┘                  │
│         │                                                                    │
│         ▼                                                                    │
│   ┌───────────────────────────────────────────────────────┐                 │
│   │              Circuit Breaker Layer                    │                 │
│   │   OpenAI CB │ Groq CB │ Claude CB │ Gemini CB         │                 │
│   │   (Resilience4j — failure threshold + auto-failover)  │                 │
│   └───────────────────────────────────────────────────────┘                 │
│         │                                                                    │
│         ▼                                                                    │
│   ┌─────────────────────────────────────────────────────┐                   │
│   │         LLM Provider (streaming or blocking)        │                   │
│   │      OpenAI  │  Groq  │  Anthropic  │  Gemini       │                   │
│   └─────────────────────────────────────────────────────┘                   │
│         │                                                                    │
│         ▼                                                                    │
│   ┌─────────────────────────────────────────────────────┐                   │
│   │              Async Post-Processing                  │                   │
│   │   Cache write  │  Usage log  │  Budget deduction    │                   │
│   │   Metrics emit │  Cost calc  │  Embedding store     │                   │
│   └─────────────────────────────────────────────────────┘                   │
│         │                                                                    │
│         ▼                                                                    │
│   Response + GatewayMeta { provider, latencyMs, cacheHit, cost }            │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

### Backend

| Layer | Technology | Why |
|---|---|---|
| Runtime | Java 17, Spring Boot 3.3.5 | Virtual threads ready, production-proven |
| Web | Spring MVC + WebFlux | MVC for REST, WebFlux for reactive SSE streaming |
| Security | Spring Security 6 | Custom API-key filter chain; SHA-256 hashed keys — raw key never stored |
| ORM | Spring Data JPA + Hibernate 6 | Full JPA lifecycle with pgvector type support |
| Vector DB | PostgreSQL 16 + pgvector | Native cosine similarity; no separate vector database needed |
| Migrations | Flyway 10 | Version-controlled schema; reproducible from scratch |
| Cache | Redis 7 (Lettuce) | Exact cache + rate limit counters + embedding cache |
| Resilience | Resilience4j 2.2 | Circuit breaker, retry, rate limiter — all per-provider |
| Observability | Micrometer + Prometheus | Custom business metrics (cost, cache ratio, circuit state) |
| API Docs | SpringDoc OpenAPI 2.6 | Auto-generated Swagger UI from annotations |
| Build | Maven 3 + Lombok + MapStruct | Zero boilerplate, clean DTO mapping |
| Infra | Docker Compose | One command spins up Postgres, Redis, Prometheus, Grafana |

### Frontend

| Layer | Technology |
|---|---|
| Framework | React 19 + Vite + TypeScript |
| 3D Orb | Three.js + React Three Fiber |
| Animations | Framer Motion |
| Charts | Recharts |
| Styling | Tailwind CSS + shadcn/ui |
| State | React hooks (no external library needed) |
| Dashboard | TanStack Query + Recharts |

---

## Quick Start

### Prerequisites

- Java 17 (Adoptium Temurin)
- Maven 3.9+
- Docker Desktop
- Groq API Key — free at [console.groq.com](https://console.groq.com)

### 1. Clone

```bash
git clone https://github.com/yashpratap-dev/llm-gateway.git
cd llm-gateway
```

### 2. Configure

```bash
cp .env.example .env
```

Open `.env` and set:

```env
GROQ_API_KEY=gsk_your_key_here
# OPENAI_API_KEY=sk_your_key_here  (optional — needed for semantic cache embeddings)
```

### 3. Start infrastructure

```bash
docker compose up -d
```

Starts: PostgreSQL `:5433` · Redis `:6380` · Prometheus `:9090` · Grafana `:3001`

### 4. Run the backend

```bash
# From IntelliJ — Run LlmGatewayApplication
# or from terminal:
mvn spring-boot:run
```

Gateway starts on `http://localhost:8081`

### 5. Create your first tenant + API key

```bash
# 1. Create tenant
curl -X POST http://localhost:8081/api/v1/admin/tenants \
  -H "Content-Type: application/json" \
  -d '{"name": "my-app", "plan": "FREE"}'

# Copy the "id" from the response (your tenantId)

# 2. Generate API key
curl -X POST http://localhost:8081/api/v1/admin/tenants/{tenantId}/keys \
  -H "Content-Type: application/json" \
  -d '{"name": "dev-key"}'

# Save the "rawKey" — shown ONCE, never stored
```

### 6. Make your first request

```bash
curl -X POST http://localhost:8081/api/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer lgw_your_raw_key" \
  -d '{
    "model": "auto",
    "messages": [
      {"role": "user", "content": "Explain circuit breakers in distributed systems"}
    ]
  }'
```

Response:

```json
{
  "success": true,
  "data": {
    "id": "chatcmpl-abc123",
    "model": "llama-3.3-70b-versatile",
    "provider": "GROQ",
    "choices": [{ "message": { "role": "assistant", "content": "..." } }],
    "usage": { "promptTokens": 42, "completionTokens": 180, "costUsd": 0.0000058 },
    "gatewayMeta": { "provider": "GROQ", "cacheHit": false, "latencyMs": 612 }
  }
}
```

### 7. Start the Playground UI

```bash
cd frontend-enterprise
npm install
npm run dev
# Open http://localhost:3004
```

---

## API Reference

### Chat

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `POST` | `/api/v1/chat/completions` | Blocking chat completion | API Key |
| `POST` | `/api/v1/chat/completions/stream` | SSE token-by-token streaming | API Key |

**Streaming SSE events:**

```
event: token
data: {"delta": "Hello"}

event: token
data: {"delta": " world"}

event: done
data: {"provider": "GROQ", "latencyMs": 843, "cacheHit": false}
```

### Admin

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/admin/tenants` | Create tenant |
| `GET` | `/api/v1/admin/tenants` | List all tenants |
| `POST` | `/api/v1/admin/tenants/{id}/keys` | Generate API key |
| `DELETE` | `/api/v1/admin/keys/{keyId}` | Revoke API key |
| `GET/PUT` | `/api/v1/admin/tenants/{id}/routing-policy` | Get/update routing policy |
| `GET` | `/api/v1/admin/analytics/overview` | Usage overview |
| `GET` | `/api/v1/admin/analytics/by-provider` | Cost breakdown by provider |
| `GET` | `/api/v1/admin/analytics/budget/{tenantId}` | Budget status |

### Health & Observability

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/health/providers` | Per-provider health + circuit state |
| `GET` | `/actuator/health` | Spring Boot health check |
| `GET` | `/actuator/prometheus` | Prometheus scrape endpoint |
| `GET` | `/swagger-ui/index.html` | Interactive API explorer |

---

## Semantic Cache Deep Dive

The semantic cache is the most interesting piece of this gateway. It prevents paying for the same prompt twice — even when users paraphrase it.

### Pipeline

```
1. Normalize prompt
   └── system msg + last 3 turns + current user msg → lowercase → trim → collapse whitespace

2. Check Redis embedding cache
   └── key: emb:v1:{model}:SHA256(normalized)
   └── HIT → skip OpenAI embedding API call entirely (saves ~500ms + cost)
   └── MISS → call text-embedding-3-small

3. Store embedding in Redis (24h TTL)

4. pgvector cosine similarity search
   └── SELECT * FROM semantic_cache_entries
       WHERE 1 - (embedding <=> $1) >= 0.92
       ORDER BY embedding <=> $1 LIMIT 1

5. HIT (≥ 0.92 similarity) → return cached response, no LLM call
   MISS → call LLM, store response + embedding async
```

### Configuration

```yaml
cache:
  semantic:
    enabled: true
    similarity-threshold: 0.92
    ttl-hours: 24
    model-isolation: true
    embedding-model-version: text-embedding-3-small
    eviction:
      cron: "0 0 * * * *"   # top of every hour
```

### Prometheus Metrics

| Metric | Description |
|---|---|
| `llm_semantic_cache_hits_total` | Requests served from semantic cache |
| `llm_semantic_cache_misses_total` | Cache misses (LLM called) |
| `llm_semantic_similarity_score` | Similarity score of matched entry |
| `llm_embedding_cache_hits_total` | Embedding vector served from Redis |
| `llm_embedding_cache_misses_total` | Embedding API called |

---

## Design Decisions

These aren't arbitrary choices — each one maps to a real production concern.

**SHA-256 for API key storage**  
Raw keys are never stored. Only a SHA-256 hex digest lives in the database. If the database is compromised, no key is recoverable. This is exactly how Stripe and OpenAI store API keys.

**Two cache layers, not one**  
Exact match is O(1) with zero embedding cost. Semantic match catches paraphrased prompts but costs an embedding call. Running exact first means semantic is only invoked on a true cache miss — minimizing unnecessary API spend.

**Circuit breakers per provider, not per gateway**  
A Groq outage shouldn't trip the OpenAI circuit. Isolation at the provider level means one failing provider degrades gracefully while the others remain unaffected.

**pgvector over a separate vector database**  
Pinecone and Weaviate add operational complexity and cost. pgvector gives cosine similarity search inside the same PostgreSQL instance the rest of the gateway already uses — no extra infrastructure, no eventual consistency surprises.

**Async post-processing**  
Cache writes, usage logging, and budget deductions happen in a separate thread after the response is returned. The client gets their answer faster; the gateway does bookkeeping in the background.

**Per-tenant routing policies**  
A cost-sensitive tenant routes to the cheapest model. A latency-sensitive tenant routes to the fastest. Routing is a business decision, not a technical one — so it belongs in configuration, not code.

---

## Project Structure

```
llm-gateway/
│
├── src/main/java/dev/yashpratap/llmgateway/
│   ├── LlmGatewayApplication.java
│   │
│   ├── security/                    # API key auth filter chain
│   │   ├── ApiKeyFilter.java        # Extracts Bearer token, invokes provider
│   │   ├── ApiKeyAuthenticationProvider.java  # SHA-256 lookup + tenant bind
│   │   └── SecurityConfig.java      # Filter chain config, endpoint whitelisting
│   │
│   ├── domain/                      # JPA entities
│   │   ├── Tenant.java
│   │   ├── ApiKey.java              # key_hash, key_prefix, status
│   │   ├── Budget.java
│   │   ├── RoutingPolicy.java
│   │   ├── UsageLog.java
│   │   └── ModelPricing.java
│   │
│   ├── provider/                    # LLM provider adapters
│   │   ├── LLMProvider.java         # Interface: generate() + stream()
│   │   ├── ProviderRegistry.java    # Runtime provider map
│   │   ├── groq/GroqProvider.java
│   │   ├── openai/OpenAIProvider.java
│   │   ├── claude/ClaudeProvider.java
│   │   └── gemini/GeminiProvider.java
│   │
│   ├── routing/                     # Routing strategies
│   │   ├── Router.java              # Strategy selector
│   │   ├── CostRouter.java
│   │   ├── LatencyRouter.java
│   │   └── PriorityRouter.java
│   │
│   ├── cache/                       # Two-layer caching
│   │   ├── RedisCacheService.java   # Exact cache (SHA-256 keyed)
│   │   └── SemanticCacheService.java  # pgvector similarity search
│   │
│   ├── billing/                     # Budget + usage
│   │   ├── BudgetService.java
│   │   └── UsageLogger.java
│   │
│   ├── metrics/                     # Prometheus instrumentation
│   │   └── GatewayMetricsService.java
│   │
│   └── controller/
│       ├── ChatController.java      # /chat/completions + /stream
│       ├── AdminController.java     # Tenant + key management
│       └── HealthController.java
│
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/                # Flyway V1–V6
│
├── frontend-enterprise/             # FRIDAY UI (React 19 + Three.js)
│   └── src/
│       ├── pages/                   # Playground, Dashboard, Providers...
│       ├── components/orb/          # Three.js AI orb
│       ├── hooks/useStreamingChat.ts  # SSE streaming hook
│       └── mock/                    # Mock data for non-wired pages
│
├── dashboard/                       # Admin dashboard (TanStack Query)
├── playground/                      # Lightweight original playground
├── docker-compose.yml
└── prometheus.yml
```

---

## Environment Variables

| Variable | Default | Required | Description |
|---|---|---|---|
| `GROQ_API_KEY` | — | **Yes** | Groq cloud API key |
| `OPENAI_API_KEY` | — | No | OpenAI key (for semantic cache embeddings) |
| `DB_URL` | `jdbc:postgresql://localhost:5433/llmgateway` | No | PostgreSQL JDBC URL |
| `DB_USERNAME` | `llmgateway` | No | PostgreSQL username |
| `DB_PASSWORD` | `llmgateway` | No | PostgreSQL password |
| `REDIS_HOST` | `localhost` | No | Redis hostname |
| `REDIS_PORT` | `6380` | No | Redis port |

---

## Roadmap

- [x] Multi-provider routing (OpenAI, Groq, Claude, Gemini)
- [x] SSE streaming with per-chunk telemetry
- [x] API key auth (SHA-256, never stored in plaintext)
- [x] Multi-tenant isolation
- [x] Rate limiting (Redis fixed-window)
- [x] Budget enforcement
- [x] Exact Redis cache
- [x] Semantic cache (pgvector cosine similarity)
- [x] Circuit breakers (Resilience4j per-provider)
- [x] Prometheus metrics + Grafana dashboard
- [x] FRIDAY enterprise UI (React 19 + Three.js)
- [x] Admin dashboard (TanStack Query)
- [ ] Production deployment (Railway / Render)
- [ ] Kafka-based async analytics pipeline
- [ ] Prompt template versioning
- [ ] WebSocket support

---

<div align="center">

Built by [Yash Pratap](https://github.com/yashpratap-dev) · Java Backend Developer

*This project is a portfolio demonstration of production-grade backend engineering.*  
*Every feature is fully implemented, tested, and runnable locally.*

</div>
