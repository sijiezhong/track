/**
 * Track SDK 主入口
 * @packageDocumentation
 */

import { Tracker } from './core/tracker';
import { TrackerConfig, UserInfo, EventData } from './types';

/**
 * 全局 Tracker 实例存储（支持多实例）
 */
const trackers: Map<string | number, Tracker> = new Map();

/**
 * 创建 Tracker 实例
 * 
 * @param config - SDK 配置
 * @returns Tracker 实例
 * 
 * @example
 * ```ts
 * const tracker = createTracker({
 *   endpoint: 'https://api.example.com',
 *   projectId: 1,
 *   autoStart: true
 * });
 * ```
 */
export function createTracker(config: TrackerConfig): Tracker {
  const tracker = new Tracker(config);
  trackers.set(config.projectId, tracker);
  return tracker;
}

/**
 * 获取 Tracker 实例
 * 
 * @param projectId - 项目 ID
 * @returns Tracker 实例，如果不存在返回 undefined
 */
export function getTracker(projectId: string | number): Tracker | undefined {
  return trackers.get(projectId);
}

/**
 * 销毁 Tracker 实例
 * 
 * @param projectId - 项目 ID
 */
export function destroyTracker(projectId: string | number): void {
  const tracker = trackers.get(projectId);
  if (tracker) {
    tracker.stop();
    trackers.delete(projectId);
  }
}

/**
 * 便捷方法：快速初始化并返回 Tracker
 * 
 * @param config - SDK 配置
 * @returns Tracker 实例
 * 
 * @example
 * ```ts
 * const tracker = init({
 *   endpoint: 'https://api.example.com',
 *   projectId: 1
 * });
 * 
 * tracker.trackEvent('custom_event', { key: 'value' });
 * ```
 */
export function init(config: TrackerConfig): Tracker {
  return createTracker(config);
}

// 导出类型
export * from './types';

// 导出 Tracker 类（供高级用法）
export { Tracker } from './core/tracker';

// 导出工具函数
export * from './utils';

// 导出常量
export * from './constants';

// 默认导出
export default {
  init,
  createTracker,
  getTracker,
  destroyTracker,
};

