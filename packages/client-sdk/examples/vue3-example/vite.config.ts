import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  base: "/examples/vue3/",
  server: {
    proxy: {
      "/api": {
        target: "https://track.zhongsijie.cn",
        changeOrigin: true,
        secure: true,
      },
    },
  },
});
