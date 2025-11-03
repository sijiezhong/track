/**
 * 重试机制测试
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { retry, RetryOptions } from '../../../src/api/retry';

describe('retry', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('成功时应该立即返回', async () => {
    const fn = vi.fn().mockResolvedValue('success');
    const result = await retry(fn);

    expect(result.success).toBe(true);
    expect(result.data).toBe('success');
    expect(result.retries).toBe(0);
    expect(fn).toHaveBeenCalledTimes(1);
  });

  it('失败后重试应该成功', async () => {
    const fn = vi
      .fn()
      .mockRejectedValueOnce(new Error('fail'))
      .mockResolvedValueOnce('success');

    const promise = retry(fn, { maxRetries: 3, retryDelay: 1000 });

    // 等待第一次失败
    await vi.advanceTimersByTimeAsync(0);
    // 等待重试
    await vi.advanceTimersByTimeAsync(1000);

    const result = await promise;

    expect(result.success).toBe(true);
    expect(result.data).toBe('success');
    expect(result.retries).toBe(1);
    expect(fn).toHaveBeenCalledTimes(2);
  });

  it('超过最大重试次数应该失败', async () => {
    const fn = vi.fn().mockRejectedValue(new Error('fail'));

    const promise = retry(fn, { maxRetries: 2, retryDelay: 1000 });

    // 等待所有重试
    await vi.advanceTimersByTimeAsync(5000);

    const result = await promise;

    expect(result.success).toBe(false);
    expect(result.error).toBeTruthy();
    expect(result.retries).toBe(2);
    expect(fn).toHaveBeenCalledTimes(3); // 初始 + 2 次重试
  });

  it('应该使用指数退避', async () => {
    const fn = vi.fn().mockRejectedValue(new Error('fail'));
    const callTimes: number[] = [];

    const originalSetTimeout = global.setTimeout;
    vi.spyOn(global, 'setTimeout').mockImplementation((callback, delay) => {
      callTimes.push(Date.now());
      return originalSetTimeout(callback, delay);
    });

    const promise = retry(fn, {
      maxRetries: 2,
      retryDelay: 1000,
      retryBackoff: 2,
    });

    await vi.advanceTimersByTimeAsync(5000);
    await promise;

    // 验证延迟时间递增
    expect(callTimes.length).toBeGreaterThan(1);
  });

  it('shouldRetry 应该控制是否重试', async () => {
    const error = new Error('should not retry');
    const fn = vi.fn().mockRejectedValue(error);

    const shouldRetry = (err: Error) => err.message !== 'should not retry';

    const promise = retry(fn, {
      maxRetries: 5,
      retryDelay: 100,
      shouldRetry,
    });

    await vi.advanceTimersByTimeAsync(1000);
    const result = await promise;

    expect(result.success).toBe(false);
    expect(result.retries).toBe(0); // 不应该重试
    expect(fn).toHaveBeenCalledTimes(1);
  });
});

