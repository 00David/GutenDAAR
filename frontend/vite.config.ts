import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    // listen on the LAN too, so the dev server can be opened from a phone
    host: true,
    watch: { usePolling: true },
    // dev only: forward /api to the backend (override with API_PROXY_TARGET=http://<ip>:8080)
    proxy: { '/api': process.env.API_PROXY_TARGET ?? 'http://localhost:8080' },
  },
  build: {
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true,
  },
})
