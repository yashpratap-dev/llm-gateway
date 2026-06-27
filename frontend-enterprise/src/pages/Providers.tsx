import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, CheckCircle, AlertCircle, XCircle } from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Tabs } from '../components/ui/Tabs';
import { Gauge } from '../components/ui/Gauge';
import { Progress } from '../components/ui/Progress';
import { SparklineChart } from '../components/charts/SparklineChart';
import { providersData, providerDetails } from '../mock/providers';
import type { Provider } from '../types';

const statusColor = (s: Provider['status']) => {
  if (s === 'healthy') return '#18D46B';
  if (s === 'degraded') return '#FFC857';
  return '#FF5A5A';
};

const circuitBadge = (state: Provider['circuitState']) => {
  if (state === 'closed') return <Badge variant="success" size="sm">CLOSED</Badge>;
  if (state === 'half-open') return <Badge variant="warning" size="sm">HALF-OPEN</Badge>;
  return <Badge variant="danger" size="sm">OPEN</Badge>;
};

const statusBadge = (s: Provider['status']) => {
  if (s === 'healthy') return <Badge variant="success" size="sm" dot>Healthy</Badge>;
  if (s === 'degraded') return <Badge variant="warning" size="sm" dot>Degraded</Badge>;
  return <Badge variant="danger" size="sm" dot>Unhealthy</Badge>;
};

const detailTabs = [
  { id: 'overview', label: 'Overview' },
  { id: 'health', label: 'Health' },
  { id: 'models', label: 'Models' },
  { id: 'limits', label: 'Limits' },
  { id: 'policy', label: 'Policy' },
];

export function Providers() {
  const [selectedProvider, setSelectedProvider] = useState<Provider | null>(null);
  const [detailTab, setDetailTab] = useState('overview');

  const details = selectedProvider
    ? providerDetails[selectedProvider.id as keyof typeof providerDetails]
    : null;

  return (
    <div className="flex h-screen overflow-hidden">
      <div className="flex-1 overflow-y-auto">
        <div className="px-6 py-5 border-b border-white/8">
          <h1 className="text-xl font-bold text-white">Providers</h1>
          <p className="text-xs text-text-secondary mt-0.5">
            Monitor and manage LLM provider health and routing
          </p>
        </div>

        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="p-6 grid grid-cols-2 gap-4"
        >
          {providersData.map((provider) => (
            <motion.div
              key={provider.id}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              whileHover={{ scale: 1.01 }}
            >
              <Card
                hover
                padding="md"
                onClick={() => {
                  setSelectedProvider(provider);
                  setDetailTab('overview');
                }}
                className={`cursor-pointer transition-all ${
                  selectedProvider?.id === provider.id
                    ? 'border-accent-primary/40 bg-accent-primary/5'
                    : ''
                }`}
              >
                {/* Header */}
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <ProviderLogo id={provider.id} />
                    <div>
                      <div className="flex items-center gap-2">
                        <h3 className="text-sm font-bold text-white">{provider.name}</h3>
                        {provider.id === 'openai' && (
                          <Badge variant="info" size="sm">Primary</Badge>
                        )}
                      </div>
                      <p className="text-[10px] text-text-secondary mt-0.5">
                        {provider.models} models available
                      </p>
                    </div>
                  </div>
                  <div className="flex flex-col items-end gap-1">
                    {statusBadge(provider.status)}
                    {circuitBadge(provider.circuitState)}
                  </div>
                </div>

                {/* Latency sparkline */}
                <div className="flex items-end gap-3 mb-4">
                  <div>
                    <p className="text-[10px] text-text-secondary">p95 Latency</p>
                    <p
                      className="text-2xl font-bold"
                      style={{ color: statusColor(provider.status) }}
                    >
                      {provider.latencyP95}ms
                    </p>
                  </div>
                  <SparklineChart
                    data={provider.sparkline}
                    color={statusColor(provider.status)}
                    height={40}
                    width={100}
                  />
                </div>

                {/* Gauges */}
                <div className="grid grid-cols-2 gap-3 mb-4">
                  <Gauge
                    value={provider.rpm}
                    max={provider.rpmMax}
                    label="RPM"
                    size={110}
                    color="#19D3FF"
                  />
                  <Gauge
                    value={provider.tpm}
                    max={provider.tpmMax}
                    label="TPM"
                    size={110}
                    color="#19D3FF"
                  />
                </div>

                {/* Stats row */}
                <div className="grid grid-cols-3 gap-2 text-center border-t border-white/8 pt-3">
                  <div>
                    <p className="text-[10px] text-text-secondary">Active Req</p>
                    <p className="text-sm font-semibold text-white">{provider.currentRequests}</p>
                  </div>
                  <div>
                    <p className="text-[10px] text-text-secondary">Cost/h</p>
                    <p className="text-sm font-semibold text-status-success">
                      ${provider.costPerHour.toFixed(2)}
                    </p>
                  </div>
                  <div>
                    <p className="text-[10px] text-text-secondary">Health</p>
                    <p
                      className="text-sm font-semibold"
                      style={{ color: statusColor(provider.status) }}
                    >
                      {provider.health.toFixed(1)}%
                    </p>
                  </div>
                </div>

                {/* Health bar */}
                <div className="mt-3">
                  <Progress
                    value={provider.health}
                    color={statusColor(provider.status)}
                    height={3}
                    animated
                  />
                </div>
              </Card>
            </motion.div>
          ))}
        </motion.div>
      </div>

      {/* Detail panel */}
      <AnimatePresence>
        {selectedProvider && details && (
          <motion.aside
            initial={{ width: 0, opacity: 0 }}
            animate={{ width: 360, opacity: 1 }}
            exit={{ width: 0, opacity: 0 }}
            transition={{ duration: 0.25 }}
            className="flex-shrink-0 h-screen overflow-y-auto border-l border-white/8 bg-bg-card"
          >
            {/* Header */}
            <div className="sticky top-0 z-10 bg-bg-card border-b border-white/8 px-4 py-3">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-3">
                  <ProviderLogo id={selectedProvider.id} size={28} />
                  <div>
                    <h2 className="text-sm font-bold text-white">{selectedProvider.name}</h2>
                    <p className="text-[10px] text-text-secondary">
                      {selectedProvider.models} models
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  {statusBadge(selectedProvider.status)}
                  <button
                    onClick={() => setSelectedProvider(null)}
                    className="p-1 rounded hover:bg-white/8 text-text-secondary hover:text-white transition-colors"
                  >
                    <X size={14} />
                  </button>
                </div>
              </div>
              <Tabs
                tabs={detailTabs}
                activeTab={detailTab}
                onChange={setDetailTab}
                variant="underline"
              />
            </div>

            <div className="p-4 space-y-4">
              {detailTab === 'overview' && (
                <>
                  {/* Provider Health */}
                  <Section title="Provider Health">
                    <StatRow label="Status" value={selectedProvider.status.charAt(0).toUpperCase() + selectedProvider.status.slice(1)} />
                    <StatRow label="Uptime" value={`${details.uptime.toFixed(2)}%`} />
                    <StatRow label="p95 Latency" value={`${selectedProvider.latencyP95}ms`} />
                    <StatRow label="Error Rate" value={`${details.errorRate.toFixed(2)}%`} />
                  </Section>

                  {/* Health Checks */}
                  <Section title="Health Checks">
                    {details.healthChecks.map((hc) => (
                      <div key={hc.name} className="flex items-center justify-between py-1.5 border-b border-white/4 last:border-0">
                        <div className="flex items-center gap-2">
                          {hc.status === 'pass' ? (
                            <CheckCircle size={12} className="text-status-success" />
                          ) : hc.status === 'warn' ? (
                            <AlertCircle size={12} className="text-status-warning" />
                          ) : (
                            <XCircle size={12} className="text-status-danger" />
                          )}
                          <span className="text-xs text-text-primary">{hc.name}</span>
                        </div>
                        <span className="text-[10px] text-text-secondary">{hc.ts}</span>
                      </div>
                    ))}
                  </Section>

                  {/* Recent Failures */}
                  <Section title="Recent Failures">
                    {details.recentFailures.length === 0 ? (
                      <div className="text-center py-4">
                        <CheckCircle size={24} className="text-status-success mx-auto mb-2" />
                        <p className="text-xs font-semibold text-text-primary">No recent failures</p>
                        <p className="text-[10px] text-text-secondary">Everything looks good</p>
                      </div>
                    ) : (
                      details.recentFailures.map((f, i) => (
                        <div key={i} className="py-1.5 border-b border-white/4 last:border-0">
                          <div className="flex items-center gap-2 mb-0.5">
                            <XCircle size={10} className="text-status-danger flex-shrink-0" />
                            <span className="text-[10px] text-text-secondary">{f.time}</span>
                          </div>
                          <p className="text-[11px] text-text-primary pl-4">{f.error}</p>
                        </div>
                      ))
                    )}
                  </Section>
                </>
              )}

              {detailTab === 'models' && (
                <Section title="Model Costs (per 1M tokens)">
                  <table className="w-full text-xs">
                    <thead>
                      <tr className="border-b border-white/8">
                        <th className="text-left py-2 text-text-secondary font-medium">Model</th>
                        <th className="text-right py-2 text-text-secondary font-medium">Input</th>
                        <th className="text-right py-2 text-text-secondary font-medium">Output</th>
                      </tr>
                    </thead>
                    <tbody>
                      {details.modelCosts.map(m => (
                        <tr key={m.model} className="border-b border-white/4 last:border-0">
                          <td className="py-2 font-mono text-text-primary text-[11px]">{m.model}</td>
                          <td className="py-2 text-right text-accent-primary font-mono">${m.input.toFixed(2)}</td>
                          <td className="py-2 text-right text-accent-primary font-mono">${m.output.toFixed(2)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </Section>
              )}

              {detailTab === 'limits' && (
                <>
                  <Section title="Rate Limits">
                    <StatRow label="RPM Limit" value={selectedProvider.rpmMax.toLocaleString()} />
                    <StatRow label="TPM Limit" value={`${(selectedProvider.tpmMax / 1_000_000).toFixed(0)}M`} />
                    <div className="py-2 space-y-2">
                      <div>
                        <div className="flex justify-between text-[11px] mb-1">
                          <span className="text-text-secondary">RPM Remaining</span>
                          <span className="text-accent-primary">{(selectedProvider.rpmMax - selectedProvider.rpm).toLocaleString()}</span>
                        </div>
                        <Progress
                          value={selectedProvider.rpm}
                          max={selectedProvider.rpmMax}
                          color="#19D3FF"
                          height={4}
                          animated
                        />
                      </div>
                      <div>
                        <div className="flex justify-between text-[11px] mb-1">
                          <span className="text-text-secondary">TPM Remaining</span>
                          <span className="text-accent-primary">
                            {((selectedProvider.tpmMax - selectedProvider.tpm) / 1_000_000).toFixed(1)}M
                          </span>
                        </div>
                        <Progress
                          value={selectedProvider.tpm}
                          max={selectedProvider.tpmMax}
                          color="#19D3FF"
                          height={4}
                          animated
                        />
                      </div>
                    </div>
                  </Section>

                  <Section title="Circuit Breaker">
                    <StatRow label="State" value={selectedProvider.circuitState.toUpperCase()} />
                    <StatRow label="Failure Threshold" value={`${details.failureThreshold} failures`} />
                    <StatRow label="Recovery Timeout" value={`${details.recoveryTimeout}s`} />
                    <StatRow label="Half-Open Max Calls" value={String(details.halfOpenMaxCalls)} />
                  </Section>
                </>
              )}

              {detailTab === 'policy' && (
                <Section title="Fallback Order">
                  {details.fallbackOrder.map((item, i) => (
                    <div key={i} className="flex items-center gap-3 py-2 border-b border-white/4 last:border-0">
                      <span className="w-5 h-5 rounded-full bg-accent-primary/10 border border-accent-primary/20 flex items-center justify-center flex-shrink-0">
                        <span className="text-[9px] font-bold text-accent-primary">{i + 1}</span>
                      </span>
                      <span className="text-xs text-text-primary">{item}</span>
                      {i === 0 && <Badge variant="info" size="sm" className="ml-auto">Primary</Badge>}
                    </div>
                  ))}
                </Section>
              )}

              {detailTab === 'health' && (
                <>
                  <Section title="Real-time Metrics">
                    <StatRow label="Current Requests" value={String(selectedProvider.currentRequests)} />
                    <StatRow label="Circuit State" value={selectedProvider.circuitState.toUpperCase()} />
                    <StatRow label="Cost (1h)" value={`$${selectedProvider.costPerHour.toFixed(2)}`} />
                  </Section>
                  <Section title="Health Checks">
                    {details.healthChecks.map((hc) => (
                      <div key={hc.name} className="flex items-center justify-between py-1.5">
                        <div className="flex items-center gap-2">
                          {hc.status === 'pass' ? (
                            <CheckCircle size={12} className="text-status-success" />
                          ) : hc.status === 'warn' ? (
                            <AlertCircle size={12} className="text-status-warning" />
                          ) : (
                            <XCircle size={12} className="text-status-danger" />
                          )}
                          <span className="text-xs text-text-primary">{hc.name}</span>
                        </div>
                        <span className="text-[10px] text-text-secondary">{hc.ts}</span>
                      </div>
                    ))}
                  </Section>
                </>
              )}
            </div>
          </motion.aside>
        )}
      </AnimatePresence>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div>
      <h3 className="text-[10px] font-semibold text-text-secondary uppercase tracking-wider mb-2">{title}</h3>
      <div className="bg-white/3 rounded-lg px-3 py-1 border border-white/6">{children}</div>
    </div>
  );
}

function StatRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between py-1.5 border-b border-white/4 last:border-0">
      <span className="text-[11px] text-text-secondary">{label}</span>
      <span className="text-[11px] text-text-primary font-medium">{value}</span>
    </div>
  );
}

function ProviderLogo({ id, size = 36 }: { id: string; size?: number }) {
  const colors: Record<string, string> = {
    openai: '#19D3FF',
    groq: '#6AE3FF',
    claude: '#FFC857',
    gemini: '#A78BFA',
  };
  const letters: Record<string, string> = {
    openai: 'OA',
    groq: 'GQ',
    claude: 'CL',
    gemini: 'GM',
  };

  return (
    <div
      style={{ width: size, height: size, borderColor: colors[id] + '40', color: colors[id] }}
      className="rounded-xl border-2 flex items-center justify-center flex-shrink-0 font-bold"
      title={id}
    >
      <span style={{ fontSize: size * 0.28 }}>{letters[id] || id.slice(0, 2).toUpperCase()}</span>
    </div>
  );
}
