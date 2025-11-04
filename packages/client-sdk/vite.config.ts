import { defineConfig } from "vite";
import { resolve } from "path";
import dts from "vite-plugin-dts";

export default defineConfig({
  plugins: [
    // 仅为主 SDK 入口生成类型声明
    dts({
      insertTypesEntry: true,
      include: ["src/**/*"],
      exclude: ["src/__tests__/**/*", "src/**/*.test.ts", "src/**/*.spec.ts"],
      outDir: "dist",
      copyDtsFiles: true,
    }),
  ],
  build: {
    lib: {
      entry: resolve(__dirname, "src/index.ts"),
      name: "Track",
      formats: ["es", "cjs", "umd"],
      fileName: (format) => {
        if (format === "es") return "index.js";
        if (format === "cjs") return "index.cjs";
        return "index.umd.js";
      },
    },
    rollupOptions: {
      output: {
        name: "Track",
        globals: {},
        exports: "named",
      },
    },
    sourcemap: true,
    minify: "esbuild",
  },
});
