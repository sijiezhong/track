/**
 * SDK Tracker 实例管理
 */

import { init, Tracker } from '@track/sdk';
import { SDK_CONFIG } from './config';

/**
 * SDK Tracker 实例
 */
export let tracker: Tracker | undefined;

/**
 * 初始化 SDK（确保只初始化一次）
 */
export function initializeTracker(): Tracker {
  // 检查全局对象中是否已有实例（防止热重载重复初始化）
  if (typeof window !== 'undefined' && (window as any).__trackSDK) {
    tracker = (window as any).__trackSDK;
    console.log('[TrackSDK] Using existing tracker instance from window.__trackSDK');
    return tracker;
  }

  // 检查是否已经初始化
  if (tracker) {
    console.log('[TrackSDK] Tracker already initialized, reusing existing instance');
    return tracker;
  }

  console.log('[TrackSDK] Initializing new tracker instance');

  tracker = init({
    endpoint: SDK_CONFIG.endpoint,
    projectId: SDK_CONFIG.projectId,
    autoStart: SDK_CONFIG.autoStart,
    debug: SDK_CONFIG.debug,
    batchSize: SDK_CONFIG.batchSize,
    batchTimeout: SDK_CONFIG.batchTimeout,
    collectors: {
      pageview: true,
      click: true,
      performance: false,
      error: true,
    },
  });

  // 将 tracker 挂载到全局对象，方便在浏览器控制台中使用
  // 同时用于防止热重载时重复初始化
  if (typeof window !== 'undefined') {
    (window as any).__trackSDK = tracker;
  }

  return tracker;
}

/**
 * 获取 Tracker 实例
 */
export function getTracker(): Tracker {
  if (!tracker) {
    return initializeTracker();
  }
  return tracker;
}

