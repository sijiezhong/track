/**
 * 事件类型压缩码枚举（用于减少URL长度）
 * 
 * 与后端 EventTypeEnum 保持一致，确保前后端压缩码一致。
 * 事件类型从平均10字符缩减到2字符，节省约80%字符空间。
 * 
 * @packageDocumentation
 */

/**
 * 事件类型压缩码枚举
 */
export enum EventTypeCode {
  /** 页面访问事件 */
  PAGEVIEW = 'pv',
  /** 点击事件 */
  CLICK = 'ck',
  /** 性能数据事件 */
  PERFORMANCE = 'pf',
  /** 错误事件 */
  ERROR = 'er',
  /** 自定义事件 */
  CUSTOM = 'ct',
}

/**
 * 完整事件名映射
 */
export const EVENT_TYPE_MAP: Record<EventTypeCode, string> = {
  [EventTypeCode.PAGEVIEW]: 'pageview',
  [EventTypeCode.CLICK]: 'click',
  [EventTypeCode.PERFORMANCE]: 'performance',
  [EventTypeCode.ERROR]: 'error',
  [EventTypeCode.CUSTOM]: 'custom',
} as const;

/**
 * 事件名到压缩码的映射
 */
export const EVENT_NAME_TO_CODE: Record<string, EventTypeCode> = {
  'pageview': EventTypeCode.PAGEVIEW,
  'click': EventTypeCode.CLICK,
  'performance': EventTypeCode.PERFORMANCE,
  'error': EventTypeCode.ERROR,
  'custom': EventTypeCode.CUSTOM,
};

/**
 * 将事件名转换为压缩码
 * 
 * @param eventName - 完整事件名（如 "pageview", "click"）
 * @returns 对应的事件类型压缩码，未匹配时返回 CUSTOM
 */
export function getEventTypeCode(eventName: string): EventTypeCode {
  return EVENT_NAME_TO_CODE[eventName.toLowerCase()] || EventTypeCode.CUSTOM;
}

/**
 * 将压缩码转换为完整事件名
 * 
 * @param code - 压缩码（如 "pv", "ck"）
 * @returns 完整事件名，未匹配时返回 "custom"
 */
export function getEventTypeName(code: EventTypeCode): string {
  return EVENT_TYPE_MAP[code] || 'custom';
}

