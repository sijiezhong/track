import { Track } from "../core/tracker";
import { EventData, EventType } from "../types";
import { isBrowser } from "../utils";

/**
 * 自动采集管理器
 * 负责 PV 采集、页面停留时长采集和 SPA 路由监听
 */
export class AutoTrack {
  private tracker: Track;
  private pageEnterTime: number = 0;
  private historyIntercepted: boolean = false;
  private originalPushState: typeof history.pushState;
  private originalReplaceState: typeof history.replaceState;
  private pageViewHandlers: Array<() => void> = [];
  private pageStayHandlers: Array<() => void> = [];

  constructor(tracker: Track) {
    this.tracker = tracker;
    this.originalPushState = history.pushState;
    this.originalReplaceState = history.replaceState;
  }

  /**
   * 设置自动采集
   */
  setupAutoTrack(): void {
    if (!isBrowser()) {
      return;
    }

    // 记录页面进入时间
    this.pageEnterTime = Date.now();

    // 监听页面加载（首次加载）
    if (document.readyState === "complete") {
      this.trackPageView();
    } else {
      window.addEventListener("load", () => {
        this.trackPageView();
      });
    }

    // 监听 SPA 路由变化
    this.interceptHistoryMethods();

    // 监听 hash 变化（hash 路由）
    window.addEventListener("hashchange", () => {
      this.trackPageView();
    });

    // 监听 popstate（浏览器前进/后退）
    window.addEventListener("popstate", () => {
      this.trackPageView();
    });

    // 监听页面离开（计算停留时长）
    window.addEventListener("pagehide", () => {
      this.trackPageStay();
    });

    // 监听 visibilitychange（页面切换到后台时计算停留时长）
    document.addEventListener("visibilitychange", () => {
      if (document.hidden) {
        this.trackPageStay();
      } else {
        // 页面重新可见时，重新记录进入时间
        this.pageEnterTime = Date.now();
      }
    });
  }

  /**
   * 移除自动采集
   */
  removeAutoTrack(): void {
    if (!isBrowser()) {
      return;
    }

    // 恢复原始 History API
    if (this.historyIntercepted) {
      history.pushState = this.originalPushState;
      history.replaceState = this.originalReplaceState;
      this.historyIntercepted = false;
    }

    // 移除所有事件监听器
    this.pageViewHandlers.forEach((handler) => {
      window.removeEventListener("load", handler);
      window.removeEventListener("hashchange", handler);
      window.removeEventListener("popstate", handler);
    });

    this.pageStayHandlers.forEach((handler) => {
      window.removeEventListener("pagehide", handler);
      document.removeEventListener("visibilitychange", handler);
    });

    this.pageViewHandlers = [];
    this.pageStayHandlers = [];
  }

  /**
   * 上报页面浏览事件（PV）
   */
  private trackPageView(): void {
    if (!this.tracker.isStarted()) {
      return;
    }

    const pageViewEvent: EventData = {
      type: EventType.PAGE_VIEW,
      properties: {
        pageUrl: window.location.href,
        pageTitle: document.title,
        referrer: document.referrer,
        timestamp: Date.now(),
      },
    };

    this.tracker.addEvent(pageViewEvent);

    // 重置页面进入时间（用于计算停留时长）
    this.pageEnterTime = Date.now();
  }

  /**
   * 上报页面停留时长事件
   */
  private trackPageStay(): void {
    if (!this.tracker.isStarted() || this.pageEnterTime === 0) {
      return;
    }

    const now = Date.now();
    const stayDuration = now - this.pageEnterTime;

    // 只上报停留时长大于 0 的事件
    if (stayDuration > 0) {
      const pageStayEvent: EventData = {
        type: EventType.PAGE_STAY,
        properties: {
          pageUrl: window.location.href,
          pageTitle: document.title,
          duration: stayDuration, // 停留时长（毫秒）
          timestamp: now,
        },
      };

      this.tracker.addEvent(pageStayEvent);
    }

    // 重置进入时间
    this.pageEnterTime = 0;
  }

  /**
   * 拦截 History API（用于 SPA 路由变化检测）
   */
  private interceptHistoryMethods(): void {
    if (this.historyIntercepted) {
      return;
    }

    const self = this;

    // 拦截 pushState
    history.pushState = function (
      ...args: Parameters<typeof history.pushState>
    ) {
      self.originalPushState.apply(history, args);
      // 使用 setTimeout 确保 DOM 更新完成
      setTimeout(() => {
        self.trackPageView();
      }, 0);
    };

    // 拦截 replaceState
    history.replaceState = function (
      ...args: Parameters<typeof history.replaceState>
    ) {
      self.originalReplaceState.apply(history, args);
      setTimeout(() => {
        self.trackPageView();
      }, 0);
    };

    this.historyIntercepted = true;
  }
}
