// import { defineConfig } from 'vite'
// import react from '@vitejs/plugin-react'

// // https://vite.dev/config/
// export default defineConfig({
//   plugins: [react()],
// })
// vite.config.js（完整可运行版本）
// vite.config.js
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';
// ✅ 导入 ESM 环境下的路径处理工具
import { fileURLToPath, URL } from 'url';

// ✅ 手动实现 ESM 环境的 __dirname
const __dirname = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  plugins: [react()],
  envPrefix: 'REACT_APP_',
  resolve: {
    alias: {
      // ✅ 使用手动实现的 __dirname 配置别名
      '@': path.resolve(__dirname, './src'),
    },
    extensions: ['.mjs', '.js', '.jsx', '.json'],
  },
  server: {
    port: 5173,
    open: true,
    cors: true,
    fs: {
      strict: false,
      allow: [__dirname], 
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom', '@mui/material'],
        },
      },
    },
  },
});