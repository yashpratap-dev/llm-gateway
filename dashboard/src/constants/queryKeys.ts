export const QUERY_KEYS = {
  OVERVIEW:        ['overview']        as const,
  TENANTS:         ['tenants']         as const,
  PROVIDER_HEALTH: ['providerHealth']  as const,
  ACTUATOR_HEALTH: ['actuatorHealth']  as const,
  PROVIDER_COSTS:  ['providerCosts']   as const,
  API_KEYS:   (tenantId: string) => ['apiKeys',   tenantId] as const,
  POLICY:     (tenantId: string) => ['policy',    tenantId] as const,
  BUDGET:     (tenantId: string) => ['budget',    tenantId] as const,
} as const;
