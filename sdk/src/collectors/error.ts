/**
 * 错误采集器
 * @packageDocumentation
 */

import { Collector } from './types';
import { Tracker } from '../core/tracker';
import { ErrorContent } from '../types';

/**
 * 错误采集器
 * 
 * @remarks
 * 监听 JavaScript 错误和未处理的 Promise  rejection
 */
export class ErrorCollector implements Collector {
  private tracker: Tracker | null = null;
  private errorHandler: ((e: ErrorEvent) => void) | null = null;
  private unhandledRejectionHandler:
    | ((e: PromiseRejectionEvent) => void)
    | null = null;
  private isStarted: boolean = false;

  /**
   * 获取采集器名称
   * 
   * @returns 采集器名称
   */
  getName(): string {
    return 'error';
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

    // 监听 JavaScript 错误
    this.errorHandler = this.handleError.bind(this);
    window.addEventListener('error', this.errorHandler, true);

    // 监听未处理的 Promise rejection
    this.unhandledRejectionHandler = this.handleUnhandledRejection.bind(this);
    window.addEventListener(
      'unhandledrejection',
      this.unhandledRejectionHandler
    );
  }

  /**
   * 停止采集器
   */
  stop(): void {
    if (!this.isStarted) {
      return;
    }

    this.isStarted = false;

    if (this.errorHandler) {
      window.removeEventListener('error', this.errorHandler, true);
      this.errorHandler = null;
    }

    if (this.unhandledRejectionHandler) {
      window.removeEventListener(
        'unhandledrejection',
        this.unhandledRejectionHandler
      );
      this.unhandledRejectionHandler = null;
    }

    this.tracker = null;
  }

  /**
   * 处理 JavaScript 错误
   */
  private handleError(event: ErrorEvent): void {
    if (!this.tracker) {
      return;
    }

    // 忽略某些错误（如同源策略错误）
    if (
      event.message &&
      (event.message.includes('Script error') ||
        event.message.includes('Non-Error promise rejection'))
    ) {
      return;
    }

    const content: ErrorContent = {
      errorType: 'javascript',
      message: event.message || 'Unknown error',
      filename: event.filename || undefined,
      lineno: event.lineno || undefined,
      colno: event.colno || undefined,
    };

    // 尝试获取错误堆栈
    if (event.error && event.error.stack) {
      content.stack = event.error.stack;
    }

    // 资源加载错误
    if (event.target && event.target !== window) {
      const target = event.target as HTMLElement;
      content.errorType = 'resource';
      content.resourceError = {
        tagName: target.tagName,
      };

      if (target instanceof HTMLImageElement) {
        content.resourceError.src = target.src;
      } else if (target instanceof HTMLLinkElement) {
        content.resourceError.href = target.href;
      } else if (target instanceof HTMLScriptElement) {
        content.resourceError.src = target.src;
      }
    }

    this.tracker.trackEvent('error', content);
  }

  /**
   * 处理未处理的 Promise rejection
   */
  private handleUnhandledRejection(event: PromiseRejectionEvent): void {
    if (!this.tracker) {
      return;
    }

    const content: ErrorContent = {
      errorType: 'promise',
      message: 'Unhandled Promise Rejection',
    };

    // 提取错误信息
    if (event.reason) {
      if (event.reason instanceof Error) {
        content.message = event.reason.message;
        content.stack = event.reason.stack;
      } else if (typeof event.reason === 'string') {
        content.message = event.reason;
      } else {
        content.message = JSON.stringify(event.reason);
      }
    }

    this.tracker.trackEvent('error', content);
  }
}

