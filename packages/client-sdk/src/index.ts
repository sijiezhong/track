// 导出主类和单例实例
export { Track, track } from "./core/tracker";

// 导出类型定义
export type {
  UserConfig,
  TrackConfig,
  ClickTrackConfig,
  ElementFilterConfig,
  ContentFilterConfig,
  VisibilityFilterConfig,
  BehaviorFilterConfig,
  SamplingConfig,
  ContextFilterConfig,
  EventData,
} from "./types";

// 导出枚举（值导出，不是类型导出）
export { EventType } from "./types";

// 导出预设配置
export {
  defaultClickTrackConfig,
  STRICT_CLICK_TRACK_CONFIG,
  BALANCED_CLICK_TRACK_CONFIG,
  LOOSE_CLICK_TRACK_CONFIG,
  PRESET_CONFIGS,
} from "./types";

// 导出工具函数（可选）
export { getDomPath } from "./utils/dom";
export {
  removeNulls,
  hashCode,
  isBrowser,
  isLocalStorageAvailable,
  getCookie,
  debounce,
  throttle,
} from "./utils";

// 默认导出单例实例
import { track } from "./core/tracker";
export default track;
