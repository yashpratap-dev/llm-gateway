import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Card } from '../components/ui/Card';
import { AreaChart } from '../components/charts/AreaChart';
import { LineChart } from '../components/charts/LineChart';
import { BarChart } from '../components/charts/BarChart';
import {
  volumeData,
  latencyData,
  costBreakdownData,
  errorRateData,
  providerComparisonData,
  cacheHitData,
} from '../mock/analytics';

const ranges = ['7d', '30d', '90d'];

export function Analytics() {
  const [range, setRange] = useState('7d');

  return (
    <div className="h-screen overflow-y-auto">
      {/* Header */}
      <div className="sticky top-0 z-10 bg-bg border-b border-white/8 px-6 py-4 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-white">Analytics</h1>
          <p className="text-xs text-text-secondary mt-0.5">Deep-dive metrics across your LLM gateway</p>
        </div>
        <div className="flex gap-1 p-1 bg-white/4 rounded-xl border border-white/8">
          {ranges.map(r => (
            <button
              key={r}
              onClick={() => setRange(r)}
              className={`px-3 py-1 rounded-lg text-xs font-medium transition-colors ${
                range === r
                  ? 'bg-white/10 text-white'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              {r}
            </button>
          ))}
        </div>
      </div>

      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="p-6 space-y-6"
      >
        {/* Row 1: Volume + Error Rate */}
        <div className="grid grid-cols-2 gap-4">
          <Card padding="md">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-sm font-semibold text-text-primary">Request Volume</h2>
              <span className="text-xs text-text-secondary">Last 7 days</span>
            </div>
            <AreaChart
              data={volumeData}
              xKey="date"
              yKey="requests"
              color="#19D3FF"
              height={200}
              formatY={v => `${(v / 1_000_000).toFixed(1)}M`}
              formatTooltip={v => `${(v / 1_000_000).toFixed(2)}M requests`}
            />
          </Card>

          <Card padding="md">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-sm font-semibold text-text-primary">Error Rate</h2>
              <div className="flex items-center gap-2 text-xs">
                <div className="flex items-center gap-1">
                  <div className="w-2 h-2 rounded-full bg-accent-primary" />
                  <span className="text-text-secondary">Error Rate</span>
                </div>
                <div className="flex items-center gap-1">
                  <div className="w-2 h-0.5 bg-status-danger" />
                  <span className="text-text-secondary">Threshold</span>
                </div>
              </div>
            </div>
            <LineChart
              data={errorRateData}
              xKey="date"
              lines={[{ key: 'errorRate', color: '#19D3FF', label: 'Error Rate %' }]}
              height={200}
              referenceLineY={2.0}
              referenceLineLabel="2% SLA"
              formatY={v => `${v}%`}
            />
          </Card>
        </div>

        {/* Row 2: Latency Distribution */}
        <Card padding="md">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-text-primary">Latency Distribution</h2>
            <div className="flex items-center gap-4 text-xs">
              {[
                { label: 'P50', color: '#18D46B' },
                { label: 'P95', color: '#19D3FF' },
                { label: 'P99', color: '#FFC857' },
              ].map(({ label, color }) => (
                <div key={label} className="flex items-center gap-1.5">
                  <div className="w-2 h-2 rounded-full" style={{ background: color }} />
                  <span className="text-text-secondary">{label}</span>
                </div>
              ))}
            </div>
          </div>
          <LineChart
            data={latencyData}
            xKey="date"
            lines={[
              { key: 'p50', color: '#18D46B', label: 'P50' },
              { key: 'p95', color: '#19D3FF', label: 'P95' },
              { key: 'p99', color: '#FFC857', label: 'P99' },
            ]}
            height={220}
            formatY={v => `${v}ms`}
          />
        </Card>

        {/* Row 3: Cost Breakdown + Cache Hit Rate */}
        <div className="grid grid-cols-2 gap-4">
          <Card padding="md">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-sm font-semibold text-text-primary">Cost Breakdown by Provider</h2>
            </div>
            <BarChart
              data={costBreakdownData}
              xKey="date"
              bars={[
                { key: 'openai', color: '#19D3FF', label: 'OpenAI' },
                { key: 'groq', color: '#6AE3FF', label: 'Groq' },
                { key: 'claude', color: '#FFC857', label: 'Claude' },
                { key: 'gemini', color: '#A78BFA', label: 'Gemini' },
              ]}
              stacked
              height={220}
              formatY={v => `$${(v / 1000).toFixed(0)}K`}
            />
          </Card>

          <Card padding="md">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-sm font-semibold text-text-primary">Cache Hit Rate Over Time</h2>
              <div className="flex items-center gap-4 text-xs">
                {[
                  { label: 'Semantic', color: '#19D3FF' },
                  { label: 'Exact', color: '#6AE3FF' },
                ].map(({ label, color }) => (
                  <div key={label} className="flex items-center gap-1.5">
                    <div className="w-2 h-2 rounded-full" style={{ background: color }} />
                    <span className="text-text-secondary">{label}</span>
                  </div>
                ))}
              </div>
            </div>
            <LineChart
              data={cacheHitData}
              xKey="date"
              lines={[
                { key: 'semantic', color: '#19D3FF', label: 'Semantic Hit Rate' },
                { key: 'exact', color: '#6AE3FF', label: 'Exact Hit Rate' },
              ]}
              height={220}
              formatY={v => `${v}%`}
            />
          </Card>
        </div>

        {/* Row 4: Provider Comparison */}
        <Card padding="md">
          <h2 className="text-sm font-semibold text-text-primary mb-4">Provider Comparison</h2>
          <div className="grid grid-cols-3 gap-6">
            {/* Latency */}
            <div>
              <p className="text-xs text-text-secondary mb-3">p95 Latency (ms)</p>
              {providerComparisonData.map(p => (
                <div key={p.provider} className="mb-3">
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-text-primary">{p.provider}</span>
                    <span className="text-text-secondary font-mono">{p.latency}ms</span>
                  </div>
                  <div className="h-1.5 rounded-full overflow-hidden bg-white/6">
                    <div
                      className="h-full rounded-full"
                      style={{
                        width: `${(p.latency / 2000) * 100}%`,
                        background: p.latency < 300 ? '#18D46B' : p.latency < 800 ? '#19D3FF' : '#FF5A5A',
                      }}
                    />
                  </div>
                </div>
              ))}
            </div>

            {/* Cost */}
            <div>
              <p className="text-xs text-text-secondary mb-3">Input Cost ($ per 1M tokens)</p>
              {providerComparisonData.map(p => (
                <div key={p.provider} className="mb-3">
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-text-primary">{p.provider}</span>
                    <span className="text-accent-primary font-mono">${p.cost.toFixed(2)}</span>
                  </div>
                  <div className="h-1.5 rounded-full overflow-hidden bg-white/6">
                    <div
                      className="h-full rounded-full bg-accent-primary"
                      style={{ width: `${(p.cost / 3.5) * 100}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>

            {/* Requests */}
            <div>
              <p className="text-xs text-text-secondary mb-3">Total Requests</p>
              {providerComparisonData.map(p => (
                <div key={p.provider} className="mb-3">
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-text-primary">{p.provider}</span>
                    <span className="text-text-secondary font-mono">
                      {(p.requests / 1_000_000).toFixed(1)}M
                    </span>
                  </div>
                  <div className="h-1.5 rounded-full overflow-hidden bg-white/6">
                    <div
                      className="h-full rounded-full bg-accent-secondary"
                      style={{ width: `${(p.requests / 10_000_000) * 100}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </Card>
      </motion.div>
    </div>
  );
}
