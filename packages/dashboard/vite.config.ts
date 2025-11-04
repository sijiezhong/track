import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import { viteMockServe } from "vite-plugin-mock";
import path from "path";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd());
  const enableMock = env.VITE_ENABLE_MOCK === "true" && mode === "development";
  return {
    plugins: [
      react(),
      enableMock &&
        viteMockServe({
          mockPath: "mock",
          localEnabled: true,
          prodEnabled: false,
          watchFiles: true,
        }),
    ].filter(Boolean) as any,
    resolve: {
      alias: { "@": path.resolve(__dirname, "src") },
    },
    server: {
      port: 3000,
    },
  };
});
