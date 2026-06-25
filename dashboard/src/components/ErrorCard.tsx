interface Props {
  readonly message: string;
  readonly onRetry?: () => void;
}

export function ErrorCard({ message, onRetry }: Props) {
  return (
    <div style={{
      background: '#fce8e6', border: '1px solid #f28b82',
      borderRadius: 8, padding: '0.875rem 1.25rem',
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      marginBottom: '1rem',
    }}>
      <span style={{ color: '#c5221f', fontSize: '0.875rem' }}>⚠ {message}</span>
      {onRetry && (
        <button onClick={onRetry} style={{
          background: '#c5221f', color: '#fff', border: 'none',
          borderRadius: 4, padding: '0.3rem 0.75rem',
          fontSize: '0.8rem', cursor: 'pointer',
        }}>
          Retry
        </button>
      )}
    </div>
  );
}
