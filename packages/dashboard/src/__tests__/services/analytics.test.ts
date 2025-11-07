import { describe, it, expect } from "vitest";
import * as analytics from "@/services/analytics";

describe("analytics service", () => {
  const testParams = {
    appId: "test-app",
    start: "2024-01-01T00:00:00",
    end: "2024-01-07T23:59:59",
  };

  describe("getOverview", () => {
    it("should return overview data with correct structure", async () => {
      const result = await analytics.getOverview(testParams);

      expect(result).toHaveProperty("pv");
      expect(result).toHaveProperty("uv");
      expect(result).toHaveProperty("bounceRate");
      expect(result).toHaveProperty("avgDurationSec");
      expect(result).toHaveProperty("timezone");

      expect(typeof result.pv).toBe("number");
      expect(typeof result.uv).toBe("number");
      expect(typeof result.bounceRate).toBe("number");
      expect(typeof result.avgDurationSec).toBe("number");
      expect(typeof result.timezone).toBe("string");
    });

    it("should require appId", async () => {
      // appId 现在是必填的，不传应该会报错
      await expect(
        analytics.getOverview({
          start: testParams.start,
          end: testParams.end,
        } as any),
      ).rejects.toThrow();
    });

    it("should handle optional start and end", async () => {
      const result = await analytics.getOverview({
        appId: testParams.appId,
      });

      expect(result).toBeDefined();
    });
  });

  describe("getPVUVSeries", () => {
    it("should return PV/UV series with correct structure", async () => {
      const result = await analytics.getPVUVSeries({
        ...testParams,
        interval: "hour",
      });

      expect(result).toHaveProperty("series");
      expect(result).toHaveProperty("interval");
      expect(result).toHaveProperty("timezone");

      expect(Array.isArray(result.series)).toBe(true);
      expect(result.series.length).toBeGreaterThan(0);

      if (result.series.length > 0) {
        expect(result.series[0]).toHaveProperty("ts");
        expect(result.series[0]).toHaveProperty("pv");
        expect(result.series[0]).toHaveProperty("uv");

        expect(typeof result.series[0].pv).toBe("number");
        expect(typeof result.series[0].uv).toBe("number");
      }
    });

    it("should support different intervals", async () => {
      const hourResult = await analytics.getPVUVSeries({
        ...testParams,
        interval: "hour",
      });
      const dayResult = await analytics.getPVUVSeries({
        ...testParams,
        interval: "day",
      });

      expect(hourResult.interval).toBe("hour");
      expect(dayResult.interval).toBe("day");
    });
  });

  describe("getPagesTop", () => {
    it("should return pages top list with correct structure", async () => {
      const result = await analytics.getPagesTop({
        ...testParams,
        limit: 10,
      });

      expect(result).toHaveProperty("list");
      expect(result).toHaveProperty("total");

      expect(Array.isArray(result.list)).toBe(true);
      expect(typeof result.total).toBe("number");

      if (result.list.length > 0) {
        expect(result.list[0]).toHaveProperty("pageUrl");
        expect(result.list[0]).toHaveProperty("pv");
        expect(result.list[0]).toHaveProperty("uv");
        expect(result.list[0]).toHaveProperty("avgDurationSec");
      }
    });

    it("should respect limit parameter", async () => {
      const result = await analytics.getPagesTop({
        ...testParams,
        limit: 5,
      });

      expect(result.list.length).toBeLessThanOrEqual(5);
    });
  });

  describe("getEventsDistribution", () => {
    it("should return events distribution with correct structure", async () => {
      const result = await analytics.getEventsDistribution(testParams);

      expect(result).toHaveProperty("list");
      expect(Array.isArray(result.list)).toBe(true);

      if (result.list.length > 0) {
        expect(result.list[0]).toHaveProperty("type");
        expect(result.list[0]).toHaveProperty("value");

        expect(typeof result.list[0].type).toBe("string");
        expect(typeof result.list[0].value).toBe("number");
      }
    });
  });

  describe("getWebVitals", () => {
    it("should return web vitals with correct structure", async () => {
      const result = await analytics.getWebVitals({
        ...testParams,
        metric: "LCP",
      });

      expect(result).toHaveProperty("p50");
      expect(result).toHaveProperty("p75");
      expect(result).toHaveProperty("p95");
      expect(result).toHaveProperty("unit");

      expect(typeof result.p50).toBe("number");
      expect(typeof result.p75).toBe("number");
      expect(typeof result.p95).toBe("number");
      expect(typeof result.unit).toBe("string");

      // 验证分位数逻辑：p50 < p75 < p95
      expect(result.p50).toBeLessThanOrEqual(result.p75);
      expect(result.p75).toBeLessThanOrEqual(result.p95);
    });

    it("should support different metrics", async () => {
      const lcpResult = await analytics.getWebVitals({
        ...testParams,
        metric: "LCP",
      });
      const fidResult = await analytics.getWebVitals({
        ...testParams,
        metric: "FID",
      });

      expect(lcpResult).toBeDefined();
      expect(fidResult).toBeDefined();
    });
  });

  describe("getWebVitalsSeries", () => {
    it("should return web vitals series with correct structure", async () => {
      const result = await analytics.getWebVitalsSeries({
        ...testParams,
        metric: "LCP",
        interval: "hour",
      });

      expect(result).toHaveProperty("series");
      expect(result).toHaveProperty("interval");
      expect(result).toHaveProperty("timezone");

      expect(Array.isArray(result.series)).toBe(true);

      if (result.series.length > 0) {
        expect(result.series[0]).toHaveProperty("ts");
        expect(result.series[0]).toHaveProperty("p50");
        expect(result.series[0]).toHaveProperty("p75");
        expect(result.series[0]).toHaveProperty("p95");
      }
    });
  });

  describe("getCustomEvents", () => {
    it("should return custom events series with correct structure", async () => {
      const result = await analytics.getCustomEvents({
        ...testParams,
        groupBy: "day",
      });

      expect(result).toHaveProperty("series");
      expect(result).toHaveProperty("total");
      expect(result).toHaveProperty("groupBy");

      expect(Array.isArray(result.series)).toBe(true);
      expect(typeof result.total).toBe("number");
      expect(typeof result.groupBy).toBe("string");

      if (result.series.length > 0) {
        expect(result.series[0]).toHaveProperty("ts");
        expect(result.series[0]).toHaveProperty("count");
      }
    });

    it("should support eventId filter", async () => {
      const result = await analytics.getCustomEvents({
        ...testParams,
        eventId: "button_click",
        groupBy: "hour",
      });

      expect(result).toBeDefined();
      expect(result.groupBy).toBe("hour");
    });
  });

  describe("getCustomEventsTop", () => {
    it("should return custom events top with correct structure", async () => {
      const result = await analytics.getCustomEventsTop({
        ...testParams,
        limit: 10,
      });

      expect(result).toHaveProperty("list");
      expect(result).toHaveProperty("total");

      expect(Array.isArray(result.list)).toBe(true);
      expect(typeof result.total).toBe("number");

      if (result.list.length > 0) {
        expect(result.list[0]).toHaveProperty("eventId");
        expect(result.list[0]).toHaveProperty("count");
      }
    });
  });

  describe("getErrorsTrend", () => {
    it("should return errors trend with correct structure", async () => {
      const result = await analytics.getErrorsTrend({
        ...testParams,
        interval: "hour",
      });

      expect(result).toHaveProperty("series");
      expect(result).toHaveProperty("interval");
      expect(result).toHaveProperty("timezone");

      expect(Array.isArray(result.series)).toBe(true);

      if (result.series.length > 0) {
        expect(result.series[0]).toHaveProperty("ts");
        expect(result.series[0]).toHaveProperty("count");
      }
    });
  });

  describe("getErrorsTop", () => {
    it("should return errors top with correct structure", async () => {
      const result = await analytics.getErrorsTop({
        ...testParams,
        limit: 10,
      });

      expect(result).toHaveProperty("list");
      expect(result).toHaveProperty("total");

      expect(Array.isArray(result.list)).toBe(true);
      expect(typeof result.total).toBe("number");

      if (result.list.length > 0) {
        expect(result.list[0]).toHaveProperty("fingerprint");
        expect(result.list[0]).toHaveProperty("message");
        expect(result.list[0]).toHaveProperty("count");
        expect(result.list[0]).toHaveProperty("firstSeen");
        expect(result.list[0]).toHaveProperty("lastSeen");
      }
    });
  });
});
