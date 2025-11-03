/**
 * 点击事件采集器
 * @packageDocumentation
 */

import { Collector } from './types';
import { Tracker } from '../core/tracker';
import { ClickFilter } from './click-filter';
import { createLogger, Logger } from '../utils/logger';

/**
 * 点击事件采集器
 * 
 * @remarks
 * 监听页面点击事件，自动上报点击数据
 */
export class ClickCollector implements Collector {
  private tracker: Tracker | null = null;
  private clickHandler: ((e: MouseEvent) => void) | null = null;
  private isStarted: boolean = false;
  private debounceTimer: number | null = null;
  private debounceDelay: number = 200; // 防抖延迟 200ms，给手动上报留出时间
  /** 最近一次点击的时间戳，用于防止重复上报 */
  private lastClickTime: number = 0;
  /** 点击去重的时间窗口（毫秒） */
  private readonly CLICK_DEDUP_WINDOW = 300;
  /** 最近一次上报的元素标识（用于去重） */
  private lastReportedElement: string | null = null;
  /** 点击过滤器 */
  private filter: ClickFilter | null = null;
  /** 日志器 */
  private logger: Logger | null = null;

  /**
   * 获取采集器名称
   * 
   * @returns 采集器名称
   */
  getName(): string {
    return 'click';
  }

  /**
   * 启动采集器
   * 
   * @param tracker - Tracker 实例
   */
  start(tracker: Tracker): void {
    if (this.isStarted) {
      console.warn('[ClickCollector] Already started, skipping...');
      return;
    }

    // 防御性检查：确保之前的监听器已被移除（防止重复绑定）
    if (this.clickHandler) {
      console.warn('[ClickCollector] Found existing click handler, removing first...');
      document.removeEventListener('click', this.clickHandler, true);
      this.clickHandler = null;
    }

    this.tracker = tracker;
    this.isStarted = true;

    // 初始化日志器
    this.logger = createLogger(tracker.getConfig().debug);

    // 初始化过滤器
    const filterConfig = tracker.getConfig().clickFilter;
    if (filterConfig) {
      this.filter = new ClickFilter(filterConfig);
    } else {
      // 使用默认配置
      this.filter = new ClickFilter({ enabled: true });
    }

    // 使用事件委托到 document
    this.clickHandler = this.handleClick.bind(this);
    document.addEventListener('click', this.clickHandler, true); // 使用捕获阶段
  }

  /**
   * 停止采集器
   */
  stop(): void {
    if (!this.isStarted) {
      return;
    }

    this.isStarted = false;

    if (this.clickHandler) {
      document.removeEventListener('click', this.clickHandler, true);
      this.clickHandler = null;
    }

    if (this.debounceTimer !== null) {
      clearTimeout(this.debounceTimer);
      this.debounceTimer = null;
    }

    // 重置状态
    this.lastClickTime = 0;
    this.lastReportedElement = null;
    this.filter = null;
    this.logger = null;

    this.tracker = null;
  }

  /**
   * 处理点击事件
   */
  private handleClick(e: MouseEvent): void {
    if (!this.tracker) {
      return;
    }

    const target = e.target as HTMLElement;
    if (!target) {
      return;
    }

    // 生成元素唯一标识（用于去重）
    const elementKey = `${target.tagName}-${target.id || ''}-${target.className || ''}`;

    // 检查点击去重：防止短时间内同一元素重复上报（考虑事件冒泡）
    const now = Date.now();
    if (now - this.lastClickTime < this.CLICK_DEDUP_WINDOW && 
        this.lastReportedElement === elementKey) {
      // 距离上次点击太近且是同一个元素，跳过（可能是事件冒泡）
      return;
    }

    // 防抖处理：防止快速连续点击导致多次上报
    if (this.debounceTimer !== null) {
      clearTimeout(this.debounceTimer);
    }
    
    this.debounceTimer = window.setTimeout(() => {
      this.debounceTimer = null;

      // 检查是否有任何手动上报（不限制类型）
      // 如果点击触发了任何手动上报事件（如 custom），则跳过自动点击上报
      if (this.tracker?.hasRecentManualEvent()) {
        // 在时间窗口内有手动上报事件（任何类型），跳过自动点击上报
        this.logger?.debug?.('Skipping auto click event, manual event detected');
        return;
      }

      // 再次检查去重（防抖期间可能有新的点击）
      const currentTime = Date.now();
      if (currentTime - this.lastClickTime < this.CLICK_DEDUP_WINDOW && 
          this.lastReportedElement === elementKey) {
        return;
      }

      // 更新最后点击时间和元素标识
      this.lastClickTime = currentTime;
      this.lastReportedElement = elementKey;

      // 检查是否应该追踪此点击（过滤逻辑）
      if (this.filter && !this.filter.shouldTrack(target)) {
        this.logger?.debug?.('Click filtered out', {
          tag: target.tagName.toLowerCase(),
          id: target.id,
          className: target.className,
        });
        return;
      }

      // 采集元素信息
      const content = this.extractElementInfo(target, e);

      // 上报点击事件
      this.tracker!.trackEvent('click', content);
    }, this.debounceDelay);
  }

  /**
   * 提取元素信息
   * 
   * @param element - 目标元素
   * @param event - 鼠标事件
   * @returns 元素信息对象
   */
  private extractElementInfo(
    element: HTMLElement,
    event: MouseEvent
  ): Record<string, unknown> {
    const info: Record<string, unknown> = {
      tag: element.tagName.toLowerCase(),
      x: event.clientX,
      y: event.clientY,
    };

    // ID
    if (element.id) {
      info.id = element.id;
    }

    // 类名
    if (element.className && typeof element.className === 'string') {
      info.className = element.className;
    }

    // 文本内容（截取前 100 个字符）
    const text = element.textContent || element.innerText;
    if (text) {
      info.text = text.trim().substring(0, 100);
    }

    // 选择器（简化版）
    info.selector = this.generateSelector(element);

    // 链接地址
    if (element instanceof HTMLAnchorElement) {
      info.href = element.href;
    }

    return info;
  }

  /**
   * 生成元素选择器
   * 
   * @param element - 目标元素
   * @returns 选择器字符串
   */
  private generateSelector(element: HTMLElement): string {
    const parts: string[] = [];

    // ID 优先
    if (element.id) {
      parts.push(`#${element.id}`);
      return parts.join(' > ');
    }

    // 类名
    if (element.className && typeof element.className === 'string') {
      const classes = element.className.split(/\s+/).filter(Boolean);
      if (classes.length > 0) {
        parts.push(`.${classes[0]}`);
      }
    }

    // 标签名
    parts.push(element.tagName.toLowerCase());

    // 如果不够具体，添加父元素信息
    if (element.parentElement && parts.length < 3) {
      const parentSelector = this.generateSelector(element.parentElement);
      return `${parentSelector} > ${parts.join('')}`;
    }

    return parts.join('');
  }
}

