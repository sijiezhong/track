/**
 * 批量像素上报
 * 
 * 使用压缩和Base64编码将多个事件合并为单个请求，减少URL长度和请求数量。
 * 
 * @packageDocumentation
 */

import { EventData } from '../types';
import { API_PATHS } from '../constants';
import { buildQueryString } from '../utils/url';
import { compressBatchEvents, encodeBatchEvents } from '../core/batch-merger';
import { PixelOptions } from './pixel';

/**
 * 批量像素上报选项
 */
export interface PixelBatchOptions extends PixelOptions {
  /** 是否启用批量模式（默认 true） */
  useBatch?: boolean;
}

/**
 * 使用像素上报发送批量事件
 * 
 * @param events - 事件数据数组
 * @param options - 上报选项
 * @returns Promise，成功时 resolve，失败时 reject
 * 
 * @remarks
 * 批量上报会压缩事件数据并使用Base64编码，以减少URL长度。
 * URL参数使用缩写形式（t, s, u, b, bt）以进一步减少长度。
 * 如果URL超过限制，会自动拆分成多个批次发送。
 * 
 * @example
 * ```ts
 * await sendPixelBatch([
 *   { event_type: 'pageview', event_content: { url: '/' } },
 *   { event_type: 'click', event_content: { element: 'button' } }
 * ], {
 *   endpoint: 'https://api.example.com',
 *   projectId: 1,
 *   sessionId: 'abc123'
 * });
 * ```
 */
export function sendPixelBatch(
  events: EventData[],
  options: PixelBatchOptions
): Promise<void> {
  if (events.length === 0) {
    return Promise.resolve();
  }

  // 最大URL长度（提供安全余量）
  const MAX_URL_LENGTH = 2000;
  
  // 尝试发送单个批次，如果URL太长则拆分
  return sendBatchWithSplit(events, options, MAX_URL_LENGTH);
}

/**
 * 发送批量事件（如果URL太长则自动拆分）
 * 
 * @param events - 事件数组
 * @param options - 上报选项
 * @param maxUrlLength - 最大URL长度
 * @returns Promise
 */
async function sendBatchWithSplit(
  events: EventData[],
  options: PixelBatchOptions,
  maxUrlLength: number
): Promise<void> {
  // 先尝试完整发送
  const url = buildBatchUrl(events, options);
  
  if (url.length <= maxUrlLength) {
    // URL 长度在限制内，直接发送
    return sendSingleBatch(events, options);
  }
  
  // URL 太长，需要拆分
  // 计算基础URL长度（不含事件数据）
  const baseParams = buildBaseParams(options);
  const baseUrl = `${options.endpoint}${API_PATHS.PIXEL}?${baseParams}`;
  const baseLength = baseUrl.length + 10; // 预留一些空间（bt=1&b=）
  
  // 估计每个事件平均占用多少字符（保守估计）
  // 这里使用二分法逐步减少事件数量，直到URL长度合适
  let batchSize = events.length;
  let attemptCount = 0;
  const maxAttempts = 10; // 最多尝试10次，避免无限循环
  
  // 从全部事件开始，逐步减半直到找到合适的批次大小
  while (batchSize > 0 && attemptCount < maxAttempts) {
    const testBatch = events.slice(0, batchSize);
    const testCompressed = compressBatchEvents(testBatch);
    const testB64 = encodeBatchEvents(testCompressed);
    const testUrl = `${baseUrl}&b=${testB64}`;
    
    if (testUrl.length <= maxUrlLength) {
      // 找到了合适的批次大小，开始分批发送
      return sendMultipleBatches(events, options, batchSize);
    }
    
    // URL 仍然太长，减少批次大小（减半）
    batchSize = Math.floor(batchSize / 2);
    attemptCount++;
  }
  
  // 如果仍然无法找到合适的批次大小，按单个事件发送（降级）
  if (batchSize === 0) {
    batchSize = 1;
  }
  
  return sendMultipleBatches(events, options, batchSize);
}

/**
 * 构建批量上报的基础参数（不含事件数据）
 * 
 * @param options - 上报选项
 * @returns 查询参数字符串
 */
function buildBaseParams(options: PixelBatchOptions): string {
  const params: string[] = [];
  
  params.push(`t=${encodeURIComponent(String(options.projectId))}`);
  params.push(`bt=1`);
  
  if (options.sessionId) {
    params.push(`s=${encodeURIComponent(options.sessionId)}`);
  }
  if (options.userId !== undefined) {
    params.push(`u=${encodeURIComponent(String(options.userId))}`);
  }
  
  return params.join('&');
}

/**
 * 构建批量上报的完整URL
 * 
 * @param events - 事件数组
 * @param options - 上报选项
 * @returns 完整URL
 */
function buildBatchUrl(
  events: EventData[],
  options: PixelBatchOptions
): string {
  const compressed = compressBatchEvents(events);
  const eventsB64 = encodeBatchEvents(compressed);
  const baseParams = buildBaseParams(options);
  return `${options.endpoint}${API_PATHS.PIXEL}?${baseParams}&b=${eventsB64}`;
}

/**
 * 发送单个批次
 * 
 * @param events - 事件数组
 * @param options - 上报选项
 * @returns Promise
 */
function sendSingleBatch(
  events: EventData[],
  options: PixelBatchOptions
): Promise<void> {
  return new Promise((resolve, reject) => {
    const url = buildBatchUrl(events, options);
    
    // 创建 Image 对象发送请求
    const img = new Image();
    
    // 批量请求超时时间稍长（因为数据量大）
    const timeout = setTimeout(() => {
      img.onload = null;
      img.onerror = null;
      reject(new Error('Pixel batch tracking timeout'));
    }, 10000);

    img.onload = () => {
      clearTimeout(timeout);
      resolve();
    };

    img.onerror = (error) => {
      clearTimeout(timeout);
      reject(
        new Error(`Pixel batch tracking failed: ${error instanceof Error ? error.message : 'Unknown error'}`)
      );
    };

    // 开始加载图片（发送请求）
    img.src = url;
  });
}

/**
 * 分批发送多个批次
 * 
 * @param events - 所有事件
 * @param options - 上报选项
 * @param batchSize - 每批大小
 * @returns Promise
 */
async function sendMultipleBatches(
  events: EventData[],
  options: PixelBatchOptions,
  batchSize: number
): Promise<void> {
  // 将事件数组分割成多个批次
  const batches: EventData[][] = [];
  for (let i = 0; i < events.length; i += batchSize) {
    batches.push(events.slice(i, i + batchSize));
  }
  
  // 并发发送所有批次（使用 Promise.allSettled 确保所有批次都尝试发送）
  const results = await Promise.allSettled(
    batches.map(batch => sendSingleBatch(batch, options))
  );
  
  // 检查是否有失败的批次
  const failures = results.filter(r => r.status === 'rejected');
  if (failures.length > 0) {
    const errorMessages = failures
      .map((r: PromiseRejectedResult) => r.reason?.message || 'Unknown error')
      .join('; ');
    throw new Error(`Failed to send ${failures.length} out of ${batches.length} batches: ${errorMessages}`);
  }
}

