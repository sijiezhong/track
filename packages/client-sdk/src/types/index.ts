/**
 * 事件类型枚举
 * 注意：此枚举值与服务端必须完全一致，修改时需同步更新服务端
 */
export enum EventType {
  PAGE_VIEW = 1, // 页面浏览
  CLICK = 2, // 点击事件
  PERFORMANCE = 3, // 性能指标
  ERROR = 4, // 错误监控
  CUSTOM = 5, // 自定义事件
  PAGE_STAY = 6, // 页面停留
}

/**
 * 用户配置
 */
export interface UserConfig {
  appId: string;
  userId: string;
  userProps?: Record<string, any>;
}

/**
 * 元素过滤配置
 */
export interface ElementFilterConfig {
  /** 排除的标签名 */
  excludeTags: string[];
  /** 排除的选择器 */
  excludeSelectors: string[];
  /** 仅采集的选择器（白名单模式） */
  includeSelectors: string[];
  /** 必须的属性（至少满足一个） */
  requireAttributes: string[];
  /** 排除的属性 */
  excludeAttributes: string[];
}

/**
 * 内容过滤配置
 */
export interface ContentFilterConfig {
  /** 排除无文本内容，默认 false */
  excludeEmptyText: boolean;
  /** 最小文本长度，默认 0 */
  minTextLength: number;
  /** 要求有 aria-label 或 title，默认 false */
  requireAriaLabel: boolean;
}

/**
 * 可见性过滤配置
 */
export interface VisibilityFilterConfig {
  /** 排除隐藏元素，默认 true */
  excludeHidden: boolean;
  /** 排除视口外元素，默认 false */
  excludeOutOfViewport: boolean;
  /** 最小尺寸 */
  minSize: { width: number; height: number };
}

/**
 * 行为过滤配置
 */
export interface BehaviorFilterConfig {
  /** 最小点击间隔（毫秒），默认 100 */
  minClickInterval: number;
  /** 每秒最大点击次数，默认 10 */
  maxClicksPerSecond: number;
  /** 同一元素最大连续点击次数，默认 3 */
  maxConsecutiveClicks: number;
  /** 验证点击位置，默认 true */
  validateClickPosition: boolean;
}

/**
 * 采样配置
 */
export interface SamplingConfig {
  /** 是否启用采样，默认 false */
  enabled: boolean;
  /** 采样率（0-1），默认 1.0 */
  sampleRate: number;
  /** 采样策略 */
  strategy: "random" | "consistent";
  /** 按元素类型采样率 */
  elementTypeRates: Partial<Record<string, number>>;
}

/**
 * 上下文过滤配置
 */
export interface ContextFilterConfig {
  /** 排除页眉页脚，默认 false */
  excludeHeaderFooter: boolean;
  /** 排除侧边栏，默认 false */
  excludeSidebar: boolean;
  /** 仅采集首屏，默认 false */
  onlyFirstScreen: boolean;
}

/**
 * 点击采集配置
 */
export interface ClickTrackConfig {
  /** 是否启用点击采集，默认 true */
  enabled: boolean;
  /** 元素过滤配置 */
  elementFilter: ElementFilterConfig;
  /** 内容过滤配置 */
  contentFilter: ContentFilterConfig;
  /** 可见性过滤配置 */
  visibilityFilter: VisibilityFilterConfig;
  /** 行为过滤配置 */
  behaviorFilter: BehaviorFilterConfig;
  /** 采样配置 */
  sampling: SamplingConfig;
  /** 上下文过滤配置 */
  contextFilter: ContextFilterConfig;
  /** 是否输出调试信息，默认 false */
  debug: boolean;
}

/**
 * 追踪配置
 */
export interface TrackConfig {
  /** 服务端端点地址 */
  endpoint: string;
  /** Session 存活时长（分钟），默认 1440（24小时），0 表示不过期 */
  sessionTTL?: number;
  /** 是否启用自动采集，默认 true */
  autoTrack?: boolean;
  /** 是否启用性能监控，默认 false */
  performance?: boolean;
  /** 是否启用错误监控，默认 false */
  errorTrack?: boolean;
  /** 批量上报大小，默认 10 */
  batchSize?: number;
  /** 批量上报等待时间（毫秒），默认 5000 */
  batchWait?: number;
  /** 是否启用调试模式，默认 false */
  debug?: boolean;
  /** 点击采集配置 */
  clickTrack?: Partial<ClickTrackConfig> | false;
}

/**
 * 事件数据
 */
export interface EventData {
  /** 事件类型 */
  type: EventType;
  /** 自定义事件唯一标识符 */
  eventId?: string;
  /** 事件属性 */
  properties: Record<string, any>;
}

/**
 * 默认点击采集配置
 */
export const defaultClickTrackConfig: ClickTrackConfig = {
  enabled: true,

  elementFilter: {
    excludeTags: ["script", "style", "meta", "link", "noscript"],
    excludeSelectors: [
      "[data-no-track]",
      "[data-track-ignore]",
      ".track-ignore",
      ".logo",
      ".icon",
      ".decoration",
    ],
    includeSelectors: [], // 空数组表示不启用白名单模式
    requireAttributes: [], // 空数组表示不强制要求属性
    excludeAttributes: ["disabled", "aria-hidden"],
  },

  contentFilter: {
    excludeEmptyText: false, // 不过滤空文本，因为可能有图标按钮
    minTextLength: 0,
    requireAriaLabel: false,
  },

  visibilityFilter: {
    excludeHidden: true,
    excludeOutOfViewport: false,
    minSize: { width: 10, height: 10 },
  },

  behaviorFilter: {
    minClickInterval: 100,
    maxClicksPerSecond: 10,
    maxConsecutiveClicks: 3,
    validateClickPosition: true,
  },

  sampling: {
    enabled: false,
    sampleRate: 1.0,
    strategy: "consistent",
    elementTypeRates: {},
  },

  contextFilter: {
    excludeHeaderFooter: false,
    excludeSidebar: false,
    onlyFirstScreen: false,
  },

  debug: false,
};

/**
 * 严格模式配置（高质量数据）
 */
export const STRICT_CLICK_TRACK_CONFIG: ClickTrackConfig = {
  enabled: true,
  elementFilter: {
    excludeTags: [
      "script",
      "style",
      "meta",
      "link",
      "noscript",
      "svg",
      "path",
      "img",
    ],
    excludeSelectors: [
      "[data-no-track]",
      ".logo",
      ".icon",
      ".decoration",
      ".background",
      "::-webkit-scrollbar",
    ],
    includeSelectors: [
      "button",
      "a[href]",
      '[role="button"]',
      "[data-track]",
      "[data-track-click]",
    ],
    requireAttributes: ["onclick", "href", "role", "data-track"],
    excludeAttributes: ["disabled", "aria-hidden", "data-testid"],
  },
  contentFilter: {
    excludeEmptyText: true,
    minTextLength: 1,
    requireAriaLabel: true,
  },
  visibilityFilter: {
    excludeHidden: true,
    excludeOutOfViewport: true,
    minSize: { width: 20, height: 20 },
  },
  behaviorFilter: {
    minClickInterval: 200,
    maxClicksPerSecond: 5,
    maxConsecutiveClicks: 2,
    validateClickPosition: true,
  },
  sampling: {
    enabled: true,
    sampleRate: 0.8,
    strategy: "consistent",
    elementTypeRates: {
      button: 1.0,
      a: 1.0,
      div: 0.5,
      span: 0.3,
    },
  },
  contextFilter: {
    excludeHeaderFooter: true,
    excludeSidebar: true,
    onlyFirstScreen: false,
  },
  debug: false,
};

/**
 * 平衡模式配置（推荐）
 */
export const BALANCED_CLICK_TRACK_CONFIG: ClickTrackConfig = {
  enabled: true,
  elementFilter: {
    excludeTags: ["script", "style", "meta", "link"],
    excludeSelectors: ["[data-no-track]", ".track-ignore"],
    includeSelectors: [],
    requireAttributes: [],
    excludeAttributes: ["disabled"],
  },
  contentFilter: {
    excludeEmptyText: false,
    minTextLength: 0,
    requireAriaLabel: false,
  },
  visibilityFilter: {
    excludeHidden: true,
    excludeOutOfViewport: false,
    minSize: { width: 10, height: 10 },
  },
  behaviorFilter: {
    minClickInterval: 100,
    maxClicksPerSecond: 10,
    maxConsecutiveClicks: 3,
    validateClickPosition: true,
  },
  sampling: {
    enabled: false,
    sampleRate: 1.0,
    strategy: "consistent",
    elementTypeRates: {},
  },
  contextFilter: {
    excludeHeaderFooter: false,
    excludeSidebar: false,
    onlyFirstScreen: false,
  },
  debug: false,
};

/**
 * 宽松模式配置（全量采集）
 */
export const LOOSE_CLICK_TRACK_CONFIG: ClickTrackConfig = {
  enabled: true,
  elementFilter: {
    excludeTags: ["script", "style"],
    excludeSelectors: ["[data-no-track]"],
    includeSelectors: [],
    requireAttributes: [],
    excludeAttributes: [],
  },
  contentFilter: {
    excludeEmptyText: false,
    minTextLength: 0,
    requireAriaLabel: false,
  },
  visibilityFilter: {
    excludeHidden: true,
    excludeOutOfViewport: false,
    minSize: { width: 5, height: 5 },
  },
  behaviorFilter: {
    minClickInterval: 50,
    maxClicksPerSecond: 20,
    maxConsecutiveClicks: 10,
    validateClickPosition: false,
  },
  sampling: {
    enabled: false,
    sampleRate: 1.0,
    strategy: "consistent",
    elementTypeRates: {},
  },
  contextFilter: {
    excludeHeaderFooter: false,
    excludeSidebar: false,
    onlyFirstScreen: false,
  },
  debug: false,
};

/**
 * 预设配置
 */
export const PRESET_CONFIGS = {
  CLICK_TRACK_STRICT: STRICT_CLICK_TRACK_CONFIG,
  CLICK_TRACK_BALANCED: BALANCED_CLICK_TRACK_CONFIG,
  CLICK_TRACK_LOOSE: LOOSE_CLICK_TRACK_CONFIG,
} as const;
