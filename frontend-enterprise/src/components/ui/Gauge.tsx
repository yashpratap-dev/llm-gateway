import React from 'react';

interface GaugeProps {
  value: number;
  max: number;
  label: string;
  unit?: string;
  color?: string;
  size?: number;
}

function formatValue(value: number): string {
  if (value >= 1_000_000) {
    return (value / 1_000_000).toFixed(1) + 'M';
  }
  if (value >= 1_000) {
    return (value / 1_000).toFixed(1) + 'K';
  }
  return value.toString();
}

export function Gauge({
  value,
  max,
  label,
  unit = '',
  color = '#19D3FF',
  size = 120,
}: GaugeProps) {
  const radius = (size - 16) / 2;
  const circumference = Math.PI * radius; // half circle = PI * r
  const percentage = Math.min(value / max, 1);
  const strokeDashoffset = circumference * (1 - percentage);

  const cx = size / 2;
  const cy = size / 2 + 10; // offset center down to make room for semi-circle

  return (
    <div className="flex flex-col items-center">
      <div style={{ width: size, height: size * 0.6 }} className="relative overflow-hidden">
        <svg
          width={size}
          height={size}
          viewBox={`0 0 ${size} ${size}`}
          style={{ transform: 'rotate(180deg)', transformOrigin: `${cx}px ${cy}px` }}
        >
          {/* Background arc */}
          <path
            d={`M ${16 / 2} ${cy} A ${radius} ${radius} 0 0 1 ${size - 16 / 2} ${cy}`}
            fill="none"
            stroke="rgba(255,255,255,0.06)"
            strokeWidth="8"
            strokeLinecap="round"
          />
          {/* Value arc */}
          <path
            d={`M ${16 / 2} ${cy} A ${radius} ${radius} 0 0 1 ${size - 16 / 2} ${cy}`}
            fill="none"
            stroke={color}
            strokeWidth="8"
            strokeLinecap="round"
            strokeDasharray={circumference}
            strokeDashoffset={strokeDashoffset}
            style={{ transition: 'stroke-dashoffset 0.6s ease' }}
          />
        </svg>
        {/* Center text */}
        <div
          className="absolute bottom-0 left-0 right-0 flex flex-col items-center"
          style={{ transform: 'translateY(4px)' }}
        >
          <span className="text-sm font-semibold text-white">
            {formatValue(value)}
          </span>
          <span className="text-[10px] text-text-secondary">{unit}</span>
        </div>
      </div>
      <div className="flex justify-between w-full mt-1 px-1">
        <span className="text-[10px] text-text-secondary">0</span>
        <span className="text-[10px] text-text-secondary font-medium">{label}</span>
        <span className="text-[10px] text-text-secondary">{formatValue(max)}</span>
      </div>
    </div>
  );
}
