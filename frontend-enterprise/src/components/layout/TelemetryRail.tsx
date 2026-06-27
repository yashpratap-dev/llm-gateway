import React from 'react';
import { motion } from 'framer-motion';
import {
  CheckCircle,
  AlertTriangle,
  XCircle,
  Zap,
  Copy,
} from 'lucide-react';
import { Badge } from '../ui/Badge';
import { Progress } from '../ui/Progress';
import type { TelemetryData, GatewayEvent } from '../../types';

interface TelemetryRailProps {
  telemetry: TelemetryData;
  events: GatewayEvent[];
}

function timeAgo(date: Date): string {
  const secs = Math.floor((Date.now() - date.getTime()) / 1000);
  if (secs < 60) return `${secs}s ago`;
  if (secs < 3600) return `${Math.floor(secs / 60)}m ago`;
  return `${Math.floor(secs / 3600)}h ago`;
}

const eventIcon = (type: GatewayEvent['type']) => {
  const cls = 'w-4 h-4 flex-shrink-0';
  switch (type) {
    case 'success': return <CheckCircle className={`${cls} text-status-success`} />;
    case 'warning': return <AlertTriangle className={`${cls} text-status-warning`} />;
    case 'error': return <XCircle className={`${cls} text-status-danger`} />;
    case 'circuit': return <Zap className={`${cls} text-accent-primary`} />;
  }
};

export function TelemetryRail({ telemetry, events }: TelemetryRailProps) {
  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text).catch(() => {});
  };

  return (
    <aside className="w-72 flex-shrink-0 h-screen overflow-y-auto border-l border-white/8 bg-bg-card">
      {/* Header */}
      <div className="sticky top-0 z-10 bg-bg-card border-b border-white/8 px-4 py-3 flex items-center justify-between">
        <span className="text-xs font-semibold text-text-primary">Request Inspector</span>
        <span className="flex items-center gap-1.5">
          <span className="w-1.5 h-1.5 rounded-full bg-status-success animate-pulse" />
          <span className="text-[10px] text-status-success font-medium">LIVE</span>
        </span>
      </div>

      <div className="p-4 space-y-4">
        {/* Request Overview */}
        <Section title="Request Overview">
          <Row label="Provider" value={telemetry.provider} />
          <Row label="Model" value={telemetry.model} mono />
          <Row
            label="Latency"
            value={`${telemetry.latency}ms`}
            valueColor="text-status-success"
          />
          <Row
            label="Cost"
            value={`$${telemetry.cost.toFixed(6)}`}
            valueColor="text-status-success"
          />
          <div className="flex items-center justify-between py-1">
            <span className="text-[11px] text-text-secondary">Status</span>
            <Badge variant="success" size="sm">
              {telemetry.status} OK
            </Badge>
          </div>
          <Row
            label="Duration"
            value={`${(telemetry.latency / 1000).toFixed(2)}s`}
          />
        </Section>

        {/* Cache */}
        <Section title="Cache">
          <div className="flex items-center justify-between py-1">
            <span className="text-[11px] text-text-secondary">Semantic Cache</span>
            <Badge variant={telemetry.semanticCacheHit ? 'success' : 'neutral'} size="sm">
              {telemetry.semanticCacheHit ? 'HIT' : 'MISS'}
            </Badge>
          </div>
          <div className="flex items-center justify-between py-1">
            <span className="text-[11px] text-text-secondary">Exact Cache</span>
            <Badge variant={telemetry.exactCacheHit ? 'success' : 'neutral'} size="sm">
              {telemetry.exactCacheHit ? 'HIT' : 'MISS'}
            </Badge>
          </div>
          <div className="py-1 space-y-1">
            <div className="flex justify-between">
              <span className="text-[11px] text-text-secondary">Similarity Score</span>
              <span className="text-[11px] text-accent-primary font-mono font-semibold">
                {telemetry.similarityScore.toFixed(2)}
              </span>
            </div>
            <Progress
              value={telemetry.similarityScore * 100}
              color="#19D3FF"
              height={3}
              animated
            />
          </div>
        </Section>

        {/* Token Usage */}
        <Section title="Token Usage">
          <Row
            label="Prompt Tokens"
            value={telemetry.promptTokens.toLocaleString()}
            mono
          />
          <Row
            label="Completion Tokens"
            value={telemetry.completionTokens.toLocaleString()}
            mono
          />
          <Row
            label="Total Tokens"
            value={telemetry.totalTokens.toLocaleString()}
            mono
            highlight
          />
        </Section>

        {/* Infrastructure */}
        <Section title="Infrastructure">
          <Row label="Embedding Model" value={telemetry.embeddingModel} mono small />
          <div className="flex items-center justify-between py-1">
            <span className="text-[11px] text-text-secondary">Redis</span>
            <Badge variant={telemetry.redisConnected ? 'success' : 'danger'} size="sm" dot>
              {telemetry.redisConnected ? 'CONNECTED' : 'DISCONNECTED'}
            </Badge>
          </div>
          <div className="flex items-center justify-between py-1">
            <span className="text-[11px] text-text-secondary">PostgreSQL</span>
            <Badge variant={telemetry.postgresConnected ? 'success' : 'danger'} size="sm" dot>
              {telemetry.postgresConnected ? 'CONNECTED' : 'DISCONNECTED'}
            </Badge>
          </div>
        </Section>

        {/* Gateway & Routing */}
        <Section title="Gateway & Routing">
          <div className="flex items-center justify-between py-1">
            <span className="text-[11px] text-text-secondary">Circuit Breaker</span>
            <Badge
              variant={
                telemetry.circuitState === 'closed'
                  ? 'success'
                  : telemetry.circuitState === 'half-open'
                  ? 'warning'
                  : 'danger'
              }
              size="sm"
            >
              {telemetry.circuitState.toUpperCase()}
            </Badge>
          </div>
          <Row label="Tenant" value={telemetry.tenant} mono small />
          <div className="py-1 space-y-0.5">
            <span className="text-[11px] text-text-secondary block">Request ID</span>
            <code className="text-[10px] text-accent-primary font-mono break-all">
              {telemetry.requestId}
            </code>
          </div>
          <div className="py-1 flex items-center justify-between">
            <div className="space-y-0.5">
              <span className="text-[11px] text-text-secondary block">API Key Used</span>
              <code className="text-[11px] text-white font-mono">{telemetry.apiKey}</code>
            </div>
            <button
              onClick={() => copyToClipboard(telemetry.apiKey)}
              className="p-1.5 rounded hover:bg-white/8 text-text-secondary hover:text-white transition-colors"
            >
              <Copy size={12} />
            </button>
          </div>
        </Section>

        {/* Gateway Events */}
        <div className="border-t border-white/8 pt-4">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-semibold text-text-primary">Gateway Events</span>
            <span className="flex items-center gap-1">
              <span className="w-1.5 h-1.5 rounded-full bg-status-success animate-pulse" />
              <span className="text-[10px] text-status-success">Live</span>
            </span>
          </div>
          <div className="space-y-2">
            {events.slice(0, 8).map((event) => (
              <motion.div
                key={event.id}
                initial={{ opacity: 0, y: -6 }}
                animate={{ opacity: 1, y: 0 }}
                className="event-item flex gap-2 p-2 rounded-lg bg-white/3 border border-white/6"
              >
                {eventIcon(event.type)}
                <div className="min-w-0 flex-1">
                  <p className="text-[11px] font-semibold text-text-primary truncate">
                    {event.title}
                  </p>
                  <p className="text-[10px] text-text-secondary leading-relaxed mt-0.5">
                    {event.description}
                  </p>
                  <div className="flex items-center gap-1 mt-1">
                    <code className="text-[9px] text-text-secondary font-mono">
                      {event.reqId}
                    </code>
                    <span className="text-[9px] text-text-secondary ml-auto flex-shrink-0">
                      {timeAgo(event.timestamp)}
                    </span>
                  </div>
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      </div>
    </aside>
  );
}

function Section({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div>
      <h3 className="text-[10px] font-semibold text-text-secondary uppercase tracking-wider mb-2">
        {title}
      </h3>
      <div className="space-y-0.5 bg-white/3 rounded-lg px-3 py-1 border border-white/6">
        {children}
      </div>
    </div>
  );
}

function Row({
  label,
  value,
  mono = false,
  small = false,
  valueColor,
  highlight = false,
}: {
  label: string;
  value: string;
  mono?: boolean;
  small?: boolean;
  valueColor?: string;
  highlight?: boolean;
}) {
  return (
    <div className="flex items-center justify-between py-1">
      <span className="text-[11px] text-text-secondary">{label}</span>
      <span
        className={`${small ? 'text-[10px]' : 'text-[11px]'} ${
          mono ? 'font-mono' : 'font-medium'
        } ${highlight ? 'text-white font-semibold' : valueColor || 'text-text-primary'} text-right max-w-[55%] truncate`}
      >
        {value}
      </span>
    </div>
  );
}
