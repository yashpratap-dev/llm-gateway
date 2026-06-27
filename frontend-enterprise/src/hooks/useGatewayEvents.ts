import { useState, useEffect, useCallback } from 'react';
import type { GatewayEvent, GatewayEventType } from '../types';
import { mockGatewayEvents } from '../mock/dashboard';

const eventTemplates: Array<{
  type: GatewayEventType;
  title: string;
  descriptions: string[];
}> = [
  {
    type: 'success',
    title: 'Request Completed',
    descriptions: [
      'gpt-4.1 responded in 398ms via OpenAI',
      'llama-3.3-70b responded in 201ms via Groq',
      'claude-3-7-sonnet responded in 821ms via Anthropic',
    ],
  },
  {
    type: 'success',
    title: 'Cache Hit',
    descriptions: [
      'Semantic cache hit with similarity 0.94 — saved $0.0038',
      'Exact cache hit — saved $0.0052',
      'Semantic cache hit with similarity 0.91 — saved $0.0029',
    ],
  },
  {
    type: 'warning',
    title: 'High Latency',
    descriptions: [
      'Claude p95 latency exceeded 800ms threshold',
      'Gemini response time elevated: 1.8s',
      'OpenAI streaming timeout approaching limit',
    ],
  },
  {
    type: 'error',
    title: 'Rate Limit Hit',
    descriptions: [
      'Gemini API returned 429 Too Many Requests',
      'Claude API rate limit — routing to fallback',
      'OpenAI RPM limit reached — queuing requests',
    ],
  },
  {
    type: 'circuit',
    title: 'Circuit Breaker Event',
    descriptions: [
      'Gemini circuit opened after 5 consecutive failures',
      'Claude circuit entering half-open state',
      'OpenAI circuit closed — service recovered',
    ],
  },
];

let eventCounter = 100;

function generateEvent(): GatewayEvent {
  const template = eventTemplates[Math.floor(Math.random() * eventTemplates.length)];
  const description = template.descriptions[Math.floor(Math.random() * template.descriptions.length)];
  const id = `evt-${++eventCounter}`;
  const reqId = `req_01JX${Math.random().toString(36).substring(2, 10).toUpperCase()}`;

  return {
    id,
    type: template.type,
    title: template.title,
    description,
    reqId,
    timestamp: new Date(),
  };
}

export function useGatewayEvents(maxEvents = 20) {
  const [events, setEvents] = useState<GatewayEvent[]>(mockGatewayEvents);

  const addEvent = useCallback(() => {
    setEvents(prev => {
      const next = [generateEvent(), ...prev];
      return next.slice(0, maxEvents);
    });
  }, [maxEvents]);

  useEffect(() => {
    // Add new events at random intervals between 2-6 seconds
    const getInterval = () => 2000 + Math.random() * 4000;

    let timeout: ReturnType<typeof setTimeout>;

    const schedule = () => {
      timeout = setTimeout(() => {
        addEvent();
        schedule();
      }, getInterval());
    };

    schedule();

    return () => clearTimeout(timeout);
  }, [addEvent]);

  return events;
}
