import { useState, useCallback, useRef, useEffect } from 'react';
import { createParser } from 'eventsource-parser';
import type { Message, StreamTokenEvent, StreamDoneEvent, StreamErrorEvent } from '../types/chat';

const API_KEY = import.meta.env.VITE_API_KEY as string;

export function useStreamingChat() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [isStreaming, setIsStreaming] = useState(false);
  const abortRef = useRef<AbortController | null>(null);
  // historyRef mirrors messages state — avoids stale closure in sendMessage
  const historyRef = useRef<Message[]>([]);

  // Keep historyRef in sync with messages state
  useEffect(() => {
    historyRef.current = messages;
  }, [messages]);

  // Abort any in-flight request on unmount
  useEffect(() => {
    return () => { abortRef.current?.abort(); };
  }, []);

  const sendMessage = useCallback(async (content: string) => {
    if (!content.trim() || isStreaming) return;

    const userMessage: Message = { role: 'user', content };

    // Use functional update — never read from messages directly here
    setMessages(prev => [...prev, userMessage, {
      role: 'assistant',
      content: '',
      streaming: true,
    }]);
    setIsStreaming(true);

    const controller = new AbortController();
    abortRef.current = controller;

    // Build history from ref — guaranteed fresh, not a stale closure
    const history = [...historyRef.current, userMessage].map(m => ({
      role: m.role,
      content: m.content,
    }));

    try {
      const response = await fetch('/api/v1/chat/completions/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${API_KEY}`,
        },
        body: JSON.stringify({
          model: 'auto',
          messages: history,
          stream: true,
        }),
        signal: controller.signal,
      });

      if (!response.ok || !response.body) {
        throw new Error(`HTTP ${response.status}`);
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let accumulated = '';

      const parser = createParser({
        onEvent: (event) => {
        if (event.event === 'token') {
          const data = JSON.parse(event.data) as StreamTokenEvent;
          accumulated += data.delta;
          const snapshot = accumulated;
          setMessages(prev => {
            const next = [...prev];
            const last = next[next.length - 1];
            if (last.role === 'assistant') {
              next[next.length - 1] = { ...last, content: snapshot, streaming: true };
            }
            return next;
          });
        } else if (event.event === 'done') {
          const data = JSON.parse(event.data) as StreamDoneEvent;
          const finalContent = accumulated;
          setMessages(prev => {
            const next = [...prev];
            const last = next[next.length - 1];
            if (last.role === 'assistant') {
              next[next.length - 1] = {
                ...last,
                content: finalContent,
                streaming: false,
                provider: data.provider,
                latencyMs: data.latencyMs,
                cacheHit: data.cacheHit,
              };
            }
            return next;
          });
          setIsStreaming(false);
        } else if (event.event === 'error') {
          const data = JSON.parse(event.data) as StreamErrorEvent;
          setMessages(prev => {
            const next = [...prev];
            const last = next[next.length - 1];
            if (last.role === 'assistant') {
              next[next.length - 1] = {
                ...last,
                content: '',
                streaming: false,
                error: data.message,
              };
            }
            return next;
          });
          setIsStreaming(false);
        }
        }
      });

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        parser.feed(decoder.decode(value, { stream: true }));
      }

    } catch (err: unknown) {
      if (err instanceof Error && err.name === 'AbortError') return;
      setMessages(prev => {
        const next = [...prev];
        const last = next[next.length - 1];
        if (last.role === 'assistant') {
          next[next.length - 1] = {
            ...last,
            content: '',
            streaming: false,
            error: err instanceof Error ? err.message : 'Unknown error',
          };
        }
        return next;
      });
      setIsStreaming(false);
    }
  }, [isStreaming]);

  return { messages, isStreaming, sendMessage };
}
