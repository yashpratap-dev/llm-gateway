import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { tenantService } from '../services/tenantService';
import { ErrorCard } from '../components/ErrorCard';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { QUERY_KEYS } from '../constants/queryKeys';
import { ROUTING_STRATEGIES, PLANS, RAW_KEY_HIDE_AFTER_MS } from '../api/constants';
import { getErrorMessage } from '../api/client';
import type { Tenant, ApiKey, GeneratedApiKey, RoutingPolicy, BudgetStatus } from '../types/api';

function useDebounce<T>(value: T, delay: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return debounced;
}

export function Tenants() {
  const queryClient = useQueryClient();
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [showCreateTenant, setShowCreateTenant] = useState(false);
  const [newTenantName, setNewTenantName] = useState('');
  const [newTenantPlan, setNewTenantPlan] = useState('FREE');
  const [newKeyName, setNewKeyName] = useState<Record<string, string>>({});
  const [newKeys, setNewKeys] = useState<Record<string, GeneratedApiKey | null>>({});
  const [copiedKey, setCopiedKey] = useState<string | null>(null);
  const [clipboardError, setClipboardError] = useState(false);
  const [pendingPolicy, setPendingPolicy] = useState<Record<string, string>>({});
  const [localError, setLocalError] = useState<string | null>(null);
  const [keys, setKeys] = useState<Record<string, ApiKey[]>>({});
  const [policies, setPolicies] = useState<Record<string, RoutingPolicy>>({});
  const [budgets, setBudgets] = useState<Record<string, BudgetStatus | null>>({});
  const hideTimers = useRef<Record<string, ReturnType<typeof setTimeout>>>({});

  const debouncedSearch = useDebounce(search, 300);

  // Cleanup all timers on unmount
  useEffect(() => {
    const timers = hideTimers.current;
    return () => { Object.values(timers).forEach(clearTimeout); };
  }, []);

  const { data: tenants = [], isLoading, error } = useQuery({
    queryKey: QUERY_KEYS.TENANTS,
    queryFn: tenantService.getTenants,
  });

  const createTenantMutation = useMutation({
    mutationFn: ({ name, plan }: { name: string; plan: string }) =>
      tenantService.createTenant(name, plan),
    onSuccess: (tenant) => {
      queryClient.setQueryData<Tenant[]>(QUERY_KEYS.TENANTS, prev => [...(prev ?? []), tenant]);
      setNewTenantName('');
      setNewTenantPlan('FREE');
      setShowCreateTenant(false);
      setLocalError(null);
    },
    onError: (err) => setLocalError(getErrorMessage(err, 'Failed to create tenant')),
  });

  const revokeKeyMutation = useMutation({
    mutationFn: ({ keyId }: { keyId: string; tenantId: string }) =>
      tenantService.revokeApiKey(keyId),
    onMutate: ({ tenantId, keyId }) => {
      const previous = keys[tenantId] ?? [];
      setKeys(prev => ({ ...prev, [tenantId]: (prev[tenantId] ?? []).filter(k => k.id !== keyId) }));
      return { previous, tenantId };
    },
    onError: (err, _vars, context) => {
      if (context) setKeys(prev => ({ ...prev, [context.tenantId]: context.previous }));
      setLocalError(getErrorMessage(err, 'Failed to revoke key'));
    },
  });

  const updatePolicyMutation = useMutation({
    mutationFn: ({ tenantId, strategy }: { tenantId: string; strategy: string }) =>
      tenantService.updateRoutingPolicy(tenantId, strategy),
    onSuccess: (policy, { tenantId }) => {
      setPolicies(prev => ({ ...prev, [tenantId]: policy }));
      setLocalError(null);
    },
    onError: (err) => setLocalError(getErrorMessage(err, 'Failed to update policy')),
  });

  const createKeyMutation = useMutation({
    mutationFn: ({ tenantId, keyName }: { tenantId: string; keyName: string }) =>
      tenantService.createApiKey(tenantId, keyName),
    onSuccess: (generated, { tenantId }) => {
      setKeys(prev => ({
        ...prev,
        [tenantId]: [...(prev[tenantId] ?? []), {
          id: generated.id,
          keyPrefix: generated.keyPrefix,
          name: generated.name,
          status: generated.status,
          createdAt: generated.createdAt,
        }],
      }));
      setNewKeys(prev => ({ ...prev, [tenantId]: generated }));
      setNewKeyName(prev => ({ ...prev, [tenantId]: '' }));
      if (hideTimers.current[tenantId]) clearTimeout(hideTimers.current[tenantId]);
      hideTimers.current[tenantId] = setTimeout(() => {
        setNewKeys(prev => ({ ...prev, [tenantId]: null }));
      }, RAW_KEY_HIDE_AFTER_MS);
      setLocalError(null);
    },
    onError: (err) => setLocalError(getErrorMessage(err, 'Failed to create key')),
  });

  const expandTenant = useCallback(async (id: string) => {
    if (expandedId === id) { setExpandedId(null); return; }
    setExpandedId(id);
    const [keysResult, policyResult, budgetResult] = await Promise.allSettled([
      tenantService.getApiKeys(id),
      tenantService.getRoutingPolicy(id),
      tenantService.getBudget(id),
    ]);
    if (keysResult.status === 'fulfilled') setKeys(prev => ({ ...prev, [id]: keysResult.value }));
    if (policyResult.status === 'fulfilled') {
      const policy = policyResult.value;
      setPolicies(prev => ({ ...prev, [id]: policy }));
      setPendingPolicy(prev => ({ ...prev, [id]: policy.strategy }));
    }
    if (budgetResult.status === 'fulfilled') setBudgets(prev => ({ ...prev, [id]: budgetResult.value }));
    else setBudgets(prev => ({ ...prev, [id]: null }));
  }, [expandedId]);

  const copyToClipboard = async (text: string, id: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedKey(id);
      setClipboardError(false);
      setTimeout(() => setCopiedKey(null), 2000);
    } catch {
      setClipboardError(true);
      setTimeout(() => setClipboardError(false), 3000);
    }
  };

  const confirmRevoke = (tenantId: string, keyId: string, keyName: string) => {
    if (window.confirm(`Revoke API key "${keyName}"? This cannot be undone.`)) {
      revokeKeyMutation.mutate({ tenantId, keyId });
    }
  };

  const filtered = useMemo(
    () => tenants.filter(t => t.name.toLowerCase().includes(debouncedSearch.toLowerCase())),
    [tenants, debouncedSearch]
  );

  const errMsg = error ? getErrorMessage(error, 'Failed to load tenants') : localError;

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
        <h1 style={{ fontSize: '1.25rem', fontWeight: 700 }}>Tenants</h1>
        <button
          onClick={() => setShowCreateTenant(v => !v)}
          style={{
            background: '#1a73e8', color: '#fff', border: 'none',
            borderRadius: 4, padding: '0.5rem 1rem', cursor: 'pointer', fontSize: '0.875rem',
          }}
        >
          + Create Tenant
        </button>
      </div>

      {errMsg && <ErrorCard message={errMsg} />}
      {clipboardError && <ErrorCard message="Could not copy to clipboard — please copy manually." />}

      {showCreateTenant && (
        <div style={{ background: '#fff', border: '1px solid #e0e0e0', borderRadius: 8, padding: '1.25rem', marginBottom: '1rem', display: 'flex', gap: '0.75rem', alignItems: 'flex-end', flexWrap: 'wrap' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.75rem', color: '#666', marginBottom: 4 }}>Name *</label>
            <input
              value={newTenantName}
              onChange={e => setNewTenantName(e.target.value)}
              placeholder="my-tenant"
              style={{ width: 200 }}
              onKeyDown={e => {
                if (e.key === 'Enter' && newTenantName.trim()) {
                  createTenantMutation.mutate({ name: newTenantName.trim(), plan: newTenantPlan });
                }
              }}
            />
          </div>
          <div>
            <label style={{ display: 'block', fontSize: '0.75rem', color: '#666', marginBottom: 4 }}>Plan</label>
            <select value={newTenantPlan} onChange={e => setNewTenantPlan(e.target.value)}>
              {PLANS.map(p => <option key={p}>{p}</option>)}
            </select>
          </div>
          <button
            onClick={() => {
              if (newTenantName.trim()) {
                createTenantMutation.mutate({ name: newTenantName.trim(), plan: newTenantPlan });
              }
            }}
            disabled={createTenantMutation.isPending || !newTenantName.trim()}
            style={{ background: '#1a73e8', color: '#fff', border: 'none', borderRadius: 4, padding: '0.5rem 1rem', cursor: 'pointer' }}
          >
            {createTenantMutation.isPending ? 'Creating...' : 'Create'}
          </button>
          <button
            onClick={() => setShowCreateTenant(false)}
            style={{ background: 'transparent', border: '1px solid #ccc', borderRadius: 4, padding: '0.5rem 1rem', cursor: 'pointer' }}
          >
            Cancel
          </button>
        </div>
      )}

      <div style={{ marginBottom: '1rem' }}>
        <input
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder="Search tenants..."
          style={{ width: 280 }}
        />
      </div>

      {isLoading ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
          <LoadingSkeleton height={52} />
          <LoadingSkeleton height={52} />
        </div>
      ) : (
        <div style={{ background: '#fff', border: '1px solid #e0e0e0', borderRadius: 8, overflow: 'hidden' }}>
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Plan</th>
                <th>Created</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={4} style={{ textAlign: 'center', color: '#999', padding: '2rem' }}>
                    {search ? 'No tenants match your search' : 'No tenants yet'}
                  </td>
                </tr>
              ) : filtered.flatMap(tenant => {
                const rows = [
                  <tr
                    key={tenant.id}
                    style={{ cursor: 'pointer' }}
                    onClick={() => void expandTenant(tenant.id)}
                  >
                    <td style={{ fontWeight: 500 }}>{tenant.name}</td>
                    <td>
                      <span style={{ background: '#f0f4ff', color: '#1a73e8', padding: '2px 8px', borderRadius: 4, fontSize: '0.75rem', fontWeight: 600 }}>
                        {tenant.plan}
                      </span>
                    </td>
                    <td style={{ color: '#888', fontSize: '0.85rem' }}>{new Date(tenant.createdAt).toLocaleDateString()}</td>
                    <td style={{ color: '#1a73e8', fontSize: '0.85rem' }}>{expandedId === tenant.id ? '▲' : '▼'}</td>
                  </tr>,
                ];

                if (expandedId === tenant.id) {
                  rows.push(
                    <tr key={`${tenant.id}-exp`}>
                      <td colSpan={4} style={{ background: '#f8f9fa', padding: '1.5rem' }}>

                        {/* Routing Policy */}
                        <div style={{ marginBottom: '1.5rem' }}>
                          <h3 style={{ fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.5rem' }}>Routing Policy</h3>
                          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                            <select
                              value={pendingPolicy[tenant.id] ?? policies[tenant.id]?.strategy ?? 'PRIORITY'}
                              onChange={e => setPendingPolicy(prev => ({ ...prev, [tenant.id]: e.target.value }))}
                            >
                              {ROUTING_STRATEGIES.map(s => <option key={s}>{s}</option>)}
                            </select>
                            <button
                              onClick={() => updatePolicyMutation.mutate({
                                tenantId: tenant.id,
                                strategy: pendingPolicy[tenant.id] ?? 'PRIORITY',
                              })}
                              disabled={updatePolicyMutation.isPending}
                              style={{ background: '#1a73e8', color: '#fff', border: 'none', borderRadius: 4, padding: '0.4rem 0.75rem', cursor: 'pointer', fontSize: '0.8rem' }}
                            >
                              {updatePolicyMutation.isPending ? 'Saving...' : 'Save'}
                            </button>
                          </div>
                        </div>

                        {/* Budget */}
                        {budgets[tenant.id] != null && (
                          <div style={{ marginBottom: '1.5rem', fontSize: '0.85rem' }}>
                            <h3 style={{ fontWeight: 600, marginBottom: '0.5rem' }}>Budget</h3>
                            <div style={{ display: 'flex', gap: '1.5rem' }}>
                              <span>Limit: <strong>${Number(budgets[tenant.id]!.limit).toFixed(2)}</strong></span>
                              <span>Spent: <strong>${Number(budgets[tenant.id]!.spent).toFixed(6)}</strong></span>
                              <span>Remaining: <strong>${Number(budgets[tenant.id]!.remaining).toFixed(2)}</strong></span>
                            </div>
                          </div>
                        )}

                        {/* API Keys */}
                        <div>
                          <h3 style={{ fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.75rem' }}>API Keys</h3>

                          {newKeys[tenant.id] != null && (
                            <div style={{ background: '#fff3e0', border: '1px solid #ffb74d', borderRadius: 6, padding: '0.75rem 1rem', marginBottom: '1rem' }}>
                              <div style={{ fontSize: '0.75rem', color: '#e65100', fontWeight: 600, marginBottom: 6 }}>
                                ⚠ Copy this key now — it will auto-hide in {RAW_KEY_HIDE_AFTER_MS / 1000}s and cannot be retrieved again
                              </div>
                              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                                <code style={{ background: '#fff', padding: '0.25rem 0.5rem', borderRadius: 4, fontSize: '0.8rem', flex: 1, overflowX: 'auto', wordBreak: 'break-all' }}>
                                  {newKeys[tenant.id]!.rawKey}
                                </code>
                                <button
                                  onClick={() => void copyToClipboard(newKeys[tenant.id]!.rawKey, tenant.id)}
                                  style={{
                                    background: copiedKey === tenant.id ? '#0f9d58' : '#333',
                                    color: '#fff', border: 'none', borderRadius: 4,
                                    padding: '0.35rem 0.75rem', cursor: 'pointer', fontSize: '0.75rem', whiteSpace: 'nowrap',
                                  }}
                                >
                                  {copiedKey === tenant.id ? '✓ Copied' : 'Copy'}
                                </button>
                                <button
                                  onClick={() => setNewKeys(prev => ({ ...prev, [tenant.id]: null }))}
                                  style={{ background: 'transparent', border: '1px solid #ccc', borderRadius: 4, padding: '0.35rem 0.5rem', cursor: 'pointer', fontSize: '0.75rem' }}
                                >
                                  ✕
                                </button>
                              </div>
                            </div>
                          )}

                          {(keys[tenant.id]?.length ?? 0) > 0 ? (
                            <table style={{ marginBottom: '0.75rem' }}>
                              <thead>
                                <tr>
                                  <th>Name</th>
                                  <th>Prefix</th>
                                  <th>Status</th>
                                  <th>Created</th>
                                  <th>Action</th>
                                </tr>
                              </thead>
                              <tbody>
                                {(keys[tenant.id] ?? []).map(k => (
                                  <tr key={k.id}>
                                    <td>{k.name}</td>
                                    <td><code style={{ fontSize: '0.8rem' }}>{k.keyPrefix}...</code></td>
                                    <td>
                                      <span style={{ color: k.status === 'ACTIVE' ? '#1e7e34' : '#c5221f', fontWeight: 600, fontSize: '0.8rem' }}>
                                        {k.status}
                                      </span>
                                    </td>
                                    <td style={{ fontSize: '0.8rem', color: '#888' }}>{new Date(k.createdAt).toLocaleDateString()}</td>
                                    <td>
                                      <button
                                        onClick={() => confirmRevoke(tenant.id, k.id, k.name)}
                                        disabled={revokeKeyMutation.isPending}
                                        style={{ background: 'transparent', color: '#c5221f', border: '1px solid #c5221f', borderRadius: 4, padding: '0.25rem 0.5rem', cursor: 'pointer', fontSize: '0.75rem' }}
                                      >
                                        Revoke
                                      </button>
                                    </td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          ) : (
                            <p style={{ fontSize: '0.85rem', color: '#999', marginBottom: '0.75rem' }}>No API keys yet</p>
                          )}

                          <div style={{ display: 'flex', gap: '0.5rem' }}>
                            <input
                              value={newKeyName[tenant.id] ?? ''}
                              onChange={e => setNewKeyName(prev => ({ ...prev, [tenant.id]: e.target.value }))}
                              placeholder="Key name (required)"
                              style={{ width: 200 }}
                            />
                            <button
                              onClick={() => {
                                const name = newKeyName[tenant.id]?.trim();
                                if (name) createKeyMutation.mutate({ tenantId: tenant.id, keyName: name });
                              }}
                              disabled={createKeyMutation.isPending || !(newKeyName[tenant.id]?.trim())}
                              style={{ background: '#1a73e8', color: '#fff', border: 'none', borderRadius: 4, padding: '0.4rem 0.75rem', cursor: 'pointer', fontSize: '0.8rem' }}
                            >
                              {createKeyMutation.isPending ? 'Creating...' : '+ Create Key'}
                            </button>
                          </div>
                        </div>
                      </td>
                    </tr>
                  );
                }

                return rows;
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
