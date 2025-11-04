import type { Plugin } from "vite";
import type { TrackConfig } from "../src/types";

export interface TrackPluginOptions extends Partial<TrackConfig> {
  /** 是否自动注入到页面 */
  autoInject?: boolean;
  /** 全局变量名，默认为 'Track' */
  globalName?: string;
}

/**
 * Vite Plugin for Track SDK
 * 自动注入 SDK 到页面，并暴露全局变量
 */
export function trackPlugin(options: TrackPluginOptions = {}): Plugin {
  const { autoInject = true, globalName = "Track", ...trackConfig } = options;

  return {
    name: "track-plugin",
    apply: "build", // 只在构建时应用
    transformIndexHtml(html: string) {
      if (!autoInject) {
        return html;
      }

      // 生成 SDK 初始化代码
      const initCode = `
<script>
  // Track SDK 将通过全局变量 ${globalName} 暴露
  // 使用方式：
  // await ${globalName}.init({ appId: 'xxx', userId: 'xxx' }, { endpoint: '${trackConfig.endpoint || ""}' });
  // ${globalName}.start();
</script>`;

      // 在 </head> 之前注入
      if (html.includes("</head>")) {
        return html.replace("</head>", `${initCode}</head>`);
      }

      // 如果没有 head 标签，在 html 开始后注入
      if (html.includes("<html")) {
        return html.replace("<html", `${initCode}<html`);
      }

      // 如果都没有，在开头注入
      return initCode + html;
    },
    configResolved(config) {
      // 配置解析完成后的钩子
      // 可以在这里修改构建配置
    },
  };
}
