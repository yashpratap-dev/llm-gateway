import React from 'react';
import {
  AreaChart as ReAreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';

interface AreaChartProps {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  data: any[];
  xKey: string;
  yKey: string;
  color?: string;
  height?: number;
  formatY?: (value: number) => string;
  formatTooltip?: (value: number) => string;
}

const CustomTooltip = ({
  active,
  payload,
  label,
  formatTooltip,
}: {
  active?: boolean;
  payload?: Array<{ value: number }>;
  label?: string;
  formatTooltip?: (v: number) => string;
}) => {
  if (!active || !payload?.length) return null;
  const val = payload[0].value;
  return (
    <div className="bg-bg-card border border-white/10 rounded-lg px-3 py-2 text-xs">
      <p className="text-text-secondary mb-0.5">{label}</p>
      <p className="text-accent-primary font-semibold">
        {formatTooltip ? formatTooltip(val) : val.toLocaleString()}
      </p>
    </div>
  );
};

export function AreaChart({
  data,
  xKey,
  yKey,
  color = '#19D3FF',
  height = 200,
  formatY,
  formatTooltip,
}: AreaChartProps) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <ReAreaChart data={data} margin={{ top: 4, right: 4, bottom: 0, left: 0 }}>
        <defs>
          <linearGradient id={`area-gradient-${yKey}`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor={color} stopOpacity={0.2} />
            <stop offset="95%" stopColor={color} stopOpacity={0} />
          </linearGradient>
        </defs>
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
        <Tooltip
          content={
            <CustomTooltip formatTooltip={formatTooltip} />
          }
        />
        <Area
          type="monotone"
          dataKey={yKey}
          stroke={color}
          strokeWidth={2}
          fill={`url(#area-gradient-${yKey})`}
          dot={false}
          activeDot={{ r: 4, fill: color, stroke: '#050505', strokeWidth: 2 }}
        />
      </ReAreaChart>
    </ResponsiveContainer>
  );
}
