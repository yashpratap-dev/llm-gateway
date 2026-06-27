import React from 'react';

interface ProgressProps {
  value: number;
  max?: number;
  color?: string;
  height?: number;
  showLabel?: boolean;
  className?: string;
  animated?: boolean;
}

export function Progress({
  value,
  max = 100,
  color = '#19D3FF',
  height = 4,
  showLabel = false,
  className = '',
  animated = false,
}: ProgressProps) {
  const percentage = Math.min((value / max) * 100, 100);

  return (
    <div className={`w-full ${className}`}>
      {showLabel && (
        <div className="flex justify-between mb-1">
          <span className="text-xs text-text-secondary">{value.toLocaleString()}</span>
          <span className="text-xs text-text-secondary">{percentage.toFixed(1)}%</span>
        </div>
      )}
      <div
        className="w-full rounded-full overflow-hidden"
        style={{ height, background: 'rgba(255,255,255,0.06)' }}
      >
        <div
          className={`h-full rounded-full ${animated ? 'transition-all duration-700' : ''}`}
          style={{
            width: `${percentage}%`,
            background: color,
          }}
        />
      </div>
    </div>
  );
}
