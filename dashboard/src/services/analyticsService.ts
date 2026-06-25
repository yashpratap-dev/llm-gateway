import { apiClient } from '../api/client';
import { ENDPOINTS } from '../api/constants';
import type {
  ApiResponse,
  OverviewData,
  ProviderCostSummary,
  BudgetStatus,
  ProviderHealthData,
  ActuatorHealth,
} from '../types/api';

export const analyticsService = {
  getOverview: () =>
    apiClient.get<ApiResponse<OverviewData>>(ENDPOINTS.OVERVIEW)
      .then(r => r.data.data as OverviewData),

  getProviderCosts: () =>
    apiClient.get<ApiResponse<ProviderCostSummary[]>>(ENDPOINTS.PROVIDER_COSTS)
      .then(r => r.data.data as ProviderCostSummary[]),

  getBudget: (tenantId: string) =>
    apiClient.get<ApiResponse<BudgetStatus>>(ENDPOINTS.BUDGET(tenantId))
      .then(r => r.data.data as BudgetStatus),

  getProviderHealth: () =>
    apiClient.get<ApiResponse<ProviderHealthData>>(ENDPOINTS.PROVIDER_HEALTH)
      .then(r => r.data.data as ProviderHealthData),

  getActuatorHealth: () =>
    apiClient.get<ActuatorHealth>(ENDPOINTS.ACTUATOR_HEALTH)
      .then(r => r.data),
};
