import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { '@': path.resolve(__dirname, 'src') },
  },
  server: {
    host: '127.0.0.1', // 仅监听本机，避免 Node 枚举网卡报错 uv_interface_addresses
    port: 5173,
    proxy: {
      '/api': {
        // 默认经网关 8080；若不启网关则改为 http://localhost:8081
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8081',
        changeOrigin: true,
        ws: true,
      },
    },
  },
});
