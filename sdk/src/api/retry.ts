/**
 * 重试机制
 * @packageDocumentation
 */

/**
 * 重试配置
 */
export interface RetryOptions {
  /** 最大重试次数，默认 5 */
  maxRetries?: number;
  /** 初始重试延迟（毫秒），默认 1000 */
  retryDelay?: number;
  /** 重试退避倍数，默认 2（指数退避） */
  retryBackoff?: number;
  /** 是否应该重试的函数，默认重试所有错误 */
  shouldRetry?: (error: Error) => boolean;
}

/**
 * 重试结果
 */
export interface RetryResult<T> {
  /** 是否成功 */
  success: boolean;
  /** 结果数据（成功时） */
  data?: T;
  /** 错误信息（失败时） */
  error?: Error;
  /** 实际重试次数 */
  retries: number;
}

/**
 * 使用重试机制执行异步函数
 * 
 * @param fn - 要执行的异步函数
 * @param options - 重试配置
 * @returns 重试结果
 * 
 * @example
 * ```ts
 * const result = await retry(
 *   () => fetch('/api/data'),
 *   { maxRetries: 3, retryDelay: 1000 }
 * );
 * ```
 */
export async function retry<T>(
  fn: () => Promise<T>,
  options: RetryOptions = {}
): Promise<RetryResult<T>> {
  const {
    maxRetries = 5,
    retryDelay = 1000,
    retryBackoff = 2,
    shouldRetry = () => true,
  } = options;

  let lastError: Error | undefined;
  let delay = retryDelay;

  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      const data = await fn();
      return {
        success: true,
        data,
        retries: attempt,
      };
    } catch (error) {
      lastError = error instanceof Error ? error : new Error(String(error));

      // 检查是否应该重试
      if (!shouldRetry(lastError)) {
        return {
          success: false,
          error: lastError,
          retries: attempt,
        };
      }

      // 如果还有重试机会，等待后重试
      if (attempt < maxRetries) {
        await sleep(delay);
        delay *= retryBackoff; // 指数退避
      }
    }
  }

  return {
    success: false,
    error: lastError,
    retries: maxRetries,
  };
}

/**
 * 延迟函数
 * 
 * @param ms - 延迟毫秒数
 * @returns Promise
 */
function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

