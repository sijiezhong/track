/**
 * 点击采集器测试
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { ClickCollector } from '../../../src/collectors/click';
import { Tracker } from '../../../src/core/tracker';
import { TrackerConfig } from '../../../src/types';

describe('ClickCollector', () => {
  let collector: ClickCollector;
  let tracker: Tracker;
  let trackEventSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    vi.useFakeTimers();
    collector = new ClickCollector();
    const config: TrackerConfig = {
      endpoint: 'https://api.example.com',
      projectId: 1,
      autoStart: false,
    };
    tracker = new Tracker(config);
    trackEventSpy = vi.spyOn(tracker, 'trackEvent');
  });

  afterEach(() => {
    collector.stop();
    trackEventSpy.mockRestore();
    vi.useRealTimers();
    // 清理 DOM
    document.body.innerHTML = '';
  });

  it('应该获取正确的采集器名称', () => {
    expect(collector.getName()).toBe('click');
  });

  it('应该采集点击事件', async () => {
    collector.start(tracker);

    const button = document.createElement('button');
    button.id = 'test-button';
    button.textContent = 'Click me';
    document.body.appendChild(button);

    button.click();

    // 等待防抖处理
    await vi.advanceTimersByTimeAsync(400);

    expect(trackEventSpy).toHaveBeenCalled();
    expect(trackEventSpy).toHaveBeenCalledWith('click', expect.objectContaining({
      tag: 'button',
      id: 'test-button',
    }));
  });

  it('应该提取元素信息', async () => {
    collector.start(tracker);

    const link = document.createElement('a');
    link.href = 'https://example.com';
    link.className = 'test-link';
    link.textContent = 'Link Text';
    document.body.appendChild(link);

    link.click();

    await vi.advanceTimersByTimeAsync(400);

    expect(trackEventSpy).toHaveBeenCalledWith('click', expect.objectContaining({
      tag: 'a',
      href: expect.stringContaining('example.com'), // jsdom 可能会规范化 URL
      className: 'test-link',
      text: 'Link Text',
    }));
  });

  it('停止后应该不再采集', async () => {
    collector.start(tracker);
    collector.stop();

    const button = document.createElement('button');
    document.body.appendChild(button);
    button.click();

    await vi.advanceTimersByTimeAsync(400);

    expect(trackEventSpy).not.toHaveBeenCalled();
  });

  it('应该防抖处理快速点击', async () => {
    collector.start(tracker);

    const button = document.createElement('button');
    document.body.appendChild(button);

    // 快速点击多次
    button.click();
    button.click();
    button.click();

    await vi.advanceTimersByTimeAsync(400);

    // 应该只触发一次（防抖）
    expect(trackEventSpy).toHaveBeenCalledTimes(1);
  });
});

