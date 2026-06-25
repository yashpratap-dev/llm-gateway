import { LoadingSkeleton } from './LoadingSkeleton';

interface Props {
  readonly label: string;
  readonly value: string | number;
  readonly sub?: string;
  readonly color?: string;
  readonly loading?: boolean;
}

export function StatCard({ label, value, sub, color = '#1a73e8', loading = false }: Props) {
  return (
    <div style={{
      background: '#fff', border: '1px solid #e0e0e0',
      borderRadius: 8, padding: '1.5rem', flex: 1, minWidth: 160,
    }}>
      <div style={{ fontSize: '0.7rem', color: '#666', marginBottom: 8, textTransform: 'uppercase', letterSpacing: '0.05em', fontWeight: 600 }}>
        {label}
      </div>
      {loading
        ? <LoadingSkeleton height={36} borderRadius={4} />
        : <>
            <div style={{ fontSize: '1.875rem', fontWeight: 700, color, lineHeight: 1.2 }}>{value}</div>
            {sub && <div style={{ fontSize: '0.75rem', color: '#888', marginTop: 4 }}>{sub}</div>}
          </>
      }
    </div>
  );
}
