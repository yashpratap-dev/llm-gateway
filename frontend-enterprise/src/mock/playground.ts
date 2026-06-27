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

export const sampleResponses = [
  'I can help you with that. Let me analyze the request carefully and provide a comprehensive answer.',
  'Here is the information you requested:\n\n```typescript\nconst example = {\n  key: "value",\n  nested: {\n    data: [1, 2, 3]\n  }\n};\n```\n\nThis demonstrates the requested pattern.',
  'Based on the context provided, the optimal approach would be to use a layered architecture that separates concerns and enables horizontal scaling.',
];
