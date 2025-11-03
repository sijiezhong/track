/**
 * 事件队列测试
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { EventQueue } from '../../../src/core/queue';
import { Storage } from '../../../src/core/storage';
import { EventData } from '../../../src/types';

describe('EventQueue', () => {
  let storage: Storage;
  let queue: EventQueue;
  let flushCallback: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    localStorage.clear();
    storage = new Storage();
    queue = new EventQueue(storage, 100, 5000);
    flushCallback = vi.fn();
    queue.setFlushCallback(flushCallback);
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe('添加事件', () => {
    it('应该添加事件到队列', () => {
      const event: EventData = { event_type: 'test' };
      const added = queue.add(event);
      expect(added).toBe(true);
      expect(queue.size()).toBe(1);
    });

    it('应该批量添加事件', () => {
      const events: EventData[] = [
        { event_type: 'test1' },
        { event_type: 'test2' },
      ];
      const count = queue.addBatch(events);
      expect(count).toBe(2);
      expect(queue.size()).toBe(2);
    });

    it('队列满时应该返回 false', () => {
      const smallQueue = new EventQueue(storage, 2, 5000);
      smallQueue.setFlushCallback(flushCallback);

      expect(smallQueue.add({ event_type: 'test1' })).toBe(true);
      expect(smallQueue.add({ event_type: 'test2' })).toBe(true);
      expect(smallQueue.add({ event_type: 'test3' })).toBe(false);
    });
  });

  describe('队列操作', () => {
    it('isEmpty 应该正确判断空队列', () => {
      expect(queue.isEmpty()).toBe(true);
      queue.add({ event_type: 'test' });
      expect(queue.isEmpty()).toBe(false);
    });

    it('size 应该返回队列长度', () => {
      expect(queue.size()).toBe(0);
      queue.add({ event_type: 'test1' });
      expect(queue.size()).toBe(1);
      queue.add({ event_type: 'test2' });
      expect(queue.size()).toBe(2);
    });

    it('getAll 应该返回所有事件的副本', () => {
      queue.add({ event_type: 'test1' });
      queue.add({ event_type: 'test2' });

      const all = queue.getAll();
      expect(all.length).toBe(2);
      expect(all[0].event_type).toBe('test1');
      expect(all[1].event_type).toBe('test2');

      // 修改返回的数组不应影响队列
      all.push({ event_type: 'test3' });
      expect(queue.size()).toBe(2);
    });

    it('clear 应该清空队列', () => {
      queue.add({ event_type: 'test1' });
      queue.add({ event_type: 'test2' });
      queue.clear();
      expect(queue.size()).toBe(0);
      expect(queue.isEmpty()).toBe(true);
    });
  });

  describe('刷新和上报', () => {
    it('flush 应该返回并清空所有事件', () => {
      queue.add({ event_type: 'test1' });
      queue.add({ event_type: 'test2' });

      const events = queue.flush();
      expect(events.length).toBe(2);
      expect(queue.size()).toBe(0);
    });

    it('shouldFlush 应该正确判断是否需要上报', () => {
      expect(queue.shouldFlush(10)).toBe(false);
      for (let i = 0; i < 10; i++) {
        queue.add({ event_type: `test${i}` });
      }
      expect(queue.shouldFlush(10)).toBe(true);
    });

    it('触发刷新应该调用回调', () => {
      queue.add({ event_type: 'test' });
      queue.triggerFlush();

      expect(flushCallback).toHaveBeenCalledTimes(1);
      expect(flushCallback).toHaveBeenCalledWith([
        { event_type: 'test' },
      ]);
    });
  });

  describe('定时器触发', () => {
    it('应该启动刷新定时器', () => {
      queue.add({ event_type: 'test' });
      vi.advanceTimersByTime(5000);

      expect(flushCallback).toHaveBeenCalled();
    });

    it('应该清除定时器', () => {
      queue.add({ event_type: 'test' });
      queue.clear();

      vi.advanceTimersByTime(5000);
      expect(flushCallback).not.toHaveBeenCalled();
    });
  });

  describe('持久化', () => {
    it('应该从存储中加载队列', () => {
      const event: EventData = { event_type: 'test' };
      queue.add(event);

      const newQueue = new EventQueue(storage);
      newQueue.setFlushCallback(flushCallback);

      // 应该加载之前保存的事件
      const loaded = newQueue.getAll();
      expect(loaded.length).toBeGreaterThan(0);
    });
  });
});

