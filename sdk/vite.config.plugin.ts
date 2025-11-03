import { defineConfig } from 'vite';
import { resolve } from 'path';
import dts from 'vite-plugin-dts';

export default defineConfig({
  plugins: [
    dts({
      include: ['src/vite-plugin.ts'],
      outDir: 'dist',
    }),
  ],
  build: {
    lib: {
      entry: resolve(__dirname, 'src/vite-plugin.ts'),
      name: 'TrackVitePlugin',
      formats: ['es'],
      fileName: () => 'vite-plugin.esm.js',
    },
    rollupOptions: {
      external: ['vite', 'path'],
      output: {
        globals: {},
      },
    },
    sourcemap: true,
  },
});

