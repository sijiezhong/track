/**
 * 像素上报（1x1 GIF）
 * @packageDocumentation
 */

import { EventData } from '../types';
import { API_PATHS, HEADERS } from '../constants';
import { buildQueryString } from '../utils/url';
import { generateUUID } from '../utils/uuid';

/**
 * 像素上报选项
 */
export interface PixelOptions {
  /** 服务端地址 */
  endpoint: string;
  /** 项目 ID（租户 ID） */
  projectId: string | number;
  /** 会话 ID */
  sessionId?: string;
  /** 用户 ID */
  userId?: string | number;
  /** 幂等键（可选） */
  idempotencyKey?: string;
}

/**
 * 使用像素上报发送事件
 * 
 * @param event - 事件数据
 * @param options - 上报选项
 * @returns Promise，成功时 resolve，失败时 reject
 * 
 * @remarks
 * 使用 1x1 GIF 图片请求规避跨域问题，兼容性好
 * 
 * @example
 * ```ts
 * await sendPixel({
 *   event_type: 'pageview',
 *   event_content: { url: '/' }
 * }, {
 *   endpoint: 'https://api.example.com',
 *   projectId: 1,
 *   sessionId: 'abc123'
 * });
 * ```
 */
export function sendPixel(
  event: EventData,
  options: PixelOptions
): Promise<void> {
  return new Promise((resolve, reject) => {
    const {
      endpoint,
      projectId,
      sessionId,
      userId,
      idempotencyKey = generateUUID(),
    } = options;

    // 构建查询参数
    const params: Record<string, unknown> = {
      eventName: event.event_type || 'pixel',
      tenantId: projectId, // 添加租户 ID 到 URL 参数（避免跨域问题）
    };

    // 添加事件内容（如果存在）
    if (event.event_content) {
      // 将对象序列化为 JSON 字符串
      params.eventContent = JSON.stringify(event.event_content);
    }

    // 添加会话和用户信息
    if (sessionId) {
      params.sessionId = sessionId;
    }
    if (userId !== undefined) {
      params.userId = userId;
    }

    // 构建完整 URL
    const queryString = buildQueryString(params);
    const url = `${endpoint}${API_PATHS.PIXEL}?${queryString}`;

    // 创建 Image 对象发送请求
    const img = new Image();

    // 设置超时处理
    const timeout = setTimeout(() => {
      img.onload = null;
      img.onerror = null;
      reject(new Error('Pixel tracking timeout'));
    }, 5000);

    // 设置事件处理器
    img.onload = () => {
      clearTimeout(timeout);
      resolve();
    };

    img.onerror = (error) => {
      clearTimeout(timeout);
      reject(
        new Error(`Pixel tracking failed: ${error instanceof Error ? error.message : 'Unknown error'}`)
      );
    };

    // 注意：像素上报通常无法设置自定义请求头，租户 ID 可以通过 URL 参数传递
    // 但根据 API 文档，租户 ID 应该通过 X-Tenant-Id 请求头传递
    // 由于 Image 对象限制，建议使用 REST API 上报以获得完整的请求头支持
    // 开始加载图片（发送请求）
    img.src = url;
  });
}

