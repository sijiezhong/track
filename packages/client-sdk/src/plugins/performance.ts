import { Track } from "../core/tracker";
import { EventData, EventType } from "../types";
import { isBrowser } from "../utils";

/**
 * 性能监控插件
 * 使用 Performance API 采集页面性能指标
 */
export class PerformanceMonitor {
  private tracker: Track;
  private monitored: boolean = false;

  constructor(tracker: Track) {
    this.tracker = tracker;
  }

  /**
   * 设置性能监控
   */
  setup(): void {
    if (!isBrowser() || this.monitored) {
      return;
    }

    // 等待页面加载完成后再采集性能指标
    if (document.readyState === "complete") {
      this.collectPerformance();
    } else {
      window.addEventListener("load", () => {
        // 延迟一点时间，确保所有性能指标都已收集
        setTimeout(() => {
          this.collectPerformance();
        }, 1000);
      });
    }

    this.monitored = true;
  }

  /**
   * 移除性能监控
   */
  remove(): void {
    this.monitored = false;
  }

  /**
   * 采集性能指标
   */
  private collectPerformance(): void {
    if (!this.tracker.isStarted() || !window.performance) {
      return;
    }

    try {
      const timing = performance.timing;
      const navigation = (performance as any).navigation;

      // 计算关键性能指标
      const performanceData: Record<string, any> = {
        // DNS 查询时间
        dns: timing.domainLookupEnd - timing.domainLookupStart,

        // TCP 连接时间
        tcp: timing.connectEnd - timing.connectStart,

        // SSL 握手时间（如果使用 HTTPS）
        ssl:
          timing.secureConnectionStart > 0
            ? timing.connectEnd - timing.secureConnectionStart
            : 0,

        // TTFB (Time To First Byte)
        ttfb: timing.responseStart - timing.requestStart,

        // 响应时间
        response: timing.responseEnd - timing.responseStart,

        // DOM 解析时间
        domParse: timing.domInteractive - timing.responseEnd,

        // DOM 内容加载时间
        domContentLoaded:
          timing.domContentLoadedEventEnd - timing.domContentLoadedEventStart,

        // DOM 加载完成时间
        domComplete: timing.domComplete - timing.domInteractive,

        // 页面加载总时间
        loadTime: timing.loadEventEnd - timing.navigationStart,

        // 首次绘制时间 (First Paint)
        firstPaint: this.getFirstPaint(),

        // 首次内容绘制时间 (First Contentful Paint)
        fcp: this.getFirstContentfulPaint(),

        // 页面 URL
        pageUrl: window.location.href,

        // 页面标题
        pageTitle: document.title,

        // 时间戳
        timestamp: Date.now(),
      };

      // 添加导航类型
      if (navigation) {
        const navigationTypes: Record<number, string> = {
          0: "navigate",
          1: "reload",
          2: "back_forward",
          255: "reserved",
        };
        performanceData.navigationType =
          navigationTypes[navigation.type] || "unknown";
      }

      // 添加资源加载信息（如果可用）
      if (performance.getEntriesByType) {
        const resourceEntries = performance.getEntriesByType(
          "resource",
        ) as PerformanceResourceTiming[];
        if (resourceEntries.length > 0) {
          performanceData.resourceCount = resourceEntries.length;

          // 计算资源加载时间统计
          const resourceLoadTimes = resourceEntries
            .map((entry) => {
              if (entry.responseEnd && entry.startTime) {
                return entry.responseEnd - entry.startTime;
              }
              return 0;
            })
            .filter((time) => time > 0);

          if (resourceLoadTimes.length > 0) {
            performanceData.resourceAvgLoadTime =
              resourceLoadTimes.reduce((a, b) => a + b, 0) /
              resourceLoadTimes.length;
            performanceData.resourceMaxLoadTime = Math.max(
              ...resourceLoadTimes,
            );
          }
        }
      }

      // 上报性能事件
      const performanceEvent: EventData = {
        type: EventType.PERFORMANCE,
        properties: performanceData,
      };

      this.tracker.addEvent(performanceEvent);
    } catch (error) {
      console.warn("[Track SDK] Failed to collect performance metrics:", error);
    }
  }

  /**
   * 获取首次绘制时间 (First Paint)
   */
  private getFirstPaint(): number {
    if (!window.performance || !window.performance.getEntriesByType) {
      return 0;
    }

    try {
      const paintEntries = performance.getEntriesByType(
        "paint",
      ) as PerformancePaintTiming[];
      const firstPaint = paintEntries.find(
        (entry) => entry.name === "first-paint",
      );
      return firstPaint ? firstPaint.startTime : 0;
    } catch {
      return 0;
    }
  }

  /**
   * 获取首次内容绘制时间 (First Contentful Paint)
   */
  private getFirstContentfulPaint(): number {
    if (!window.performance || !window.performance.getEntriesByType) {
      return 0;
    }

    try {
      const paintEntries = performance.getEntriesByType(
        "paint",
      ) as PerformancePaintTiming[];
      const fcp = paintEntries.find(
        (entry) => entry.name === "first-contentful-paint",
      );
      return fcp ? fcp.startTime : 0;
    } catch {
      return 0;
    }
  }
}
