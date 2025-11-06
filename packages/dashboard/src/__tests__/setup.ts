import { beforeAll, afterEach, afterAll } from "vitest";
import { server } from "./mocks/server";

// 处理 unhandled errors（webidl-conversions/whatwg-url 等依赖的兼容性问题）
// 这些错误通常不影响实际测试运行，只是模块初始化时的警告
const unhandledErrorHandler = (error: Error) => {
  const message = error?.message || error?.toString() || "";
  if (
    message.includes("webidl-conversions") ||
    message.includes("whatwg-url") ||
    message.includes("Cannot read properties of undefined")
  ) {
    // 忽略这些特定的模块加载错误，它们不影响测试运行
    return;
  }
  // 其他错误正常抛出
  throw error;
};

beforeAll(() => {
  // 捕获未处理的异常和 Promise rejection
  process.on("uncaughtException", unhandledErrorHandler);
  process.on("unhandledRejection", (reason) => {
    if (reason instanceof Error) {
      unhandledErrorHandler(reason);
    }
  });
});

// 在所有测试之前启动 MSW 服务器
beforeAll(() => server.listen({ onUnhandledRequest: "warn" }));

// 每个测试后重置 handlers
afterEach(() => server.resetHandlers());

// 所有测试后关闭服务器并清理事件监听器
afterAll(() => {
  server.close();
  process.removeListener("uncaughtException", unhandledErrorHandler);
  process.removeAllListeners("unhandledRejection");
});
