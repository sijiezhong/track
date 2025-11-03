/**
 * 事件相关类型定义
 * @packageDocumentation
 */

/**
 * 事件类型枚举
 */
export enum EventType {
  /** 页面访问 */
  PAGEVIEW = 'pageview',
  /** 点击事件 */
  CLICK = 'click',
  /** 性能数据 */
  PERFORMANCE = 'performance',
  /** 错误事件 */
  ERROR = 'error',
  /** 自定义事件 */
  CUSTOM = 'custom',
}

/**
 * 事件内容类型（任意对象）
 */
export type EventContent = Record<string, unknown>;

/**
 * 事件数据结构
 * 
 * @remarks
 * 前端只上报 event_type 和 event_content，其他字段由服务端补全
 */
export interface EventData {
  /** 事件类型（对应后端的 event_type） */
  event_type: string;
  /** 事件内容（对应后端的 event_content，JSON 对象） */
  event_content?: EventContent;
}

/**
 * 批量事件数据
 */
export interface BatchEventData {
  /** 事件列表 */
  events: EventData[];
}

/**
 * 页面访问事件内容
 */
export interface PageViewContent extends EventContent {
  /** 页面 URL */
  url?: string;
  /** 页面标题 */
  title?: string;
  /** 来源页面 */
  referrer?: string;
  /** 页面路径 */
  path?: string;
  /** 页面查询参数 */
  search?: string;
  /** 页面哈希 */
  hash?: string;
}

/**
 * 点击事件内容
 */
export interface ClickContent extends EventContent {
  /** 元素标签名 */
  tag?: string;
  /** 元素 ID */
  id?: string;
  /** 元素类名 */
  className?: string;
  /** 元素文本内容 */
  text?: string;
  /** 元素选择器 */
  selector?: string;
  /** 点击位置 X 坐标 */
  x?: number;
  /** 点击位置 Y 坐标 */
  y?: number;
  /** 目标 URL（如果是链接） */
  href?: string;
}

/**
 * 性能事件内容
 */
export interface PerformanceContent extends EventContent {
  /** 页面加载时间（毫秒） */
  loadTime?: number;
  /** DOM 内容加载时间（毫秒） */
  domContentLoaded?: number;
  /** 首次内容绘制时间（毫秒） */
  firstPaint?: number;
  /** 首次有意义绘制时间（毫秒） */
  firstContentfulPaint?: number;
  /** 最大内容绘制时间（毫秒） */
  largestContentfulPaint?: number;
  /** 首次输入延迟（毫秒） */
  firstInputDelay?: number;
  /** 累积布局偏移 */
  cumulativeLayoutShift?: number;
  /** 资源加载时间列表 */
  resources?: Array<{
    name: string;
    duration: number;
    size?: number;
    type?: string;
  }>;
}

/**
 * 错误事件内容
 */
export interface ErrorContent extends EventContent {
  /** 错误消息 */
  message?: string;
  /** 错误堆栈 */
  stack?: string;
  /** 错误文件名 */
  filename?: string;
  /** 错误行号 */
  lineno?: number;
  /** 错误列号 */
  colno?: number;
  /** 错误类型 */
  errorType?: 'javascript' | 'promise' | 'resource';
  /** 资源错误信息（如果是资源加载错误） */
  resourceError?: {
    tagName?: string;
    href?: string;
    src?: string;
  };
}

