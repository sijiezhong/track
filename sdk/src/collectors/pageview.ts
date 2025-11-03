/**
 * 页面访问采集器
 * @packageDocumentation
 */

import { Collector } from './types';
import { Tracker } from '../core/tracker';

/**
 * 页面访问采集器
 * 
 * @remarks
 * 监听页面访问和 SPA 路由变化，自动上报页面访问事件
 */
export class PageViewCollector implements Collector {
  private tracker: Tracker | null = null;
  private originalPushState: typeof history.pushState | null = null;
  private originalReplaceState: typeof history.replaceState | null = null;
  private isStarted: boolean = false;
  private lastPageViewUrl: string | null = null;
  private initialPageViewTimer: number | null = null;

  /**
   * 获取采集器名称
   * 
   * @returns 采集器名称
   */
  getName(): string {
    return 'pageview';
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

    // 监听 popstate 事件（浏览器前进/后退）
    window.addEventListener('popstate', this.handlePopState);

    // 拦截 pushState 和 replaceState（SPA 路由变化）
    this.originalPushState = history.pushState;
    this.originalReplaceState = history.replaceState;

    history.pushState = new Proxy(history.pushState, {
      apply: (target, thisArg, args) => {
        const result = Reflect.apply(target, thisArg, args);
        this.handleRouteChange();
        return result;
      },
    });

    history.replaceState = new Proxy(history.replaceState, {
      apply: (target, thisArg, args) => {
        const result = Reflect.apply(target, thisArg, args);
        this.handleRouteChange();
        return result;
      },
    });

    // 初始页面访问（延迟一点确保页面信息完整）
    // 使用 timer 变量以便在 stop 时能够清理
    this.initialPageViewTimer = window.setTimeout(() => {
      this.initialPageViewTimer = null;
      this.trackPageView();
    }, 100);
  }

  /**
   * 停止采集器
   */
  stop(): void {
    if (!this.isStarted) {
      return;
    }

    this.isStarted = false;

    // 清理初始页面访问定时器
    if (this.initialPageViewTimer !== null) {
      clearTimeout(this.initialPageViewTimer);
      this.initialPageViewTimer = null;
    }

    // 移除事件监听
    window.removeEventListener('popstate', this.handlePopState);

    // 恢复原始方法
    if (this.originalPushState) {
      history.pushState = this.originalPushState;
    }
    if (this.originalReplaceState) {
      history.replaceState = this.originalReplaceState;
    }

    this.tracker = null;
    this.lastPageViewUrl = null;
  }

  /**
   * 处理 popstate 事件
   */
  private handlePopState = (): void => {
    this.trackPageView();
  };

  /**
   * 处理路由变化
   */
  private handleRouteChange(): void {
    // 延迟一下确保 URL 已更新
    setTimeout(() => {
      this.trackPageView();
    }, 0);
  }

  /**
   * 上报页面访问事件
   */
  private trackPageView(): void {
    if (!this.tracker) {
      return;
    }

    // 获取当前页面 URL，用于去重
    const currentUrl = typeof window !== 'undefined' ? window.location.href : '';
    
    // 防止短时间内重复上报相同 URL（去重窗口：500ms）
    // 如果 URL 相同且距离上次上报时间很短，跳过
    if (this.lastPageViewUrl === currentUrl) {
      // 如果 URL 相同，不重复上报（可能是路由变化但 URL 未变，或者重复触发）
      return;
    }

    this.lastPageViewUrl = currentUrl;
    this.tracker.trackPageView();

    // 500ms 后重置 URL 记录，允许同一页面再次上报（比如用户刷新）
    setTimeout(() => {
      // 只有在 URL 仍然相同时才重置（如果 URL 已变化，保持新 URL）
      if (this.lastPageViewUrl === currentUrl) {
        this.lastPageViewUrl = null;
      }
    }, 500);
  }
}

