/**
 * 核心 Tracker 类
 * @packageDocumentation
 */

import { TrackerConfig, EventData, UserInfo, UserMode } from '../types';
import { Storage, createStorage } from './storage';
import { SessionManager } from './session';
import { EventQueue } from './queue';
import { sendBatch } from '../api/batch';
import { sendPixelBatch } from '../api/pixel-batch';
import { BatchMerger } from './batch-merger';
import { generateUUID, generateShortId } from '../utils/uuid';
import { createLogger, Logger } from '../utils/logger';
import { DEFAULT_CONFIG, STORAGE_KEYS, BATCH_PIXEL_CONFIG } from '../constants';
import type { Collector } from '../collectors/types';
import {
  PageViewCollector,
  ClickCollector,
  PerformanceCollector,
  ErrorCollector,
} from '../collectors';

/**
 * 内部配置类型（必需字段和可选字段的组合）
 */
type InternalTrackerConfig = Omit<Required<Omit<TrackerConfig, 'sessionId' | 'anonymousId' | 'batchPixel' | 'clickFilter'>>, 'retry' | 'collectors'> & {
  retry: Required<NonNullable<TrackerConfig['retry']>>;
  collectors: Required<NonNullable<TrackerConfig['collectors']>>;
  batchPixel?: TrackerConfig['batchPixel'];
  clickFilter?: TrackerConfig['clickFilter'];
  sessionId?: string;
  anonymousId?: string;
};

/**
 * Tracker 实例
 * 
 * @remarks
 * SDK 的核心类，负责初始化、事件采集协调、用户身份管理和事件上报
 */
export class Tracker {
  private config: InternalTrackerConfig;
  private storage: Storage;
  private sessionManager: SessionManager;
  private eventQueue: EventQueue;
  private logger: Logger;
  private collectors: Map<string, Collector> = new Map();
  private userId: string | number | null = null;
  private anonymousId: string;
  private isTracking: boolean = false;
  private batchMerger: BatchMerger | null = null;
  /** 手动上报的事件记录（用于避免与自动采集重复） */
  private manualEventRecords: Map<string, number> = new Map();
  /** 最后一次手动上报的时间戳（任何类型） */
  private lastManualEventTime: number = 0;
  /** 手动上报事件的有效时间窗口（毫秒） */
  private readonly MANUAL_EVENT_WINDOW = 500;

  /**
   * 创建 Tracker 实例
   * 
   * @param config - SDK 配置
   */
  constructor(config: TrackerConfig) {
    // 合并默认配置
    this.config = {
      endpoint: config.endpoint,
      projectId: config.projectId,
      autoStart: config.autoStart ?? DEFAULT_CONFIG.AUTO_START,
      batchSize: config.batchSize ?? DEFAULT_CONFIG.BATCH_SIZE,
      batchTimeout: config.batchTimeout ?? DEFAULT_CONFIG.BATCH_TIMEOUT,
      retry: {
        maxRetries: config.retry?.maxRetries ?? DEFAULT_CONFIG.MAX_RETRIES,
        retryDelay: config.retry?.retryDelay ?? DEFAULT_CONFIG.RETRY_DELAY,
        retryBackoff: config.retry?.retryBackoff ?? DEFAULT_CONFIG.RETRY_BACKOFF,
      },
      collectors: {
        pageview: config.collectors?.pageview ?? true,
        click: config.collectors?.click ?? true,
        performance: config.collectors?.performance ?? false,
        error: config.collectors?.error ?? true,
      },
      debug: config.debug ?? DEFAULT_CONFIG.DEBUG,
      usePixel: config.usePixel ?? true, // 默认使用像素上报以避免跨域问题
      batchPixel: config.batchPixel,
      clickFilter: config.clickFilter,
      sessionId: config.sessionId ?? undefined,
      anonymousId: config.anonymousId ?? undefined,
    };

    // 初始化批量合并器（如果启用批量像素上报）
    if (this.config.usePixel && this.shouldUseBatchPixel()) {
      this.initializeBatchMerger();
    }

    // 初始化存储
    this.storage = createStorage();

    // 初始化会话管理
    this.sessionManager = new SessionManager(this.storage);

    // 初始化事件队列
    this.eventQueue = new EventQueue(
      this.storage,
      100,
      this.config.batchTimeout
    );

    // 设置队列刷新回调
    this.eventQueue.setFlushCallback((events) => {
      this.flushEvents(events);
    });

    // 初始化日志器
    this.logger = createLogger(this.config.debug);

    // 初始化匿名 ID
    this.anonymousId = this.config.anonymousId || this.loadOrGenerateAnonymousId();

    // 加载用户 ID
    this.loadUserId();

    // 自动启动（如果配置）
    if (this.config.autoStart) {
      this.start();
    }

    this.logger.info('Tracker initialized', this.config);
  }

  /**
   * 启动自动采集
   * 
   * @remarks
   * 启动所有启用的采集器
   */
  start(): void {
    if (this.isTracking) {
      this.logger.warn('Tracker is already started, skipping...');
      return;
    }

    // 确保之前已停止所有采集器（防御性编程）
    if (this.collectors.size > 0) {
      this.logger.warn('Found existing collectors, stopping them first...');
      for (const collector of this.collectors.values()) {
        try {
          collector.stop();
        } catch (e) {
          this.logger.error('Error stopping collector', e);
        }
      }
      this.collectors.clear();
    }

    this.isTracking = true;
    this.logger.info('Starting trackers...');

    // 启动启用的采集器
    if (this.config.collectors.pageview) {
      const collector = new PageViewCollector();
      this.registerCollector('pageview', collector);
    }

    if (this.config.collectors.click) {
      const collector = new ClickCollector();
      this.registerCollector('click', collector);
    }

    if (this.config.collectors.performance) {
      const collector = new PerformanceCollector();
      this.registerCollector('performance', collector);
    }

    if (this.config.collectors.error) {
      const collector = new ErrorCollector();
      this.registerCollector('error', collector);
    }

    this.logger.info(`Started ${this.collectors.size} collector(s)`);
  }

  /**
   * 停止自动采集
   */
  stop(): void {
    if (!this.isTracking) {
      this.logger.warn('Tracker is not started');
      return;
    }

    this.isTracking = false;
    this.logger.info('Stopping trackers...');

    // 停止所有采集器
    for (const collector of this.collectors.values()) {
      collector.stop();
    }

    // 清空采集器
    this.collectors.clear();

    // 最后上报一次
    this.flush();
  }

  /**
   * 设置用户身份（切换到实名模式）
   * 
   * @param userInfo - 用户信息
   * 
   * @remarks
   * 设置 userId 后，后续事件将包含用户信息
   */
  setUser(userInfo: UserInfo): void {
    this.userId = userInfo.userId;
    this.storage.setItem(STORAGE_KEYS.USER_ID, String(this.userId));
    this.logger.info('User set', userInfo);
  }

  /**
   * 清除用户身份（切换到匿名模式）
   * 
   * @remarks
   * 清除 userId，但保持会话和匿名 ID
   */
  clearUser(): void {
    this.userId = null;
    this.storage.removeItem(STORAGE_KEYS.USER_ID);
    this.logger.info('User cleared, switched to anonymous mode');
  }

  /**
   * 获取当前用户模式
   * 
   * @returns 用户模式
   */
  getUserMode(): UserMode {
    return this.userId !== null ? UserMode.IDENTIFIED : UserMode.ANONYMOUS;
  }

  /**
   * 手动上报事件
   * 
   * @param eventType - 事件类型
   * @param eventContent - 事件内容（可选）
   * 
   * @example
   * ```ts
   * tracker.trackEvent('custom_event', { key: 'value' });
   * ```
   */
  trackEvent(eventType: string, eventContent?: Record<string, unknown>): void {
    const event: EventData = {
      event_type: eventType,
      event_content: eventContent,
    };

    // 记录手动上报的事件，用于避免自动采集器重复上报
    this.recordManualEvent(eventType);

    this.addToQueue(event);
  }

  /**
   * 上报页面访问事件
   * 
   * @param url - 页面 URL（可选，默认当前页面）
   * @param title - 页面标题（可选，默认当前标题）
   */
  trackPageView(url?: string, title?: string): void {
    const event: EventData = {
      event_type: 'pageview',
      event_content: {
        url: url || (typeof window !== 'undefined' ? window.location.href : ''),
        title: title || (typeof document !== 'undefined' ? document.title : ''),
        path: typeof window !== 'undefined' ? window.location.pathname : '',
        search: typeof window !== 'undefined' ? window.location.search : '',
        hash: typeof window !== 'undefined' ? window.location.hash : '',
        referrer: typeof document !== 'undefined' ? document.referrer : '',
      },
    };

    this.addToQueue(event);
  }

  /**
   * 立即上报队列中的所有事件
   */
  flush(): void {
    const events = this.eventQueue.flush();
    if (events.length > 0) {
      this.flushEvents(events);
    }
  }

  /**
   * 注册采集器
   * 
   * @param name - 采集器名称
   * @param collector - 采集器实例
   * 
   * @internal
   */
  registerCollector(name: string, collector: Collector): void {
    this.collectors.set(name, collector);
    if (this.isTracking) {
      collector.start(this);
    }
  }

  /**
   * 获取配置
   * 
   * @returns 当前配置的副本
   */
  getConfig(): Readonly<InternalTrackerConfig> {
    return { ...this.config };
  }

  /**
   * 获取会话 ID
   * 
   * @returns 会话 ID
   */
  getSessionId(): string {
    return this.sessionManager.getSessionId();
  }

  /**
   * 获取匿名 ID
   * 
   * @returns 匿名 ID
   */
  getAnonymousId(): string {
    return this.anonymousId;
  }

  /**
   * 获取用户 ID
   * 
   * @returns 用户 ID，如果未设置返回 null
   */
  getUserId(): string | number | null {
    return this.userId;
  }

  /**
   * 添加事件到队列
   * 
   * @param event - 事件数据
   */
  private addToQueue(event: EventData): void {
    // 如果启用批量像素上报，使用合并器（不使用 eventQueue）
    if (this.config.usePixel && this.batchMerger) {
      // 直接使用批量合并器，不经过 eventQueue，避免重复处理
      this.batchMerger.add(event);
      this.logger.debug('Event added to batch merger', event);
      return;
    }

    // 使用传统队列模式
    const added = this.eventQueue.add(event);
    if (!added) {
      this.logger.warn('Event queue is full, event dropped', event);
      return;
    }

    // 检查是否需要立即上报（达到批量阈值）
    if (this.eventQueue.shouldFlush(this.config.batchSize)) {
      this.logger.debug('Batch size reached, flushing queue');
      this.flush();
    } else if (this.config.usePixel && this.eventQueue.size() === 1) {
      // 像素上报模式下，如果队列中只有1个事件，立即上报（降低延迟）
      // 使用 setTimeout(0) 确保不阻塞当前事件循环
      setTimeout(() => {
        if (this.eventQueue.size() === 1) {
          this.flush();
        }
      }, 0);
    }
  }

  /**
   * 上报事件（批量）
   * 
   * @param events - 事件数组
   */
  private async flushEvents(events: EventData[]): Promise<void> {
    if (events.length === 0) {
      return;
    }

    this.logger.debug(`Flushing ${events.length} events`);

    try {
      // 如果启用批量像素上报且事件数量 > 1，使用批量上报
      if (this.config.usePixel && 
          this.shouldUseBatchPixel() && 
          events.length > 1) {
        await sendPixelBatch(events, {
          endpoint: this.config.endpoint,
          projectId: this.config.projectId,
          sessionId: this.getSessionId(),
          userId: this.userId !== null ? String(this.userId) : undefined,
        });
        
        // 续期会话
        this.sessionManager.renew();
        
        this.logger.info(`Successfully sent ${events.length} events (batch pixel)`);
      } else {
        // 降级到原有的批量上报逻辑（单个像素或REST API）
        const result = await sendBatch(events, {
          endpoint: this.config.endpoint,
          projectId: this.config.projectId,
          sessionId: this.getSessionId(),
          userId: this.userId !== null ? String(this.userId) : undefined,
        });

        // 检查结果状态码
        if (result.code >= 400) {
          throw new Error(result.message || 'Batch send failed');
        }

        // 续期会话
        this.sessionManager.renew();

        this.logger.info(`Successfully sent ${events.length} events`);
      }
    } catch (error) {
      // 上报失败，检查事件是否已有重试标记，避免无限循环
      const shouldRequeue = events.some((e: any) => !e.__retried);
      if (shouldRequeue) {
        // 标记事件已重试，并重新加入队列
        const retriedEvents = events.map((e: any) => ({
          ...e,
          __retried: true,
        }));
        this.eventQueue.addBatch(retriedEvents);
        this.logger.error('Failed to send events, re-queued (will retry once)', error);
      } else {
        // 已经重试过，丢弃事件避免无限循环
        this.logger.error('Failed to send events after retry, events dropped', error);
      }
    }
  }

  /**
   * 加载或生成匿名 ID
   * 
   * @returns 匿名 ID
   */
  private loadOrGenerateAnonymousId(): string {
    let anonymousId = this.storage.getItem(STORAGE_KEYS.ANONYMOUS_ID);
    if (!anonymousId) {
      anonymousId = generateShortId('track');
      this.storage.setItem(STORAGE_KEYS.ANONYMOUS_ID, anonymousId);
    }
    return anonymousId;
  }

  /**
   * 加载用户 ID
   */
  private loadUserId(): void {
    const stored = this.storage.getItem(STORAGE_KEYS.USER_ID);
    if (stored) {
      // 尝试解析为数字，如果失败则使用字符串
      const numValue = Number(stored);
      if (!isNaN(numValue) && isFinite(numValue) && String(numValue) === stored) {
        this.userId = numValue;
      } else {
        this.userId = stored;
      }
    }
  }

  /**
   * 判断是否应该使用批量像素上报
   * 
   * @returns 如果应该使用批量像素上报返回 true，否则返回 false
   */
  private shouldUseBatchPixel(): boolean {
    const batchPixelConfig = this.config.batchPixel;
    return (batchPixelConfig?.enabled ?? BATCH_PIXEL_CONFIG.ENABLED) && this.config.usePixel;
  }

  /**
   * 初始化批量合并器
   */
  private initializeBatchMerger(): void {
    const batchPixelConfig = this.config.batchPixel;
    this.batchMerger = new BatchMerger({
      maxEvents: batchPixelConfig?.maxEvents ?? BATCH_PIXEL_CONFIG.MAX_EVENTS,
      mergeWindow: batchPixelConfig?.mergeWindow ?? BATCH_PIXEL_CONFIG.MERGE_WINDOW,
      maxUrlLength: batchPixelConfig?.maxUrlLength ?? BATCH_PIXEL_CONFIG.MAX_URL_LENGTH,
      useCompression: batchPixelConfig?.useCompression ?? BATCH_PIXEL_CONFIG.USE_COMPRESSION,
    });

    this.batchMerger.setFlushCallback((events) => {
      this.flushEvents(events);
    });
  }

  /**
   * 记录手动上报的事件
   * 
   * @param eventType - 事件类型
   * @internal
   */
  recordManualEvent(eventType: string): void {
    const now = Date.now();
    this.manualEventRecords.set(eventType, now);
    this.lastManualEventTime = now; // 更新最后手动上报时间

    // 清理过期记录（超过时间窗口的记录）
    const cutoffTime = now - this.MANUAL_EVENT_WINDOW;
    for (const [type, timestamp] of this.manualEventRecords.entries()) {
      if (timestamp < cutoffTime) {
        this.manualEventRecords.delete(type);
      }
    }
  }

  /**
   * 检查指定事件类型是否在最近的时间窗口内被手动上报
   * 
   * @param eventType - 事件类型
   * @returns 如果在时间窗口内有手动上报返回 true，否则返回 false
   * 
   * @remarks
   * 此方法用于自动采集器检查是否需要跳过自动上报（如果已有手动上报）
   */
  hasManualEvent(eventType: string): boolean {
    const timestamp = this.manualEventRecords.get(eventType);
    if (!timestamp) {
      return false;
    }

    const now = Date.now();
    const isWithinWindow = (now - timestamp) <= this.MANUAL_EVENT_WINDOW;

    if (!isWithinWindow) {
      // 清理过期记录
      this.manualEventRecords.delete(eventType);
      return false;
    }

    return true;
  }

  /**
   * 检查最近是否有任何手动上报事件（不限制类型）
   * 
   * @returns 如果在时间窗口内有任何手动上报返回 true，否则返回 false
   * 
   * @remarks
   * 用于点击采集器：如果用户点击触发了任何手动上报（如 custom 事件），
   * 则跳过自动点击上报，避免重复上报
   */
  hasRecentManualEvent(): boolean {
    if (this.lastManualEventTime === 0) {
      return false;
    }

    const now = Date.now();
    const isWithinWindow = (now - this.lastManualEventTime) <= this.MANUAL_EVENT_WINDOW;

    if (!isWithinWindow) {
      this.lastManualEventTime = 0;
      return false;
    }

    return true;
  }
}

