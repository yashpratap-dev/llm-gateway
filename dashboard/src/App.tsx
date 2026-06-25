import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Sidebar } from './components/Sidebar';
import { Overview } from './pages/Overview';
import { Providers } from './pages/Providers';
import { Tenants } from './pages/Tenants';
import { Analytics } from './pages/Analytics';
import { NotFound } from './pages/NotFound';
import { PAGES } from './constants/pages';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 10_000,
      retry: 2,
      retryDelay: (attempt) => Math.min(1000 * 2 ** attempt, 10_000),
    },
  },
});

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <div style={{ display: 'flex', minHeight: '100vh', background: '#f8f9fa' }}>
          <Sidebar />
          <main style={{ flex: 1, padding: '2rem', overflowY: 'auto', maxWidth: 'calc(100vw - 220px)' }}>
            <Routes>
              <Route path="/" element={<Navigate to={`/${PAGES.OVERVIEW}`} replace />} />
              <Route path={`/${PAGES.OVERVIEW}`}   element={<Overview />} />
              <Route path={`/${PAGES.PROVIDERS}`}  element={<Providers />} />
              <Route path={`/${PAGES.TENANTS}`}    element={<Tenants />} />
              <Route path={`/${PAGES.ANALYTICS}`}  element={<Analytics />} />
              <Route path="*" element={<NotFound />} />
            </Routes>
          </main>
        </div>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
