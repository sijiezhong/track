import { UserConfig, TrackConfig, EventData } from "../types";
import { isLocalStorageAvailable } from "../utils";

/**
 * 存储键名常量
 */
const STORAGE_KEYS = {
  USER_CONFIG: "__track_user_config__",
  TRACK_CONFIG: "__track_track_config__",
  OFFLINE_EVENTS: "__track_offline_events__",
} as const;

/**
 * 存储管理类
 * 负责用户配置、追踪配置和离线事件的存储
 */
export class Storage {
  private storage: globalThis.Storage | null = null;
  private available: boolean = false;

  constructor() {
    this.available = isLocalStorageAvailable();
    if (this.available) {
      this.storage = window.localStorage;
    }
  }

  /**
   * 保存用户配置
   */
  saveUserConfig(config: UserConfig): void {
    if (!this.available || !this.storage) {
      return;
    }

    try {
      this.storage.setItem(STORAGE_KEYS.USER_CONFIG, JSON.stringify(config));
    } catch (error) {
      console.warn("[Track SDK] Failed to save user config:", error);
    }
  }

  /**
   * 获取用户配置
   */
  getUserConfig(): UserConfig | null {
    if (!this.available || !this.storage) {
      return null;
    }

    try {
      const value = this.storage.getItem(STORAGE_KEYS.USER_CONFIG);
      return value ? JSON.parse(value) : null;
    } catch (error) {
      console.warn("[Track SDK] Failed to get user config:", error);
      return null;
    }
  }

  /**
   * 保存追踪配置
   */
  saveTrackConfig(config: TrackConfig): void {
    if (!this.available || !this.storage) {
      return;
    }

    try {
      this.storage.setItem(STORAGE_KEYS.TRACK_CONFIG, JSON.stringify(config));
    } catch (error) {
      console.warn("[Track SDK] Failed to save track config:", error);
    }
  }

  /**
   * 获取追踪配置
   */
  getTrackConfig(): TrackConfig | null {
    if (!this.available || !this.storage) {
      return null;
    }

    try {
      const value = this.storage.getItem(STORAGE_KEYS.TRACK_CONFIG);
      return value ? JSON.parse(value) : null;
    } catch (error) {
      console.warn("[Track SDK] Failed to get track config:", error);
      return null;
    }
  }

  /**
   * 保存离线事件到队列
   */
  saveOfflineEvents(events: EventData[]): Promise<void> {
    return new Promise((resolve) => {
      if (!this.available || !this.storage) {
        resolve();
        return;
      }

      try {
        // 获取现有离线事件
        const existingEvents = this.getAllOfflineEvents();

        // 合并新事件
        const allEvents = [...existingEvents, ...events];

        // 保存（限制最大数量，避免存储过大）
        const maxEvents = 1000;
        const eventsToSave = allEvents.slice(-maxEvents);

        this.storage.setItem(
          STORAGE_KEYS.OFFLINE_EVENTS,
          JSON.stringify(eventsToSave),
        );
        resolve();
      } catch (error) {
        console.warn("[Track SDK] Failed to save offline events:", error);
        resolve(); // 即使失败也 resolve，避免阻塞
      }
    });
  }

  /**
   * 获取所有离线事件
   */
  getAllOfflineEvents(): EventData[] {
    if (!this.available || !this.storage) {
      return [];
    }

    try {
      const value = this.storage.getItem(STORAGE_KEYS.OFFLINE_EVENTS);
      return value ? JSON.parse(value) : [];
    } catch (error) {
      console.warn("[Track SDK] Failed to get offline events:", error);
      return [];
    }
  }

  /**
   * 清空离线事件队列
   */
  clearOfflineEvents(): Promise<void> {
    return new Promise((resolve) => {
      if (!this.available || !this.storage) {
        resolve();
        return;
      }

      try {
        this.storage.removeItem(STORAGE_KEYS.OFFLINE_EVENTS);
        resolve();
      } catch (error) {
        console.warn("[Track SDK] Failed to clear offline events:", error);
        resolve();
      }
    });
  }

  /**
   * 清除所有存储数据
   */
  clearAll(): void {
    if (!this.available || !this.storage) {
      return;
    }

    try {
      Object.values(STORAGE_KEYS).forEach((key) => {
        this.storage?.removeItem(key);
      });
    } catch (error) {
      console.warn("[Track SDK] Failed to clear all storage:", error);
    }
  }
}
