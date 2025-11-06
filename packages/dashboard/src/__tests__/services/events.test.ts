import { describe, it, expect } from "vitest";
import * as events from "@/services/events";

describe("events service", () => {
  const testParams = {
    appId: "test-app",
    start: "2024-01-01T00:00:00",
    end: "2024-01-07T23:59:59",
  };

  describe("getEvents", () => {
    it("should return events list with correct structure", async () => {
      const result = await events.getEvents(testParams);

      expect(result).toHaveProperty("items");
      expect(result).toHaveProperty("page");

      expect(Array.isArray(result.items)).toBe(true);
      expect(result.page).toHaveProperty("index");
      expect(result.page).toHaveProperty("size");
      expect(result.page).toHaveProperty("total");

      expect(typeof result.page.index).toBe("number");
      expect(typeof result.page.size).toBe("number");
      expect(typeof result.page.total).toBe("number");
    });

    it("should handle optional appId", async () => {
      const result = await events.getEvents({
        start: testParams.start,
        end: testParams.end,
      });

      expect(result).toBeDefined();
      expect(Array.isArray(result.items)).toBe(true);
    });

    it("should support pagination", async () => {
      const result = await events.getEvents({
        ...testParams,
        page: 1,
        size: 20,
      });

      expect(result.page.index).toBe(1);
      expect(result.page.size).toBe(20);
    });

    it("should support default pagination", async () => {
      const result = await events.getEvents({
        appId: testParams.appId,
      });

      expect(result.page.index).toBeGreaterThanOrEqual(1);
      expect(result.page.size).toBeGreaterThan(0);
    });

    it("should support type filter", async () => {
      const result = await events.getEvents({
        ...testParams,
        type: 1, // PAGE_VIEW
      });

      expect(result).toBeDefined();
      expect(Array.isArray(result.items)).toBe(true);
    });

    it("should support keyword filter", async () => {
      const result = await events.getEvents({
        ...testParams,
        keyword: "search",
      });

      expect(result).toBeDefined();
      expect(Array.isArray(result.items)).toBe(true);
    });

    it("should return events with required fields", async () => {
      const result = await events.getEvents(testParams);

      if (result.items.length > 0) {
        const event = result.items[0];
        expect(event).toBeDefined();
        // 验证事件对象是有效的对象
        expect(typeof event).toBe("object");
      }
    });
  });
});
