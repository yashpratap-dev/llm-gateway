import { ChatWindow } from './components/ChatWindow';
import { InputBar } from './components/InputBar';
import { useStreamingChat } from './hooks/useStreamingChat';

export default function App() {
  const { messages, isStreaming, sendMessage } = useStreamingChat();

  return (
    <div style={{
      height: '100vh',
      display: 'flex',
      flexDirection: 'column',
      background: '#0A0A0A',
    }}>
      <div style={{
        borderBottom: '1px solid #2B2B2B',
        padding: '1rem 2rem',
        display: 'flex',
        alignItems: 'center',
        gap: '0.75rem',
      }}>
        <span style={{
          fontSize: '0.9rem',
          letterSpacing: '0.15em',
          textTransform: 'uppercase',
          color: '#F5F5F5',
        }}>
          FRIDAY
        </span>
        <span style={{ color: '#2B2B2B' }}>·</span>
        <span style={{
          fontSize: '0.75rem',
          color: '#9A9A9A',
          letterSpacing: '0.1em',
        }}>
          LLM GATEWAY
        </span>
      </div>

      <ChatWindow messages={messages} />
      <InputBar onSend={sendMessage} disabled={isStreaming} />
    </div>
  );
}
