export const API_BASE = '/api/v1';

export const ENDPOINTS = {
  OVERVIEW:        `${API_BASE}/admin/analytics/overview`,
  TENANTS:         `${API_BASE}/admin/tenants`,
  TENANT_KEYS:     (id: string) => `${API_BASE}/admin/tenants/${id}/keys`,
  REVOKE_KEY:      (keyId: string) => `${API_BASE}/admin/keys/${keyId}`,
  ROUTING_POLICY:  (id: string) => `${API_BASE}/admin/tenants/${id}/routing-policy`,
  PROVIDER_COSTS:  `${API_BASE}/admin/analytics/by-provider`,
  BUDGET:          (id: string) => `${API_BASE}/admin/analytics/budget/${id}`,
  PROVIDER_HEALTH: `${API_BASE}/health/providers`,
  ACTUATOR_HEALTH: '/actuator/health',
} as const;

export const ROUTING_STRATEGIES = ['PRIORITY', 'COST', 'LATENCY'] as const;
export const PLANS               = ['FREE', 'PRO', 'ENTERPRISE']   as const;

export const RAW_KEY_HIDE_AFTER_MS =
  Number(import.meta.env.VITE_RAW_KEY_HIDE_AFTER_MS) || 60_000;
