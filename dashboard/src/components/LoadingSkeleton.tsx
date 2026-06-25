interface Props {
  readonly height?: number;
  readonly width?: string;
  readonly borderRadius?: number;
}

export function LoadingSkeleton({ height = 20, width = '100%', borderRadius = 4 }: Props) {
  return (
    <div style={{
      height, width, borderRadius,
      background: 'linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%)',
      backgroundSize: '200% 100%',
      animation: 'shimmer 1.5s infinite',
    }} />
  );
}
