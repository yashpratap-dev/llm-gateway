import React, { useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  MessageSquare,
  BarChart2,
  Settings,
  ChevronLeft,
  ChevronRight,
  Database,
  Activity,
  Key,
  Heart,
  Cpu,
  Circle,
} from 'lucide-react';
import { Progress } from '../ui/Progress';

const navItems = [
  { path: '/', label: 'Playground', icon: MessageSquare },
  { path: '/providers', label: 'Providers', icon: Cpu },
  { path: '/analytics', label: 'Analytics', icon: BarChart2 },
  { path: '/cache', label: 'Semantic Cache', icon: Database },
  { path: '/api-keys', label: 'API Keys', icon: Key },
  { path: '/health', label: 'Health', icon: Heart },
  { path: '/settings', label: 'Settings', icon: Settings },
];

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
}

export function Sidebar({ collapsed, onToggle }: SidebarProps) {
  const location = useLocation();

  return (
    <motion.aside
      animate={{ width: collapsed ? 64 : 220 }}
      transition={{ duration: 0.25, ease: 'easeInOut' }}
      className="flex-shrink-0 h-screen bg-bg-card border-r border-white/8 flex flex-col overflow-hidden relative z-20"
    >
      {/* Logo */}
      <div className="flex items-center gap-3 px-4 py-5 border-b border-white/8 flex-shrink-0">
        <HexagonLogo />
        <AnimatePresence>
          {!collapsed && (
            <motion.div
              initial={{ opacity: 0, x: -8 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -8 }}
              transition={{ duration: 0.18 }}
              className="overflow-hidden whitespace-nowrap"
            >
              <span className="text-sm font-bold text-white tracking-tight">LLM Gateway</span>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Tenant */}
      <AnimatePresence>
        {!collapsed && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="px-4 py-3 border-b border-white/8 flex-shrink-0"
          >
            <div className="flex items-center gap-2">
              <Circle size={6} className="fill-status-success text-status-success flex-shrink-0" />
              <span className="text-xs font-medium text-text-primary truncate">Acme Corp</span>
              <span className="text-[10px] text-text-secondary ml-auto flex-shrink-0">Production</span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Nav */}
      <nav className="flex-1 px-2 py-3 space-y-0.5 overflow-y-auto overflow-x-hidden">
        {navItems.map(({ path, label, icon: Icon }) => {
          const isActive =
            path === '/'
              ? location.pathname === '/'
              : location.pathname.startsWith(path);

          return (
            <NavLink key={path} to={path}>
              <motion.div
                whileHover={{ x: 2 }}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors relative ${
                  isActive
                    ? 'text-accent-primary bg-accent-primary/8'
                    : 'text-text-secondary hover:text-text-primary hover:bg-white/5'
                }`}
              >
                {isActive && (
                  <motion.div
                    layoutId="sidebar-active"
                    className="absolute left-0 top-1 bottom-1 w-0.5 bg-accent-primary rounded-full"
                  />
                )}
                <Icon size={16} className="flex-shrink-0" />
                <AnimatePresence>
                  {!collapsed && (
                    <motion.span
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      exit={{ opacity: 0 }}
                      className="truncate"
                    >
                      {label}
                    </motion.span>
                  )}
                </AnimatePresence>
              </motion.div>
            </NavLink>
          );
        })}
      </nav>

      {/* Bottom section */}
      <div className="flex-shrink-0 border-t border-white/8 p-3 space-y-3">
        {/* Usage */}
        <AnimatePresence>
          {!collapsed && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="space-y-1"
            >
              <div className="flex justify-between items-center">
                <span className="text-[10px] text-text-secondary">Monthly Usage</span>
                <span className="text-[10px] text-accent-primary font-mono">85%</span>
              </div>
              <Progress value={85} color="#19D3FF" height={3} animated />
              <div className="flex justify-between items-center">
                <span className="text-[10px] text-text-secondary">$109,165 / $128K</span>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* System status */}
        <AnimatePresence>
          {!collapsed && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="flex items-center gap-2"
            >
              <Activity size={12} className="text-status-success flex-shrink-0" />
              <span className="text-[10px] text-text-secondary">System Status</span>
              <span className="text-[10px] text-status-success ml-auto font-medium">Operational</span>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Collapse button */}
        <button
          onClick={onToggle}
          className="w-full flex items-center justify-center gap-2 py-1.5 rounded-lg text-text-secondary hover:text-text-primary hover:bg-white/5 transition-colors text-xs"
        >
          {collapsed ? <ChevronRight size={14} /> : (
            <>
              <ChevronLeft size={14} />
              <span>Collapse</span>
            </>
          )}
        </button>

        {/* User avatar */}
        <div className={`flex items-center gap-2 ${collapsed ? 'justify-center' : ''}`}>
          <div className="w-7 h-7 rounded-full bg-accent-primary/20 border border-accent-primary/30 flex items-center justify-center flex-shrink-0">
            <span className="text-[10px] font-semibold text-accent-primary">YP</span>
          </div>
          <AnimatePresence>
            {!collapsed && (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="overflow-hidden"
              >
                <p className="text-xs font-medium text-text-primary truncate">Yash Pratap</p>
                <p className="text-[10px] text-text-secondary truncate">Admin</p>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </motion.aside>
  );
}

function HexagonLogo() {
  return (
    <svg
      width="28"
      height="28"
      viewBox="0 0 28 28"
      fill="none"
      className="flex-shrink-0"
    >
      <polygon
        points="14,2 24,7.5 24,20.5 14,26 4,20.5 4,7.5"
        fill="none"
        stroke="#19D3FF"
        strokeWidth="1.5"
      />
      <polygon
        points="14,7 20,10.5 20,17.5 14,21 8,17.5 8,10.5"
        fill="#19D3FF"
        fillOpacity="0.15"
      />
      <circle cx="14" cy="14" r="3" fill="#19D3FF" />
    </svg>
  );
}
