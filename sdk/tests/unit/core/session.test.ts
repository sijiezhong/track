/**
 * 会话管理测试
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { SessionManager } from '../../../src/core/session';
import { Storage } from '../../../src/core/storage';

describe('SessionManager', () => {
  let storage: Storage;
  let sessionManager: SessionManager;

  beforeEach(() => {
    localStorage.clear();
    storage = new Storage();
    sessionManager = new SessionManager(storage, 30 * 60 * 1000); // 30 分钟
  });

  describe('会话 ID 生成', () => {
    it('应该生成会话 ID', () => {
      const sessionId = sessionManager.getSessionId();
      expect(sessionId).toBeTruthy();
      expect(typeof sessionId).toBe('string');
    });

    it('多次调用应返回相同的会话 ID', () => {
      const id1 = sessionManager.getSessionId();
      const id2 = sessionManager.getSessionId();
      expect(id1).toBe(id2);
    });

    it('会话未过期时应返回相同 ID', () => {
      const id1 = sessionManager.getSessionId();
      // 模拟时间流逝但未过期
      vi.useFakeTimers();
      vi.advanceTimersByTime(10 * 60 * 1000); // 10 分钟
      const id2 = sessionManager.getSessionId();
      expect(id1).toBe(id2);
      vi.useRealTimers();
    });
  });

  describe('会话过期', () => {
    it('会话过期后应生成新 ID', () => {
      const id1 = sessionManager.getSessionId();
      // 模拟时间流逝超过超时时间
      vi.useFakeTimers();
      vi.advanceTimersByTime(31 * 60 * 1000); // 31 分钟
      const id2 = sessionManager.getSessionId();
      expect(id1).not.toBe(id2);
      vi.useRealTimers();
    });

    it('isValid 应该正确判断会话有效性', () => {
      sessionManager.getSessionId();
      expect(sessionManager.isValid()).toBe(true);

      // 模拟过期
      vi.useFakeTimers();
      vi.advanceTimersByTime(31 * 60 * 1000);
      expect(sessionManager.isValid()).toBe(false);
      vi.useRealTimers();
    });
  });

  describe('会话续期', () => {
    it('renew 应该更新会话时间戳', () => {
      const id1 = sessionManager.getSessionId();
      vi.useFakeTimers();
      vi.advanceTimersByTime(25 * 60 * 1000); // 25 分钟

      sessionManager.renew();

      // 再等 10 分钟，应该仍然有效
      vi.advanceTimersByTime(10 * 60 * 1000);
      const id2 = sessionManager.getSessionId();
      expect(id1).toBe(id2);

      vi.useRealTimers();
    });
  });

  describe('会话重置', () => {
    it('reset 应该生成新的会话 ID', () => {
      const id1 = sessionManager.getSessionId();
      sessionManager.reset();
      const id2 = sessionManager.getSessionId();
      expect(id1).not.toBe(id2);
    });
  });

  describe('持久化', () => {
    it('应该从存储中加载会话', () => {
      const manager1 = new SessionManager(storage);
      const id1 = manager1.getSessionId();

      // 创建新实例，应该加载相同的会话
      const manager2 = new SessionManager(storage);
      const id2 = manager2.getSessionId();

      expect(id1).toBe(id2);
    });

    it('过期的会话不应被加载', () => {
      const manager1 = new SessionManager(storage);
      const id1 = manager1.getSessionId();

      // 手动设置过期时间戳
      storage.setItem(
        '__track_session_timestamp',
        (Date.now() - 31 * 60 * 1000).toString()
      );

      const manager2 = new SessionManager(storage);
      const id2 = manager2.getSessionId();

      expect(id1).not.toBe(id2);
    });
  });
});

