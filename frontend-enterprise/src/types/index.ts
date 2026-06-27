// Provider types
export type ProviderStatus = 'healthy' | 'degraded' | 'unhealthy';
export type CircuitState = 'closed' | 'half-open' | 'open';

export interface Provider {
  id: string;
  name: string;
  status: ProviderStatus;
  latencyP95: number;
  models: number;
  rpm: number;
  rpmMax: number;
  tpm: number;
  tpmMax: number;
  circuitState: CircuitState;
  currentRequests: number;
  costPerHour: number;
  health: number;
  sparkline: number[];
}

// Dashboard types
export interface KPICard {
  label: string;
  value: string;
  trend: number;
  sparkline: number[];
  unit?: string;
}

export interface ProviderRoutingRow {
  model: string;
  openai: number | null;
  groq: number | null;
  claude: number | null;
  gemini: number | null;
  fallback: string;
}

export interface TrafficPoint {
  time: string;
  requests: number;
}

export interface ProviderMixItem {
  name: string;
  value: number;
  color: string;
}

export interface CostPoint {
  date: string;
  cost: number;
}

export interface TenantCost {
  tenant: string;
  cost: number;
  maxCost: number;
}

export interface CachePerformance {
  hitRate: number;
  semanticHitRate: number;
  semanticTrend: number;
  exactHitRate: number;
  exactTrend: number;
  totalRequests: string;
}

export interface CircuitEvent {
  day: string;
  opened: number;
  halfOpen: number;
  closed: number;
}

export interface TopModel {
  model: string;
  requests: number;
  maxRequests: number;
}

// Gateway event types
export type GatewayEventType = 'success' | 'warning' | 'error' | 'circuit';

export interface GatewayEvent {
  id: string;
  type: GatewayEventType;
  title: string;
  description: string;
  reqId: string;
  timestamp: Date;
}

// Playground types
export type MessageRole = 'user' | 'assistant';

export interface ChatMessage {
  id: string;
  role: MessageRole;
  content: string;
  timestamp: Date;
}

export interface PlaygroundConfig {
  provider: string;
  model: string;
  temperature: number;
  maxTokens: number;
  streaming: boolean;
}

export interface TelemetryData {
  provider: string;
  model: string;
  latency: number;
  cost: number;
  status: number;
  semanticCacheHit: boolean;
  exactCacheHit: boolean;
  similarityScore: number;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  embeddingModel: string;
  redisConnected: boolean;
  postgresConnected: boolean;
  circuitState: CircuitState;
  tenant: string;
  requestId: string;
  apiKey: string;
}

// Analytics types
export interface VolumePoint {
  date: string;
  requests: number;
}

export interface LatencyPoint {
  date: string;
  p50: number;
  p95: number;
  p99: number;
}

export interface CostBreakdownPoint {
  date: string;
  openai: number;
  groq: number;
  claude: number;
  gemini: number;
}

export interface ErrorRatePoint {
  date: string;
  errorRate: number;
  threshold: number;
}

export interface ProviderComparisonItem {
  provider: string;
  latency: number;
  cost: number;
  requests: number;
}

export interface CacheHitPoint {
  date: string;
  semantic: number;
  exact: number;
}

// Cache types
export interface CacheEntry {
  id: string;
  query: string;
  model: string;
  similarity: number;
  hits: number;
  createdAt: string;
  expiresAt: string;
  size: number;
}

// Health types
export interface HealthCheck {
  service: string;
  status: 'up' | 'down' | 'degraded';
  latency: number;
  uptime: number;
  lastChecked: string;
  details: string;
}

// API Key types
export interface ApiKey {
  id: string;
  name: string;
  key: string;
  tenant: string;
  createdAt: string;
  lastUsed: string;
  requests: number;
  status: 'active' | 'revoked';
  scopes: string[];
}
