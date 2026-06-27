import type { ChatMessage, TelemetryData, PlaygroundConfig } from '../types';

export const initialMessages: ChatMessage[] = [
  {
    id: 'msg-001',
    role: 'user',
    content: 'Write a SQL query to find the top 10 customers by total revenue in the last 30 days, joining orders and customers tables.',
    timestamp: new Date(Date.now() - 90000),
  },
  {
    id: 'msg-002',
    role: 'assistant',
    content: `Here's a SQL query to find the top 10 customers by total revenue in the last 30 days:

\`\`\`sql
SELECT
  c.customer_id,
  c.name,
  c.email,
  COUNT(o.order_id)      AS total_orders,
  SUM(o.total_amount)    AS total_revenue
FROM customers c
INNER JOIN orders o
  ON c.customer_id = o.customer_id
WHERE o.created_at >= NOW() - INTERVAL '30 days'
  AND o.status = 'completed'
GROUP BY
  c.customer_id,
  c.name,
  c.email
ORDER BY total_revenue DESC
LIMIT 10;
\`\`\`

**Key points:**
- Filters orders from the last 30 days using \`NOW() - INTERVAL '30 days'\`
- Only counts \`completed\` orders to exclude refunds and cancellations
- Groups by customer to aggregate order counts and revenue
- Orders by \`total_revenue DESC\` to get the highest spenders first
- Limits to top 10 results

If you're using MySQL instead of PostgreSQL, replace \`INTERVAL '30 days'\` with \`INTERVAL 30 DAY\`.`,
    timestamp: new Date(Date.now() - 85000),
  },
];

export const defaultConfig: PlaygroundConfig = {
  provider: 'openai',
  model: 'gpt-4o-2024-11-20',
  temperature: 0.7,
  maxTokens: 2048,
  streaming: true,
};

export const telemetryData: TelemetryData = {
  provider: 'OpenAI',
  model: 'gpt-4o-2024-11-20',
  latency: 1420,
  cost: 0.002341,
  status: 200,
  semanticCacheHit: true,
  exactCacheHit: false,
  similarityScore: 0.92,
  promptTokens: 432,
  completionTokens: 812,
  totalTokens: 1244,
  embeddingModel: 'text-embedding-3-large',
  redisConnected: true,
  postgresConnected: true,
  circuitState: 'closed',
  tenant: 'acme-corp',
  requestId: 'req_01JX6Z3Q7V8K2M5Y8D2PT1T6A48',
  apiKey: 'sk-gw••••••••8f2a',
};

export const providerModels: Record<string, string[]> = {
  openai: [
    'gpt-4.1',
    'gpt-4o-2024-11-20',
    'gpt-4o-mini',
    'gpt-3.5-turbo',
  ],
  claude: [
    'claude-3-7-sonnet-20250219',
    'claude-3-5-haiku-20241022',
    'claude-3-opus-20240229',
    'claude-3-sonnet-20240229',
  ],
  groq: [
    'llama-3.3-70b-versatile',
    'llama-3.1-8b-instant',
    'mixtral-8x7b-32768',
    'gemma2-9b-it',
  ],
  gemini: [
    'gemini-2.5-pro',
    'gemini-2.0-flash',
    'gemini-1.5-pro',
    'gemini-1.5-flash',
  ],
};

// ─── Context-aware response engine ───────────────────────────────────────────

const R_SQL = `Here's a practical SQL query using a JOIN and aggregation:

\`\`\`sql
SELECT
  u.id,
  u.name,
  u.email,
  COUNT(o.id)       AS total_orders,
  SUM(o.amount)     AS total_revenue,
  MAX(o.created_at) AS last_order_date
FROM users u
INNER JOIN orders o
  ON o.user_id = u.id
WHERE o.created_at >= NOW() - INTERVAL '30 days'
  AND o.status = 'completed'
GROUP BY u.id, u.name, u.email
ORDER BY total_revenue DESC
LIMIT 20;
\`\`\`

**Key points:**
- \`INNER JOIN\` returns only users who have completed orders. Switch to \`LEFT JOIN\` to include users with zero orders.
- \`NOW() - INTERVAL '30 days'\` is PostgreSQL syntax. MySQL equivalent: \`DATE_SUB(NOW(), INTERVAL 30 DAY)\`.
- Prepend \`EXPLAIN ANALYZE\` to inspect the execution plan before deploying.
- Index \`orders(user_id, status, created_at)\` for this query to scale beyond a few million rows.`;

const R_JAVA = `Here's a typed Spring Boot REST controller with validation:

\`\`\`java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<Page<UserDto>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
            userService.findAll(PageRequest.of(page, size)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody CreateUserRequest req) {
        return userService.create(req);
    }

    @GetMapping("/{id}")
    public UserDto getById(@PathVariable Long id) {
        return userService.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "User not found: " + id));
    }
}
\`\`\`

**Service layer with JPA:**
\`\`\`java
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repo;
    private final UserMapper     mapper;

    public Page<UserDto> findAll(Pageable p) {
        return repo.findAll(p).map(mapper::toDto);
    }

    @Transactional
    public UserDto create(CreateUserRequest req) {
        User user = mapper.toEntity(req);
        return mapper.toDto(repo.save(user));
    }

    public Optional<UserDto> findById(Long id) {
        return repo.findById(id).map(mapper::toDto);
    }
}
\`\`\`

Add a \`@ControllerAdvice\` class with \`@ExceptionHandler\` methods to centralise error responses across all controllers.`;

const R_REDIS = `Redis is an in-memory data structure store used for caching, pub/sub, queues, and rate limiting.

**Core commands:**
\`\`\`bash
# Strings with expiry
SET session:abc "user:42" EX 1800
GET session:abc

# Hash — structured data without serialisation overhead
HSET user:42 name "Alice" role "admin" plan "pro"
HGETALL user:42

# Sorted set — leaderboards, priority queues
ZADD requests:rate 1718000000 "tenant:acme"
ZRANGEBYSCORE requests:rate 1718000000 +inf WITHSCORES
\`\`\`

**Spring Boot caching with Redis:**
\`\`\`java
@Cacheable(value = "users", key = "#id", unless = "#result == null")
public UserDto findById(Long id) {
    return repo.findById(id).map(mapper::toDto).orElse(null);
}

@CacheEvict(value = "users", key = "#id")
public void delete(Long id) {
    repo.deleteById(id);
}
\`\`\`

**Semantic caching** extends this pattern by embedding the prompt into a vector, then checking cosine similarity against cached embeddings. A similarity score above ~0.92 returns the cached response directly — saving an upstream LLM call without a noticeable quality difference for the end user.`;

const R_DOCKER = `**Dockerfile for a Spring Boot application:**
\`\`\`dockerfile
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

COPY target/*.jar app.jar

RUN addgroup -S app && adduser -S app -G app
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
\`\`\`

**docker-compose for local development:**
\`\`\`yaml
services:
  app:
    build: .
    ports: ["8080:8080"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/appdb
      SPRING_REDIS_HOST: redis
    depends_on: [db, redis]

  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: appdb
      POSTGRES_PASSWORD: secret
    volumes: [pgdata:/var/lib/postgresql/data]

  redis:
    image: redis:7-alpine
    command: redis-server --save 60 1 --loglevel warning

volumes:
  pgdata:
\`\`\`

**Minimal Kubernetes Deployment:**
\`\`\`yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: app
spec:
  replicas: 3
  selector:
    matchLabels: { app: app }
  template:
    metadata:
      labels: { app: app }
    spec:
      containers:
        - name: app
          image: registry.example.com/app:1.0.0
          ports: [{ containerPort: 8080 }]
          resources:
            requests: { cpu: "250m", memory: "256Mi" }
            limits:   { cpu: "500m", memory: "512Mi" }
          readinessProbe:
            httpGet: { path: /actuator/health, port: 8080 }
            initialDelaySeconds: 15
\`\`\``;

const R_REACT = `Here's a typed React component with a co-located custom hook:

\`\`\`tsx
// types.ts
interface User {
  id:    number;
  name:  string;
  email: string;
  role:  'admin' | 'viewer';
}

// useUsers.ts
function useUsers() {
  const [users,   setUsers]   = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetch('/api/v1/users')
      .then(r => r.ok ? r.json() as Promise<User[]> : Promise.reject(r.statusText))
      .then(data  => { if (!cancelled) setUsers(data); })
      .catch(err  => { if (!cancelled) setError(String(err)); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, []);

  return { users, loading, error };
}

// UserList.tsx
export function UserList() {
  const { users, loading, error } = useUsers();

  if (loading) return <div className="text-sm text-muted">Loading...</div>;
  if (error)   return <div className="text-sm text-red-400">{error}</div>;

  return (
    <ul className="space-y-2">
      {users.map(u => (
        <li key={u.id} className="flex items-center justify-between p-3 rounded-lg bg-white/4 border border-white/8">
          <div>
            <p className="text-sm font-medium">{u.name}</p>
            <p className="text-xs text-muted">{u.email}</p>
          </div>
          <span className="text-xs px-2 py-0.5 rounded-full bg-white/8">{u.role}</span>
        </li>
      ))}
    </ul>
  );
}
\`\`\`

**Key patterns:**
- Cleanup flag (\`cancelled\`) prevents state updates after unmount — avoids the "Can't perform state update on unmounted component" warning.
- \`Promise.reject(r.statusText)\` surfaces HTTP errors that \`fetch\` silently swallows for non-2xx responses.
- Keep hooks colocated with the component that owns the data; lift only when two components genuinely share the same state.`;

const R_GREETING = `Hello! I'm the FRIDAY gateway assistant.

I can help you with:

- **SQL & databases** — queries, JOINs, indexing, query optimization
- **Java & Spring Boot** — REST APIs, JPA entities, dependency injection
- **Redis & caching** — cache strategy, semantic similarity, TTL design
- **Docker & Kubernetes** — Dockerfiles, Compose files, K8s manifests
- **React & TypeScript** — components, hooks, typed patterns

What are you working on?`;

const R_DEFAULT = `A few approaches worth considering:

**Option A — simple and direct**
\`\`\`
Input → Validate → Process → Persist → Respond
\`\`\`
Start here. Measure before optimising.

**Option B — if throughput matters**
Decouple the write path from the read path. Persist to a queue (Kafka, SQS) and process asynchronously. Serve reads from a cache layer (Redis, CDN).

**Option C — if consistency matters more than availability**
Wrap the operation in a transaction. Accept the latency cost of synchronous persistence and return only after the commit succeeds.

**Checklist before shipping:**
1. Define the request/response contract first
2. Validate at the system boundary — not inside business logic
3. Return structured errors with an \`error.code\` and \`error.message\`
4. Add a correlation ID to every response for tracing

If you share the specific language, framework, or constraint you're working within I can give you something more precise.`;

const PATTERNS: Array<{ re: RegExp; response: string }> = [
  {
    re: /\b(hello|hi|hey|howdy|what can you|what do you|greetings)\b/,
    response: R_GREETING,
  },
  {
    re: /\b(sql|select|insert|update|delete|join|query|queries|database|postgres|postgresql|mysql|sqlite|table|schema|index|migration|sequelize|prisma|knex)\b/,
    response: R_SQL,
  },
  {
    re: /\b(java|spring|springboot|maven|gradle|jpa|hibernate|controller|repository|autowired|pom\.xml|junit|mockito|lombok|restcontroller|requestmapping)\b/,
    response: R_JAVA,
  },
  {
    re: /\b(redis|cache|caching|semantic cache|evict|ttl|hit rate|miss rate|memcache|in-memory|invalidate|expiry)\b/,
    response: R_REDIS,
  },
  {
    re: /\b(docker|dockerfile|container|kubernetes|k8s|pod|helm|docker-compose|compose|image|registry|kubectl|namespace|deployment|ingress)\b/,
    response: R_DOCKER,
  },
  {
    re: /\b(react|typescript|tsx|jsx|component|hook|usestate|useeffect|usecallback|usememo|props|context|vite|nextjs|next\.js|remix|tailwind)\b/,
    response: R_REACT,
  },
];

export function getContextualResponse(userMessage: string): string {
  const lower = userMessage.toLowerCase();
  for (const { re, response } of PATTERNS) {
    if (re.test(lower)) return response;
  }
  return R_DEFAULT;
}
