import { useState, type KeyboardEvent } from 'react';

interface Props {
  onSend: (content: string) => void;
  disabled: boolean;
}

export function InputBar({ onSend, disabled }: Props) {
  const [value, setValue] = useState('');

  const handleSend = () => {
    if (!value.trim() || disabled) return;
    onSend(value.trim());
    setValue('');
  };

  const handleKey = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div style={{
      borderTop: '1px solid #2B2B2B',
      padding: '1rem 2rem',
      display: 'flex',
      gap: '1rem',
      background: '#0A0A0A',
    }}>
      <input
        type="text"
        value={value}
        onChange={e => setValue(e.target.value)}
        onKeyDown={handleKey}
        disabled={disabled}
        placeholder={disabled ? 'friday is thinking...' : 'send a message'}
        style={{
          flex: 1,
          background: 'transparent',
          border: '1px solid #2B2B2B',
          color: '#F5F5F5',
          padding: '0.6rem 1rem',
          fontFamily: 'inherit',
          fontSize: '0.9rem',
          outline: 'none',
          opacity: disabled ? 0.5 : 1,
        }}
      />
      <button
        onClick={handleSend}
        disabled={disabled || !value.trim()}
        style={{
          background: disabled || !value.trim() ? 'transparent' : '#F5F5F5',
          color: disabled || !value.trim() ? '#2B2B2B' : '#0A0A0A',
          border: '1px solid #2B2B2B',
          padding: '0.6rem 1.5rem',
          fontFamily: 'inherit',
          fontSize: '0.9rem',
          cursor: disabled || !value.trim() ? 'not-allowed' : 'pointer',
          transition: 'all 0.1s',
        }}
      >
        SEND
      </button>
    </div>
  );
}
