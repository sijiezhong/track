import { EventData } from "../types";
import { Storage } from "./storage";
import { Sender } from "./sender";

/**
 * 批量管理器类
 * 负责事件队列管理、批量上报和离线队列处理
 */
export class BatchManager {
  private queue: EventData[] = [];
  private storage: Storage;
  private sender: Sender;
  private endpoint: string;
  private batchSize: number;
  private batchWait: number;
  private batchTimer: number | null = null;
  private unloadHandlerAttached: boolean = false;

  constructor(
    storage: Storage,
    sender: Sender,
    endpoint: string,
    batchSize: number = 10,
    batchWait: number = 5000,
  ) {
    this.storage = storage;
    this.sender = sender;
    this.endpoint = endpoint;
    this.batchSize = batchSize;
    this.batchWait = batchWait;
    this.setupUnloadHandler();
  }

  /**
   * 添加事件到队列
   */
  addEvent(event: EventData): void {
    this.queue.push(event);

    // 检查批次大小
    if (this.queue.length >= this.batchSize) {
      this.sendBatch().catch((error) => {
        // 静默处理错误，避免未处理的 promise rejection
        if (error instanceof Error && error.message === "Session expired") {
          // Session 失效错误会被重新抛出，由调用者处理
          // 这里不需要额外处理
        }
      });
      return;
    }

    // 设置定时器，超时自动发送
    if (this.batchTimer === null) {
      this.batchTimer = window.setTimeout(() => {
        this.sendBatch().catch((error) => {
          // 静默处理错误，避免未处理的 promise rejection
          if (error instanceof Error && error.message === "Session expired") {
            // Session 失效错误会被重新抛出，由调用者处理
            // 这里不需要额外处理
          }
        });
        this.batchTimer = null;
      }, this.batchWait);
    }
  }

  /**
   * 发送批次事件
   *
   * @param events - 可选，指定要发送的事件数组，如果不提供则发送队列中的所有事件
   */
  private async sendBatch(events?: EventData[]): Promise<void> {
    const eventsToSend = events || this.queue;
    if (eventsToSend.length === 0) {
      return;
    }

    // 清除定时器
    if (this.batchTimer !== null) {
      clearTimeout(this.batchTimer);
      this.batchTimer = null;
    }

    try {
      await this.sender.sendEvents(eventsToSend);

      // 成功发送，从队列中移除
      if (events === undefined) {
        // 发送的是队列中的所有事件
        this.queue = [];
      } else {
        // 发送的是指定的事件，从队列中移除这些事件
        this.queue = this.queue.filter((e) => !events.includes(e));
      }
    } catch (error) {
      // 发送失败
      if (error instanceof Error && error.message === "Session expired") {
        // Session 失效，保留在队列中，等待重新 init 后重试
        throw error; // 重新抛出，让调用者处理
      } else {
        // 网络错误，保存到离线队列
        await this.storage.saveOfflineEvents(eventsToSend);
        // 从队列中移除
        if (events === undefined) {
          this.queue = [];
        } else {
          this.queue = this.queue.filter((e) => !events.includes(e));
        }
      }
    }
  }

  /**
   * 设置页面卸载时的可靠发送
   */
  private setupUnloadHandler(): void {
    if (this.unloadHandlerAttached || typeof window === "undefined") {
      return;
    }

    // 使用 pagehide 事件（比 beforeunload 更可靠）
    window.addEventListener(
      "pagehide",
      () => {
        this.flushOnUnload();
      },
      { capture: true },
    );

    this.unloadHandlerAttached = true;
  }

  /**
   * 页面卸载时发送数据
   */
  private flushOnUnload(): void {
    // 发送队列中的事件
    if (this.queue.length > 0) {
      // 使用 sendBeacon（最可靠）
      if (typeof navigator !== "undefined" && navigator.sendBeacon) {
        const payload = {
          e: this.queue.map((e) => ({
            t: e.type,
            id: e.eventId || undefined,
            p: e.properties,
          })),
        };

        const blob = new Blob([JSON.stringify(payload)], {
          type: "application/json",
        });

        navigator.sendBeacon(`${this.endpoint}/api/ingest`, blob);
      }
    }

    // 发送离线队列
    this.flushOfflineEvents();
  }

  /**
   * 刷新离线队列（尝试发送离线事件）
   */
  async flushOfflineEvents(): Promise<void> {
    const offlineEvents = this.storage.getAllOfflineEvents();
    if (offlineEvents.length === 0) {
      return;
    }

    try {
      await this.sender.sendEvents(offlineEvents);
      // 成功发送，清空离线队列
      await this.storage.clearOfflineEvents();
    } catch (error) {
      // 仍然失败，保留在离线队列
      // 下次再尝试
    }
  }

  /**
   * 强制刷新（立即发送队列中的所有事件）
   */
  async flush(): Promise<void> {
    if (this.queue.length === 0) {
      return;
    }

    const eventsToSend = [...this.queue];
    await this.sendBatch(eventsToSend);
  }

  /**
   * 获取队列长度
   */
  getQueueLength(): number {
    return this.queue.length;
  }

  /**
   * 更新批量配置
   */
  updateConfig(batchSize?: number, batchWait?: number): void {
    if (batchSize !== undefined) {
      this.batchSize = batchSize;
    }
    if (batchWait !== undefined) {
      this.batchWait = batchWait;
    }
  }

  /**
   * 更新端点地址
   */
  setEndpoint(endpoint: string): void {
    this.endpoint = endpoint;
  }
}
