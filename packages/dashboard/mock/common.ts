import Mock from "mockjs";
import type { MockMethod } from "vite-plugin-mock";

export function ok<T>(data: T) {
  return data;
}

export function error(code = "INTERNAL_ERROR", status = 500) {
  return {
    status,
    body: { code, message: "服务异常", traceId: Mock.Random.guid() },
  };
}

export function maybeError(url: string) {
  const u = new URL("http://x" + url);
  if (u.searchParams.get("error") === "1") return error("MOCK_ERROR", 500);
  if (u.searchParams.get("429") === "1")
    return {
      status: 429,
      headers: { "Retry-After": "30" },
      body: {
        code: "RATE_LIMIT",
        message: "频率受限",
        traceId: Mock.Random.guid(),
      },
    };
  return null;
}

export type MockDef = MockMethod;
