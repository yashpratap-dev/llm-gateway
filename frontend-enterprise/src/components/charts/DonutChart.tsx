import React from 'react';
import {
  PieChart,
  Pie,
  Cell,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';

interface DonutSlice {
  name: string;
  value: number;
  color: string;
}

interface DonutChartProps {
  data: DonutSlice[];
  centerLabel?: string;
  centerValue?: string;
  height?: number;
  innerRadius?: number;
  outerRadius?: number;
}

const CustomTooltip = ({
  active,
  payload,
}: {
  active?: boolean;
  payload?: Array<{ name: string; value: number; payload: { color: string } }>;
}) => {
  if (!active || !payload?.length) return null;
  const { name, value, payload: p } = payload[0];
  return (
    <div className="bg-bg-card border border-white/10 rounded-lg px-3 py-2 text-xs">
      <div className="flex items-center gap-2">
        <div className="w-2 h-2 rounded-full" style={{ background: p.color }} />
        <span className="text-text-secondary">{name}:</span>
        <span className="font-semibold text-white">{value.toFixed(1)}%</span>
      </div>
    </div>
  );
};

export function DonutChart({
  data,
  centerLabel,
  centerValue,
  height = 200,
  innerRadius = 55,
  outerRadius = 80,
}: DonutChartProps) {
  return (
    <div className="relative" style={{ height }}>
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie
            data={data}
            dataKey="value"
            nameKey="name"
            cx="50%"
            cy="50%"
            innerRadius={innerRadius}
            outerRadius={outerRadius}
            paddingAngle={2}
            startAngle={90}
            endAngle={-270}
          >
            {data.map((entry, idx) => (
              <Cell key={idx} fill={entry.color} stroke="transparent" />
            ))}
          </Pie>
          <Tooltip content={<CustomTooltip />} />
        </PieChart>
      </ResponsiveContainer>

      {(centerLabel || centerValue) && (
        <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
          {centerValue && (
            <span className="text-xl font-bold text-white">{centerValue}</span>
          )}
          {centerLabel && (
            <span className="text-[10px] text-text-secondary mt-0.5">{centerLabel}</span>
          )}
        </div>
      )}
    </div>
  );
}
