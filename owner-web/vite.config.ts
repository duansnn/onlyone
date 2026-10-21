import { fileURLToPath, URL } from 'node:url';
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

/**
 * Owner Mall 前端构建配置。
 *
 * 开发期通过代理把 /api、/actuator、/v3/api-docs、/files 转发到后端 41452。
 * 因此浏览器侧与后端同源，无需处理跨域。
 */
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const backend = env.VITE_BACKEND_ORIGIN || 'http://localhost:41452';
  const devPort = Number(env.VITE_DEV_PORT || 5273);

  // 开发服务器与 preview 使用同一套代理规则
  const proxy = {
    '/api': { target: backend, changeOrigin: true },
    // 商品图片等本地上传文件的访问路径（后端静态映射 /files/**）
    '/files': { target: backend, changeOrigin: true },
    '/actuator': { target: backend, changeOrigin: true },
    '/v3/api-docs': { target: backend, changeOrigin: true },
    '/doc.html': { target: backend, changeOrigin: true },
    '/druid': { target: backend, changeOrigin: true },
  };

  return {
    plugins: [react()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      host: '0.0.0.0',
      port: devPort,
      strictPort: false,
      // 手机通过内网穿透（cloudflared / localhost.run 等）访问时，Host 头是隧道域名，
      // Vite 默认只放行 localhost 与 IP，会直接返回 "Blocked request"，这里放开
      allowedHosts: true,
      proxy,
    },
    preview: {
      host: '0.0.0.0',
      port: 4173,
      strictPort: false,
      allowedHosts: true,
      proxy,
    },
    build: {
      outDir: 'dist',
      sourcemap: false,
      chunkSizeWarningLimit: 2000,
      rollupOptions: {
        output: {
          // Vite 8（Rolldown）只接受函数形式的 manualChunks
          manualChunks(moduleId: string) {
            const id = moduleId.replace(/\\/g, '/');
            if (!id.includes('node_modules')) return undefined;
            if (id.includes('/antd/') || id.includes('/@ant-design/') || id.includes('/rc-')) return 'antd';
            if (
              id.includes('/react/') ||
              id.includes('/react-dom/') ||
              id.includes('/react-router') ||
              id.includes('/scheduler/')
            ) {
              return 'react';
            }
            return undefined;
          },
        },
      },
    },
  };
});
