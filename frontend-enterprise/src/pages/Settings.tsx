import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Save } from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Toggle } from '../components/ui/Toggle';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';

export function Settings() {
  const [settings, setSettings] = useState({
    semanticCache: true,
    exactCache: true,
    streaming: true,
    retryOnFailure: true,
    circuitBreaker: true,
    rateLimiting: true,
    costTracking: true,
    requestLogging: true,
    metricsExport: true,
    similarityThreshold: 0.85,
    defaultMaxTokens: 2048,
    defaultTemperature: 0.7,
    defaultProvider: 'openai',
    fallbackChain: 'openai,groq,claude,gemini',
    budgetCap: 150000,
  });

  const update = (key: string, value: unknown) =>
    setSettings(s => ({ ...s, [key]: value }));

  return (
    <div className="h-screen overflow-y-auto">
      <div className="px-6 py-5 border-b border-white/8 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-white">Settings</h1>
          <p className="text-xs text-text-secondary mt-0.5">
            Configure gateway behaviour and defaults
          </p>
        </div>
        <Button variant="primary" size="sm" icon={<Save size={14} />}>
          Save Changes
        </Button>
      </div>

      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="p-6 space-y-6 max-w-3xl"
      >
        {/* Features */}
        <Card padding="md">
          <h2 className="text-sm font-semibold text-text-primary mb-4">Feature Toggles</h2>
          <div className="space-y-4">
            {[
              { key: 'semanticCache', label: 'Semantic Cache', desc: 'Vector similarity cache using pgvector' },
              { key: 'exactCache', label: 'Exact Cache', desc: 'Exact string match Redis cache' },
              { key: 'streaming', label: 'SSE Streaming', desc: 'Enable server-sent events for streaming responses' },
              { key: 'retryOnFailure', label: 'Retry on Failure', desc: 'Automatically retry failed requests up to 3 times' },
              { key: 'circuitBreaker', label: 'Circuit Breaker', desc: 'Resilience4j circuit breaker per provider' },
              { key: 'rateLimiting', label: 'Rate Limiting', desc: 'Per-tenant RPM and TPM limits' },
              { key: 'costTracking', label: 'Cost Tracking', desc: 'Track token usage and estimate costs' },
              { key: 'requestLogging', label: 'Request Logging', desc: 'Log all requests to PostgreSQL' },
              { key: 'metricsExport', label: 'Prometheus Metrics', desc: 'Export metrics to Prometheus scrape endpoint' },
            ].map(item => (
              <div key={item.key} className="flex items-center justify-between py-1 border-b border-white/6 last:border-0">
                <div>
                  <p className="text-sm text-text-primary font-medium">{item.label}</p>
                  <p className="text-xs text-text-secondary mt-0.5">{item.desc}</p>
                </div>
                <Toggle
                  checked={settings[item.key as keyof typeof settings] as boolean}
                  onChange={v => update(item.key, v)}
                />
              </div>
            ))}
          </div>
        </Card>

        {/* Defaults */}
        <Card padding="md">
          <h2 className="text-sm font-semibold text-text-primary mb-4">Request Defaults</h2>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-text-primary font-medium">Default Provider</p>
                <p className="text-xs text-text-secondary">Primary LLM provider for new requests</p>
              </div>
              <select
                value={settings.defaultProvider}
                onChange={e => update('defaultProvider', e.target.value)}
                className="bg-white/5 border border-white/10 rounded-lg px-3 py-1.5 text-sm text-text-primary focus:outline-none focus:border-accent-primary/40"
              >
                {['openai', 'groq', 'claude', 'gemini'].map(p => (
                  <option key={p}>{p}</option>
                ))}
              </select>
            </div>

            <div>
              <div className="flex justify-between mb-2">
                <p className="text-sm text-text-primary font-medium">Default Temperature</p>
                <span className="text-sm text-accent-primary font-mono">{settings.defaultTemperature.toFixed(1)}</span>
              </div>
              <input
                type="range"
                min={0}
                max={2}
                step={0.1}
                value={settings.defaultTemperature}
                onChange={e => update('defaultTemperature', parseFloat(e.target.value))}
                className="w-full"
              />
            </div>

            <div className="flex items-center justify-between">
              <p className="text-sm text-text-primary font-medium">Default Max Tokens</p>
              <input
                type="number"
                min={256}
                max={8192}
                step={256}
                value={settings.defaultMaxTokens}
                onChange={e => update('defaultMaxTokens', parseInt(e.target.value))}
                className="w-24 bg-white/5 border border-white/10 rounded-lg px-2 py-1.5 text-sm text-text-primary focus:outline-none focus:border-accent-primary/40"
              />
            </div>
          </div>
        </Card>

        {/* Routing */}
        <Card padding="md">
          <h2 className="text-sm font-semibold text-text-primary mb-4">Routing & Resilience</h2>
          <div className="space-y-4">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <p className="text-sm text-text-primary font-medium">Fallback Chain</p>
                <Badge variant="neutral" size="sm">Comma-separated</Badge>
              </div>
              <p className="text-xs text-text-secondary mb-2">
                Order of provider fallback when primary fails
              </p>
              <input
                type="text"
                value={settings.fallbackChain}
                onChange={e => update('fallbackChain', e.target.value)}
                className="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-sm text-text-primary font-mono focus:outline-none focus:border-accent-primary/40"
              />
            </div>

            <div>
              <div className="flex items-center justify-between mb-1">
                <p className="text-sm text-text-primary font-medium">Semantic Similarity Threshold</p>
                <span className="text-sm text-accent-primary font-mono">{settings.similarityThreshold.toFixed(2)}</span>
              </div>
              <p className="text-xs text-text-secondary mb-2">
                Minimum cosine similarity score for cache hit
              </p>
              <input
                type="range"
                min={0.5}
                max={1.0}
                step={0.01}
                value={settings.similarityThreshold}
                onChange={e => update('similarityThreshold', parseFloat(e.target.value))}
                className="w-full"
              />
            </div>
          </div>
        </Card>

        {/* Budget */}
        <Card padding="md">
          <h2 className="text-sm font-semibold text-text-primary mb-4">Budget Controls</h2>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-text-primary font-medium">Monthly Budget Cap</p>
                <p className="text-xs text-text-secondary">Hard limit for monthly LLM spend (USD)</p>
              </div>
              <div className="flex items-center gap-1">
                <span className="text-sm text-text-secondary">$</span>
                <input
                  type="number"
                  value={settings.budgetCap}
                  onChange={e => update('budgetCap', parseInt(e.target.value))}
                  className="w-28 bg-white/5 border border-white/10 rounded-lg px-2 py-1.5 text-sm text-text-primary focus:outline-none focus:border-accent-primary/40"
                />
              </div>
            </div>

            <div className="p-3 rounded-lg bg-status-warning/8 border border-status-warning/20">
              <p className="text-xs text-status-warning font-medium">
                Current monthly spend: $128,430 — 85.6% of budget cap
              </p>
            </div>
          </div>
        </Card>

        {/* Danger zone */}
        <Card padding="md" className="border-status-danger/20">
          <h2 className="text-sm font-semibold text-status-danger mb-4">Danger Zone</h2>
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-text-primary font-medium">Clear Semantic Cache</p>
                <p className="text-xs text-text-secondary">Remove all 18,420 cached entries</p>
              </div>
              <Button variant="danger" size="sm">Clear Cache</Button>
            </div>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-text-primary font-medium">Reset Circuit Breakers</p>
                <p className="text-xs text-text-secondary">Force all circuit breakers to CLOSED state</p>
              </div>
              <Button variant="danger" size="sm">Reset All</Button>
            </div>
          </div>
        </Card>
      </motion.div>
    </div>
  );
}
