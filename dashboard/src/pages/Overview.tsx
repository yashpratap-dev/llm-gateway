import { useMemo } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { analyticsService } from '../services/analyticsService';
import { StatCard } from '../components/StatCard';
import { ErrorCard } from '../components/ErrorCard';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { QUERY_KEYS } from '../constants/queryKeys';
import { getErrorMessage } from '../api/client';

const REFETCH_INTERVAL = Number(import.meta.env.VITE_REFRESH_INTERVAL_OVERVIEW) || 30_000;

export function Overview() {
  const queryClient = useQueryClient();
  const { data, isLoading, error, refetch, dataUpdatedAt } = useQuery({
    queryKey: QUERY_KEYS.OVERVIEW,
    queryFn: analyticsService.getOverview,
    refetchInterval: REFETCH_INTERVAL,
  });

  const chartData = useMemo(() => data?.providers ?? [], [data?.providers]);
  const lastRefresh = useMemo(
    () => dataUpdatedAt ? new Date(dataUpdatedAt).toLocaleTimeString() : '—',
    [dataUpdatedAt]
  );

  const errMsg = error ? getErrorMessage(error, 'Failed to load overview') : null;

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
        <h1 style={{ fontSize: '1.25rem', fontWeight: 700 }}>Overview</h1>
        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          <span style={{ fontSize: '0.75rem', color: '#888' }}>Updated {lastRefresh}</span>
          <button onClick={() => refetch()} style={{
            background: '#1a73e8', color: '#fff', border: 'none',
            borderRadius: 4, padding: '0.35rem 0.75rem', cursor: 'pointer', fontSize: '0.8rem',
          }}>Refresh</button>
        </div>
      </div>

      {errMsg && (
        <ErrorCard
          message={errMsg}
          onRetry={() => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.OVERVIEW })}
        />
      )}

      <div style={{ display: 'flex', gap: '1rem', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
        <StatCard label="Total Requests" value={data?.totalRequests ?? '—'} loading={isLoading} color="#1a73e8" />
        <StatCard label="Total Cost" value={data ? `$${data.totalCostUsd.toFixed(6)}` : '—'} loading={isLoading} color="#0f9d58" />
        <StatCard label="Cache Hit Ratio" value={data ? `${(data.cacheHitRatio * 100).toFixed(1)}%` : '—'} loading={isLoading} color="#f4b400" sub="served from cache" />
        <StatCard label="Active Tenants" value={data?.activeTenantsCount ?? '—'} loading={isLoading} color="#db4437" />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1rem' }}>
        {(['requestCount', 'totalCost'] as const).map(metric => (
          <div key={metric} style={{ background: '#fff', border: '1px solid #e0e0e0', borderRadius: 8, padding: '1.5rem' }}>
            <h2 style={{ fontSize: '0.875rem', fontWeight: 600, marginBottom: '1rem', color: '#333' }}>
              {metric === 'requestCount' ? 'Requests by Provider' : 'Cost by Provider (USD)'}
            </h2>
            {isLoading ? (
              <LoadingSkeleton height={200} />
            ) : !chartData.length ? (
              <div style={{ height: 200, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#999', fontSize: '0.875rem' }}>
                No provider data yet
              </div>
            ) : (
              <ResponsiveContainer width="100%" height={200}>
                <BarChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                  <XAxis dataKey="provider" tick={{ fontSize: 12 }} />
                  <YAxis
                    tick={{ fontSize: 12 }}
                    tickFormatter={metric === 'totalCost' ? (v: number) => `$${v.toFixed(4)}` : undefined}
                  />
                  <Tooltip
                    formatter={(v) =>
                      metric === 'totalCost'
                        ? [`$${v !== undefined ? Number(v).toFixed(6) : '0.000000'}`, 'Cost']
                        : [v !== undefined ? Number(v) : 0, 'Requests']
                    }
                  />
                  <Bar
                    dataKey={metric}
                    fill={metric === 'requestCount' ? '#1a73e8' : '#0f9d58'}
                    radius={[4, 4, 0, 0]}
                  />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
