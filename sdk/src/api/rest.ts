/**
 * REST API 上报
 * @packageDocumentation
 */

import { EventData, EventCollectRequest, IdempotentSummary, ApiResponse } from '../types';
import { API_PATHS, HEADERS } from '../constants';
import { generateUUID } from '../utils/uuid';

/**
 * REST API 上报选项
 */
export interface RestApiOptions {
  /** 服务端地址 */
  endpoint: string;
  /** 项目 ID（租户 ID） */
  projectId: string | number;
  /** 会话 ID */
  sessionId?: string;
  /** 用户 ID */
  userId?: string | number;
  /** 幂等键（可选，会自动生成） */
  idempotencyKey?: string;
}

/**
 * 发送单个事件（POST 方式）
 * 
 * @param event - 事件数据
 * @param options - 上报选项
 * @returns Promise，返回服务端响应
 * 
 * @example
 * ```ts
 * const result = await sendEvent({
 *   event_type: 'pageview',
 *   event_content: { url: '/' }
 * }, {
 *   endpoint: 'https://api.example.com',
 *   projectId: 1,
 *   sessionId: 'abc123'
 * });
 * ```
 */
export async function sendEvent(
  event: EventData,
  options: RestApiOptions
): Promise<ApiResponse<IdempotentSummary>> {
  const {
    endpoint,
    projectId,
    sessionId,
    userId,
    idempotencyKey = generateUUID(),
  } = options;

  // 构建请求体（对应后端 EventCollectRequest）
  const requestBody: EventCollectRequest = {
    event_type: event.event_type,
    event_content: event.event_content as Record<string, unknown>,
    sessionId: sessionId,
    userId: userId !== undefined ? (typeof userId === 'number' ? userId : parseInt(String(userId), 10)) : undefined,
    tenantId: typeof projectId === 'number' ? projectId : parseInt(String(projectId), 10),
  };

  // 构建请求头
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    [HEADERS.TENANT_ID]: String(projectId),
  };

  // 添加幂等键（如果提供）
  if (idempotencyKey) {
    headers[HEADERS.IDEMPOTENCY_KEY] = idempotencyKey;
  }

  // 发送请求
  const response = await fetch(`${endpoint}${API_PATHS.COLLECT}`, {
    method: 'POST',
    headers,
    body: JSON.stringify(requestBody),
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const result = await response.json();
  return result as ApiResponse<IdempotentSummary>;
}

/**
 * 发送单个事件（GET 方式）
 * 
 * @param event - 事件数据
 * @param options - 上报选项
 * @returns Promise，返回服务端响应
 * 
 * @remarks
 * 通过查询参数传递事件信息，适用于简单场景
 */
export async function sendEventByGet(
  event: EventData,
  options: RestApiOptions
): Promise<ApiResponse<IdempotentSummary>> {
  const {
    endpoint,
    projectId,
    sessionId,
    userId,
    idempotencyKey = generateUUID(),
  } = options;

  // 构建查询参数
  const params = new URLSearchParams();
  params.append('eventName', event.event_type || '');
  params.append('sessionId', sessionId || '');

  if (userId !== undefined) {
    params.append('userId', String(userId));
  }

  if (projectId !== undefined) {
    params.append('tenantId', String(projectId));
  }

  // 构建请求头
  const headers: HeadersInit = {
    [HEADERS.TENANT_ID]: String(projectId),
  };

  // 添加幂等键（如果提供）
  if (idempotencyKey) {
    headers[HEADERS.IDEMPOTENCY_KEY] = idempotencyKey;
  }

  // 发送请求
  const url = `${endpoint}${API_PATHS.COLLECT_GET}?${params.toString()}`;
  const response = await fetch(url, {
    method: 'GET',
    headers,
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const result = await response.json();
  return result as ApiResponse<IdempotentSummary>;
}

