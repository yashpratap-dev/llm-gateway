import type { AssistantMessage } from '../types/chat';

interface Props {
  message: AssistantMessage;
}

export function MetadataBar({ message }: Props) {
  if (message.streaming || (!message.provider && !message.error)) return null;

  if (message.error) {
    return (
      <div style={{ color: '#9A9A9A', fontSize: '0.75rem', marginTop: '0.25rem' }}>
        ── error: {message.error} ──
      </div>
    );
  }

  const latency = message.cacheHit
    ? 'CACHED'
    : `${((message.latencyMs ?? 0) / 1000).toFixed(2)}s`;

  return (
    <div style={{ color: '#9A9A9A', fontSize: '0.75rem', marginTop: '0.25rem' }}>
      ── {message.provider} · {latency} ──
    </div>
  );
}
