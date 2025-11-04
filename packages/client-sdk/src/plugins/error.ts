import { Track } from "../core/tracker";
import { EventData, EventType } from "../types";
import { isBrowser } from "../utils";

/**
 * 错误监控插件
 * 监听 JavaScript 错误和 Promise 错误
 */
export class ErrorMonitor {
  private tracker: Track;
  private errorHandler: ((event: ErrorEvent) => void) | null = null;
  private unhandledRejectionHandler:
    | ((event: PromiseRejectionEvent) => void)
    | null = null;
  private monitored: boolean = false;

  constructor(tracker: Track) {
    this.tracker = tracker;
  }

  /**
   * 设置错误监控
   */
  setup(): void {
    if (!isBrowser() || this.monitored) {
      return;
    }

    // 监听 JavaScript 错误
    this.errorHandler = (event: ErrorEvent) => {
      this.handleError(event);
    };
    window.addEventListener("error", this.errorHandler, true);

    // 监听未处理的 Promise 错误
    this.unhandledRejectionHandler = (event: PromiseRejectionEvent) => {
      this.handleUnhandledRejection(event);
    };
    window.addEventListener(
      "unhandledrejection",
      this.unhandledRejectionHandler,
    );

    this.monitored = true;
  }

  /**
   * 移除错误监控
   */
  remove(): void {
    if (!isBrowser()) {
      return;
    }

    if (this.errorHandler) {
      window.removeEventListener("error", this.errorHandler, true);
      this.errorHandler = null;
    }

    if (this.unhandledRejectionHandler) {
      window.removeEventListener(
        "unhandledrejection",
        this.unhandledRejectionHandler,
      );
      this.unhandledRejectionHandler = null;
    }

    this.monitored = false;
  }

  /**
   * 处理 JavaScript 错误
   */
  private handleError(event: ErrorEvent): void {
    if (!this.tracker.isStarted()) {
      return;
    }

    try {
      const errorData: Record<string, any> = {
        // 错误信息
        message: event.message || "Unknown error",

        // 错误文件
        filename: event.filename || "",

        // 错误行号
        lineno: event.lineno || 0,

        // 错误列号
        colno: event.colno || 0,

        // 错误堆栈
        stack: event.error?.stack || "",

        // 错误类型
        type: event.error?.name || "Error",

        // 页面 URL
        pageUrl: window.location.href,

        // 页面标题
        pageTitle: document.title,

        // 用户代理
        userAgent: navigator.userAgent,

        // 时间戳
        timestamp: Date.now(),
      };

      // 上报错误事件
      const errorEvent: EventData = {
        type: EventType.ERROR,
        properties: errorData,
      };

      this.tracker.addEvent(errorEvent);
    } catch (e) {
      // 错误监控本身出错，不处理，避免循环
      console.warn("[Track SDK] Failed to handle error:", e);
    }
  }

  /**
   * 处理未处理的 Promise 错误
   */
  private handleUnhandledRejection(event: PromiseRejectionEvent): void {
    if (!this.tracker.isStarted()) {
      return;
    }

    try {
      const reason = event.reason;
      let errorMessage = "Unhandled Promise Rejection";
      let errorStack = "";
      let errorType = "PromiseRejection";

      if (reason instanceof Error) {
        errorMessage = reason.message;
        errorStack = reason.stack || "";
        errorType = reason.name || "Error";
      } else if (typeof reason === "string") {
        errorMessage = reason;
      } else if (typeof reason === "object" && reason !== null) {
        errorMessage = JSON.stringify(reason);
      }

      const errorData: Record<string, any> = {
        // 错误信息
        message: errorMessage,

        // 错误类型
        type: errorType,

        // 错误堆栈
        stack: errorStack,

        // Promise 拒绝原因
        reason: reason,

        // 页面 URL
        pageUrl: window.location.href,

        // 页面标题
        pageTitle: document.title,

        // 用户代理
        userAgent: navigator.userAgent,

        // 时间戳
        timestamp: Date.now(),
      };

      // 上报错误事件
      const errorEvent: EventData = {
        type: EventType.ERROR,
        properties: errorData,
      };

      this.tracker.addEvent(errorEvent);
    } catch (e) {
      // 错误监控本身出错，不处理，避免循环
      console.warn("[Track SDK] Failed to handle unhandled rejection:", e);
    }
  }
}
