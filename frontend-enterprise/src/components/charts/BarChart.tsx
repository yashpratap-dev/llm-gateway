import React from 'react';
import {
  BarChart as ReBarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
  Cell,
} from 'recharts';

interface BarConfig {
  key: string;
  color: string;
  label?: string;
}

interface BarChartProps {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  data: any[];
  xKey: string;
  bars: BarConfig[];
  height?: number;
  horizontal?: boolean;
  stacked?: boolean;
  formatY?: (value: number) => string;
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

export function BarChart({
  data,
  xKey,
  bars,
  height = 200,
  horizontal = false,
  stacked = false,
  formatY,
}: BarChartProps) {
  if (horizontal) {
    return (
      <ResponsiveContainer width="100%" height={height}>
        <ReBarChart
          data={data}
          layout="vertical"
          margin={{ top: 4, right: 16, bottom: 0, left: 0 }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" horizontal={false} />
          <XAxis
            type="number"
            tick={{ fill: '#A8B0B8', fontSize: 10 }}
            axisLine={false}
            tickLine={false}
            tickFormatter={formatY}
          />
          <YAxis
            dataKey={xKey}
            type="category"
            tick={{ fill: '#A8B0B8', fontSize: 10 }}
            axisLine={false}
            tickLine={false}
            width={80}
          />
          <Tooltip content={<CustomTooltip />} />
          {bars.map((b) => (
            <Bar
              key={b.key}
              dataKey={b.key}
              fill={b.color}
              radius={[0, 4, 4, 0]}
              name={b.label || b.key}
              stackId={stacked ? 'stack' : undefined}
            >
              {data.map((_, idx) => (
                <Cell key={idx} fill={bars.length === 1 && data[idx] ? b.color : b.color} />
              ))}
            </Bar>
          ))}
        </ReBarChart>
      </ResponsiveContainer>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={height}>
      <ReBarChart data={data} margin={{ top: 4, right: 4, bottom: 0, left: 0 }}>
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
        {bars.length > 1 && (
          <Legend
            iconType="circle"
            iconSize={8}
            formatter={(v) => (
              <span style={{ color: '#A8B0B8', fontSize: 10 }}>{v}</span>
            )}
          />
        )}
        {bars.map((b) => (
          <Bar
            key={b.key}
            dataKey={b.key}
            fill={b.color}
            radius={[4, 4, 0, 0]}
            name={b.label || b.key}
            stackId={stacked ? 'stack' : undefined}
            maxBarSize={32}
          />
        ))}
      </ReBarChart>
    </ResponsiveContainer>
  );
}
