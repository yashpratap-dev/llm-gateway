import { useNavigate } from 'react-router-dom';
import { PAGES } from '../constants/pages';

export function NotFound() {
  const navigate = useNavigate();
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '60vh', gap: '1rem' }}>
      <div style={{ fontSize: '3rem', color: '#e0e0e0' }}>404</div>
      <div style={{ color: '#666' }}>Page not found</div>
      <button
        onClick={() => navigate(`/${PAGES.OVERVIEW}`)}
        style={{
          background: '#1a73e8', color: '#fff', border: 'none',
          borderRadius: 4, padding: '0.5rem 1.25rem', cursor: 'pointer',
        }}
      >
        Go to Overview
      </button>
    </div>
  );
}
