import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import ReactMarkdown from 'react-markdown';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import {
  Send,
  Paperclip,
  Mic,
  Upload,
  Square,
  RotateCcw,
  Copy,
  ArrowDown,
  Save,
  Download,
  ChevronDown,
} from 'lucide-react';
import { AIOrb } from '../components/orb/AIOrb';
import { TelemetryRail } from '../components/layout/TelemetryRail';
import { Tabs } from '../components/ui/Tabs';
import { Toggle } from '../components/ui/Toggle';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { useStreamingChat } from '../hooks/useStreamingChat';
import { useGatewayEvents } from '../hooks/useGatewayEvents';
import { initialMessages, defaultConfig, telemetryData, providerModels } from '../mock/playground';

const providerTabs = [
  { id: 'openai', label: 'OpenAI' },
  { id: 'claude', label: 'Claude' },
  { id: 'groq', label: 'Groq' },
  { id: 'gemini', label: 'Gemini' },
];

export function Playground() {
  const [config, setConfig] = useState(defaultConfig);
  const [input, setInput] = useState('');
  const [autoScroll, setAutoScroll] = useState(true);
  const chatEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const events = useGatewayEvents();

  const { messages, isStreaming, streamingContent, sendMessage, stopStreaming, regenerate } =
    useStreamingChat(initialMessages);

  useEffect(() => {
    if (autoScroll && chatEndRef.current) {
      chatEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, streamingContent, autoScroll]);

  const handleSend = () => {
    if (!input.trim() || isStreaming) return;
    sendMessage(input);
    setInput('');
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleTextareaChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInput(e.target.value);
    e.target.style.height = 'auto';
    e.target.style.height = Math.min(e.target.scrollHeight, 160) + 'px';
  };

  const copyMessage = (content: string) => {
    navigator.clipboard.writeText(content).catch(() => {});
  };

  const currentModels = providerModels[config.provider] || [];

  return (
    <div className="flex h-screen overflow-hidden">
      {/* Main Playground */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="flex-1 flex flex-col overflow-hidden"
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-white/8 flex-shrink-0">
          <div>
            <h1 className="text-xl font-bold text-white">Playground</h1>
            <p className="text-xs text-text-secondary mt-0.5">
              Test prompts across providers with real-time telemetry
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Button variant="ghost" size="sm" icon={<Save size={14} />}>
              Save
            </Button>
            <Button variant="ghost" size="sm" icon={<Download size={14} />}>
              Export
            </Button>
          </div>
        </div>

        {/* Config bar */}
        <div className="flex items-start gap-4 px-6 py-3 border-b border-white/8 flex-shrink-0 flex-wrap">
          {/* Provider tabs */}
          <Tabs
            tabs={providerTabs}
            activeTab={config.provider}
            onChange={(id) =>
              setConfig(c => ({
                ...c,
                provider: id,
                model: providerModels[id]?.[0] || c.model,
              }))
            }
          />

          {/* Model dropdown */}
          <div className="flex items-center gap-2">
            <span className="text-xs text-text-secondary">Model</span>
            <div className="relative">
              <select
                value={config.model}
                onChange={e => setConfig(c => ({ ...c, model: e.target.value }))}
                className="appearance-none bg-white/5 border border-white/10 rounded-lg px-3 py-1.5 text-sm text-text-primary pr-8 focus:outline-none focus:border-accent-primary/50"
              >
                {currentModels.map(m => (
                  <option key={m} value={m}>{m}</option>
                ))}
              </select>
              <ChevronDown size={12} className="absolute right-2.5 top-2.5 text-text-secondary pointer-events-none" />
            </div>
          </div>

          {/* Temperature */}
          <div className="flex items-center gap-2">
            <span className="text-xs text-text-secondary">Temp</span>
            <input
              type="range"
              min={0}
              max={2}
              step={0.1}
              value={config.temperature}
              onChange={e => setConfig(c => ({ ...c, temperature: parseFloat(e.target.value) }))}
              className="w-20"
            />
            <span className="text-xs text-accent-primary font-mono w-6">{config.temperature.toFixed(1)}</span>
          </div>

          {/* Max tokens */}
          <div className="flex items-center gap-2">
            <span className="text-xs text-text-secondary">Max Tokens</span>
            <input
              type="number"
              min={256}
              max={8192}
              step={256}
              value={config.maxTokens}
              onChange={e => setConfig(c => ({ ...c, maxTokens: parseInt(e.target.value) || 2048 }))}
              className="w-20 bg-white/5 border border-white/10 rounded-lg px-2 py-1 text-sm text-text-primary focus:outline-none focus:border-accent-primary/50"
            />
          </div>

          {/* Streaming toggle */}
          <div className="flex items-center gap-2">
            <Toggle
              checked={config.streaming}
              onChange={v => setConfig(c => ({ ...c, streaming: v }))}
              label="Streaming"
            />
          </div>
        </div>

        {/* Chat area — relative container so orb can be absolute-positioned */}
        <div className="flex-1 relative overflow-hidden">

          {/* Orb: fixed to left-center of chat area, behind messages, not in scroll flow */}
          <div
            style={{
              position:   'absolute',
              left:       '20px',
              top:        '50%',
              transform:  'translateY(-50%)',
              zIndex:     0,
              pointerEvents: 'none',
            }}
          >
            <AIOrb streaming={isStreaming} size={300} />
          </div>

          {/* Scrollable messages — pushed right to clear the orb */}
          <div
            className="absolute inset-0 overflow-y-auto py-4 pr-6 space-y-6"
            style={{ paddingLeft: '330px', zIndex: 1 }}
          >
            {/* Empty state hint */}
            {messages.length === 0 && !isStreaming && (
              <div className="flex flex-col gap-1 mt-8">
                <p className="text-sm text-text-secondary">Send a message to get started</p>
                <p className="text-xs text-text-secondary/50">
                  Provider: {config.provider} · Model: {config.model}
                </p>
              </div>
            )}

            {/* Messages */}
            {messages.map((msg) => (
              <motion.div
                key={msg.id}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'} gap-3`}
              >
                {msg.role === 'assistant' && (
                  <div className="w-8 h-8 rounded-full bg-white/8 border border-white/10 flex items-center justify-center flex-shrink-0 mt-0.5">
                    <div className="w-4 h-4">
                      <svg viewBox="0 0 16 16" fill="none">
                        <circle cx="8" cy="8" r="4" fill="#19D3FF" fillOpacity="0.6" />
                        <circle cx="8" cy="8" r="2" fill="#19D3FF" />
                      </svg>
                    </div>
                  </div>
                )}

                <div className={msg.role === 'user' ? 'max-w-[55%] order-first' : 'max-w-[60%]'}>
                  <div
                    className={`rounded-2xl px-5 py-4 text-sm ${
                      msg.role === 'user'
                        ? 'bg-accent-primary/15 border border-accent-primary/20 text-text-primary ml-auto'
                        : 'bg-white/4 border border-white/8 text-text-primary'
                    }`}
                  >
                    {msg.role === 'assistant' ? (
                      <ReactMarkdown
                        components={{
                          code({ className, children }) {
                            const match = /language-(\w+)/.exec(className || '');
                            const isBlock = match !== null;
                            if (isBlock) {
                              return (
                                <div className="code-block my-2 rounded-lg overflow-x-auto border border-white/10">
                                  <div className="flex items-center justify-between px-3 py-1.5 bg-white/5 border-b border-white/8">
                                    <span className="text-[10px] text-text-secondary font-mono uppercase">
                                      {match[1]}
                                    </span>
                                    <button
                                      onClick={() => copyMessage(String(children))}
                                      className="flex items-center gap-1 text-[10px] text-text-secondary hover:text-text-primary transition-colors"
                                    >
                                      <Copy size={10} />
                                      Copy
                                    </button>
                                  </div>
                                  <SyntaxHighlighter
                                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                                    style={vscDarkPlus as any}
                                    language={match[1]}
                                    PreTag="div"
                                    customStyle={{
                                      margin: 0,
                                      background: 'rgba(255,255,255,0.02)',
                                      fontSize: '12px',
                                      lineHeight: '1.6',
                                    }}
                                  >
                                    {String(children).replace(/\n$/, '')}
                                  </SyntaxHighlighter>
                                </div>
                              );
                            }
                            return (
                              <code className="bg-white/8 rounded px-1 py-0.5 text-accent-primary text-[11px] font-mono">
                                {children}
                              </code>
                            );
                          },
                          p: ({ children }) => (
                            <p className="mb-2 last:mb-0 leading-[1.75]">{children}</p>
                          ),
                          ul: ({ children }) => (
                            <ul className="list-disc list-inside space-y-1 mb-2">{children}</ul>
                          ),
                          ol: ({ children }) => (
                            <ol className="list-decimal list-inside space-y-1 mb-2">{children}</ol>
                          ),
                          strong: ({ children }) => (
                            <strong className="font-semibold text-white">{children}</strong>
                          ),
                        }}
                      >
                        {msg.content}
                      </ReactMarkdown>
                    ) : (
                      <p className="leading-relaxed">{msg.content}</p>
                    )}
                  </div>
                  <p className="text-[10px] text-text-secondary mt-1 px-1">
                    {msg.timestamp.toLocaleTimeString()}
                  </p>
                </div>

                {msg.role === 'user' && (
                  <div className="w-8 h-8 rounded-full bg-accent-primary/20 border border-accent-primary/30 flex items-center justify-center flex-shrink-0 mt-0.5">
                    <span className="text-[10px] font-semibold text-accent-primary">YP</span>
                  </div>
                )}
              </motion.div>
            ))}

            {/* Streaming message */}
            <AnimatePresence>
              {isStreaming && (
                <motion.div
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0 }}
                  className="flex justify-start gap-3"
                >
                  <div className="w-8 h-8 rounded-full bg-accent-primary/10 border border-accent-primary/30 flex items-center justify-center flex-shrink-0 mt-0.5">
                    <div className="w-3 h-3 rounded-full bg-accent-primary/60 animate-pulse" />
                  </div>
                  <div className="max-w-[60%]">
                    <div className="rounded-2xl px-5 py-4 text-sm bg-white/4 border border-white/8">
                      {streamingContent ? (
                        <ReactMarkdown
                          components={{
                            p: ({ children }) => <p className="mb-2 last:mb-0 leading-[1.75]">{children}</p>,
                          }}
                        >
                          {streamingContent}
                        </ReactMarkdown>
                      ) : (
                        <div className="flex items-center gap-1.5 py-1">
                          <span className="text-text-secondary text-xs">Thinking</span>
                          {[0, 1, 2].map(i => (
                            <div
                              key={i}
                              className="thinking-dot w-1.5 h-1.5 rounded-full bg-accent-primary"
                              style={{ animationDelay: `${i * 0.2}s` }}
                            />
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>

            <div ref={chatEndRef} />
          </div>
        </div>

        {/* Streaming controls */}
        <AnimatePresence>
          {isStreaming && (
            <motion.div
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 8 }}
              className="flex items-center justify-center gap-2 py-2 border-t border-white/8 flex-shrink-0"
            >
              <Button variant="danger" size="sm" icon={<Square size={12} />} onClick={stopStreaming}>
                Stop
              </Button>
              <Button
                variant="ghost"
                size="sm"
                icon={<ArrowDown size={12} />}
                onClick={() => setAutoScroll(v => !v)}
              >
                {autoScroll ? 'Auto Scroll On' : 'Auto Scroll Off'}
              </Button>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Non-streaming toolbar */}
        <AnimatePresence>
          {!isStreaming && messages.length > 0 && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="flex items-center justify-center gap-2 py-1.5 border-t border-white/8 flex-shrink-0"
            >
              <Button variant="ghost" size="sm" icon={<RotateCcw size={12} />} onClick={regenerate}>
                Regenerate
              </Button>
              <Button
                variant="ghost"
                size="sm"
                icon={<Copy size={12} />}
                onClick={() =>
                  copyMessage(messages[messages.length - 1]?.content || '')
                }
              >
                Copy Last
              </Button>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Input bar */}
        <div className="px-4 py-3 border-t border-white/8 flex-shrink-0">
          <Card padding="none" className="flex flex-col gap-0 border-accent-primary/20">
            {/* Drop zone hint */}
            <div className="flex items-center gap-2 px-4 pt-2 pb-1">
              <Upload size={11} className="text-text-secondary" />
              <span className="text-[10px] text-text-secondary">Drop files here to upload</span>
            </div>

            {/* Textarea + actions */}
            <div className="flex items-end gap-2 px-3 pb-3">
              <button className="p-1.5 rounded-lg text-text-secondary hover:text-text-primary hover:bg-white/8 transition-colors flex-shrink-0">
                <Paperclip size={16} />
              </button>
              <button className="p-1.5 rounded-lg text-text-secondary hover:text-text-primary hover:bg-white/8 transition-colors flex-shrink-0">
                <Mic size={16} />
              </button>

              <textarea
                ref={textareaRef}
                value={input}
                onChange={handleTextareaChange}
                onKeyDown={handleKeyDown}
                placeholder="Send a message... (Enter to send, Shift+Enter for newline)"
                rows={1}
                className="flex-1 resize-none bg-transparent text-sm text-text-primary placeholder-text-secondary focus:outline-none min-h-[36px] leading-relaxed py-1.5"
                style={{ maxHeight: 160 }}
                disabled={isStreaming}
              />

              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={handleSend}
                disabled={!input.trim() || isStreaming}
                className={`flex-shrink-0 w-9 h-9 rounded-full flex items-center justify-center transition-colors ${
                  input.trim() && !isStreaming
                    ? 'bg-accent-primary text-bg'
                    : 'bg-white/8 text-text-secondary'
                }`}
              >
                <Send size={15} />
              </motion.button>
            </div>
          </Card>
        </div>
      </motion.div>

      {/* Telemetry Rail */}
      <TelemetryRail telemetry={telemetryData} events={events} />
    </div>
  );
}
