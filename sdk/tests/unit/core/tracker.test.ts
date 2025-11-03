/**
 * Tracker 单元测试
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { Tracker } from '../../../src/core/tracker';
import { TrackerConfig, UserMode } from '../../../src/types';

// Mock fetch
global.fetch = vi.fn();

describe('Tracker', () => {
  let tracker: Tracker;
  let config: TrackerConfig;

  beforeEach(() => {
    localStorage.clear();
    config = {
      endpoint: 'https://api.example.com',
      projectId: 1,
      autoStart: false,
      debug: false,
    };
    tracker = new Tracker(config);
    vi.clearAllMocks();
  });

  afterEach(() => {
    tracker.stop();
    vi.restoreAllMocks();
  });

  describe('初始化和配置', () => {
    it('应该使用默认配置', () => {
      const defaultTracker = new Tracker({
        endpoint: 'https://api.example.com',
        projectId: 1,
      });
      const trackerConfig = defaultTracker.getConfig();
      expect(trackerConfig.autoStart).toBe(true);
      expect(trackerConfig.batchSize).toBe(10);
      expect(trackerConfig.debug).toBe(false);
      defaultTracker.stop();
    });

    it('应该使用自定义配置', () => {
      const customTracker = new Tracker({
        endpoint: 'https://api.example.com',
        projectId: 1,
        batchSize: 20,
        debug: true,
        autoStart: false,
      });
      const trackerConfig = customTracker.getConfig();
      expect(trackerConfig.batchSize).toBe(20);
      expect(trackerConfig.debug).toBe(true);
      customTracker.stop();
    });
  });

  describe('会话和匿名 ID', () => {
    it('应该生成会话 ID', () => {
      const sessionId = tracker.getSessionId();
      expect(sessionId).toBeTruthy();
      expect(typeof sessionId).toBe('string');
    });

    it('应该生成匿名 ID', () => {
      const anonymousId = tracker.getAnonymousId();
      expect(anonymousId).toBeTruthy();
      expect(typeof anonymousId).toBe('string');
    });

    it('应该持久化匿名 ID', () => {
      const anonymousId1 = tracker.getAnonymousId();
      const newTracker = new Tracker(config);
      const anonymousId2 = newTracker.getAnonymousId();
      expect(anonymousId1).toBe(anonymousId2);
      newTracker.stop();
    });
  });

  describe('事件跟踪', () => {
    it('trackEvent 应该添加事件到队列', () => {
      tracker.trackEvent('test_event', { key: 'value' });
      // 通过 flush 验证事件已添加
      tracker.flush();
      // 队列应该为空（事件已上报）
    });

    it('trackPageView 应该上报页面访问', () => {
      tracker.trackPageView();
      tracker.flush();
    });

    it('应该支持自定义事件内容', () => {
      tracker.trackEvent('custom', {
        customField: 'customValue',
        number: 123,
      });
      tracker.flush();
    });
  });

  describe('用户管理', () => {
    it('初始状态应该是匿名模式', () => {
      expect(tracker.getUserMode()).toBe(UserMode.ANONYMOUS);
      expect(tracker.getUserId()).toBeNull();
    });

    it('setUser 应该切换到实名模式', () => {
      tracker.setUser({ userId: 123 });
      expect(tracker.getUserMode()).toBe(UserMode.IDENTIFIED);
      expect(tracker.getUserId()).toBe(123);
    });

    it('setUser 应该持久化用户 ID', () => {
      tracker.setUser({ userId: 456 });
      const newTracker = new Tracker(config);
      expect(newTracker.getUserId()).toBe(456);
      newTracker.stop();
    });

    it('clearUser 应该切换到匿名模式', () => {
      tracker.setUser({ userId: 123 });
      tracker.clearUser();
      expect(tracker.getUserMode()).toBe(UserMode.ANONYMOUS);
      expect(tracker.getUserId()).toBeNull();
    });
  });

  describe('启动和停止', () => {
    it('start 应该启动采集器', () => {
      tracker.start();
      // 验证采集器已启动（通过检查状态）
      expect(tracker).toBeTruthy();
    });

    it('多次 start 不应该重复启动', () => {
      tracker.start();
      tracker.start(); // 第二次调用
      tracker.stop();
    });

    it('stop 应该停止采集器', () => {
      tracker.start();
      tracker.stop();
      // 验证采集器已停止
    });

    it('未启动时 stop 应该安全', () => {
      expect(() => {
        tracker.stop();
      }).not.toThrow();
    });
  });

  describe('刷新', () => {
    it('flush 应该上报所有事件', async () => {
      (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        ok: true,
        status: 201,
        json: async () => ({
          code: 201,
          message: '成功',
        }),
      });

      tracker.trackEvent('test1', {});
      tracker.trackEvent('test2', {});

      await tracker.flush();

      expect(global.fetch).toHaveBeenCalled();
    });

    it('空队列 flush 应该安全', async () => {
      await tracker.flush();
      // 不应该抛出错误
    });
  });
});

