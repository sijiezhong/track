import { http, HttpResponse } from "msw";

// 使用相对路径匹配所有请求
export const handlers = [
  // Analytics endpoints
  http.get("/api/analytics/overview", ({ request }) => {
    const url = new URL(request.url);
    const appId = url.searchParams.get("appId");

    if (!appId || appId.trim() === "") {
      return HttpResponse.json(
        {
          code: "BAD_REQUEST",
          message: "appId is required and cannot be empty",
        },
        { status: 400 },
      );
    }

    return HttpResponse.json({
      pv: 1000,
      uv: 500,
      bounceRate: 0.35,
      avgDurationSec: 120.5,
      timezone: "UTC",
    });
  }),

  http.get("/api/analytics/pv-uv/series", ({ request }) => {
    const url = new URL(request.url);
    const appId = url.searchParams.get("appId");
    const interval = url.searchParams.get("interval") || "hour";

    if (!appId || appId.trim() === "") {
      return HttpResponse.json(
        {
          code: "BAD_REQUEST",
          message: "appId is required and cannot be empty",
        },
        { status: 400 },
      );
    }

    return HttpResponse.json({
      series: [
        { ts: "2024-01-01T00:00:00", pv: 100, uv: 50 },
        { ts: "2024-01-01T01:00:00", pv: 150, uv: 75 },
      ],
      interval,
      timezone: "UTC",
    });
  }),

  http.get("/api/analytics/pages/top", ({ request }) => {
    const url = new URL(request.url);
    const appId = url.searchParams.get("appId");

    if (!appId || appId.trim() === "") {
      return HttpResponse.json(
        {
          code: "BAD_REQUEST",
          message: "appId is required and cannot be empty",
        },
        { status: 400 },
      );
    }

    return HttpResponse.json({
      list: [
        { pageUrl: "/page1", pv: 1000, uv: 500, avgDurationSec: 120.5 },
        { pageUrl: "/page2", pv: 800, uv: 400, avgDurationSec: 90.0 },
      ],
      total: 2,
    });
  }),

  http.get("/api/analytics/events-distribution", ({ request }) => {
    const url = new URL(request.url);
    const appId = url.searchParams.get("appId");

    if (!appId || appId.trim() === "") {
      return HttpResponse.json(
        {
          code: "BAD_REQUEST",
          message: "appId is required and cannot be empty",
        },
        { status: 400 },
      );
    }

    return HttpResponse.json({
      list: [
        { type: "page_view", value: 5000 },
        { type: "click", value: 3000 },
        { type: "error", value: 100 },
      ],
    });
  }),

  http.get("/api/analytics/web-vitals", ({ request }) => {
    const url = new URL(request.url);
    const appId = url.searchParams.get("appId");

    if (!appId || appId.trim() === "") {
      return HttpResponse.json(
        {
          code: "BAD_REQUEST",
          message: "appId is required and cannot be empty",
        },
        { status: 400 },
      );
    }

    return HttpResponse.json({
      p50: 1200.0,
      p75: 2040.0,
      p95: 3480.0,
      unit: "ms",
    });
  }),

  http.get("/api/analytics/web-vitals/series", ({ request }) => {
    const url = new URL(request.url);
    const appId = url.searchParams.get("appId");

    if (!appId || appId.trim() === "") {
      return HttpResponse.json(
        {
          code: "BAD_REQUEST",
          message: "appId is required and cannot be empty",
        },
        { status: 400 },
      );
    }

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
    const url = new URL(request.url);
    const appId = url.searchParams.get("appId");
    const groupBy = url.searchParams.get("groupBy") || "day";

    if (!appId || appId.trim() === "") {
      return HttpResponse.json(
        {
          code: "BAD_REQUEST",
          message: "appId is required and cannot be empty",
        },
        { status: 400 },
      );
    }

    return HttpResponse.json({
      series: [
        { ts: "2024-01-01", count: 100 },
        { ts: "2024-01-02", count: 150 },
      ],
      total: 250,
      groupBy,
    });
  }),

  http.get("/api/analytics/custom-events/top", ({ request }) => {
    const url = new URL(request.url);
    const appId = url.searchParams.get("appId");

    if (!appId || appId.trim() === "") {
      return HttpResponse.json(
        {
          code: "BAD_REQUEST",
          message: "appId is required and cannot be empty",
        },
        { status: 400 },
      );
    }

    return HttpResponse.json({
      list: [
        { eventId: "event1", count: 1000 },
        { eventId: "event2", count: 800 },
      ],
      total: 1800,
    });
  }),

  http.get("/api/analytics/errors/trend", ({ request }) => {
    const url = new URL(request.url);
    const appId = url.searchParams.get("appId");

    if (!appId || appId.trim() === "") {
      return HttpResponse.json(
        {
          code: "BAD_REQUEST",
          message: "appId is required and cannot be empty",
        },
        { status: 400 },
      );
    }

    return HttpResponse.json({
      series: [
        { ts: "2024-01-01T00:00:00", count: 10 },
        { ts: "2024-01-01T01:00:00", count: 15 },
      ],
      interval: "hour",
      timezone: "UTC",
    });
  }),

  http.get("/api/analytics/errors/top", ({ request }) => {
    const url = new URL(request.url);
    const appId = url.searchParams.get("appId");

    if (!appId || appId.trim() === "") {
      return HttpResponse.json(
        {
          code: "BAD_REQUEST",
          message: "appId is required and cannot be empty",
        },
        { status: 400 },
      );
    }

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
    const url = new URL(request.url);
    const appId = url.searchParams.get("appId");
    const page = parseInt(url.searchParams.get("page") || "1", 10);
    const size = parseInt(url.searchParams.get("size") || "50", 10);

    if (!appId || appId.trim() === "") {
      return HttpResponse.json(
        {
          code: "BAD_REQUEST",
          message: "appId is required and cannot be empty",
        },
        { status: 400 },
      );
    }

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
