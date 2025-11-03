/**
 * SDK 常量定义
 * @packageDocumentation
 */

/**
 * 默认配置常量
 */
export const DEFAULT_CONFIG = {
  /** 批量上报阈值，达到此数量时立即上报（像素上报模式下建议为 1） */
  BATCH_SIZE: 1, // 改为 1，降低延迟，立即上报
  /** 批量上报超时时间（毫秒），超时后自动上报 */
  BATCH_TIMEOUT: 1000, // 改为 1000ms，更快响应
  /** 最大重试次数 */
  MAX_RETRIES: 5,
  /** 初始重试延迟（毫秒） */
  RETRY_DELAY: 1000,
  /** 重试退避倍数 */
  RETRY_BACKOFF: 2,
  /** 会话过期时间（毫秒），默认 30 分钟 */
  SESSION_TIMEOUT: 30 * 60 * 1000,
  /** 是否自动启动采集 */
  AUTO_START: true,
  /** 是否启用调试模式 */
  DEBUG: false,
} as const;

/**
 * 存储键名常量
 */
export const STORAGE_KEYS = {
  /** 会话 ID 存储键 */
  SESSION_ID: '__track_session_id',
  /** 匿名 ID 存储键 */
  ANONYMOUS_ID: '__track_anonymous_id',
  /** 用户 ID 存储键 */
  USER_ID: '__track_user_id',
  /** 事件队列存储键 */
  EVENT_QUEUE: '__track_event_queue',
  /** 会话时间戳存储键 */
  SESSION_TIMESTAMP: '__track_session_timestamp',
} as const;

/**
 * API 路径常量
 */
export const API_PATHS = {
  /** 事件采集接口（POST） */
  COLLECT: '/api/v1/events/collect',
  /** 事件采集接口（GET） */
  COLLECT_GET: '/api/v1/events/collect',
  /** 批量事件采集接口 */
  BATCH_COLLECT: '/api/v1/events/collect/batch',
  /** 像素上报接口 */
  PIXEL: '/api/v1/pixel.gif',
} as const;

/**
 * HTTP 请求头常量
 */
export const HEADERS = {
  /** 租户 ID 请求头 */
  TENANT_ID: 'X-Tenant-Id',
  /** 幂等键请求头 */
  IDEMPOTENCY_KEY: 'Idempotency-Key',
} as const;

/**
 * 事件类型常量
 */
export const EVENT_TYPES = {
  /** 页面访问 */
  PAGEVIEW: 'pageview',
  /** 点击事件 */
  CLICK: 'click',
  /** 性能数据 */
  PERFORMANCE: 'performance',
  /** 错误事件 */
  ERROR: 'error',
  /** 自定义事件 */
  CUSTOM: 'custom',
} as const;

/**
 * 采集器名称常量
 */
export const COLLECTOR_NAMES = {
  /** 页面访问采集器 */
  PAGEVIEW: 'pageview',
  /** 点击采集器 */
  CLICK: 'click',
  /** 性能采集器 */
  PERFORMANCE: 'performance',
  /** 错误采集器 */
  ERROR: 'error',
} as const;

/**
 * 批量像素上报配置常量
 */
export const BATCH_PIXEL_CONFIG = {
  /** 是否启用批量像素上报 */
  ENABLED: true,
  /** 单个请求最大事件数 */
  MAX_EVENTS: 10,
  /** 合并窗口时间（毫秒） */
  MERGE_WINDOW: 200,
  /** 最大URL长度（字符） */
  MAX_URL_LENGTH: 1500,
  /** 是否启用Base64压缩 */
  USE_COMPRESSION: true,
} as const;

