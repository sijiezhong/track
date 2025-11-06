import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import { viteMockServe } from "vite-plugin-mock";
import path from "path";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd());

  // Mock 开关：通过 VITE_ENABLE_MOCK 环境变量控制
  // 仅在开发环境且明确启用时才加载 mock
  const enableMock = env.VITE_ENABLE_MOCK === "true" && mode === "development";

  // 从环境变量读取代理目标
  const proxyTarget = env.VITE_PROXY_TARGET;

  // 如果启用了 mock 或配置了 VITE_API_BASE_URL，则不需要 proxy
  const useProxy = !enableMock && !env.VITE_API_BASE_URL && proxyTarget;

  return {
    plugins: [
      react(),
      // 条件性加载 mock 插件
      enableMock &&
        viteMockServe({
          mockPath: "mock",
          enable: true,
          watchFiles: true,
        }),
    ].filter(Boolean) as any,
    resolve: {
      alias: { "@": path.resolve(__dirname, "src") },
    },
    server: {
      port: 3000,
      // 仅在未启用 mock 且配置了 VITE_PROXY_TARGET 且未配置 VITE_API_BASE_URL 时启用 proxy
      ...(useProxy && {
        proxy: {
          // 代理所有 /api 开头的请求到后端服务
          "/api": {
            target: proxyTarget,
            changeOrigin: true,
            secure: true,
            // 如果需要重写路径，可以配置 rewrite
            // rewrite: (path) => path.replace(/^\/api/, ''),
          },
        },
      }),
    },
  };
});
