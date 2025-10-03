import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 7825,
    host: '0.0.0.0',
    proxy: {
      '/tiles': {
        target: 'http://216.238.79.60:8123',
        changeOrigin: true,
      },
      '/events': {
        target: 'http://216.238.79.60:8123',
        changeOrigin: true,
        ws: true,
      },
      '/api': {
        target: 'http://216.238.79.60:8123',
        changeOrigin: true,
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  }
})
