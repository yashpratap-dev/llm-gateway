import { useState, useRef, useCallback } from 'react';
import type { ChatMessage } from '../types';

// API key comes from .env (VITE_ prefix exposes it to the browser bundle)
const API_KEY = import.meta.env.VITE_API_KEY as string;
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

// ─── Backend SSE payload shapes ────────────────────────────────────────────
interface TokenPayload { delta: string }
interface ErrorPayload { message: string; errorCode: string }
// DonePayload: { provider, latencyMs, cacheHit } — available if needed for telemetry

// ─── Non-streaming response shape ─────────────────────────────────────────
interface GatewayResponse {
  data?: {
    choices?: Array<{ message?: { content?: string } }>;
  };
}

// ─── Incremental SSE buffer parser ─────────────────────────────────────────
// Accumulates raw bytes across multiple read() calls and yields complete
// SSE blocks (delimited by \n\n). Handles the Spring ServerSentEvent wire
// format: "event: <name>\ndata: <json>\n\n"
function processSseBuffer(
  buffer:  string,
  chunk:   string,
  onToken: (delta: string) => void,
  onError: (msg: string) => void,
): string {
  buffer += chunk;
  const blocks = buffer.split('\n\n');
  const remaining = blocks.pop() ?? ''; // last item may be an incomplete block

  for (const block of blocks) {
    if (!block.trim()) continue;
    let event = '';
    let data  = '';
    for (const line of block.split('\n')) {
      if (line.startsWith('event:')) event = line.slice(6).trim();
      else if (line.startsWith('data:')) data  = line.slice(5).trim();
    }
    if (!event || !data) continue;

    if (event === 'token') {
      try {
        const p = JSON.parse(data) as TokenPayload;
        if (p.delta) onToken(p.delta);
      } catch { /* malformed JSON — skip */ }
    } else if (event === 'error') {
      try {
        const e = JSON.parse(data) as ErrorPayload;
        onError(`${e.errorCode}: ${e.message}`);
      } catch {
        onError('Unknown streaming error');
      }
    }
    // 'done' event carries provider/latency metadata — ignored here
  }

  return remaining;
}

// ─── Hook ──────────────────────────────────────────────────────────────────

export interface UseStreamingChatReturn {
  messages: ChatMessage[];
  isStreaming: boolean;
  streamingContent: string;
  sendMessage:  (content: string, streaming?: boolean) => void;
  stopStreaming: () => void;
  regenerate:   (streaming?: boolean) => void;
  clearMessages: () => void;
}

export function useStreamingChat(initialMessages: ChatMessage[]): UseStreamingChatReturn {
  const [messages,         setMessages]         = useState<ChatMessage[]>(initialMessages);
  const [isStreaming,      setIsStreaming]       = useState(false);
  const [streamingContent, setStreamingContent] = useState('');

  // Ref mirror of messages so we can read latest value synchronously
  // without closing over stale state in callbacks.
  const messagesRef = useRef<ChatMessage[]>(initialMessages);
  const abortRef    = useRef<AbortController | null>(null);
  const lastUserRef = useRef('');

  /** setMessages wrapper that also keeps messagesRef in sync. */
  const setMsgs = useCallback(
    (updater: (prev: ChatMessage[]) => ChatMessage[]) => {
      setMessages(prev => {
        const next = updater(prev);
        messagesRef.current = next;
        return next;
      });
    },
    []
  );

  /** Commit the completed assistant message and clear streaming state. */
  const finalise = useCallback((content: string) => {
    if (!content.trim()) {
      setIsStreaming(false);
      setStreamingContent('');
      return;
    }
    const msg: ChatMessage = {
      id:        `msg-${Date.now()}`,
      role:      'assistant',
      content,
      timestamp: new Date(),
    };
    setMsgs(prev => [...prev, msg]);
    setIsStreaming(false);
    setStreamingContent('');
  }, [setMsgs]);

  /**
   * Core fetch logic — used by both sendMessage and regenerate.
   *
   * Uses a Vite dev-server proxy for /api/* so the browser never crosses
   * origins (Spring Security has no CORS config). In production point
   * VITE_API_BASE_URL at the backend and configure CORS headers there.
   */
  const callApi = useCallback(async (
    userContent: string,
    history:     ChatMessage[],
    streaming:   boolean,
  ) => {
    const ctrl = new AbortController();
    abortRef.current = ctrl;

    setIsStreaming(true);
    setStreamingContent('');

    const messages = [
      ...history.map(m => ({ role: m.role, content: m.content })),
      { role: 'user' as const, content: userContent },
    ];

    // Declared outside try/catch so the AbortError handler can commit partial content
    let accumulated = '';

    try {
      if (streaming) {
        // ── SSE streaming path ────────────────────────────────────────────
        const res = await fetch(`${API_BASE_URL}/api/v1/chat/completions/stream`, {
          method:  'POST',
          headers: {
            'Content-Type':  'application/json',
            'Authorization': `Bearer ${API_KEY}`,
          },
          body:   JSON.stringify({ model: 'auto', messages, stream: true, parameters: {} }),
          signal: ctrl.signal,
        });

        if (!res.ok) {
          const text = await res.text().catch(() => res.statusText);
          throw new Error(`Gateway ${res.status}: ${text}`);
        }

        const reader  = res.body!.getReader();
        const decoder = new TextDecoder();
        let sseBuffer = '';

        // eslint-disable-next-line no-constant-condition
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          sseBuffer = processSseBuffer(
            sseBuffer,
            decoder.decode(value, { stream: true }),
            (delta) => {
              accumulated += delta;
              setStreamingContent(accumulated);
            },
            (errMsg) => { throw new Error(errMsg); },
          );
        }

        finalise(accumulated);
      } else {
        // ── Non-streaming path ────────────────────────────────────────────
        const res = await fetch(`${API_BASE_URL}/api/v1/chat/completions`, {
          method:  'POST',
          headers: {
            'Content-Type':  'application/json',
            'Authorization': `Bearer ${API_KEY}`,
          },
          body:   JSON.stringify({ model: 'auto', messages, stream: false, parameters: {} }),
          signal: ctrl.signal,
        });

        if (!res.ok) {
          const text = await res.text().catch(() => res.statusText);
          throw new Error(`Gateway ${res.status}: ${text}`);
        }

        const json = await res.json() as GatewayResponse;
        const text = json?.data?.choices?.[0]?.message?.content ?? '';
        finalise(text);
      }
    } catch (err: unknown) {
      if (err instanceof Error && err.name === 'AbortError') {
        // User hit Stop — commit whatever was accumulated; only clear if nothing arrived yet
        if (accumulated) {
          finalise(accumulated);
        } else {
          setIsStreaming(false);
          setStreamingContent('');
        }
      } else {
        const msg = err instanceof Error ? err.message : String(err);
        finalise(`Error: ${msg}`);
      }
    } finally {
      // Safety net: always clear streaming flag even if finalise threw or was skipped
      setIsStreaming(false);
    }
  }, [finalise]);

  // ── Public API ───────────────────────────────────────────────────────────

  const sendMessage = useCallback(
    (content: string, streaming = true) => {
      if (!content.trim() || isStreaming) return;

      lastUserRef.current = content.trim();

      // Snapshot history BEFORE adding the user message; that snapshot is
      // what we send as context (callApi appends the user message itself).
      const historySnapshot = messagesRef.current;

      const userMsg: ChatMessage = {
        id:        `msg-${Date.now()}`,
        role:      'user',
        content:   content.trim(),
        timestamp: new Date(),
      };

      setMsgs(prev => [...prev, userMsg]);
      callApi(content.trim(), historySnapshot, streaming);
    },
    [isStreaming, callApi, setMsgs]
  );

  const stopStreaming = useCallback(() => {
    abortRef.current?.abort();
    setIsStreaming(false);
    setStreamingContent('');
  }, []);

  const regenerate = useCallback(
    (streaming = true) => {
      if (isStreaming || !lastUserRef.current) return;

      // historyBeforeUser is everything except the last assistant response
      // and the last user message (callApi will re-append the user message).
      let historyBeforeUser: ChatMessage[] = [];

      setMsgs(prev => {
        // Strip last assistant message if present
        const withoutAssistant =
          prev[prev.length - 1]?.role === 'assistant' ? prev.slice(0, -1) : prev;
        // Strip the last user message (we'll resend it via callApi)
        historyBeforeUser = withoutAssistant.slice(0, -1);
        return withoutAssistant;
      });

      callApi(lastUserRef.current, historyBeforeUser, streaming);
    },
    [isStreaming, callApi, setMsgs]
  );

  const clearMessages = useCallback(() => {
    setMsgs(() => []);
    lastUserRef.current = '';
  }, [setMsgs]);

  return {
    messages,
    isStreaming,
    streamingContent,
    sendMessage,
    stopStreaming,
    regenerate,
    clearMessages,
  };
}
