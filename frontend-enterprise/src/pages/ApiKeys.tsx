import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Plus, Copy, Trash2, Eye, EyeOff } from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { apiKeys } from '../mock/apikeys';

export function ApiKeys() {
  const [revealed, setRevealed] = useState<Set<string>>(new Set());

  const toggleReveal = (id: string) => {
    setRevealed(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const copyKey = (key: string) => {
    navigator.clipboard.writeText(key).catch(() => {});
  };

  const formatDate = (iso: string) =>
    new Date(iso).toLocaleDateString('en-US', {
      month: 'short', day: 'numeric', year: 'numeric',
    });

  return (
    <div className="h-screen overflow-y-auto">
      <div className="px-6 py-5 border-b border-white/8 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-white">API Keys</h1>
          <p className="text-xs text-text-secondary mt-0.5">
            Manage gateway API keys and access control
          </p>
        </div>
        <Button variant="primary" size="sm" icon={<Plus size={14} />}>
          Create Key
        </Button>
      </div>

      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="p-6 space-y-4"
      >
        {/* Summary */}
        <div className="grid grid-cols-3 gap-4">
          {[
            { label: 'Active Keys', value: apiKeys.filter(k => k.status === 'active').length.toString() },
            { label: 'Revoked Keys', value: apiKeys.filter(k => k.status === 'revoked').length.toString() },
            { label: 'Total Requests', value: apiKeys.reduce((sum, k) => sum + k.requests, 0).toLocaleString() },
          ].map(stat => (
            <Card key={stat.label} padding="md">
              <p className="text-[10px] text-text-secondary uppercase tracking-wider">{stat.label}</p>
              <p className="text-2xl font-bold text-white mt-1">{stat.value}</p>
            </Card>
          ))}
        </div>

        {/* Keys table */}
        <Card padding="none">
          <table className="w-full text-xs">
            <thead>
              <tr className="border-b border-white/8">
                <th className="text-left px-4 py-3 text-text-secondary font-medium">Name</th>
                <th className="text-left px-4 py-3 text-text-secondary font-medium">Key</th>
                <th className="text-left px-4 py-3 text-text-secondary font-medium">Tenant</th>
                <th className="text-left px-4 py-3 text-text-secondary font-medium">Scopes</th>
                <th className="text-center px-4 py-3 text-text-secondary font-medium">Requests</th>
                <th className="text-left px-4 py-3 text-text-secondary font-medium">Last Used</th>
                <th className="text-center px-4 py-3 text-text-secondary font-medium">Status</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody>
              {apiKeys.map(key => (
                <motion.tr
                  key={key.id}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className={`border-b border-white/4 transition-colors hover:bg-white/3 ${
                    key.status === 'revoked' ? 'opacity-50' : ''
                  }`}
                >
                  <td className="px-4 py-3 font-medium text-text-primary">{key.name}</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <code className="font-mono text-[11px] text-accent-primary">
                        {revealed.has(key.id) ? key.key.replace(/•/g, 'X') : key.key}
                      </code>
                      <button
                        onClick={() => toggleReveal(key.id)}
                        className="p-1 rounded hover:bg-white/8 text-text-secondary hover:text-white transition-colors"
                      >
                        {revealed.has(key.id) ? <EyeOff size={11} /> : <Eye size={11} />}
                      </button>
                      <button
                        onClick={() => copyKey(key.key)}
                        className="p-1 rounded hover:bg-white/8 text-text-secondary hover:text-white transition-colors"
                      >
                        <Copy size={11} />
                      </button>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-text-secondary font-mono">{key.tenant}</td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-1">
                      {key.scopes.map(scope => (
                        <Badge key={scope} variant="neutral" size="sm">{scope}</Badge>
                      ))}
                    </div>
                  </td>
                  <td className="px-4 py-3 text-center text-text-primary font-mono">
                    {key.requests.toLocaleString()}
                  </td>
                  <td className="px-4 py-3 text-text-secondary">{formatDate(key.lastUsed)}</td>
                  <td className="px-4 py-3 text-center">
                    {key.status === 'active' ? (
                      <Badge variant="success" size="sm" dot>Active</Badge>
                    ) : (
                      <Badge variant="danger" size="sm" dot>Revoked</Badge>
                    )}
                  </td>
                  <td className="px-4 py-3 text-center">
                    {key.status === 'active' && (
                      <button className="p-1.5 rounded hover:bg-status-danger/10 text-text-secondary hover:text-status-danger transition-colors">
                        <Trash2 size={12} />
                      </button>
                    )}
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </Card>
      </motion.div>
    </div>
  );
}
