import axios, { type AxiosError } from 'axios';

const API_KEY = import.meta.env.VITE_API_KEY as string;

export const apiClient = axios.create({ timeout: 15_000 });

// Request interceptor — inject Authorization header
apiClient.interceptors.request.use((config) => {
  config.headers['Authorization'] = `Bearer ${API_KEY}`;
  config.headers['Content-Type'] = config.headers['Content-Type'] ?? 'application/json';
  return config;
});

// Response interceptor — log errors, extract backend message
apiClient.interceptors.response.use(
  (res) => res,
  (error: AxiosError<{ message?: string; errorCode?: string }>) => {
    const status  = error.response?.status;
    const message = error.response?.data?.message ?? error.message;
    if (status === 401) console.error('[api] 401 Unauthorized — check VITE_API_KEY');
    else if (status === 500) console.error('[api] 500 Server error:', message);
    else if (error.code === 'ECONNABORTED') console.error('[api] Request timeout');
    return Promise.reject(Object.assign(error, { friendlyMessage: message }));
  }
);

/** Extract backend error message from an axios error, with a default fallback. */
export function getErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const axErr = error as AxiosError<{ message?: string }> & { friendlyMessage?: string };
    return axErr.friendlyMessage ?? axErr.response?.data?.message ?? fallback;
  }
  return fallback;
}
