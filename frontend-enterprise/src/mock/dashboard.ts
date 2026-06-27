import type {
  KPICard,
  ProviderRoutingRow,
  TrafficPoint,
  ProviderMixItem,
  CostPoint,
  TenantCost,
  CachePerformance,
  CircuitEvent,
  TopModel,
  GatewayEvent,
} from '../types';

export const kpiCards: KPICard[] = [
  {
    label: 'Requests',
    value: '24.38M',
    trend: 14.6,
    sparkline: [18.2, 19.1, 20.4, 21.8, 22.3, 23.1, 24.38],
    unit: 'M',
  },
  {
    label: 'Latency P95',
    value: '642ms',
    trend: 8.7,
    sparkline: [580, 601, 621, 638, 625, 640, 642],
  },
  {
    label: 'Cost',
    value: '$128,430.21',
    trend: 12.3,
    sparkline: [98000, 104000, 109000, 115000, 119000, 124000, 128430],
  },
  {
    label: 'Cache Hit Rate',
    value: '78.14%',
    trend: 6.1,
    sparkline: [71.2, 72.8, 74.1, 75.5, 76.3, 77.4, 78.14],
  },
];

export const providerRoutingTable: ProviderRoutingRow[] = [
  { model: 'gpt-4.1', openai: 85, groq: null, claude: 10, gemini: null, fallback: 'Groq' },
  { model: 'gpt-4o-mini', openai: 72, groq: 20, claude: null, gemini: null, fallback: 'Groq' },
  { model: 'claude-3-7-sonnet', openai: null, groq: null, claude: 88, gemini: null, fallback: 'OpenAI' },
  { model: 'gemini-2.5-pro', openai: null, groq: null, claude: null, gemini: 45, fallback: 'OpenAI' },
  { model: 'llama-3.3-70b', openai: null, groq: 94, claude: null, gemini: null, fallback: 'OpenAI' },
];

// 50 data points for live traffic (requests per second, last ~50 seconds)
export const liveTraffic: TrafficPoint[] = Array.from({ length: 50 }, (_, i) => ({
  time: `${i}s`,
  requests: Math.floor(300 + Math.sin(i * 0.3) * 150 + Math.random() * 80 + i * 8),
}));

export const providerMix: ProviderMixItem[] = [
  { name: 'OpenAI', value: 40.2, color: '#19D3FF' },
  { name: 'Groq', value: 31.7, color: '#6AE3FF' },
  { name: 'Claude', value: 18.6, color: '#FFC857' },
  { name: 'Gemini', value: 9.5, color: '#A78BFA' },
];

export const costOverTime: CostPoint[] = [
  { date: 'May 12', cost: 16240 },
  { date: 'May 13', cost: 17890 },
  { date: 'May 14', cost: 15320 },
  { date: 'May 15', cost: 19840 },
  { date: 'May 16', cost: 21200 },
  { date: 'May 17', cost: 18760 },
  { date: 'May 18', cost: 19180 },
];

export const costByTenant: TenantCost[] = [
  { tenant: 'Acme Corp', cost: 42304.11, maxCost: 60000 },
  { tenant: 'TechStart Inc', cost: 31892.44, maxCost: 60000 },
  { tenant: 'DataFlow Systems', cost: 22104.33, maxCost: 60000 },
  { tenant: 'Nexus AI', cost: 18341.21, maxCost: 60000 },
  { tenant: 'Vertex Labs', cost: 13788.12, maxCost: 60000 },
];

export const cachePerformance: CachePerformance = {
  hitRate: 78.14,
  semanticHitRate: 62.33,
  semanticTrend: 5.2,
  exactHitRate: 15.81,
  exactTrend: 0.9,
  totalRequests: '24.38M',
};

export const circuitEvents: CircuitEvent[] = [
  { day: 'Mon', opened: 3, halfOpen: 2, closed: 6 },
  { day: 'Tue', opened: 2, halfOpen: 3, closed: 7 },
  { day: 'Wed', opened: 5, halfOpen: 4, closed: 8 },
  { day: 'Thu', opened: 4, halfOpen: 2, closed: 6 },
  { day: 'Fri', opened: 3, halfOpen: 2, closed: 7 },
  { day: 'Sat', opened: 2, halfOpen: 1, closed: 5 },
  { day: 'Sun', opened: 4, halfOpen: 3, closed: 4 },
];

export const topModels: TopModel[] = [
  { model: 'gpt-4.1', requests: 8.4, maxRequests: 10 },
  { model: 'gpt-4o-mini', requests: 6.2, maxRequests: 10 },
  { model: 'claude-3-7-sonnet', requests: 4.5, maxRequests: 10 },
  { model: 'llama-3.3-70b', requests: 3.8, maxRequests: 10 },
  { model: 'gemini-2.5-pro', requests: 1.4, maxRequests: 10 },
];

export const mockGatewayEvents: GatewayEvent[] = [
  {
    id: 'evt-001',
    type: 'success',
    title: 'Request Completed',
    description: 'gpt-4.1 responded in 412ms via OpenAI',
    reqId: 'req_01JX6Z3Q',
    timestamp: new Date(Date.now() - 2000),
  },
  {
    id: 'evt-002',
    type: 'warning',
    title: 'High Latency Detected',
    description: 'Claude p95 latency exceeded 800ms threshold',
    reqId: 'req_01JX6Z2A',
    timestamp: new Date(Date.now() - 8000),
  },
  {
    id: 'evt-003',
    type: 'error',
    title: 'Rate Limit Hit',
    description: 'Gemini API returned 429 Too Many Requests',
    reqId: 'req_01JX6Z1B',
    timestamp: new Date(Date.now() - 15000),
  },
  {
    id: 'evt-004',
    type: 'circuit',
    title: 'Circuit Breaker Opened',
    description: 'Gemini circuit opened after 5 consecutive failures',
    reqId: 'req_01JX6Z0C',
    timestamp: new Date(Date.now() - 32000),
  },
  {
    id: 'evt-005',
    type: 'success',
    title: 'Cache Hit',
    description: 'Semantic cache hit with similarity 0.94 — saved $0.0042',
    reqId: 'req_01JX6YZD',
    timestamp: new Date(Date.now() - 45000),
  },
  {
    id: 'evt-006',
    type: 'success',
    title: 'Fallback Succeeded',
    description: 'Routed to Groq after OpenAI timeout — 198ms',
    reqId: 'req_01JX6YYE',
    timestamp: new Date(Date.now() - 68000),
  },
  {
    id: 'evt-007',
    type: 'warning',
    title: 'Budget Alert',
    description: 'Acme Corp reached 85% of monthly budget cap',
    reqId: 'req_01JX6YXF',
    timestamp: new Date(Date.now() - 92000),
  },
  {
    id: 'evt-008',
    type: 'circuit',
    title: 'Circuit Half-Open',
    description: 'Claude circuit entering half-open state — testing 2 calls',
    reqId: 'req_01JX6YWG',
    timestamp: new Date(Date.now() - 120000),
  },
];
