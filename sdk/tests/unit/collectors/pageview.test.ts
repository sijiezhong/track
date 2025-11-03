/**
 * 页面访问采集器测试
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { PageViewCollector } from '../../../src/collectors/pageview';
import { Tracker } from '../../../src/core/tracker';
import { TrackerConfig } from '../../../src/types';

describe('PageViewCollector', () => {
  let collector: PageViewCollector;
  let tracker: Tracker;
  let trackPageViewSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    vi.useFakeTimers();
    collector = new PageViewCollector();
    const config: TrackerConfig = {
      endpoint: 'https://api.example.com',
      projectId: 1,
      autoStart: false,
    };
    tracker = new Tracker(config);
    trackPageViewSpy = vi.spyOn(tracker, 'trackPageView');
  });

  afterEach(() => {
    collector.stop();
    trackPageViewSpy.mockRestore();
    vi.useRealTimers();
  });

  it('应该获取正确的采集器名称', () => {
    expect(collector.getName()).toBe('pageview');
  });

  it('启动后应该监听 popstate 事件', async () => {
    collector.start(tracker);

    const popstateEvent = new PopStateEvent('popstate');
    window.dispatchEvent(popstateEvent);

    // 等待事件处理
    await vi.advanceTimersByTimeAsync(100);
    expect(trackPageViewSpy).toHaveBeenCalled();
  });

  it('停止后应该移除事件监听', () => {
    collector.start(tracker);
    collector.stop();

    const popstateEvent = new PopStateEvent('popstate');
    window.dispatchEvent(popstateEvent);

    expect(trackPageViewSpy).not.toHaveBeenCalled();
  });

  it('应该拦截 pushState', async () => {
    collector.start(tracker);

    window.history.pushState({}, '', '/test');

    // 等待路由变化处理
    await vi.advanceTimersByTimeAsync(100);
    expect(trackPageViewSpy).toHaveBeenCalled();
  });

  it('应该拦截 replaceState', async () => {
    collector.start(tracker);

    window.history.replaceState({}, '', '/test2');

    // 等待路由变化处理
    await vi.advanceTimersByTimeAsync(100);
    expect(trackPageViewSpy).toHaveBeenCalled();
  });

  it('应该恢复原始的 history 方法', () => {
    const originalPushState = history.pushState;
    const originalReplaceState = history.replaceState;

    collector.start(tracker);
    collector.stop();

    expect(history.pushState).toBe(originalPushState);
    expect(history.replaceState).toBe(originalReplaceState);
  });
});

