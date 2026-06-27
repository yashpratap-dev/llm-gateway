import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Search, Trash2, RefreshCw } from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Progress } from '../components/ui/Progress';
import { DonutChart } from '../components/charts/DonutChart';
import { cacheEntries, cacheStats } from '../mock/cache';

export function SemanticCache() {
  const [search, setSearch] = useState('');

  const filtered = cacheEntries.filter(e =>
    e.query.toLowerCase().includes(search.toLowerCase()) ||
    e.model.toLowerCase().includes(search.toLowerCase())
  );

  const formatDate = (iso: string) => new Date(iso).toLocaleDateString('en-US', {
    month: 'short', day: 'numeric', year: 'numeric'
  });

  const formatSize = (bytes: number) =>
    bytes >= 1024 ? `${(bytes / 1024).toFixed(1)} KB` : `${bytes} B`;

  return (
    <div className="h-screen overflow-y-auto">
      <div className="px-6 py-5 border-b border-white/8">
        <h1 className="text-xl font-bold text-white">Semantic Cache</h1>
        <p className="text-xs text-text-secondary mt-0.5">
          Manage vector similarity cache entries and monitor hit rates
        </p>
      </div>

      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="p-6 space-y-6"
      >
        {/* Stats row */}
        <div className="grid grid-cols-5 gap-4">
          {[
            { label: 'Total Entries', value: cacheStats.totalEntries.toLocaleString() },
            { label: 'Cache Size', value: cacheStats.totalSize },
            { label: 'Avg Similarity', value: cacheStats.avgSimilarity.toFixed(3) },
            { label: 'Evicted Today', value: String(cacheStats.evictedToday) },
            { label: 'Cost Saved', value: cacheStats.hitsSavedCost },
          ].map(stat => (
            <Card key={stat.label} padding="md" hover>
              <p className="text-[10px] text-text-secondary uppercase tracking-wider">{stat.label}</p>
              <p className="text-lg font-bold text-white mt-1">{stat.value}</p>
            </Card>
          ))}
        </div>

        {/* Cache overview + donut */}
        <div className="grid grid-cols-3 gap-4">
          <div className="col-span-2 space-y-4">
            {/* Search & table */}
            <Card padding="none">
              <div className="p-4 border-b border-white/8 flex items-center gap-3">
                <div className="relative flex-1">
                  <Search size={14} className="absolute left-3 top-2.5 text-text-secondary" />
                  <input
                    type="text"
                    value={search}
                    onChange={e => setSearch(e.target.value)}
                    placeholder="Search by query or model..."
                    className="w-full pl-9 pr-4 py-2 bg-white/5 border border-white/10 rounded-lg text-sm text-text-primary placeholder-text-secondary focus:outline-none focus:border-accent-primary/40"
                  />
                </div>
                <button className="flex items-center gap-1.5 px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-xs text-text-secondary hover:border-white/20 transition-colors">
                  <RefreshCw size={12} />
                  Refresh
                </button>
              </div>

              <table className="w-full text-xs">
                <thead>
                  <tr className="border-b border-white/8">
                    <th className="text-left px-4 py-2.5 text-text-secondary font-medium">Query</th>
                    <th className="text-left px-4 py-2.5 text-text-secondary font-medium">Model</th>
                    <th className="text-center px-4 py-2.5 text-text-secondary font-medium">Similarity</th>
                    <th className="text-center px-4 py-2.5 text-text-secondary font-medium">Hits</th>
                    <th className="text-left px-4 py-2.5 text-text-secondary font-medium">Expires</th>
                    <th className="text-center px-4 py-2.5 text-text-secondary font-medium">Size</th>
                    <th className="px-4 py-2.5" />
                  </tr>
                </thead>
                <tbody>
                  {filtered.map(entry => (
                    <tr
                      key={entry.id}
                      className="border-b border-white/4 hover:bg-white/3 transition-colors"
                    >
                      <td className="px-4 py-3 max-w-0">
                        <p className="text-text-primary truncate max-w-[200px]">{entry.query}</p>
                      </td>
                      <td className="px-4 py-3">
                        <code className="text-[10px] text-accent-primary font-mono">{entry.model}</code>
                      </td>
                      <td className="px-4 py-3 text-center">
                        <div className="flex flex-col items-center gap-1">
                          <span className="text-text-primary font-semibold">{entry.similarity.toFixed(2)}</span>
                          <div className="w-12 h-1 rounded-full overflow-hidden bg-white/6">
                            <div
                              className="h-full bg-accent-primary rounded-full"
                              style={{ width: `${entry.similarity * 100}%` }}
                            />
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-center">
                        <Badge variant="info" size="sm">{entry.hits}</Badge>
                      </td>
                      <td className="px-4 py-3 text-text-secondary">{formatDate(entry.expiresAt)}</td>
                      <td className="px-4 py-3 text-center text-text-secondary">{formatSize(entry.size)}</td>
                      <td className="px-4 py-3 text-center">
                        <button className="p-1.5 rounded hover:bg-status-danger/10 text-text-secondary hover:text-status-danger transition-colors">
                          <Trash2 size={12} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </Card>
          </div>

          {/* Donut + stats */}
          <div className="space-y-4">
            <Card padding="md">
              <h2 className="text-sm font-semibold text-text-primary mb-2">Hit Rate Overview</h2>
              <DonutChart
                data={[
                  { name: 'Semantic Hit', value: 62.33, color: '#19D3FF' },
                  { name: 'Exact Hit', value: 15.81, color: '#6AE3FF' },
                  { name: 'Miss', value: 21.86, color: 'rgba(255,255,255,0.06)' },
                ]}
                centerValue="78.14%"
                centerLabel="Total Hit Rate"
                height={160}
                innerRadius={48}
                outerRadius={68}
              />
              <div className="mt-3 space-y-2">
                {[
                  { label: 'Semantic Hits', value: '62.33%', color: '#19D3FF' },
                  { label: 'Exact Hits', value: '15.81%', color: '#6AE3FF' },
                  { label: 'Misses', value: '21.86%', color: 'rgba(255,255,255,0.2)' },
                ].map(item => (
                  <div key={item.label} className="flex items-center gap-2 text-xs">
                    <div className="w-2 h-2 rounded-full flex-shrink-0" style={{ background: item.color }} />
                    <span className="text-text-secondary flex-1">{item.label}</span>
                    <span className="text-text-primary font-medium">{item.value}</span>
                  </div>
                ))}
              </div>
            </Card>

            <Card padding="md">
              <h2 className="text-sm font-semibold text-text-primary mb-3">Similarity Threshold</h2>
              <div className="space-y-3">
                <div>
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-text-secondary">Current threshold</span>
                    <span className="text-accent-primary font-mono font-semibold">0.85</span>
                  </div>
                  <input
                    type="range"
                    min={0.5}
                    max={1.0}
                    step={0.01}
                    defaultValue={0.85}
                    className="w-full"
                  />
                  <div className="flex justify-between text-[10px] text-text-secondary mt-0.5">
                    <span>0.50</span>
                    <span>1.00</span>
                  </div>
                </div>
                <p className="text-[10px] text-text-secondary leading-relaxed">
                  Queries with cosine similarity above this threshold will be served from cache.
                </p>
              </div>
            </Card>

            <Card padding="md">
              <h2 className="text-sm font-semibold text-text-primary mb-3">Eviction Policy</h2>
              <div className="space-y-2 text-xs">
                <div className="flex justify-between">
                  <span className="text-text-secondary">Strategy</span>
                  <Badge variant="info" size="sm">TTL + LRU</Badge>
                </div>
                <div className="flex justify-between">
                  <span className="text-text-secondary">Default TTL</span>
                  <span className="text-text-primary">30 days</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-text-secondary">Max Size</span>
                  <span className="text-text-primary">10 GB</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-text-secondary">Scheduler</span>
                  <Badge variant="success" size="sm" dot>Active</Badge>
                </div>
              </div>
            </Card>
          </div>
        </div>
      </motion.div>
    </div>
  );
}
