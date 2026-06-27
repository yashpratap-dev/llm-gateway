import React from 'react';
import {
  LineChart as ReLineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
  ReferenceLine,
} from 'recharts';

interface LineConfig {
  key: string;
  color: string;
  label?: string;
  dashed?: boolean;
}

interface LineChartProps {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  data: any[];
  xKey: string;
  lines: LineConfig[];
  height?: number;
  formatY?: (value: number) => string;
  referenceLineY?: number;
  referenceLineLabel?: string;
}

const CustomTooltip = ({
  active,
  payload,
  label,
}: {
  active?: boolean;
  payload?: Array<{ color: string; name: string; value: number }>;
  label?: string;
}) => {
  if (!active || !payload?.length) return null;
  return (
    <div className="bg-bg-card border border-white/10 rounded-lg px-3 py-2 text-xs space-y-1">
      <p className="text-text-secondary mb-1">{label}</p>
      {payload.map((entry) => (
        <div key={entry.name} className="flex items-center gap-2">
          <div className="w-2 h-2 rounded-full" style={{ background: entry.color }} />
          <span className="text-text-secondary">{entry.name}:</span>
          <span className="font-semibold" style={{ color: entry.color }}>
            {entry.value.toLocaleString()}
          </span>
        </div>
      ))}
    </div>
  );
};

export function LineChart({
  data,
  xKey,
  lines,
  height = 200,
  formatY,
  referenceLineY,
  referenceLineLabel,
}: LineChartProps) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <ReLineChart data={data} margin={{ top: 4, right: 4, bottom: 0, left: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" vertical={false} />
        <XAxis
          dataKey={xKey}
          tick={{ fill: '#A8B0B8', fontSize: 10 }}
          axisLine={false}
          tickLine={false}
        />
        <YAxis
          tick={{ fill: '#A8B0B8', fontSize: 10 }}
          axisLine={false}
          tickLine={false}
          tickFormatter={formatY}
          width={48}
        />
        <Tooltip content={<CustomTooltip />} />
        {lines.length > 1 && (
          <Legend
            iconType="circle"
            iconSize={8}
            formatter={(v) => (
              <span style={{ color: '#A8B0B8', fontSize: 10 }}>{v}</span>
            )}
          />
        )}
        {referenceLineY !== undefined && (
          <ReferenceLine
            y={referenceLineY}
            stroke="#FF5A5A"
            strokeDasharray="4 2"
            label={{
              value: referenceLineLabel || '',
              fill: '#FF5A5A',
              fontSize: 10,
              position: 'right',
            }}
          />
        )}
        {lines.map((l) => (
          <Line
            key={l.key}
            type="monotone"
            dataKey={l.key}
            stroke={l.color}
            strokeWidth={2}
            strokeDasharray={l.dashed ? '4 2' : undefined}
            dot={false}
            name={l.label || l.key}
            activeDot={{ r: 4, fill: l.color, stroke: '#050505', strokeWidth: 2 }}
          />
        ))}
      </ReLineChart>
    </ResponsiveContainer>
  );
}
