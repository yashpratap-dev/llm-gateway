import { useMemo } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { analyticsService } from '../services/analyticsService';
import { ErrorCard } from '../components/ErrorCard';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { QUERY_KEYS } from '../constants/queryKeys';
import { getErrorMessage } from '../api/client';

export function Analytics() {
  const queryClient = useQueryClient();
  const { data = [], isLoading, error } = useQuery({
    queryKey: QUERY_KEYS.PROVIDER_COSTS,
    queryFn: analyticsService.getProviderCosts,
  });

  const tableData = useMemo(() =>
    data.map(row => ({
      ...row,
      avgCost: row.requestCount > 0
        ? Number(row.totalCost) / row.requestCount
        : 0,
    })),
  [data]);

  const errMsg = error ? getErrorMessage(error, 'Failed to load analytics') : null;

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
        <h1 style={{ fontSize: '1.25rem', fontWeight: 700 }}>Analytics</h1>
        <button
          onClick={() => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.PROVIDER_COSTS })}
          style={{
            background: '#1a73e8', color: '#fff', border: 'none',
            borderRadius: 4, padding: '0.35rem 0.75rem', cursor: 'pointer', fontSize: '0.8rem',
          }}
        >
          Refresh
        </button>
      </div>

      <div style={{ background: '#fff3e0', border: '1px solid #ffb74d', borderRadius: 6, padding: '0.75rem 1rem', marginBottom: '1.5rem', fontSize: '0.85rem', color: '#e65100' }}>
        ℹ Streaming requests are logged with 0 tokens/cost — token-level tracking is planned for a future module.
      </div>

      {errMsg && (
        <ErrorCard
          message={errMsg}
          onRetry={() => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.PROVIDER_COSTS })}
        />
      )}

      <div style={{ background: '#fff', border: '1px solid #e0e0e0', borderRadius: 8, padding: '1.5rem', marginBottom: '1.5rem' }}>
        <h2 style={{ fontSize: '0.875rem', fontWeight: 600, marginBottom: '1rem' }}>Cost by Provider (USD)</h2>
        {isLoading ? (
          <LoadingSkeleton height={200} />
        ) : !data.length ? (
          <div style={{ height: 200, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#999' }}>
            No data yet
          </div>
        ) : (
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={data}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="provider" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} tickFormatter={(v: number) => `$${v.toFixed(5)}`} />
              <Tooltip formatter={(v) => [`$${v !== undefined ? Number(v).toFixed(6) : '0.000000'}`, 'Cost']} />
              <Bar dataKey="totalCost" fill="#0f9d58" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>

      <div style={{ background: '#fff', border: '1px solid #e0e0e0', borderRadius: 8, overflow: 'hidden' }}>
        <table>
          <thead>
            <tr>
              <th>Provider</th>
              <th>Requests</th>
              <th>Total Cost</th>
              <th>Avg Cost / Request</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={4} style={{ padding: '1rem' }}>
                  <LoadingSkeleton height={32} />
                </td>
              </tr>
            ) : !tableData.length ? (
              <tr>
                <td colSpan={4} style={{ textAlign: 'center', color: '#999', padding: '2rem' }}>
                  No data yet
                </td>
              </tr>
            ) : tableData.map(row => (
              <tr key={row.provider}>
                <td style={{ fontWeight: 600 }}>{row.provider}</td>
                <td>{row.requestCount.toLocaleString()}</td>
                <td>${Number(row.totalCost).toFixed(6)}</td>
                <td>${row.avgCost.toFixed(8)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
