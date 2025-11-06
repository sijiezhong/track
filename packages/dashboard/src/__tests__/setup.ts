import { beforeAll, afterEach, afterAll } from "vitest";
import { server } from "./mocks/server";

// 在所有测试之前启动 MSW 服务器
beforeAll(() => server.listen({ onUnhandledRequest: "warn" }));

// 每个测试后重置 handlers
afterEach(() => server.resetHandlers());

// 所有测试后关闭服务器
afterAll(() => server.close());
