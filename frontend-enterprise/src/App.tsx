import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { AppLayout } from './components/layout/AppLayout';
import { Playground } from './pages/Playground';
import { Dashboard } from './pages/Dashboard';
import { Providers } from './pages/Providers';
import { Analytics } from './pages/Analytics';
import { SemanticCache } from './pages/SemanticCache';
import { Health } from './pages/Health';
import { ApiKeys } from './pages/ApiKeys';
import { Settings } from './pages/Settings';

const pageVariants = {
  initial: { opacity: 0, y: 6 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -6 },
};

function PageWrapper({ children }: { children: React.ReactNode }) {
  return (
    <motion.div
      variants={pageVariants}
      initial="initial"
      animate="animate"
      exit="exit"
      transition={{ duration: 0.2 }}
      className="h-full"
    >
      {children}
    </motion.div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppLayout />}>
          <Route
            path="/"
            element={
              <PageWrapper>
                <Playground />
              </PageWrapper>
            }
          />
          <Route
            path="/dashboard"
            element={
              <PageWrapper>
                <Dashboard />
              </PageWrapper>
            }
          />
          <Route
            path="/providers"
            element={
              <PageWrapper>
                <Providers />
              </PageWrapper>
            }
          />
          <Route
            path="/analytics"
            element={
              <PageWrapper>
                <Analytics />
              </PageWrapper>
            }
          />
          <Route
            path="/cache"
            element={
              <PageWrapper>
                <SemanticCache />
              </PageWrapper>
            }
          />
          <Route
            path="/health"
            element={
              <PageWrapper>
                <Health />
              </PageWrapper>
            }
          />
          <Route
            path="/api-keys"
            element={
              <PageWrapper>
                <ApiKeys />
              </PageWrapper>
            }
          />
          <Route
            path="/settings"
            element={
              <PageWrapper>
                <Settings />
              </PageWrapper>
            }
          />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
