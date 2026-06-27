import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { RefreshCw, TrendingUp, TrendingDown, ExternalLink } from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { AreaChart } from '../components/charts/AreaChart';
import { DonutChart } from '../components/charts/DonutChart';
import { BarChart } from '../components/charts/BarChart';
import { SparklineChart } from '../components/charts/SparklineChart';
import { Progress } from '../components/ui/Progress';
import { useGatewayEvents } from '../hooks/useGatewayEvents';
import {
  kpiCards,
  providerRoutingTable,
  liveTraffic,
  providerMix,
  costOverTime,
  costByTenant,
  cachePerformance,
  circuitEvents,
  topModels,
} from '../mock/dashboard';
import {
  CheckCircle,
  AlertTriangle,
  XCircle,
  Zap,
} from 'lucide-react';
import type { GatewayEvent } from '../types';

const tenants = ['All Tenants', 'Acme Corp', 'TechStart Inc', 'DataFlow Systems'];
const environments = ['Production', 'Staging', 'Development'];

const eventIcon = (type: GatewayEvent['type']) => {
  const cls = 'w-4 h-4 flex-shrink-0';
  switch (type) {
    case 'success': return <CheckCircle className={`${cls} text-status-success`} />;
    case 'warning': return <AlertTriangle className={`${cls} text-status-warning`} />;
    case 'error': return <XCircle className={`${cls} text-status-danger`} />;
    case 'circuit': return <Zap className={`${cls} text-accent-primary`} />;
  }
};

function timeAgo(date: Date): string {
  const secs = Math.floor((Date.now() - date.getTime()) / 1000);
  if (secs < 60) return `${secs}s ago`;
  if (secs < 3600) return `${Math.floor(secs / 60)}m ago`;
  return `${Math.floor(secs / 3600)}h ago`;
}

export function Dashboard() {
  const [selectedTenant, setSelectedTenant] = useState('All Tenants');
  const [selectedEnv, setSelectedEnv] = useState('Production');
  const events = useGatewayEvents();

  return (
    <div className="flex h-screen overflow-hidden">
      {/* Main content */}
      <div className="flex-1 overflow-y-auto">
        {/* Top bar */}
        <div className="sticky top-0 z-10 bg-bg border-b border-white/8 px-6 py-3 flex items-center gap-3 flex-wrap">
          <h1 className="text-lg font-bold text-white mr-auto">Dashboard</h1>

          {/* Date range */}
          <button className="flex items-center gap-2 px-3 py-1.5 bg-white/5 border border-white/10 rounded-lg text-xs text-text-secondary hover:border-white/20 transition-colors">
            May 12 — May 18, 2026
          </button>

          {/* Tenant */}
          <select
            value={selectedTenant}
            onChange={e => setSelectedTenant(e.target.value)}
            className="bg-white/5 border border-white/10 rounded-lg px-3 py-1.5 text-xs text-text-primary focus:outline-none hover:border-white/20"
          >
            {tenants.map(t => <option key={t}>{t}</option>)}
          </select>

          {/* Environment */}
          <select
            value={selectedEnv}
            onChange={e => setSelectedEnv(e.target.value)}
            className="bg-white/5 border border-white/10 rounded-lg px-3 py-1.5 text-xs text-text-primary focus:outline-none hover:border-white/20"
          >
            {environments.map(e => <option key={e}>{e}</option>)}
          </select>

          <button className="flex items-center gap-1.5 px-3 py-1.5 bg-white/5 border border-white/10 rounded-lg text-xs text-text-secondary hover:border-white/20 transition-colors">
            <RefreshCw size={12} />
            30s
          </button>
        </div>

        <div className="p-6 space-y-6">
          {/* KPI cards */}
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            className="grid grid-cols-4 gap-4"
          >
            {kpiCards.map((kpi) => (
              <Card key={kpi.label} hover padding="md">
                <div className="flex items-start justify-between mb-3">
                  <div>
                    <p className="text-xs text-text-secondary">{kpi.label}</p>
                    <p className="text-2xl font-bold text-white mt-1">{kpi.value}</p>
                  </div>
                  <div className="flex items-center gap-1">
                    <TrendingUp size={12} className="text-status-success" />
                    <span className="text-xs text-status-success font-medium">
                      +{kpi.trend}%
                    </span>
                  </div>
                </div>
                <SparklineChart
                  data={kpi.sparkline}
                  color="#19D3FF"
                  width="100%"
                  height={36}
                />
              </Card>
            ))}
          </motion.div>

          {/* Row 2: Traffic + Provider Mix */}
          <div className="grid grid-cols-3 gap-4">
            {/* Live Traffic */}
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="col-span-2"
            >
              <Card padding="md">
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-sm font-semibold text-text-primary">Live Traffic</h2>
                  <div className="flex items-center gap-1.5">
                    <span className="w-1.5 h-1.5 rounded-full bg-accent-primary animate-pulse" />
                    <span className="text-[10px] text-accent-primary font-medium">LIVE</span>
                  </div>
                </div>
                <AreaChart
                  data={liveTraffic}
                  xKey="time"
                  yKey="requests"
                  color="#19D3FF"
                  height={180}
                  formatY={v => v >= 1000 ? `${(v / 1000).toFixed(0)}K` : String(v)}
                  formatTooltip={v => `${v.toLocaleString()} req/s`}
                />
              </Card>
            </motion.div>

            {/* Provider Mix */}
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.15 }}
            >
              <Card padding="md" className="h-full">
                <h2 className="text-sm font-semibold text-text-primary mb-4">Provider Mix</h2>
                <DonutChart
                  data={providerMix}
                  height={160}
                  innerRadius={50}
                  outerRadius={70}
                />
                <div className="mt-3 space-y-1.5">
                  {providerMix.map(p => (
                    <div key={p.name} className="flex items-center gap-2">
                      <div className="w-2 h-2 rounded-full flex-shrink-0" style={{ background: p.color }} />
                      <span className="text-xs text-text-secondary flex-1">{p.name}</span>
                      <span className="text-xs font-semibold text-text-primary">{p.value}%</span>
                    </div>
                  ))}
                </div>
              </Card>
            </motion.div>
          </div>

          {/* Row 3: Routing table + Cost over time */}
          <div className="grid grid-cols-5 gap-4">
            {/* Provider Routing Table */}
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
              className="col-span-3"
            >
              <Card padding="md">
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-sm font-semibold text-text-primary">Provider Routing</h2>
                  <div className="flex items-center gap-2">
                    <Badge variant="cyan" size="sm">Latency Optimized</Badge>
                    <button className="text-[10px] text-accent-primary hover:underline flex items-center gap-1">
                      View full matrix <ExternalLink size={10} />
                    </button>
                  </div>
                </div>
                <table className="w-full text-xs">
                  <thead>
                    <tr className="border-b border-white/8">
                      <th className="text-left py-2 text-text-secondary font-medium">Model / Provider</th>
                      <th className="text-center py-2 text-text-secondary font-medium">OpenAI</th>
                      <th className="text-center py-2 text-text-secondary font-medium">Groq</th>
                      <th className="text-center py-2 text-text-secondary font-medium">Claude</th>
                      <th className="text-center py-2 text-text-secondary font-medium">Gemini</th>
                      <th className="text-center py-2 text-text-secondary font-medium">Fallback</th>
                    </tr>
                  </thead>
                  <tbody>
                    {providerRoutingTable.map(row => (
                      <tr key={row.model} className="border-b border-white/4 hover:bg-white/3 transition-colors">
                        <td className="py-2.5 font-mono text-text-primary">{row.model}</td>
                        {[row.openai, row.groq, row.claude, row.gemini].map((v, i) => (
                          <td key={i} className="text-center py-2.5">
                            {v !== null ? (
                              <span className="flex items-center justify-center gap-1">
                                <span className="w-2 h-2 rounded-full bg-status-success" />
                                <span className="text-text-primary font-medium">{v}%</span>
                              </span>
                            ) : (
                              <span className="text-white/20">—</span>
                            )}
                          </td>
                        ))}
                        <td className="text-center py-2.5 text-text-secondary">{row.fallback}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </Card>
            </motion.div>

            {/* Cost over time */}
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.25 }}
              className="col-span-2"
            >
              <Card padding="md" className="h-full">
                <h2 className="text-sm font-semibold text-text-primary mb-4">Cost Over Time</h2>
                <AreaChart
                  data={costOverTime}
                  xKey="date"
                  yKey="cost"
                  color="#19D3FF"
                  height={180}
                  formatY={v => `$${(v / 1000).toFixed(0)}K`}
                  formatTooltip={v => `$${v.toLocaleString()}`}
                />
              </Card>
            </motion.div>
          </div>

          {/* Row 4: Cost by Tenant + Cache Performance + Circuit Events */}
          <div className="grid grid-cols-3 gap-4">
            {/* Cost by Tenant */}
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3 }}
            >
              <Card padding="md" className="h-full">
                <h2 className="text-sm font-semibold text-text-primary mb-4">Cost by Tenant</h2>
                <div className="space-y-3">
                  {costByTenant.map(t => (
                    <div key={t.tenant}>
                      <div className="flex justify-between text-xs mb-1">
                        <span className="text-text-secondary">{t.tenant}</span>
                        <span className="text-text-primary font-medium">
                          ${t.cost.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                        </span>
                      </div>
                      <Progress
                        value={t.cost}
                        max={t.maxCost}
                        color="#19D3FF"
                        height={3}
                        animated
                      />
                    </div>
                  ))}
                </div>
              </Card>
            </motion.div>

            {/* Cache Performance */}
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.35 }}
            >
              <Card padding="md" className="h-full">
                <h2 className="text-sm font-semibold text-text-primary mb-2">Cache Performance</h2>
                <DonutChart
                  data={[
                    { name: 'Semantic Hit', value: cachePerformance.semanticHitRate, color: '#19D3FF' },
                    { name: 'Exact Hit', value: cachePerformance.exactHitRate, color: '#6AE3FF' },
                    { name: 'Miss', value: 100 - cachePerformance.hitRate, color: 'rgba(255,255,255,0.06)' },
                  ]}
                  centerValue={`${cachePerformance.hitRate}%`}
                  centerLabel="Hit Rate"
                  height={150}
                  innerRadius={45}
                  outerRadius={65}
                />
                <div className="mt-2 space-y-2">
                  <StatRow
                    label="Semantic Hit Rate"
                    value={`${cachePerformance.semanticHitRate}%`}
                    trend={cachePerformance.semanticTrend}
                  />
                  <StatRow
                    label="Exact Hit Rate"
                    value={`${cachePerformance.exactHitRate}%`}
                    trend={cachePerformance.exactTrend}
                  />
                  <div className="flex justify-between text-xs">
                    <span className="text-text-secondary">Total Requests</span>
                    <span className="text-text-primary font-medium">{cachePerformance.totalRequests}</span>
                  </div>
                </div>
              </Card>
            </motion.div>

            {/* Circuit Events */}
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.4 }}
            >
              <Card padding="md" className="h-full">
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-sm font-semibold text-text-primary">Circuit Events</h2>
                  <span className="text-[10px] text-text-secondary">Last 7 days</span>
                </div>
                <BarChart
                  data={circuitEvents}
                  xKey="day"
                  bars={[
                    { key: 'opened', color: '#FF5A5A', label: 'Opened' },
                    { key: 'halfOpen', color: '#FFC857', label: 'Half-Open' },
                    { key: 'closed', color: '#18D46B', label: 'Closed' },
                  ]}
                  stacked
                  height={180}
                />
                <div className="flex gap-3 mt-2 justify-center">
                  {[
                    { label: 'Opened', color: '#FF5A5A', val: 23 },
                    { label: 'Half-Open', color: '#FFC857', val: 17 },
                    { label: 'Closed', color: '#18D46B', val: 43 },
                  ].map(item => (
                    <div key={item.label} className="flex items-center gap-1.5">
                      <div className="w-2 h-2 rounded-full" style={{ background: item.color }} />
                      <span className="text-[10px] text-text-secondary">{item.label}</span>
                      <span className="text-[10px] font-semibold text-text-primary">{item.val}</span>
                    </div>
                  ))}
                </div>
              </Card>
            </motion.div>
          </div>

          {/* Row 5: Top Models */}
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.45 }}
          >
            <Card padding="md">
              <h2 className="text-sm font-semibold text-text-primary mb-4">Top Models</h2>
              <div className="space-y-3">
                {topModels.map(m => (
                  <div key={m.model} className="flex items-center gap-4">
                    <span className="text-xs font-mono text-text-primary w-44 flex-shrink-0">
                      {m.model}
                    </span>
                    <div className="flex-1">
                      <Progress
                        value={m.requests}
                        max={m.maxRequests}
                        color="#19D3FF"
                        height={4}
                        animated
                      />
                    </div>
                    <span className="text-xs text-text-secondary w-16 text-right flex-shrink-0">
                      {m.requests}M req
                    </span>
                  </div>
                ))}
              </div>
            </Card>
          </motion.div>
        </div>
      </div>

      {/* Gateway Events Rail */}
      <aside className="w-64 flex-shrink-0 h-screen overflow-y-auto border-l border-white/8 bg-bg-card">
        <div className="sticky top-0 bg-bg-card border-b border-white/8 px-4 py-3 flex items-center justify-between z-10">
          <span className="text-xs font-semibold text-text-primary">Gateway Events</span>
          <span className="flex items-center gap-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-status-success animate-pulse" />
            <span className="text-[10px] text-status-success">Live</span>
          </span>
        </div>
        <div className="p-3 space-y-2">
          {events.map((event) => (
            <motion.div
              key={event.id}
              initial={{ opacity: 0, y: -6 }}
              animate={{ opacity: 1, y: 0 }}
              className="flex gap-2 p-2 rounded-lg bg-white/3 border border-white/6"
            >
              {eventIcon(event.type)}
              <div className="min-w-0 flex-1">
                <p className="text-[11px] font-semibold text-text-primary truncate">{event.title}</p>
                <p className="text-[10px] text-text-secondary leading-relaxed">{event.description}</p>
                <div className="flex items-center gap-1 mt-0.5">
                  <code className="text-[9px] text-text-secondary font-mono truncate">{event.reqId}</code>
                  <span className="text-[9px] text-text-secondary ml-auto flex-shrink-0">
                    {timeAgo(event.timestamp)}
                  </span>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </aside>
    </div>
  );
}

function StatRow({ label, value, trend }: { label: string; value: string; trend: number }) {
  return (
    <div className="flex items-center justify-between text-xs">
      <span className="text-text-secondary">{label}</span>
      <div className="flex items-center gap-1.5">
        <span className="text-text-primary font-medium">{value}</span>
        <span className="text-status-success text-[10px] flex items-center gap-0.5">
          <TrendingUp size={10} />
          +{trend}%
        </span>
      </div>
    </div>
  );
}
