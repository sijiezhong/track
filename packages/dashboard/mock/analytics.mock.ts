import Mock from "mockjs";
import type { MockDef } from "./common";
import { ok, maybeError } from "./common";

const defs: MockDef[] = [
  {
    url: "/api/analytics/overview",
    method: "get",
    response: ({ url }) => {
      const err = maybeError(url);
      if (err) return err;
      return ok({
        pv: Mock.Random.integer(1000, 100000),
        uv: Mock.Random.integer(500, 80000),
        bounceRate: Mock.Random.float(0.2, 0.8, 2, 2),
        avgDurationSec: Mock.Random.integer(10, 600),
        timezone: "Asia/Shanghai",
      });
    },
  },
  {
    url: "/api/analytics/pv-uv/series",
    method: "get",
    response: ({ url }) => {
      const err = maybeError(url);
      if (err) return err;
      const series = Array.from({ length: 24 }).map((_, i) => ({
        ts: `${String(i).padStart(2, "0")}:00`,
        pv: Mock.Random.integer(10, 500),
        uv: Mock.Random.integer(5, 300),
      }));
      return ok({ series, interval: "hour", timezone: "Asia/Shanghai" });
    },
  },
  {
    url: "/api/analytics/pages/top",
    method: "get",
    response: ({ url }) => {
      const err = maybeError(url);
      if (err) return err;
      const list = Array.from({ length: 10 }).map(() => ({
        pageUrl: Mock.Random.url("http"),
        pv: Mock.Random.integer(100, 5000),
        uv: Mock.Random.integer(50, 4000),
        avgDurationSec: Mock.Random.integer(5, 300),
      }));
      return ok({ list, total: list.length });
    },
  },
  {
    url: "/api/analytics/events-distribution",
    method: "get",
    response: ({ url }) => {
      const err = maybeError(url);
      if (err) return err;
      const list = [
        { type: "page_view", value: Mock.Random.integer(1000, 5000) },
        { type: "click", value: Mock.Random.integer(800, 4000) },
        { type: "performance", value: Mock.Random.integer(100, 800) },
        { type: "error", value: Mock.Random.integer(50, 300) },
        { type: "custom", value: Mock.Random.integer(200, 1500) },
      ];
      return ok({ list });
    },
  },
  {
    url: "/api/analytics/web-vitals",
    method: "get",
    response: ({ url, query }) => {
      const err = maybeError(url);
      if (err) return err;
      const metric = (query as any).metric || "LCP";
      const base =
        metric === "LCP"
          ? 1200
          : metric === "FID"
            ? 50
            : metric === "CLS"
              ? 0.1
              : 1000;
      return ok({
        p50: base,
        p75: base * 1.7,
        p95: base * 2.9,
        unit: metric === "CLS" ? "score" : "ms",
      });
    },
  },
  {
    url: "/api/analytics/custom-events",
    method: "get",
    response: ({ url, query }) => {
      const err = maybeError(url);
      if (err) return err;
      const groupBy = (query as any).groupBy || "day";
      const length = groupBy === "day" ? 7 : 24;
      const series = Array.from({ length }).map((_, i) => ({
        ts:
          groupBy === "day"
            ? `${i + 1}天前`
            : `${String(i).padStart(2, "0")}:00`,
        count: Mock.Random.integer(10, 500),
      }));
      return ok({
        series,
        total: series.reduce((sum, s) => sum + s.count, 0),
        groupBy,
      });
    },
  },
  {
    url: "/api/analytics/custom-events/top",
    method: "get",
    response: ({ url, query }) => {
      const err = maybeError(url);
      if (err) return err;
      const limit = Number((query as any).limit || 50);
      const list = Array.from({ length: Math.min(limit, 20) }).map(() => ({
        eventId: Mock.Random.pick([
          "button_click",
          "form_submit",
          "video_play",
          "product_view",
          "checkout_start",
          "purchase_complete",
          "share_click",
          "download_start",
        ]),
        count: Mock.Random.integer(100, 5000),
      }));
      return ok({ list, total: list.length });
    },
  },
  {
    url: "/api/analytics/web-vitals/series",
    method: "get",
    response: ({ url, query }) => {
      const err = maybeError(url);
      if (err) return err;
      const interval = (query as any).interval || "hour";
      const length = interval === "day" ? 7 : 24;
      const base = 1200;
      const series = Array.from({ length }).map((_, i) => ({
        ts:
          interval === "day"
            ? `${i + 1}天前`
            : `${String(i).padStart(2, "0")}:00`,
        p50: base + Mock.Random.integer(-200, 200),
        p75: base * 1.7 + Mock.Random.integer(-300, 300),
        p95: base * 2.9 + Mock.Random.integer(-500, 500),
      }));
      return ok({ series, interval, timezone: "Asia/Shanghai" });
    },
  },
  {
    url: "/api/analytics/errors/trend",
    method: "get",
    response: ({ url, query }) => {
      const err = maybeError(url);
      if (err) return err;
      const interval = (query as any).interval || "hour";
      const length = interval === "day" ? 7 : 24;
      const series = Array.from({ length }).map((_, i) => ({
        ts:
          interval === "day"
            ? `${i + 1}天前`
            : `${String(i).padStart(2, "0")}:00`,
        count: Mock.Random.integer(0, 50),
      }));
      return ok({ series, interval, timezone: "Asia/Shanghai" });
    },
  },
  {
    url: "/api/analytics/errors/top",
    method: "get",
    response: ({ url, query }) => {
      const err = maybeError(url);
      if (err) return err;
      const limit = Number((query as any).limit || 50);
      const errorMessages = [
        'Uncaught TypeError: Cannot read property "length" of undefined',
        "NetworkError: Failed to fetch",
        "SyntaxError: Unexpected token }",
        "ReferenceError: variable is not defined",
        'TypeError: Cannot set property "value" of null',
        "RangeError: Maximum call stack size exceeded",
        "Uncaught Promise Rejection: [object Object]",
        "Failed to load resource: the server responded with a status of 404",
      ];
      const list = Array.from({ length: Math.min(limit, 20) }).map(() => ({
        fingerprint: Mock.Random.string("lower", 16),
        message: Mock.Random.pick(errorMessages),
        count: Mock.Random.integer(10, 500),
        firstSeen: Mock.Random.datetime("yyyy-MM-dd HH:mm:ss"),
        lastSeen: Mock.Random.datetime("yyyy-MM-dd HH:mm:ss"),
      }));
      return ok({ list, total: list.length });
    },
  },
];

export default defs;
