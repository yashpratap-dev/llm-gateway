import { useState, useRef, useCallback } from 'react';
import type { ChatMessage } from '../types';
import { sampleResponses } from '../mock/playground';

interface UseStreamingChatReturn {
  messages: ChatMessage[];
  isStreaming: boolean;
  streamingContent: string;
  sendMessage: (content: string) => void;
  stopStreaming: () => void;
  regenerate: () => void;
  clearMessages: () => void;
}

export function useStreamingChat(initialMessages: ChatMessage[]): UseStreamingChatReturn {
  const [messages, setMessages] = useState<ChatMessage[]>(initialMessages);
  const [isStreaming, setIsStreaming] = useState(false);
  const [streamingContent, setStreamingContent] = useState('');
  const abortRef = useRef(false);
  const lastUserMessageRef = useRef('');

  const streamResponse = useCallback((responseText: string) => {
    setIsStreaming(true);
    setStreamingContent('');
    abortRef.current = false;

    const words = responseText.split(' ');
    let currentIndex = 0;

    const streamNextChunk = () => {
      if (abortRef.current) {
        setIsStreaming(false);
        setStreamingContent('');
        return;
      }

      if (currentIndex >= words.length) {
        // Done streaming - add message to list
        setMessages(prev => [
          ...prev,
          {
            id: `msg-${Date.now()}`,
            role: 'assistant' as const,
            content: responseText,
            timestamp: new Date(),
          },
        ]);
        setIsStreaming(false);
        setStreamingContent('');
        return;
      }

      const chunkSize = Math.floor(Math.random() * 3) + 1;
      const chunk = words.slice(currentIndex, currentIndex + chunkSize).join(' ') + ' ';
      currentIndex += chunkSize;

      setStreamingContent(prev => prev + chunk);

      const delay = 30 + Math.random() * 50;
      setTimeout(streamNextChunk, delay);
    };

    // Initial "thinking" delay
    setTimeout(streamNextChunk, 800);
  }, []);

  const sendMessage = useCallback(
    (content: string) => {
      if (!content.trim() || isStreaming) return;

      lastUserMessageRef.current = content;

      const userMessage: ChatMessage = {
        id: `msg-${Date.now()}`,
        role: 'user',
        content: content.trim(),
        timestamp: new Date(),
      };

      setMessages(prev => [...prev, userMessage]);

      // Pick a sample response
      const response = sampleResponses[Math.floor(Math.random() * sampleResponses.length)];
      streamResponse(response);
    },
    [isStreaming, streamResponse]
  );

  const stopStreaming = useCallback(() => {
    abortRef.current = true;
  }, []);

  const regenerate = useCallback(() => {
    if (isStreaming) return;

    // Remove last assistant message if exists
    setMessages(prev => {
      const last = prev[prev.length - 1];
      if (last?.role === 'assistant') {
        return prev.slice(0, -1);
      }
      return prev;
    });

    const response = sampleResponses[Math.floor(Math.random() * sampleResponses.length)];
    streamResponse(response);
  }, [isStreaming, streamResponse]);

  const clearMessages = useCallback(() => {
    setMessages([]);
  }, []);

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
