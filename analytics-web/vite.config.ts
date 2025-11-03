import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'node:path'

// https://vite.dev/config/
export default defineConfig(() => {
  // 从环境变量获取代理目标地址
  // 优先使用 .env.local 中的配置
  const apiTarget = 'https://zhongsijie.cn' // 临时硬编码
  // 如需使用环境变量：
  // import { loadEnv } from 'vite'
  // const env = loadEnv(mode, process.cwd(), '')
  // const apiTarget = env.VITE_API_TARGET || 'http://localhost:8080'

  return {
    plugins: [react()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    server: {
      port: 5173, // 固定端口
      strictPort: false, // 如果端口被占用，自动尝试下一个
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true,
          secure: false, // 临时禁用 SSL 验证，用于调试
          ws: true, // 支持 WebSocket 和长连接
          timeout: 0, // SSE 长连接，不设置超时
          rewrite: (path) => path, // 保持路径不变
          configure: (proxy) => {
            proxy.on('error', (err) => {
              console.error('❌ [Vite Proxy] 代理错误:', err.message)
            })

            proxy.on('proxyReq', (proxyReq, req) => {
              // 对于 SSE 请求，确保正确的 headers
              if (req.url?.includes('/events/stream')) {
                proxyReq.setHeader('Connection', 'keep-alive')
                proxyReq.setHeader('Cache-Control', 'no-cache')

                // 确保 X-Tenant-Id 被转发
                const headers = req.headers
                if (headers['x-tenant-id']) {
                  proxyReq.setHeader('X-Tenant-Id', headers['x-tenant-id'])
                }
              }
            })

            proxy.on('proxyRes', (proxyRes, req) => {
              const status = proxyRes.statusCode
              const url = req.url || ''

              // 对于 SSE 响应，不做缓冲
              if (url.includes('/events/stream')) {
                // SSE 连接失败时输出错误
                if (status !== 200) {
                  console.error('❌ [Vite Proxy] SSE 连接失败! Status:', status)
                }

                proxyRes.headers['Cache-Control'] = 'no-cache'
                proxyRes.headers['Connection'] = 'keep-alive'
              }
            })
          },
        },
      },
    },
  }
})
