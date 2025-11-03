/**
 * 性能数据采集器
 * @packageDocumentation
 */

import { Collector } from './types';
import { Tracker } from '../core/tracker';
import { PerformanceContent } from '../types';

/**
 * 性能数据采集器
 * 
 * @remarks
 * 采集页面性能指标，使用 requestIdleCallback 避免阻塞主线程
 */
export class PerformanceCollector implements Collector {
  private tracker: Tracker | null = null;
  private isStarted: boolean = false;
  private collected: boolean = false;

  /**
   * 获取采集器名称
   * 
   * @returns 采集器名称
   */
  getName(): string {
    return 'performance';
  }

  /**
   * 启动采集器
   * 
   * @param tracker - Tracker 实例
   */
  start(tracker: Tracker): void {
    if (this.isStarted) {
      return;
    }

    this.tracker = tracker;
    this.isStarted = true;

    // 等待页面加载完成后采集性能数据
    if (document.readyState === 'complete') {
      this.collectPerformance();
    } else {
      window.addEventListener('load', () => {
        this.collectPerformance();
      });
    }
  }

  /**
   * 停止采集器
   */
  stop(): void {
    this.isStarted = false;
    this.tracker = null;
  }

  /**
   * 采集性能数据
   */
  private collectPerformance(): void {
    if (this.collected || !this.tracker || typeof performance === 'undefined') {
      return;
    }

    // 使用 requestIdleCallback 避免阻塞主线程
    const scheduleCollection = (callback: () => void) => {
      if ('requestIdleCallback' in window) {
        requestIdleCallback(callback, { timeout: 2000 });
      } else {
        // 降级方案：延迟执行
        setTimeout(callback, 2000);
      }
    };

    scheduleCollection(() => {
      try {
        const performanceData = this.extractPerformanceData();
        if (performanceData && Object.keys(performanceData).length > 0) {
          this.tracker!.trackEvent('performance', performanceData);
          this.collected = true;
        }
      } catch (error) {
        // 忽略采集错误
        console.warn('Performance collection failed', error);
      }
    });
  }

  /**
   * 提取性能数据
   * 
   * @returns 性能数据对象
   */
  private extractPerformanceData(): Partial<PerformanceContent> {
    const data: Partial<PerformanceContent> = {};

    try {
      const timing = performance.timing;
      const navigation = performance.navigation;

      // 页面加载时间
      if (timing.loadEventEnd && timing.navigationStart) {
        data.loadTime = timing.loadEventEnd - timing.navigationStart;
      }

      // DOM 内容加载时间
      if (timing.domContentLoadedEventEnd && timing.navigationStart) {
        data.domContentLoaded =
          timing.domContentLoadedEventEnd - timing.navigationStart;
      }

      // 使用 Performance API v2 (如果可用)
      if ('getEntriesByType' in performance) {
        // 首次内容绘制 (FCP)
        const paintEntries = performance.getEntriesByType(
          'paint'
        ) as PerformancePaintTiming[];
        for (const entry of paintEntries) {
          if (entry.name === 'first-contentful-paint') {
            data.firstContentfulPaint = Math.round(entry.startTime);
          }
        }

        // 最大内容绘制 (LCP)
        if ('PerformanceObserver' in window) {
          try {
            const lcpObserver = new PerformanceObserver((list) => {
              const entries = list.getEntries();
              const lastEntry = entries[entries.length - 1] as
                | PerformanceEntry
                | undefined;
              if (lastEntry) {
                data.largestContentfulPaint = Math.round(lastEntry.startTime);
              }
            });
            lcpObserver.observe({ entryTypes: ['largest-contentful-paint'] });
            // 延迟获取 LCP
            setTimeout(() => {
              lcpObserver.disconnect();
            }, 5000);
          } catch (e) {
            // LCP 不支持
          }
        }

        // 资源加载信息
        const resourceEntries = performance.getEntriesByType(
          'resource'
        ) as PerformanceResourceTiming[];
        if (resourceEntries.length > 0) {
          data.resources = resourceEntries.slice(0, 20).map((entry) => ({
            name: entry.name,
            duration: Math.round(entry.duration),
            size: entry.transferSize || 0,
            type: this.getResourceType(entry.name),
          }));
        }
      }

      // 首次输入延迟 (FID)
      if ('PerformanceObserver' in window) {
        try {
          const fidObserver = new PerformanceObserver((list) => {
            const entries = list.getEntries();
            for (const entry of entries) {
              if (entry.entryType === 'first-input') {
                const fidEntry = entry as PerformanceEventTiming;
                data.firstInputDelay = Math.round(fidEntry.processingStart - fidEntry.startTime);
                fidObserver.disconnect();
                break;
              }
            }
          });
          fidObserver.observe({ entryTypes: ['first-input'] });
        } catch (e) {
          // FID 不支持
        }
      }
    } catch (error) {
      // 忽略错误
    }

    return data;
  }

  /**
   * 获取资源类型
   * 
   * @param url - 资源 URL
   * @returns 资源类型
   */
  private getResourceType(url: string): string {
    if (url.match(/\.(js|mjs|jsx|ts|tsx)$/i)) return 'script';
    if (url.match(/\.(css)$/i)) return 'stylesheet';
    if (url.match(/\.(png|jpg|jpeg|gif|svg|webp)$/i)) return 'image';
    if (url.match(/\.(woff|woff2|ttf|eot)$/i)) return 'font';
    if (url.match(/\.(mp4|webm|ogg)$/i)) return 'media';
    return 'other';
  }
}

