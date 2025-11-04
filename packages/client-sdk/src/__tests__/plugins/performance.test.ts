import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { PerformanceMonitor } from "../../plugins/performance";
import { Track } from "../../core/tracker";
import { EventType } from "../../types";

describe("PerformanceMonitor", () => {
  let performanceMonitor: PerformanceMonitor;
  let track: Track;
  let addEventSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    vi.useFakeTimers();
    track = Track.getInstance();
    performanceMonitor = new PerformanceMonitor(track);

    vi.spyOn(track, "isStarted").mockReturnValue(true);
    addEventSpy = vi.spyOn(track, "addEvent").mockImplementation(() => {});

    // Mock performance API
    global.performance = {
      timing: {
        navigationStart: 1000,
        domainLookupStart: 1100,
        domainLookupEnd: 1200,
        connectStart: 1200,
        connectEnd: 1300,
        secureConnectionStart: 1250,
        requestStart: 1300,
        responseStart: 1400,
        responseEnd: 1500,
        domInteractive: 1600,
        domContentLoadedEventStart: 1700,
        domContentLoadedEventEnd: 1750,
        domComplete: 1800,
        loadEventEnd: 1900,
      },
      navigation: {
        type: 0, // navigate
      },
      getEntriesByType: vi.fn().mockReturnValue([]),
    } as any;
  });

  afterEach(() => {
    vi.useRealTimers();
    performanceMonitor.remove();
    vi.restoreAllMocks();
  });

  describe("性能指标采集", () => {
    it("should collect performance metrics on page load", () => {
      Object.defineProperty(document, "readyState", {
        writable: true,
        value: "complete",
      });

      performanceMonitor.setup();
      performanceMonitor.collectPerformance();

      expect(addEventSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          type: EventType.PERFORMANCE,
          properties: expect.objectContaining({
            dns: 100,
            tcp: 100,
            ssl: 50,
            ttfb: 100,
            response: 100,
            domParse: 100,
            domContentLoaded: 50,
            domComplete: 200,
            loadTime: 900,
            pageUrl: window.location.href,
            pageTitle: document.title,
            navigationType: "navigate",
          }),
        }),
      );
    });

    it("should collect performance metrics after load event", async () => {
      Object.defineProperty(document, "readyState", {
        writable: true,
        value: "loading",
      });

      performanceMonitor.setup();

      const loadEvent = new Event("load");
      window.dispatchEvent(loadEvent);

      await vi.advanceTimersByTimeAsync(1000);

      expect(addEventSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          type: EventType.PERFORMANCE,
        }),
      );
    });

    it("should handle missing SSL connection gracefully", () => {
      Object.defineProperty(document, "readyState", {
        writable: true,
        value: "complete",
      });

      (global.performance as any).timing.secureConnectionStart = 0;

      performanceMonitor.setup();
      performanceMonitor.collectPerformance();

      expect(addEventSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          properties: expect.objectContaining({
            ssl: 0,
          }),
        }),
      );
    });

    it("should include resource loading statistics when available", () => {
      Object.defineProperty(document, "readyState", {
        writable: true,
        value: "complete",
      });

      const mockResourceEntries: PerformanceResourceTiming[] = [
        {
          startTime: 1000,
          responseEnd: 1500,
        } as PerformanceResourceTiming,
        {
          startTime: 1200,
          responseEnd: 1800,
        } as PerformanceResourceTiming,
        {
          startTime: 1300,
          responseEnd: 2000,
        } as PerformanceResourceTiming,
      ];

      (global.performance as any).getEntriesByType = vi
        .fn()
        .mockReturnValue(mockResourceEntries);

      performanceMonitor.setup();
      performanceMonitor.collectPerformance();

      expect(addEventSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          properties: expect.objectContaining({
            resourceCount: 3,
            resourceAvgLoadTime: (500 + 600 + 700) / 3,
            resourceMaxLoadTime: 700,
          }),
        }),
      );
    });

    it("should handle different navigation types", () => {
      Object.defineProperty(document, "readyState", {
        writable: true,
        value: "complete",
      });

      const navigationTypes = [0, 1, 2, 255];

      navigationTypes.forEach((navType) => {
        (global.performance as any).navigation.type = navType;
        addEventSpy.mockClear();

        performanceMonitor.collectPerformance();

        expect(addEventSpy).toHaveBeenCalled();
        const call = addEventSpy.mock.calls[0][0];
        expect(call.properties).toHaveProperty("navigationType");
      });
    });
  });

  describe("First Paint 和 FCP", () => {
    it("should get First Paint time", () => {
      Object.defineProperty(document, "readyState", {
        writable: true,
        value: "complete",
      });

      const mockPaintEntries: PerformancePaintTiming[] = [
        {
          name: "first-paint",
          startTime: 500,
        } as PerformancePaintTiming,
        {
          name: "first-contentful-paint",
          startTime: 600,
        } as PerformancePaintTiming,
      ];

      (global.performance as any).getEntriesByType = vi
        .fn()
        .mockImplementation((type: string) => {
          if (type === "paint") return mockPaintEntries;
          return [];
        });

      performanceMonitor.setup();
      performanceMonitor.collectPerformance();

      expect(addEventSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          properties: expect.objectContaining({
            firstPaint: 500,
            fcp: 600,
          }),
        }),
      );
    });

    it("should return 0 when paint entries are not available", () => {
      Object.defineProperty(document, "readyState", {
        writable: true,
        value: "complete",
      });

      (global.performance as any).getEntriesByType = vi
        .fn()
        .mockReturnValue([]);

      performanceMonitor.setup();
      performanceMonitor.collectPerformance();

      expect(addEventSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          properties: expect.objectContaining({
            firstPaint: 0,
            fcp: 0,
          }),
        }),
      );
    });
  });

  describe("错误处理", () => {
    it("should handle missing performance API gracefully", () => {
      Object.defineProperty(document, "readyState", {
        writable: true,
        value: "complete",
      });

      const originalPerformance = global.performance;
      (global as any).performance = undefined;

      const consoleSpy = vi.spyOn(console, "warn").mockImplementation(() => {});

      performanceMonitor.setup();

      // 手动调用 collectPerformance，因为 setup 中的检查会阻止调用
      (performanceMonitor as any).collectPerformance();

      expect(addEventSpy).not.toHaveBeenCalled();
      // 由于 collectPerformance 内部有检查，console.warn 可能不会被调用
      // 但至少不应该抛出错误

      global.performance = originalPerformance;
      consoleSpy.mockRestore();
    });

    it("should handle performance collection errors gracefully", () => {
      Object.defineProperty(document, "readyState", {
        writable: true,
        value: "complete",
      });

      (global.performance as any).timing = null;

      const consoleSpy = vi.spyOn(console, "warn").mockImplementation(() => {});

      performanceMonitor.setup();
      performanceMonitor.collectPerformance();

      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });
  });

  describe("setup/remove", () => {
    it("should not setup when already monitored", () => {
      Object.defineProperty(document, "readyState", {
        writable: true,
        value: "complete",
      });

      performanceMonitor.setup();
      addEventSpy.mockClear();
      performanceMonitor.setup();

      // 第二次 setup 不应该再次采集
      expect(addEventSpy).not.toHaveBeenCalled();
    });

    it("should not collect when tracker is not started", () => {
      Object.defineProperty(document, "readyState", {
        writable: true,
        value: "complete",
      });

      vi.spyOn(track, "isStarted").mockReturnValue(false);

      performanceMonitor.setup();
      performanceMonitor.collectPerformance();

      expect(addEventSpy).not.toHaveBeenCalled();
    });
  });
});
