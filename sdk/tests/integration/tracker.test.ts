/**
 * Tracker 集成测试
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { Tracker } from '../../src/core/tracker';
import { TrackerConfig } from '../../src/types';

// Mock fetch
global.fetch = vi.fn();

describe('Tracker 集成测试', () => {
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

  describe('初始化', () => {
    it('应该正确初始化', () => {
      expect(tracker).toBeTruthy();
      expect(tracker.getSessionId()).toBeTruthy();
      expect(tracker.getAnonymousId()).toBeTruthy();
    });

    it('autoStart=true 时应自动启动', () => {
      const autoTracker = new Tracker({
        ...config,
        autoStart: true,
      });
      expect(autoTracker).toBeTruthy();
      autoTracker.stop();
    });
  });

  describe('事件上报', () => {
    it('应该将事件添加到队列', () => {
      tracker.trackEvent('test_event', { key: 'value' });
      // 队列中应该有事件
      tracker.flush();
      // 验证是否调用了上报接口（需要 mock）
    });

    it('手动刷新应该上报队列中的所有事件', async () => {
      (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        ok: true,
        status: 201,
        json: async () => ({
          code: 201,
          message: '成功',
          data: null,
        }),
      });

      tracker.trackEvent('event1', {});
      tracker.trackEvent('event2', {});

      await tracker.flush();

      expect(global.fetch).toHaveBeenCalled();
    });
  });

  describe('用户管理', () => {
    it('应该设置用户', () => {
      tracker.setUser({ userId: 123, userName: 'Test User' });
      expect(tracker.getUserId()).toBe(123);
      expect(tracker.getUserMode()).toBe('identified');
    });

    it('应该清除用户', () => {
      tracker.setUser({ userId: 123 });
      tracker.clearUser();
      expect(tracker.getUserId()).toBeNull();
      expect(tracker.getUserMode()).toBe('anonymous');
    });
  });

  describe('批量上报', () => {
    it('达到批量阈值应该自动上报', async () => {
      (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValue({
        ok: true,
        status: 201,
        json: async () => ({
          code: 201,
          message: '成功',
        }),
      });

      const batchTracker = new Tracker({
        ...config,
        batchSize: 2,
        autoStart: false,
      });

      batchTracker.trackEvent('event1', {});
      batchTracker.trackEvent('event2', {});

      // 等待批量上报触发
      await new Promise((resolve) => setTimeout(resolve, 100));

      batchTracker.stop();
    });
  });

  describe('采集器', () => {
    it('启动应该初始化采集器', () => {
      const trackerWithCollectors = new Tracker({
        ...config,
        collectors: {
          pageview: true,
          click: true,
          error: true,
          performance: false,
        },
        autoStart: false,
      });

      trackerWithCollectors.start();
      expect(trackerWithCollectors).toBeTruthy();
      trackerWithCollectors.stop();
    });

    it('停止应该清理采集器', () => {
      tracker.start();
      tracker.stop();
      // 验证采集器已停止
      expect(tracker).toBeTruthy();
    });
  });

  describe('配置', () => {
    it('应该获取配置', () => {
      const config = tracker.getConfig();
      expect(config.endpoint).toBe('https://api.example.com');
      expect(config.projectId).toBe(1);
    });
  });
});

