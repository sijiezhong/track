import { setupServer } from "msw/node";
import { handlers } from "./handlers";

// 创建 MSW 服务器实例
// 注意：polyfills.ts 必须在 setupFiles 中先加载，确保全局对象正确设置
export const server = setupServer(...handlers);
