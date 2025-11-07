import { UserConfig, TrackConfig, EventData, EventType } from "../types";
import { Storage } from "./storage";
import { Sender } from "./sender";
import { BatchManager } from "./batch";
import { AutoTrack } from "../plugins/auto-track";
import { ClickTracker } from "../plugins/click-tracker";
import { PerformanceMonitor } from "../plugins/performance";
import { ErrorMonitor } from "../plugins/error";

/**
 * 主追踪器类（单例模式）
 * 负责初始化、Session 管理、事件上报等核心功能
 */
export class Track {
  private static instance: Track | null = null;

  private initialized: boolean = false;
  private started: boolean = false;
  private userConfig: UserConfig | null = null;
  private trackConfig: TrackConfig | null = null;

  private storage: Storage;
  private sender: Sender | null = null;
  private batchManager: BatchManager | null = null;
  private autoTrack: AutoTrack | null = null;
  private clickTracker: ClickTracker | null = null;
  private performanceMonitor: PerformanceMonitor | null = null;
  private errorMonitor: ErrorMonitor | null = null;

  /**
   * 私有构造函数，防止外部实例化
   */
  private constructor() {
    this.storage = new Storage();
  }

  /**
   * 获取单例实例
   */
  static getInstance(): Track {
    if (!Track.instance) {
      Track.instance = new Track();
    }
    return Track.instance;
  }

  /**
   * 初始化用户信息并注册 Session
   * 必须先调用此方法，才能调用 start()
   *
   * @param userConfig - 用户配置
   * @param trackConfig - 追踪配置（可选）
   */
  async init(userConfig: UserConfig, trackConfig?: TrackConfig): Promise<void> {
    this.userConfig = userConfig;
    this.trackConfig = trackConfig || { endpoint: "" };

    // 验证必填配置
    if (!this.trackConfig.endpoint) {
      throw new Error("endpoint is required in trackConfig");
    }

    // 调用服务端注册 Session
    const sessionTTL = this.trackConfig.sessionTTL ?? 1440; // 默认 24 小时

    try {
      const response = await fetch(`${this.trackConfig.endpoint}/api/session`, {
        method: "POST",
        mode: "cors",
        credentials: "include", // 重要：允许携带 Cookie
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          appId: userConfig.appId,
          appName: userConfig.appName,
          userId: userConfig.userId,
          userProps: userConfig.userProps || {},
          ttlMinutes: sessionTTL === 0 ? null : sessionTTL, // 0 表示不过期
        }),
      });

      if (!response.ok) {
        throw new Error(
          `Failed to initialize session: ${response.status} ${response.statusText}`,
        );
      }

      // Cookie 由服务端自动设置，浏览器会自动管理
      this.initialized = true;

      // 保存配置到本地存储
      this.storage.saveUserConfig(userConfig);
      this.storage.saveTrackConfig(this.trackConfig);

      // 初始化发送器和批量管理器
      this.sender = new Sender(this.trackConfig.endpoint, async () => {
        // Session 失效时的回调，尝试重新 init
        if (this.userConfig && this.trackConfig) {
          try {
            await this.init(this.userConfig, this.trackConfig);
          } catch (error) {
            console.warn("[Track SDK] Failed to reinitialize session:", error);
            throw error; // 重新抛出，让 Sender 知道重试失败
          }
        }
      });

      this.batchManager = new BatchManager(
        this.storage,
        this.sender,
        this.trackConfig.endpoint,
        this.trackConfig.batchSize ?? 10,
        this.trackConfig.batchWait ?? 5000,
      );

      // 尝试刷新离线队列
      await this.batchManager.flushOfflineEvents();

      // 初始化自动采集
      this.autoTrack = new AutoTrack(this);

      // 初始化点击采集
      // clickTrack 如果是 false，则不启用；如果是 undefined，使用默认配置；如果是对象，使用自定义配置
      const clickTrackConfig = this.trackConfig.clickTrack;
      if (clickTrackConfig !== false && clickTrackConfig !== undefined) {
        this.clickTracker = new ClickTracker(this, clickTrackConfig);
      } else if (clickTrackConfig === undefined) {
        // undefined 表示使用默认配置
        this.clickTracker = new ClickTracker(this);
      }

      // 初始化性能监控
      if (this.trackConfig.performance) {
        this.performanceMonitor = new PerformanceMonitor(this);
      }

      // 初始化错误监控
      if (this.trackConfig.errorTrack) {
        this.errorMonitor = new ErrorMonitor(this);
      }
    } catch (error) {
      this.initialized = false;
      throw error;
    }
  }

  /**
   * 开始上报（必须在 init 后调用）
   */
  start(): void {
    if (!this.initialized) {
      throw new Error("Must call init() before start()");
    }

    if (this.started) {
      console.warn("[Track SDK] Tracker is already started");
      return;
    }

    this.started = true;

    // 设置自动采集
    if (this.trackConfig?.autoTrack !== false && this.autoTrack) {
      this.autoTrack.setupAutoTrack();
    }

    // 设置点击采集
    if (this.clickTracker) {
      this.clickTracker.setup();
    }

    // 设置性能监控
    if (this.performanceMonitor) {
      this.performanceMonitor.setup();
    }

    // 设置错误监控
    if (this.errorMonitor) {
      this.errorMonitor.setup();
    }
  }

  /**
   * 停止上报并销毁 Session
   */
  async stop(): Promise<void> {
    if (!this.started) {
      return;
    }

    this.started = false;

    // 移除自动采集
    if (this.autoTrack) {
      this.autoTrack.removeAutoTrack();
    }

    // 移除点击采集
    if (this.clickTracker) {
      this.clickTracker.remove();
    }

    // 移除性能监控
    if (this.performanceMonitor) {
      this.performanceMonitor.remove();
    }

    // 移除错误监控
    if (this.errorMonitor) {
      this.errorMonitor.remove();
    }

    // 刷新队列，确保所有事件都发送
    if (this.batchManager) {
      await this.batchManager.flush();
    }

    // 销毁服务端 Session 并清除 Cookie
    if (this.trackConfig) {
      try {
        await fetch(`${this.trackConfig.endpoint}/api/session/destroy`, {
          method: "POST",
          mode: "cors",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
          },
        });
      } catch (e) {
        console.warn("[Track SDK] Failed to destroy session", e);
      }
    }

    // 清除本地存储
    this.storage.clearAll();

    // 重置状态
    this.initialized = false;
    this.userConfig = null;
    this.trackConfig = null;
  }

  /**
   * 上报自定义事件
   *
   * @param eventId - 自定义事件唯一标识符
   * @param properties - 事件属性（可选）
   */
  track(eventId: string, properties?: Record<string, any>): void {
    if (!this.started) {
      console.warn("[Track SDK] Tracker is not started. Call start() first.");
      return;
    }

    if (!this.batchManager) {
      console.warn("[Track SDK] BatchManager is not initialized");
      return;
    }

    const event: EventData = {
      type: EventType.CUSTOM,
      eventId: eventId,
      properties: properties || {},
    };

    this.batchManager.addEvent(event);
  }

  /**
   * 内部方法：添加事件到队列（供插件使用）
   */
  addEvent(event: EventData): void {
    if (!this.started || !this.batchManager) {
      return;
    }

    this.batchManager.addEvent(event);
  }

  /**
   * 内部方法：获取用户配置（供插件使用）
   */
  getUserConfig(): UserConfig | null {
    return this.userConfig;
  }

  /**
   * 内部方法：获取追踪配置（供插件使用）
   */
  getTrackConfig(): TrackConfig | null {
    return this.trackConfig;
  }

  /**
   * 内部方法：获取批量管理器（供插件使用）
   */
  getBatchManager(): BatchManager | null {
    return this.batchManager;
  }

  /**
   * 内部方法：是否已启动（供插件使用）
   */
  isStarted(): boolean {
    return this.started;
  }

  /**
   * 内部方法：是否已初始化（供插件使用）
   */
  isInitialized(): boolean {
    return this.initialized;
  }
}

// 导出单例实例（推荐使用方式）
export const track = Track.getInstance();
