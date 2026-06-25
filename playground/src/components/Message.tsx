import type { Message as MessageType, AssistantMessage } from '../types/chat';
import { MetadataBar } from './MetadataBar';

interface Props {
  message: MessageType;
}

export function Message({ message }: Props) {
  const isUser = message.role === 'user';
  const assistant = message as AssistantMessage;

  return (
    <div style={{ marginBottom: '1.5rem' }}>
      <div style={{ color: '#F5F5F5', lineHeight: '1.6' }}>
        <span style={{ color: '#9A9A9A', marginRight: '0.5rem' }}>
          {isUser ? 'you' : 'friday'}
        </span>
        {message.content}
        {!isUser && assistant.streaming && (
          <span style={{
            display: 'inline-block',
            width: '0.6em',
            height: '1em',
            background: '#F5F5F5',
            marginLeft: '2px',
            verticalAlign: 'text-bottom',
            animation: 'blink 1s step-end infinite',
          }} />
        )}
      </div>
      {!isUser && <MetadataBar message={assistant} />}
    </div>
  );
}
