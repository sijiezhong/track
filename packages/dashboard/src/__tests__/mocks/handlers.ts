import { http, HttpResponse } from "msw";

// 手动解析查询参数的辅助函数，避免 Node.js 环境中的 URL 相关问题
function parseQueryParams(url: string | URL): Record<string, string> {
  const urlString = typeof url === "string" ? url : url.toString();
  const queryString = urlString.split("?")[1] || "";
  const params: Record<string, string> = {};

  if (!queryString) return params;

  queryString.split("&").forEach((pair) => {
    const [key, value = ""] = pair.split("=");
    if (key) {
      params[decodeURIComponent(key)] = decodeURIComponent(value);
    }
  });

  return params;
}

// 使用相对路径匹配所有请求
export const handlers = [
  // Analytics endpoints
  http.get("/api/analytics/overview", () => {
    return HttpResponse.json({
      pv: 1000,
      uv: 500,
      bounceRate: 0.35,
      avgDurationSec: 120.5,
      timezone: "UTC",
    });
  }),

  http.get("/api/analytics/pv-uv/series", ({ request }) => {
    // 手动解析查询参数，避免 Node.js 环境中的 URL 相关问题
    const params = parseQueryParams(request.url);
    const interval = params.interval || "hour";

    return HttpResponse.json({
      series: [
        { ts: "2024-01-01T00:00:00", pv: 100, uv: 50 },
        { ts: "2024-01-01T01:00:00", pv: 150, uv: 75 },
      ],
      interval,
      timezone: "UTC",
    });
  }),

  http.get("/api/analytics/pages/top", () => {
    return HttpResponse.json({
      list: [
        { pageUrl: "/page1", pv: 1000, uv: 500, avgDurationSec: 120.5 },
        { pageUrl: "/page2", pv: 800, uv: 400, avgDurationSec: 90.0 },
      ],
      total: 2,
    });
  }),

  http.get("/api/analytics/events-distribution", () => {
    return HttpResponse.json({
      list: [
        { type: "page_view", value: 5000 },
        { type: "click", value: 3000 },
        { type: "error", value: 100 },
      ],
    });
  }),

  http.get("/api/analytics/web-vitals", () => {
    return HttpResponse.json({
      p50: 1200.0,
      p75: 2040.0,
      p95: 3480.0,
      unit: "ms",
    });
  }),

  http.get("/api/analytics/web-vitals/series", () => {
    return HttpResponse.json({
      series: [
        { ts: "2024-01-01T00:00:00", p50: 1200.0, p75: 2040.0, p95: 3480.0 },
        { ts: "2024-01-01T01:00:00", p50: 1250.0, p75: 2100.0, p95: 3500.0 },
      ],
      interval: "hour",
      timezone: "UTC",
    });
  }),

  http.get("/api/analytics/custom-events", ({ request }) => {
    // 手动解析查询参数，避免 Node.js 环境中的 URL 相关问题
    const params = parseQueryParams(request.url);
    const groupBy = params.groupBy || "day";

    return HttpResponse.json({
      series: [
        { ts: "2024-01-01", count: 100 },
        { ts: "2024-01-02", count: 150 },
      ],
      total: 250,
      groupBy,
    });
  }),

  http.get("/api/analytics/custom-events/top", () => {
    return HttpResponse.json({
      list: [
        { eventId: "event1", count: 1000 },
        { eventId: "event2", count: 800 },
      ],
      total: 1800,
    });
  }),

  http.get("/api/analytics/errors/trend", () => {
    return HttpResponse.json({
      series: [
        { ts: "2024-01-01T00:00:00", count: 10 },
        { ts: "2024-01-01T01:00:00", count: 15 },
      ],
      interval: "hour",
      timezone: "UTC",
    });
  }),

  http.get("/api/analytics/errors/top", () => {
    return HttpResponse.json({
      list: [
        {
          fingerprint: "fingerprint1",
          message: "Error 1",
          count: 100,
          firstSeen: "2024-01-01",
          lastSeen: "2024-01-02",
        },
        {
          fingerprint: "fingerprint2",
          message: "Error 2",
          count: 80,
          firstSeen: "2024-01-01",
          lastSeen: "2024-01-02",
        },
      ],
      total: 180,
    });
  }),

  // Events endpoint
  http.get("/api/events", ({ request }) => {
    // 手动解析查询参数，避免 Node.js 环境中的 URL 相关问题
    const params = parseQueryParams(request.url);
    const page = parseInt(params.page || "1", 10);
    const size = parseInt(params.size || "50", 10);

    return HttpResponse.json({
      items: [
        {
          id: 1,
          appId: "test-app",
          eventType: "page_view",
          pageUrl: "https://example.com/page",
        },
        {
          id: 2,
          appId: "test-app",
          eventType: "click",
          pageUrl: "https://example.com/page",
        },
      ],
      page: {
        index: page,
        size,
        total: 2,
      },
    });
  }),

  // Projects endpoint
  http.get("/api/projects", () => {
    return HttpResponse.json({
      list: [
        { appId: "app1", appName: "App 1" },
        { appId: "app2", appName: "App 2" },
      ],
    });
  }),
];
