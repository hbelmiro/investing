import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: '../../../target/classes/META-INF/resources',
    emptyOutDir: true,
  },
  server: {
    proxy: {
      '/brazil_stocks': 'http://localhost:8080',
      '/us_stocks': 'http://localhost:8080',
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
  },
})
