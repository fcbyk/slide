import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'


export default defineConfig({
  plugins: [
    tailwindcss(),
    vue()
  ],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:80',
        changeOrigin: true
      },
      '/internal': {
        target: 'http://localhost:80',
        changeOrigin: true
      },
      '/auto-login': {
        target: 'http://localhost:80',
        changeOrigin: true
      },
      '/socket.io': {
        target: 'http://localhost:80',
        changeOrigin: true,
        ws: true
      }
    }
  }
})
