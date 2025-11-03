/**
 * 事件队列管理
 * @packageDocumentation
 */

import { EventData } from '../types';
import { Storage } from './storage';
import { STORAGE_KEYS } from '../constants';

/**
 * 事件队列
 * 
 * @remarks
 * 负责事件的本地缓存、批量处理和自动上报触发
 */
export class EventQueue {
  private storage: Storage;
  private queue: EventData[] = [];
  private maxSize: number;
  private flushTimer: number | null = null;
  private flushCallback: ((events: EventData[]) => void) | null = null;

  /**
   * 创建事件队列
   * 
   * @param storage - 存储实例
   * @param maxSize - 最大队列长度，默认 100
   * @param batchTimeout - 批量上报超时时间（毫秒），默认 5000
   */
  constructor(
    storage: Storage,
    maxSize: number = 100,
    private batchTimeout: number = 5000
  ) {
    this.storage = storage;
    this.maxSize = maxSize;
    this.loadFromStorage();
    this.setupBeforeUnload();
  }

  /**
   * 设置刷新回调
   * 
   * @param callback - 当队列需要上报时调用的回调函数
   */
  setFlushCallback(callback: (events: EventData[]) => void): void {
    this.flushCallback = callback;
  }

  /**
   * 添加事件到队列
   * 
   * @param event - 事件数据
   * @returns 如果队列已满返回 false，否则返回 true
   */
  add(event: EventData): boolean {
    if (this.queue.length >= this.maxSize) {
      return false;
    }

    this.queue.push(event);
    this.saveToStorage();

    // 启动定时器（如果尚未启动）
    this.startFlushTimer();

    return true;
  }

  /**
   * 批量添加事件到队列
   * 
   * @param events - 事件数据数组
   * @returns 成功添加的事件数量
   */
  addBatch(events: EventData[]): number {
    let count = 0;
    for (const event of events) {
      if (this.add(event)) {
        count++;
      }
    }
    return count;
  }

  /**
   * 获取队列长度
   * 
   * @returns 队列中的事件数量
   */
  size(): number {
    return this.queue.length;
  }

  /**
   * 检查队列是否为空
   * 
   * @returns 如果队列为空返回 true，否则返回 false
   */
  isEmpty(): boolean {
    return this.queue.length === 0;
  }

  /**
   * 清空队列并返回所有事件
   * 
   * @returns 所有事件数据
   */
  flush(): EventData[] {
    const events = [...this.queue];
    this.queue = [];
    this.clearFlushTimer();
    this.saveToStorage();
    return events;
  }

  /**
   * 清空队列（不返回事件）
   */
  clear(): void {
    this.queue = [];
    this.clearFlushTimer();
    this.saveToStorage();
  }

  /**
   * 获取队列中的所有事件（不清空）
   * 
   * @returns 所有事件数据的副本
   */
  getAll(): EventData[] {
    return [...this.queue];
  }

  /**
   * 检查队列是否达到批量上报阈值
   * 
   * @param batchSize - 批量上报阈值
   * @returns 如果达到阈值返回 true，否则返回 false
   */
  shouldFlush(batchSize: number): boolean {
    return this.queue.length >= batchSize;
  }

  /**
   * 手动触发上报（如果队列非空）
   */
  triggerFlush(): void {
    if (!this.isEmpty() && this.flushCallback) {
      const events = this.flush();
      this.flushCallback(events);
    }
  }

  /**
   * 从存储中加载队列
   */
  private loadFromStorage(): void {
    try {
      const stored = this.storage.getJSON<EventData[]>(
        STORAGE_KEYS.EVENT_QUEUE
      );
      if (stored && Array.isArray(stored)) {
        // 只保留最近的事件（防止存储过多数据）
        this.queue = stored.slice(-this.maxSize);
        if (this.queue.length > 0) {
          // 如果有未上报的事件，启动定时器
          this.startFlushTimer();
        }
      }
    } catch (e) {
      // 加载失败，忽略
    }
  }

  /**
   * 保存队列到存储
   */
  private saveToStorage(): void {
    try {
      this.storage.setJSON(STORAGE_KEYS.EVENT_QUEUE, this.queue);
    } catch (e) {
      // 保存失败，忽略
    }
  }

  /**
   * 启动刷新定时器
   */
  private startFlushTimer(): void {
    // 如果定时器已启动或队列为空，不启动新的定时器
    if (this.flushTimer !== null || this.isEmpty()) {
      return;
    }

    this.flushTimer = window.setTimeout(() => {
      this.flushTimer = null;
      // 再次检查队列是否为空，避免在定时器等待期间队列被清空
      if (!this.isEmpty()) {
        this.triggerFlush();
      }
    }, this.batchTimeout);
  }

  /**
   * 清除刷新定时器
   */
  private clearFlushTimer(): void {
    if (this.flushTimer !== null) {
      clearTimeout(this.flushTimer);
      this.flushTimer = null;
    }
  }

  /**
   * 设置页面卸载时的兜底上报
   */
  private setupBeforeUnload(): void {
    if (typeof window === 'undefined') {
      return;
    }

    const handleBeforeUnload = () => {
      // 使用 sendBeacon 进行最后的上报
      if (!this.isEmpty() && this.flushCallback && typeof navigator !== 'undefined' && 'sendBeacon' in navigator) {
        const events = this.flush();
        // 注意：sendBeacon 只能发送字符串，需要序列化
        // 这里先调用回调，由回调处理实际的上报逻辑
        this.flushCallback(events);
      }
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    // 某些浏览器也支持 pagehide 事件
    window.addEventListener('pagehide', handleBeforeUnload);
  }
}

