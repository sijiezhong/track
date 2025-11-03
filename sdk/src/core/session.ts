/**
 * 会话管理
 * @packageDocumentation
 */

import { Storage } from './storage';
import { generateUUID } from '../utils/uuid';
import { STORAGE_KEYS } from '../constants';

/**
 * 会话管理器
 * 
 * @remarks
 * 负责会话 ID 的生成、持久化和过期管理
 */
export class SessionManager {
  private storage: Storage;
  private sessionId: string | null = null;
  private sessionTimestamp: number = 0;
  private sessionTimeout: number;

  /**
   * 创建会话管理器
   * 
   * @param storage - 存储实例
   * @param sessionTimeout - 会话超时时间（毫秒），默认 30 分钟
   */
  constructor(storage: Storage, sessionTimeout: number = 30 * 60 * 1000) {
    this.storage = storage;
    this.sessionTimeout = sessionTimeout;
    this.loadSession();
  }

  /**
   * 获取会话 ID
   * 
   * @returns 会话 ID，如果不存在或已过期则生成新的
   */
  getSessionId(): string {
    // 检查会话是否过期
    const now = Date.now();
    if (this.sessionId && now - this.sessionTimestamp < this.sessionTimeout) {
      // 会话未过期，返回现有会话 ID
      return this.sessionId;
    }

    // 会话过期或不存在，生成新的会话 ID
    this.sessionId = generateUUID();
    this.sessionTimestamp = now;
    this.saveSession();

    return this.sessionId;
  }

  /**
   * 续期会话
   * 
   * @remarks
   * 更新会话时间戳，延长会话有效期
   */
  renew(): void {
    if (this.sessionId) {
      this.sessionTimestamp = Date.now();
      this.saveSession();
    }
  }

  /**
   * 重置会话（生成新的会话 ID）
   */
  reset(): void {
    this.sessionId = generateUUID();
    this.sessionTimestamp = Date.now();
    this.saveSession();
  }

  /**
   * 检查会话是否有效
   * 
   * @returns 如果会话存在且未过期返回 true，否则返回 false
   */
  isValid(): boolean {
    if (!this.sessionId) {
      return false;
    }

    const now = Date.now();
    return now - this.sessionTimestamp < this.sessionTimeout;
  }

  /**
   * 从存储中加载会话
   */
  private loadSession(): void {
    try {
      const storedSessionId = this.storage.getItem(STORAGE_KEYS.SESSION_ID);
      const storedTimestamp = this.storage.getItem(STORAGE_KEYS.SESSION_TIMESTAMP);

      if (storedSessionId && storedTimestamp) {
        const timestamp = parseInt(storedTimestamp, 10);
        const now = Date.now();

        // 检查会话是否过期
        if (now - timestamp < this.sessionTimeout) {
          this.sessionId = storedSessionId;
          this.sessionTimestamp = timestamp;
        } else {
          // 会话已过期，生成新的
          this.reset();
        }
      } else {
        // 没有存储的会话，生成新的
        this.reset();
      }
    } catch (e) {
      // 加载失败，生成新的会话
      this.reset();
    }
  }

  /**
   * 保存会话到存储
   */
  private saveSession(): void {
    if (this.sessionId) {
      this.storage.setItem(STORAGE_KEYS.SESSION_ID, this.sessionId);
      this.storage.setItem(
        STORAGE_KEYS.SESSION_TIMESTAMP,
        this.sessionTimestamp.toString()
      );
    }
  }
}

