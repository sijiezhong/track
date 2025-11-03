/**
 * 采集器类型定义
 * @packageDocumentation
 */

import { Tracker } from '../core/tracker';

/**
 * 采集器接口
 * 
 * @remarks
 * 所有采集器必须实现此接口
 */
export interface Collector {
  /**
   * 启动采集器
   * 
   * @param tracker - Tracker 实例
   */
  start(tracker: Tracker): void;

  /**
   * 停止采集器
   */
  stop(): void;

  /**
   * 获取采集器名称
   * 
   * @returns 采集器名称
   */
  getName(): string;
}

