import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// All /api/* requests are proxied to the backend in dev to avoid CORS
// (Spring Security does not emit CORS headers, so direct browser calls are blocked)
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3002,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
})
