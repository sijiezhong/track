/**
 * 批量事件合并器
 * 
 * 实现智能合并策略：在合并窗口内收集事件，达到阈值或超时后批量发送。
 * 支持事件压缩、Base64编码以减少URL长度。
 * 
 * @packageDocumentation
 */

import { EventData } from '../types';
import { getEventTypeCode } from '../constants/event-types';
import { BATCH_PIXEL_CONFIG } from '../constants';

/**
 * 压缩后的事件格式（用于批量上报）
 */
interface CompressedEvent {
  /** 事件类型压缩码 */
  t: string;
  /** 事件内容（可选） */
  c?: Record<string, unknown>;
}

/**
 * 批量合并器配置
 */
interface BatchMergerConfig {
  /** 最大事件数 */
  maxEvents: number;
  /** 合并窗口时间（毫秒） */
  mergeWindow: number;
  /** 最大URL长度（字符） */
  maxUrlLength: number;
  /** 是否启用压缩 */
  useCompression: boolean;
}

/**
 * 批量事件合并器
 * 
 * 在指定的时间窗口内收集事件，达到最大事件数或超时后触发批量上报。
 */
export class BatchMerger {
  private queue: EventData[] = [];
  private mergeTimer: number | null = null;
  private flushCallback: ((events: EventData[]) => void) | null = null;
  private readonly config: BatchMergerConfig;
  /** 是否正在 flush（防止重复 flush） */
  private isFlushing: boolean = false;

  /**
   * 创建批量合并器
   * 
   * @param config - 合并器配置（可选，使用默认值）
   */
  constructor(config?: Partial<BatchMergerConfig>) {
    this.config = {
      maxEvents: config?.maxEvents ?? BATCH_PIXEL_CONFIG.MAX_EVENTS,
      mergeWindow: config?.mergeWindow ?? BATCH_PIXEL_CONFIG.MERGE_WINDOW,
      maxUrlLength: config?.maxUrlLength ?? BATCH_PIXEL_CONFIG.MAX_URL_LENGTH,
      useCompression: config?.useCompression ?? BATCH_PIXEL_CONFIG.USE_COMPRESSION,
    };
  }

  /**
   * 设置刷新回调
   * 
   * @param callback - 当事件需要刷新时调用的回调函数
   */
  setFlushCallback(callback: (events: EventData[]) => void): void {
    this.flushCallback = callback;
  }

  /**
   * 添加事件到合并队列
   * 
   * @param event - 要添加的事件
   */
  add(event: EventData): void {
    this.queue.push(event);
    
    // 检查是否需要立即发送（达到最大事件数）
    if (this.queue.length >= this.config.maxEvents) {
      this.flush();
      return;
    }

    // 重置合并计时器
    if (this.mergeTimer !== null) {
      clearTimeout(this.mergeTimer);
    }

    this.mergeTimer = window.setTimeout(() => {
      this.mergeTimer = null;
      this.flush();
    }, this.config.mergeWindow);
  }

  /**
   * 立即刷新队列
   */
  flush(): void {
    // 防止重复 flush
    if (this.isFlushing) {
      return;
    }

    if (this.queue.length === 0 || !this.flushCallback) {
      return;
    }

    this.isFlushing = true;

    const events = [...this.queue];
    this.queue = [];

    if (this.mergeTimer !== null) {
      clearTimeout(this.mergeTimer);
      this.mergeTimer = null;
    }

    // 异步执行回调，避免阻塞，执行完成后重置标志
    Promise.resolve().then(() => {
      try {
        this.flushCallback!(events);
      } finally {
        this.isFlushing = false;
      }
    });
  }

  /**
   * 清空队列
   */
  clear(): void {
    this.queue = [];
    if (this.mergeTimer !== null) {
      clearTimeout(this.mergeTimer);
      this.mergeTimer = null;
    }
  }

  /**
   * 获取队列大小
   * 
   * @returns 当前队列中的事件数量
   */
  size(): number {
    return this.queue.length;
  }

  /**
   * 检查队列是否为空
   * 
   * @returns 如果队列为空返回 true，否则返回 false
   */
  isEmpty(): boolean {
    return this.queue.length === 0;
  }
}

/**
 * 压缩事件数据（用于减少URL长度）
 * 
 * @param event - 原始事件数据
 * @returns 压缩后的事件数据
 */
export function compressEvent(event: EventData): CompressedEvent {
  const code = getEventTypeCode(event.event_type);
  const compressed: CompressedEvent = {
    t: code,
  };

  if (event.event_content && Object.keys(event.event_content).length > 0) {
    // 压缩常见字段名
    const compressedContent = compressContent(event.event_content);
    compressed.c = compressedContent;
  }

  return compressed;
}

/**
 * 压缩事件内容字段名（减少字符数）
 * 
 * @param content - 原始事件内容
 * @returns 压缩后的事件内容
 */
function compressContent(content: Record<string, unknown>): Record<string, unknown> {
  // 常见字段名压缩映射表
  const fieldMap: Record<string, string> = {
    element: 'el',
    className: 'cls',
    tagName: 'tg',
    textContent: 'txt',
    selector: 'sel',
    referrer: 'ref',
  };

  const compressed: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(content)) {
    const compressedKey = fieldMap[key] || key;
    compressed[compressedKey] = value;
  }
  return compressed;
}

/**
 * 批量压缩事件数组
 * 
 * @param events - 事件数组
 * @returns 压缩后的事件数组
 */
export function compressBatchEvents(events: EventData[]): CompressedEvent[] {
  return events.map(compressEvent);
}

/**
 * 将压缩后的事件数组编码为Base64（URL-safe）
 * 
 * @param events - 压缩后的事件数组
 * @returns Base64编码的字符串（URL-safe）
 */
export function encodeBatchEvents(events: CompressedEvent[]): string {
  const json = JSON.stringify(events);
  
  // btoa 只能处理 Latin1 字符，对于包含 Unicode 字符的字符串需要先转换为字节
  // 使用 TextEncoder 将 UTF-8 字符串转换为字节数组，然后再 Base64 编码
  let base64: string;
  
  if (typeof TextEncoder !== 'undefined') {
    // 浏览器环境：使用 TextEncoder 处理 Unicode
    const encoder = new TextEncoder();
    const bytes = encoder.encode(json);
    
    // 将字节数组转换为二进制字符串（每个字节作为一个字符）
    let binary = '';
    for (let i = 0; i < bytes.length; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    
    // 使用 btoa 编码二进制字符串
    base64 = btoa(binary);
  } else if (typeof Buffer !== 'undefined') {
    // Node.js 环境：直接使用 Buffer
    base64 = Buffer.from(json, 'utf-8').toString('base64');
  } else {
    // 降级方案：使用 encodeURIComponent + btoa（但这种方法会增加长度）
    // 先用 encodeURIComponent 转义，然后 btoa，但这会增加很多 % 字符
    // 更好的方式是在不支持 TextEncoder 的环境中提示错误
    throw new Error('TextEncoder not available. This SDK requires a modern browser.');
  }
  
  // 转换为 URL-safe Base64（+ -> -, / -> _, 移除末尾填充 =）
  return base64
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

