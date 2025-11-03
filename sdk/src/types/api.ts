/**
 * API 相关类型定义
 * @packageDocumentation
 */

/**
 * API 响应基础结构
 */
export interface ApiResponse<T = unknown> {
  /** 响应码 */
  code: number;
  /** 响应消息 */
  message: string;
  /** 响应数据 */
  data?: T;
  /** 响应时间戳 */
  timestamp?: string;
}

/**
 * 像素上报响应类型
 */
export type PixelResponse = void;

/**
 * 事件采集请求体（对应后端 EventCollectRequest）
 * 
 * @remarks
 * 前端只传递 event_type 和 event_content，其他字段由服务端补全
 */
export interface EventCollectRequest {
  /** 事件名称（对应后端的 eventName，支持别名 event_type） */
  event_type?: string;
  /** 事件名称（别名：event_type） */
  eventName?: string;
  /** 会话 ID */
  sessionId?: string;
  /** 用户 ID（可选，匿名则为空） */
  userId?: number | string;
  /** 租户 ID（可选，会从请求头 X-Tenant-Id 获取） */
  tenantId?: number;
  /** 项目 ID（别名：tenantId） */
  project_id?: number;
  /** 事件属性（JSON 对象，对应后端的 properties，支持别名 event_content） */
  event_content?: Record<string, unknown>;
  /** 事件属性（别名：event_content） */
  properties?: Record<string, unknown>;
  /** 匿名 ID */
  anonymous_id?: string;
  anonymousId?: string;
}

/**
 * 批量事件采集请求体
 */
export type BatchEventCollectRequest = EventCollectRequest[];

/**
 * 幂等性摘要（对应后端 IdempotentSummary）
 */
export interface IdempotentSummary {
  /** 事件 ID */
  eventId: number;
  /** 事件名称 */
  eventName: string;
  /** 事件时间 */
  eventTime: string;
}

/**
 * 批量上报结果项
 */
export interface BatchCollectResultItem {
  /** 原请求数组中的下标 */
  index: number;
  /** 状态：created 或 failed */
  status: 'created' | 'failed';
  /** 失败原因（失败时才有值） */
  message?: string;
}

