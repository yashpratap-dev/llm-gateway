export interface UserMessage {
  role: 'user';
  content: string;
}

export interface AssistantMessage {
  role: 'assistant';
  content: string;
  streaming?: boolean;
  provider?: string;
  latencyMs?: number;
  cacheHit?: boolean;
  error?: string;
}

export type Message = UserMessage | AssistantMessage;

export interface StreamTokenEvent {
  delta: string;
}

export interface StreamDoneEvent {
  provider: string;
  latencyMs: number;
  cacheHit: boolean;
}

export interface StreamErrorEvent {
  message: string;
  errorCode: string;
}
