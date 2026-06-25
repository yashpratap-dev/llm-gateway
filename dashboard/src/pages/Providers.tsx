import { useMemo } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { providerService } from '../services/providerService';
import { ProviderBadge } from '../components/ProviderBadge';
import { ErrorCard } from '../components/ErrorCard';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { QUERY_KEYS } from '../constants/queryKeys';
import { getErrorMessage } from '../api/client';

const REFETCH_INTERVAL = Number(import.meta.env.VITE_REFRESH_INTERVAL_PROVIDERS) || 10_000;

interface ProviderRow {
  readonly name: string;
  readonly status: string;
  readonly cbState: string;
  readonly failureRate: string;
  readonly bufferedCalls: number;
}

export function Providers() {
  const queryClient = useQueryClient();

  const healthQuery = useQuery({
    queryKey: QUERY_KEYS.PROVIDER_HEALTH,
    queryFn: providerService.getProviderHealth,
    refetchInterval: REFETCH_INTERVAL,
  });

  const actuatorQuery = useQuery({
    queryKey: QUERY_KEYS.ACTUATOR_HEALTH,
    queryFn: providerService.getActuatorHealth,
    refetchInterval: REFETCH_INTERVAL,
  });

  const rows = useMemo<ProviderRow[]>(() => {
    const health = healthQuery.data;
    const actuator = actuatorQuery.data;
    if (!health) return [];

    const cbDetails = actuator?.components?.circuitBreakers?.details ?? {};

    return Object.entries(health.providers).map(([name, status]) => {
      const cbKey = `${name.toLowerCase()}-provider`;
      const cb = cbDetails[cbKey];
      return {
        name,
        status,
        cbState:      cb?.details?.state        ?? 'UNKNOWN',
        failureRate:  cb?.details?.failureRate   ?? '—',
        bufferedCalls: cb?.details?.bufferedCalls ?? 0,
      };
    });
  }, [healthQuery.data, actuatorQuery.data]);

  const isLoading = healthQuery.isLoading;
  const errMsg = healthQuery.error
    ? getErrorMessage(healthQuery.error, 'Failed to load provider health')
    : null;

  const lastRefresh = healthQuery.dataUpdatedAt
    ? new Date(healthQuery.dataUpdatedAt).toLocaleTimeString()
    : '—';

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
        <h1 style={{ fontSize: '1.25rem', fontWeight: 700 }}>Provider Health</h1>
        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          <span style={{ fontSize: '0.75rem', color: '#888' }}>
            Auto-refreshes every {REFETCH_INTERVAL / 1000}s · Updated {lastRefresh}
          </span>
          <button
            onClick={() => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.PROVIDER_HEALTH })}
            style={{
              background: '#1a73e8', color: '#fff', border: 'none',
              borderRadius: 4, padding: '0.35rem 0.75rem', cursor: 'pointer', fontSize: '0.8rem',
            }}
          >
            Refresh
          </button>
        </div>
      </div>

      {errMsg && (
        <ErrorCard
          message={errMsg}
          onRetry={() => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.PROVIDER_HEALTH })}
        />
      )}

      <div style={{ background: '#fff', border: '1px solid #e0e0e0', borderRadius: 8, overflow: 'hidden' }}>
        {isLoading ? (
          <div style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <LoadingSkeleton height={44} />
            <LoadingSkeleton height={44} />
          </div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Provider</th>
                <th>Status</th>
                <th>Circuit Breaker</th>
                <th>Failure Rate</th>
                <th>Buffered Calls</th>
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', color: '#999', padding: '2rem' }}>
                    No providers configured
                  </td>
                </tr>
              ) : rows.map(row => (
                <tr key={row.name}>
                  <td style={{ fontWeight: 600 }}>{row.name}</td>
                  <td><ProviderBadge status={row.status} /></td>
                  <td><ProviderBadge status={row.cbState} /></td>
                  <td>{row.failureRate}</td>
                  <td>{row.bufferedCalls}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
