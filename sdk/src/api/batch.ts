/**
 * 批量上报
 * @packageDocumentation
 */

import { EventData, ApiResponse } from '../types';
import { sendPixel, PixelOptions } from './pixel';

/**
 * 批量上报选项
 */
export interface BatchOptions {
  /** 服务端地址 */
  endpoint: string;
  /** 项目 ID（租户 ID） */
  projectId: string | number;
  /** 会话 ID */
  sessionId?: string;
  /** 用户 ID */
  userId?: string | number;
}

/**
 * 批量发送事件（使用像素上报方式，避免跨域问题）
 * 
 * @param events - 事件数据数组
 * @param options - 上报选项
 * @returns Promise，返回成功数量
 * 
 * @remarks
 * 为了规避跨域问题，批量上报改为逐个使用像素上报方式发送
 * 每个事件独立发送，即使部分失败也不会影响其他事件
 * 
 * @example
 * ```ts
 * const result = await sendBatch([
 *   { event_type: 'pageview', event_content: { url: '/' } },
 *   { event_type: 'click', event_content: { element: 'button' } }
 * ], {
 *   endpoint: 'https://api.example.com',
 *   projectId: 1,
 *   sessionId: 'abc123'
 * });
 * ```
 */
export async function sendBatch(
  events: EventData[],
  options: BatchOptions
): Promise<ApiResponse<void>> {
  const {
    endpoint,
    projectId,
    sessionId,
    userId,
  } = options;

  if (events.length === 0) {
    return {
      code: 200,
      message: '成功',
      timestamp: new Date().toISOString(),
    };
  }

  // 使用像素上报逐个发送事件（避免跨域问题）
  const pixelOptions: PixelOptions = {
    endpoint,
    projectId,
    sessionId,
    userId,
  };

  // 并发发送所有事件（使用像素上报）
  const results = await Promise.allSettled(
    events.map((event) => sendPixel(event, pixelOptions))
  );

  // 统计成功和失败数量
  const successCount = results.filter((r) => r.status === 'fulfilled').length;
  const failCount = results.filter((r) => r.status === 'rejected').length;

  if (failCount === 0) {
    return {
      code: 200,
      message: `成功发送 ${successCount} 个事件`,
      timestamp: new Date().toISOString(),
    };
  } else if (successCount > 0) {
    return {
      code: 207, // 部分成功
      message: `成功发送 ${successCount} 个事件，失败 ${failCount} 个`,
      timestamp: new Date().toISOString(),
    };
  } else {
    return {
      code: 500,
      message: `所有事件发送失败`,
      timestamp: new Date().toISOString(),
    };
  }
}

