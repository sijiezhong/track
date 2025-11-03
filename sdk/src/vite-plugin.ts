/**
 * Vite Plugin for Track SDK
 * @packageDocumentation
 */

// @ts-ignore - vite is peer dependency
import type { Plugin } from 'vite';
import type { VitePluginOptions } from './types/config';

/**
 * Track SDK Vite Plugin
 * 
 * @param options - 插件配置选项
 * @returns Vite 插件实例
 * 
 * @remarks
 * 该插件会自动在应用入口注入 SDK 初始化代码，支持开发模式热更新
 * 
 * @example
 * ```ts
 * // vite.config.ts
 * import { trackPlugin } from '@track/sdk/vite-plugin';
 * 
 * export default defineConfig({
 *   plugins: [
 *     trackPlugin({
 *       endpoint: 'https://api.example.com',
 *       projectId: 1,
 *       autoStart: true
 *     })
 *   ]
 * });
 * ```
 */
export function trackPlugin(options: VitePluginOptions): Plugin {
  const {
    endpoint,
    projectId,
    autoStart = true,
    batchSize = 10,
    batchTimeout = 5000,
    collectors = ['pageview', 'click', 'error'],
    debug = false,
    usePixel = false,
  } = options;

  // 构建 SDK 初始化代码
  const buildInitCode = () => {
    const collectorsConfig = {
      pageview: collectors.includes('pageview'),
      click: collectors.includes('click'),
      performance: collectors.includes('performance'),
      error: collectors.includes('error'),
    };

    return `
// Track SDK Auto-injected by vite-plugin
import { init } from '@track/sdk';

const tracker = init({
  endpoint: ${JSON.stringify(endpoint)},
  projectId: ${typeof projectId === 'string' ? JSON.stringify(projectId) : projectId},
  autoStart: ${autoStart},
  batchSize: ${batchSize},
  batchTimeout: ${batchTimeout},
  collectors: ${JSON.stringify(collectorsConfig)},
  debug: ${debug},
  usePixel: ${usePixel},
});

// 导出 tracker 实例供应用使用
if (typeof window !== 'undefined') {
  window.__trackSDK = tracker;
}
export { tracker };
`;
  };

  const initCode = buildInitCode();

  return {
    name: 'track-sdk',
    enforce: 'pre',
    apply: 'serve', // 仅开发模式应用

    // 解析时注入代码
    resolveId(id: string): string | null {
      if (id === 'virtual:track-sdk-init') {
        return id;
      }
      return null;
    },

    // 加载时返回初始化代码
    load(id: string): string | null {
      if (id === 'virtual:track-sdk-init') {
        return initCode;
      }
      return null;
    },

    // 转换入口文件，注入 SDK 初始化
    transformIndexHtml(html: string): string {
      // 在开发模式下，通过 script 标签注入
      if (process.env.NODE_ENV === 'development') {
        return html.replace(
          '<head>',
          `<head>\n<script type="module">${initCode}</script>`
        );
      }
      return html;
    },

    // 构建时处理
    buildStart(): void {
      // 构建时可以在入口文件中自动注入
      // 这里可以根据需要扩展
    },

    // 配置构建优化
    configResolved(config: any) {
      // 优化配置：确保 SDK 相关代码不被 tree-shake
      if (config.build?.rollupOptions) {
        const treeshake = config.build.rollupOptions.treeshake;
        config.build.rollupOptions.treeshake = {
          ...(typeof treeshake === 'object' ? treeshake : {}),
          moduleSideEffects: (id: string) => {
            // SDK 相关模块不应该被 tree-shake
            if (id.includes('@track/sdk') || id.includes('track-sdk')) {
              return true;
            }
            return false;
          },
        };
      }
    },
  };
}

// 默认导出
export default trackPlugin;

