import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import path from "path";

// 立即执行全局 polyfills（在配置加载时就执行）
// 这确保在任何模块导入之前全局对象已经设置好
import "./src/__tests__/global-polyfills";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/__tests__/polyfills.ts", "./src/__tests__/setup.ts"],
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  // 定义全局变量，修复 MSW 依赖问题
  define: {
    global: "globalThis",
  },
});
