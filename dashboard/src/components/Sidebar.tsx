import { NavLink } from 'react-router-dom';
import { PAGES } from '../constants/pages';

const NAV_ITEMS = [
  { to: `/${PAGES.OVERVIEW}`,   label: '▦ Overview'   },
  { to: `/${PAGES.PROVIDERS}`,  label: '⬡ Providers'  },
  { to: `/${PAGES.TENANTS}`,    label: '⊞ Tenants'    },
  { to: `/${PAGES.ANALYTICS}`,  label: '↗ Analytics'  },
] as const;

export function Sidebar() {
  return (
    <div style={{
      width: 220, minHeight: '100vh', flexShrink: 0,
      background: '#fff', borderRight: '1px solid #e0e0e0',
      display: 'flex', flexDirection: 'column',
    }}>
      <div style={{ padding: '1.5rem', borderBottom: '1px solid #e0e0e0' }}>
        <div style={{ fontSize: '0.65rem', color: '#999', letterSpacing: '0.1em', textTransform: 'uppercase' }}>LLM Gateway</div>
        <div style={{ fontSize: '1.1rem', fontWeight: 700, color: '#1a1a1a', marginTop: 2 }}>Admin</div>
      </div>
      <nav style={{ padding: '0.75rem 0', flex: 1 }}>
        {NAV_ITEMS.map(item => (
          <NavLink key={item.to} to={item.to} style={({ isActive }) => ({
            display: 'block', padding: '0.6rem 1.5rem',
            color: isActive ? '#1a73e8' : '#444',
            fontWeight: isActive ? 600 : 400,
            textDecoration: 'none', fontSize: '0.875rem',
            borderLeft: isActive ? '3px solid #1a73e8' : '3px solid transparent',
            background: isActive ? '#f0f4ff' : 'transparent',
          })}>
            {item.label}
          </NavLink>
        ))}
      </nav>
      <div style={{ padding: '1rem 1.5rem', borderTop: '1px solid #e0e0e0' }}>
        <a href="http://localhost:3001" target="_blank" rel="noreferrer"
          style={{ fontSize: '0.8rem', color: '#1a73e8', textDecoration: 'none' }}>
          → Open Playground
        </a>
      </div>
    </div>
  );
}
