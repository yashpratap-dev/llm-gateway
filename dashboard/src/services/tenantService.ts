import { apiClient } from '../api/client';
import { ENDPOINTS } from '../api/constants';
import type {
  ApiResponse,
  Tenant,
  ApiKey,
  GeneratedApiKey,
  RoutingPolicy,
  BudgetStatus,
} from '../types/api';

export const tenantService = {
  getTenants: () =>
    apiClient.get<ApiResponse<Tenant[]>>(ENDPOINTS.TENANTS)
      .then(r => r.data.data as Tenant[]),

  createTenant: (name: string, plan: string) =>
    apiClient.post<ApiResponse<Tenant>>(ENDPOINTS.TENANTS, { name, plan })
      .then(r => r.data.data as Tenant),

  getApiKeys: (tenantId: string) =>
    apiClient.get<ApiResponse<ApiKey[]>>(ENDPOINTS.TENANT_KEYS(tenantId))
      .then(r => r.data.data as ApiKey[]),

  createApiKey: (tenantId: string, keyName: string) =>
    apiClient.post<ApiResponse<GeneratedApiKey>>(ENDPOINTS.TENANT_KEYS(tenantId), { keyName })
      .then(r => r.data.data as GeneratedApiKey),

  revokeApiKey: (keyId: string) =>
    apiClient.delete<ApiResponse<ApiKey>>(ENDPOINTS.REVOKE_KEY(keyId))
      .then(r => r.data.data as ApiKey),

  getRoutingPolicy: (tenantId: string) =>
    apiClient.get<ApiResponse<RoutingPolicy>>(ENDPOINTS.ROUTING_POLICY(tenantId))
      .then(r => r.data.data as RoutingPolicy),

  updateRoutingPolicy: (tenantId: string, strategy: string) =>
    apiClient.put<ApiResponse<RoutingPolicy>>(ENDPOINTS.ROUTING_POLICY(tenantId), { strategy })
      .then(r => r.data.data as RoutingPolicy),

  getBudget: (tenantId: string) =>
    apiClient.get<ApiResponse<BudgetStatus>>(ENDPOINTS.BUDGET(tenantId))
      .then(r => r.data.data as BudgetStatus),
};
