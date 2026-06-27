import type {
  VolumePoint,
  LatencyPoint,
  CostBreakdownPoint,
  ErrorRatePoint,
  ProviderComparisonItem,
  CacheHitPoint,
} from '../types';

export const volumeData: VolumePoint[] = [
  { date: 'Jun 22', requests: 2840000 },
  { date: 'Jun 23', requests: 3120000 },
  { date: 'Jun 24', requests: 2980000 },
  { date: 'Jun 25', requests: 3450000 },
  { date: 'Jun 26', requests: 3680000 },
  { date: 'Jun 27', requests: 4120000 },
  { date: 'Jun 28', requests: 4380000 },
];

export const latencyData: LatencyPoint[] = [
  { date: 'Jun 22', p50: 210, p95: 580, p99: 820 },
  { date: 'Jun 23', p50: 225, p95: 601, p99: 854 },
  { date: 'Jun 24', p50: 198, p95: 555, p99: 790 },
  { date: 'Jun 25', p50: 234, p95: 621, p99: 891 },
  { date: 'Jun 26', p50: 219, p95: 638, p99: 905 },
  { date: 'Jun 27', p50: 208, p95: 625, p99: 870 },
  { date: 'Jun 28', p50: 215, p95: 642, p99: 898 },
];

export const costBreakdownData: CostBreakdownPoint[] = [
  { date: 'Jun 22', openai: 8200, groq: 3100, claude: 2800, gemini: 1100 },
  { date: 'Jun 23', openai: 9100, groq: 3400, claude: 3200, gemini: 1200 },
  { date: 'Jun 24', openai: 7800, groq: 2900, claude: 2600, gemini: 900 },
  { date: 'Jun 25', openai: 10200, groq: 3800, claude: 3600, gemini: 1400 },
  { date: 'Jun 26', openai: 11400, groq: 4100, claude: 3900, gemini: 1500 },
  { date: 'Jun 27', openai: 9800, groq: 3600, claude: 3400, gemini: 1300 },
  { date: 'Jun 28', openai: 10400, groq: 3900, claude: 3600, gemini: 1400 },
];

export const errorRateData: ErrorRatePoint[] = [
  { date: 'Jun 22', errorRate: 0.8, threshold: 2.0 },
  { date: 'Jun 23', errorRate: 1.2, threshold: 2.0 },
  { date: 'Jun 24', errorRate: 0.6, threshold: 2.0 },
  { date: 'Jun 25', errorRate: 2.8, threshold: 2.0 },
  { date: 'Jun 26', errorRate: 1.9, threshold: 2.0 },
  { date: 'Jun 27', errorRate: 1.1, threshold: 2.0 },
  { date: 'Jun 28', errorRate: 0.9, threshold: 2.0 },
];

export const providerComparisonData: ProviderComparisonItem[] = [
  { provider: 'OpenAI', latency: 412, cost: 2.50, requests: 9800000 },
  { provider: 'Groq', latency: 198, cost: 0.59, requests: 7700000 },
  { provider: 'Claude', latency: 842, cost: 3.00, requests: 4530000 },
  { provider: 'Gemini', latency: 1620, cost: 1.25, requests: 2310000 },
];

export const cacheHitData: CacheHitPoint[] = [
  { date: 'Jun 22', semantic: 58.2, exact: 14.1 },
  { date: 'Jun 23', semantic: 59.8, exact: 14.4 },
  { date: 'Jun 24', semantic: 60.1, exact: 14.8 },
  { date: 'Jun 25', semantic: 61.4, exact: 15.1 },
  { date: 'Jun 26', semantic: 61.9, exact: 15.5 },
  { date: 'Jun 27', semantic: 62.1, exact: 15.7 },
  { date: 'Jun 28', semantic: 62.33, exact: 15.81 },
];
