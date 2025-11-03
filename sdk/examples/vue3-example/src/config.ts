/**
 * SDK 配置
 */

/**
 * SDK 配置常量
 */
export const SDK_CONFIG = {
  /** 服务端 API 地址 */
  endpoint: import.meta.env.VITE_API_ENDPOINT || 'http://localhost:8080',
  /** 项目 ID（对应服务端的 tenantId） */
  projectId: import.meta.env.VITE_PROJECT_ID || 1,
  /** 是否自动启动 */
  autoStart: true,
  /** 是否启用调试模式 */
  debug: true,
  /** 是否使用像素上报（默认 true，避免跨域问题） */
  usePixel: true,
  /** 批量上报阈值 */
  batchSize: 10,
  /** 批量上报超时时间（毫秒） */
  batchTimeout: 5000,
} as const;

