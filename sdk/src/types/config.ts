/**
 * SDK 配置相关类型定义
 * @packageDocumentation
 */

/**
 * SDK 重试配置
 */
export interface RetryConfig {
  /** 最大重试次数，默认 5 */
  maxRetries?: number;
  /** 初始重试延迟（毫秒），默认 1000 */
  retryDelay?: number;
  /** 重试退避倍数，默认 2（指数退避） */
  retryBackoff?: number;
}

/**
 * 采集器配置
 */
export interface CollectorConfig {
  /** 是否启用页面访问采集 */
  pageview?: boolean;
  /** 是否启用点击事件采集 */
  click?: boolean;
  /** 是否启用性能数据采集 */
  performance?: boolean;
  /** 是否启用错误采集 */
  error?: boolean;
}

/**
 * 批量像素上报配置
 */
export interface BatchPixelConfig {
  /** 是否启用批量像素上报，默认 true */
  enabled?: boolean;
  /** 单个请求最大事件数，默认 10 */
  maxEvents?: number;
  /** 合并窗口时间（毫秒），默认 200 */
  mergeWindow?: number;
  /** 最大URL长度（字符），默认 1500 */
  maxUrlLength?: number;
  /** 是否启用Base64压缩，默认 true */
  useCompression?: boolean;
}

/**
 * 点击事件过滤配置
 */
export interface ClickFilterConfig {
  /** 是否启用过滤，默认 true */
  enabled?: boolean;
  /** 最小点击区域尺寸（px），默认 10 */
  minClickSize?: number;
  /** 黑名单选择器数组 */
  blacklist?: string[];
  /** 白名单选择器数组 */
  whitelist?: string[];
  /** 忽略数据属性名称，默认 'data-track-ignore' */
  ignoreAttribute?: string;
  /** 强制追踪数据属性名称，默认 'data-track' */
  trackAttribute?: string;
}

/**
 * SDK 主配置接口
 */
export interface TrackerConfig {
  /** 服务端 API 地址（必填） */
  endpoint: string;
  /** 项目 ID，对应服务端的 tenantId（必填） */
  projectId: string | number;
  /** 是否自动启动采集，默认 true */
  autoStart?: boolean;
  /** 批量上报阈值，达到此数量时立即上报，默认 10（像素上报模式下建议设置为 1 以降低延迟） */
  batchSize?: number;
  /** 批量上报超时时间（毫秒），超时后自动上报，默认 5000（像素上报模式下建议设置为较小值，如 1000） */
  batchTimeout?: number;
  /** 重试配置 */
  retry?: RetryConfig;
  /** 采集器配置 */
  collectors?: CollectorConfig;
  /** 是否启用调试模式，默认 false */
  debug?: boolean;
  /** 是否使用像素上报（1x1 GIF），默认 false（使用 REST API） */
  usePixel?: boolean;
  /** 批量像素上报配置 */
  batchPixel?: BatchPixelConfig;
  /** 点击事件过滤配置 */
  clickFilter?: ClickFilterConfig;
  /** 自定义会话 ID（可选，通常由 SDK 自动生成） */
  sessionId?: string;
  /** 自定义匿名 ID（可选，通常由 SDK 自动生成） */
  anonymousId?: string;
}

/**
 * Vite Plugin 配置选项
 */
export interface VitePluginOptions {
  /** 服务端 API 地址（必填） */
  endpoint: string;
  /** 项目 ID（必填） */
  projectId: string | number;
  /** 是否自动启动采集 */
  autoStart?: boolean;
  /** 批量上报阈值 */
  batchSize?: number;
  /** 批量上报超时时间 */
  batchTimeout?: number;
  /** 启用的采集器列表 */
  collectors?: Array<'pageview' | 'click' | 'performance' | 'error'>;
  /** 是否启用调试模式 */
  debug?: boolean;
  /** 是否使用像素上报 */
  usePixel?: boolean;
}

