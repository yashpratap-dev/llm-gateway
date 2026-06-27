import React from 'react';
import { LineChart, Line, ResponsiveContainer, Tooltip } from 'recharts';

interface SparklineChartProps {
  data: number[];
  color?: string;
  width?: number | string;
  height?: number;
  showTooltip?: boolean;
}

export function SparklineChart({
  data,
  color = '#19D3FF',
  width = '100%',
  height = 40,
  showTooltip = false,
}: SparklineChartProps) {
  const chartData = data.map((v, i) => ({ i, v }));

  return (
    <div style={{ width, height }}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={chartData} margin={{ top: 2, right: 2, bottom: 2, left: 2 }}>
          <Line
            type="monotone"
            dataKey="v"
            stroke={color}
            strokeWidth={1.5}
            dot={false}
            isAnimationActive={false}
          />
          {showTooltip && <Tooltip wrapperStyle={{ display: 'none' }} />}
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
