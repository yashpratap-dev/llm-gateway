import React from 'react';
import { motion } from 'framer-motion';
import { CheckCircle, AlertTriangle, XCircle, RefreshCw } from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Progress } from '../components/ui/Progress';
import { healthChecks } from '../mock/health';

const statusIcon = (status: string) => {
  if (status === 'up') return <CheckCircle size={16} className="text-status-success" />;
  if (status === 'degraded') return <AlertTriangle size={16} className="text-status-warning" />;
  return <XCircle size={16} className="text-status-danger" />;
};

const statusBadge = (status: string) => {
  if (status === 'up') return <Badge variant="success" size="sm" dot>Healthy</Badge>;
  if (status === 'degraded') return <Badge variant="warning" size="sm" dot>Degraded</Badge>;
  return <Badge variant="danger" size="sm" dot>Down</Badge>;
};

export function Health() {
  const upCount = healthChecks.filter(h => h.status === 'up').length;
  const degradedCount = healthChecks.filter(h => h.status === 'degraded').length;
  const downCount = healthChecks.filter(h => h.status === 'down').length;

  const overallStatus =
    downCount > 0 ? 'degraded' : degradedCount > 0 ? 'degraded' : 'operational';

  return (
    <div className="h-screen overflow-y-auto">
      <div className="px-6 py-5 border-b border-white/8 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-white">Health</h1>
          <p className="text-xs text-text-secondary mt-0.5">
            Real-time service health and uptime monitoring
          </p>
        </div>
        <button className="flex items-center gap-1.5 px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-xs text-text-secondary hover:border-white/20 transition-colors">
          <RefreshCw size={12} />
          Refresh
        </button>
      </div>

      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="p-6 space-y-6"
      >
        {/* Overall status banner */}
        <Card padding="md" className={overallStatus === 'operational' ? 'border-status-success/20 bg-status-success/5' : 'border-status-warning/20 bg-status-warning/5'}>
          <div className="flex items-center gap-3">
            {overallStatus === 'operational' ? (
              <CheckCircle size={24} className="text-status-success" />
            ) : (
              <AlertTriangle size={24} className="text-status-warning" />
            )}
            <div>
              <p className="text-sm font-bold text-white">
                {overallStatus === 'operational' ? 'All Systems Operational' : 'Partial Degradation Detected'}
              </p>
              <p className="text-xs text-text-secondary">
                {upCount} healthy, {degradedCount} degraded, {downCount} down — Last checked 2s ago
              </p>
            </div>
            <div className="ml-auto flex gap-4">
              <div className="text-center">
                <p className="text-lg font-bold text-status-success">{upCount}</p>
                <p className="text-[10px] text-text-secondary">Healthy</p>
              </div>
              <div className="text-center">
                <p className="text-lg font-bold text-status-warning">{degradedCount}</p>
                <p className="text-[10px] text-text-secondary">Degraded</p>
              </div>
              <div className="text-center">
                <p className="text-lg font-bold text-status-danger">{downCount}</p>
                <p className="text-[10px] text-text-secondary">Down</p>
              </div>
            </div>
          </div>
        </Card>

        {/* Health grid */}
        <div className="grid grid-cols-2 gap-4">
          {healthChecks.map((check, i) => (
            <motion.div
              key={check.service}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.04 }}
            >
              <Card hover padding="md">
                <div className="flex items-start justify-between mb-3">
                  <div className="flex items-center gap-2">
                    {statusIcon(check.status)}
                    <h3 className="text-sm font-semibold text-white">{check.service}</h3>
                  </div>
                  {statusBadge(check.status)}
                </div>

                <p className="text-xs text-text-secondary mb-3">{check.details}</p>

                <div className="grid grid-cols-3 gap-2 text-center mb-3">
                  <div>
                    <p className="text-[10px] text-text-secondary">Latency</p>
                    <p className="text-sm font-semibold text-white">
                      {check.latency === 0 ? '—' : `${check.latency}ms`}
                    </p>
                  </div>
                  <div>
                    <p className="text-[10px] text-text-secondary">Uptime</p>
                    <p
                      className="text-sm font-semibold"
                      style={{
                        color:
                          check.uptime >= 99.9
                            ? '#18D46B'
                            : check.uptime >= 95
                            ? '#FFC857'
                            : '#FF5A5A',
                      }}
                    >
                      {check.uptime.toFixed(2)}%
                    </p>
                  </div>
                  <div>
                    <p className="text-[10px] text-text-secondary">Checked</p>
                    <p className="text-sm font-semibold text-white">{check.lastChecked}</p>
                  </div>
                </div>

                <Progress
                  value={check.uptime}
                  color={check.uptime >= 99 ? '#18D46B' : check.uptime >= 95 ? '#FFC857' : '#FF5A5A'}
                  height={3}
                  animated
                />
              </Card>
            </motion.div>
          ))}
        </div>
      </motion.div>
    </div>
  );
}
