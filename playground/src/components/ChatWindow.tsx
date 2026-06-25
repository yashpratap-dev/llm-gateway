import { useEffect, useRef } from 'react';
import type { Message as MessageType } from '../types/chat';
import { Message } from './Message';

interface Props {
  messages: MessageType[];
}

export function ChatWindow({ messages }: Props) {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  return (
    <div style={{
      flex: 1,
      overflowY: 'auto',
      padding: '2rem',
      scrollbarWidth: 'thin',
      scrollbarColor: '#2B2B2B #0A0A0A',
    }}>
      {messages.length === 0 && (
        <div style={{ color: '#2B2B2B', textAlign: 'center', marginTop: '30vh' }}>
          initializing friday...
        </div>
      )}
      {messages.map((msg, i) => (
        <Message key={i} message={msg} />
      ))}
      <div ref={bottomRef} />
    </div>
  );
}
