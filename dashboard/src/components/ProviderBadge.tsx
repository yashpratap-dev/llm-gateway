const BADGE_COLORS: Readonly<Record<string, { bg: string; text: string }>> = {
  UP:        { bg: '#e6f4ea', text: '#1e7e34' },
  DOWN:      { bg: '#fce8e6', text: '#c5221f' },
  CLOSED:    { bg: '#e6f4ea', text: '#1e7e34' },
  OPEN:      { bg: '#fce8e6', text: '#c5221f' },
  HALF_OPEN: { bg: '#fff3e0', text: '#e65100' },
  UNKNOWN:   { bg: '#f5f5f5', text: '#666'    },
};

interface Props {
  readonly status: string;
}

export function ProviderBadge({ status }: Props) {
  const style = BADGE_COLORS[status] ?? BADGE_COLORS['UNKNOWN']!;
  return (
    <span style={{
      background: style.bg, color: style.text,
      padding: '2px 10px', borderRadius: 12,
      fontSize: '0.75rem', fontWeight: 600,
    }}>
      {status}
    </span>
  );
}
