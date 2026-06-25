// ApiResponse wrapper — matches ApiResponse.java exactly
export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message: string | null;
  errorCode: string | null;
}

// Tenant — matches TenantResponse.java exactly (UUID → string, LocalDateTime → string)
export interface Tenant {
  id: string;
  name: string;
  plan: string;
  createdAt: string;
}

// ApiKey — matches ApiKeyResponse.java exactly
export interface ApiKey {
  id: string;
  keyPrefix: string;
  name: string;
  status: string;
  createdAt: string;
}

// GeneratedApiKey — matches GeneratedApiKeyResponse.java exactly (includes rawKey)
export interface GeneratedApiKey {
  id: string;
  keyPrefix: string;
  name: string;
  rawKey: string;
  status: string;
  createdAt: string;
}

// RoutingPolicy — matches RoutingPolicyResponse.java (updatedAt nullable)
export interface RoutingPolicy {
  tenantId: string;
  strategy: string;
  updatedAt: string | null;
}

// ProviderCostSummary — matches ProviderCostSummary.java (BigDecimal → number)
export interface ProviderCostSummary {
  provider: string;
  totalCost: number;
  requestCount: number;
}

// OverviewData — matches OverviewResponse.java exactly
export interface OverviewData {
  totalRequests: number;
  totalCostUsd: number;
  cacheHitRatio: number;
  activeTenantsCount: number;
  providers: ProviderCostSummary[];
}

// BudgetStatus — matches BudgetSummary.java (BigDecimal → number)
export interface BudgetStatus {
  limit: number;
  spent: number;
  remaining: number;
}

// ProviderHealthData — matches ProviderHealthResponse.java
export interface ProviderHealthData {
  providers: Record<string, string>;
  timestamp: string;
}

// ActuatorHealth — from /actuator/health (circuitBreakers may be absent)
export interface CircuitBreakerDetails {
  failureRate: string;
  slowCallRate: string;
  bufferedCalls: number;
  failedCalls: number;
  notPermittedCalls: number;
  state: string;
}

export interface ActuatorHealth {
  status: string;
  components: {
    circuitBreakers?: {
      status: string;
      details: Record<string, {
        status: string;
        details?: CircuitBreakerDetails;
      }>;
    };
    [key: string]: unknown;
  };
}
