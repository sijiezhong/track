# 用户点击采集策略设计文档

## 1. 背景与目标

### 1.1 问题背景

在实际业务场景中，用户的一次页面访问可能产生大量的点击事件，但并非所有点击都具有分析价值。例如：

- **装饰性元素点击**：空白区域、图标、装饰性按钮等
- **系统级交互**：滚动条、浏览器控件、开发者工具等
- **无效点击**：误触、快速连续点击、防抖未生效的重复点击
- **隐私敏感区域**：密码输入框、敏感信息区域等

采集所有点击事件会导致：
- 数据量过大，增加存储和传输成本
- 分析结果噪音过多，影响数据质量
- 用户隐私担忧，可能包含敏感交互信息

### 1.2 设计目标

1. **提高数据质量**：只采集有价值的用户交互行为
2. **降低数据成本**：减少无效数据的存储和传输
3. **保护用户隐私**：避免采集敏感交互信息
4. **灵活可配置**：支持不同业务场景的个性化配置
5. **性能优化**：减少事件处理开销，提升 SDK 性能

## 2. 点击价值评估模型

### 2.1 有价值点击的特征

#### 2.1.1 业务价值维度

**高价值点击**：
- 功能性按钮：提交、购买、登录、注册等
- 导航链接：菜单项、面包屑、侧边栏等
- 内容交互：展开/收起、点赞、收藏、分享等
- 表单元素：输入框聚焦、选择器切换等
- 列表项选择：商品卡片、文章卡片、搜索结果等

**中价值点击**：
- 辅助性操作：帮助按钮、提示气泡、设置按钮等
- 次要导航：页脚链接、相关推荐等

**低价值点击**：
- 装饰性元素：Logo、图标、背景图片等
- 空白区域：页面留白、分隔线等
- 系统元素：滚动条、浏览器控件等

#### 2.1.2 用户意图维度

**明确意图**：
- 用户主动点击，有明确的操作目标
- 点击后通常有页面变化或状态变化
- 点击行为符合用户操作习惯

**模糊意图**：
- 误触、快速划过导致的意外点击
- 无明确操作目标的探索性点击

**无意图**：
- 系统自动触发的点击
- 装饰性元素的视觉性点击

### 2.2 点击分类体系

```
点击事件
├── 业务点击（高价值）
│   ├── 转化类：提交、购买、支付等
│   ├── 导航类：菜单、链接、面包屑等
│   └── 交互类：展开、收起、切换等
├── 辅助点击（中价值）
│   ├── 帮助类：帮助按钮、提示等
│   └── 设置类：设置、偏好等
└── 无效点击（低价值/无价值）
    ├── 装饰性：Logo、图标、背景等
    ├── 系统级：滚动条、浏览器控件等
    ├── 误触：快速连续点击、意外点击等
    └── 敏感区域：密码输入、隐私区域等
```

## 3. 策略规则设计

### 3.1 元素过滤策略

#### 3.1.1 标签过滤

**排除标签**（默认不采集）：
- `script`、`style`、`meta`、`link`：页面元数据，用户通常不会点击
- `noscript`：无脚本环境下的内容
- `svg`、`path`：通常作为图标，除非是主要交互元素
- `img`：图片，除非是可点击的按钮或链接

**条件采集标签**（根据上下文判断）：
- `div`、`span`：需要判断是否有交互属性（如 `onclick`、`role`、`tabindex`）
- `a`：需要判断是否有有效的 `href` 属性
- `button`：需要判断是否禁用（`disabled`）

#### 3.1.2 选择器过滤

**排除选择器**：
```typescript
const defaultExcludeSelectors = [
  // 明确标记不采集的元素
  '[data-no-track]',
  '[data-track-ignore]',
  '.track-ignore',
  
  // 系统级元素
  '::-webkit-scrollbar',
  '::-moz-scrollbar',
  
  // 装饰性元素
  '.logo',
  '.icon',
  '.decoration',
  '.background',
  
  // 开发者工具
  '[data-testid*="test"]',  // 测试元素
];
```

**仅采集选择器**（白名单模式）：
```typescript
const includeSelectors = [
  // 功能性按钮
  'button[type="submit"]',
  'button[type="button"]',
  'a[href]',
  
  // 表单元素
  'input[type="button"]',
  'input[type="submit"]',
  'select',
  
  // 标记为可追踪的元素
  '[data-track]',
  '[data-track-click]',
];
```

#### 3.1.3 属性过滤

**必须属性**（至少满足一个）：
- `onclick`：有点击事件处理器
- `href`：链接元素有目标地址
- `role="button"`：明确标记为按钮
- `tabindex >= 0`：可键盘聚焦
- `data-track`：明确标记为可追踪
- `class` 包含交互相关关键词：`btn`、`button`、`link`、`click` 等

**排除属性**：
- `disabled`：禁用状态
- `aria-hidden="true"`：对屏幕阅读器隐藏
- `data-no-track`：明确标记不追踪
- `data-testid`：测试标识（可选配置）

### 3.2 内容过滤策略

#### 3.2.1 文本内容过滤

**空内容过滤**：
- 元素无文本内容（`innerText`、`textContent` 为空）
- 元素无 `aria-label`、`title` 等可访问性文本
- 元素无 `id`、`class` 等标识符

**最小文本长度**：
- 对于纯文本点击，文本长度应大于等于 1 个字符
- 对于图标按钮，应有 `aria-label` 或 `title` 属性

#### 3.2.2 可见性过滤

**不可见元素排除**：
- `display: none`
- `visibility: hidden`
- `opacity: 0`（完全透明）
- `width: 0` 或 `height: 0`（尺寸为 0）
- 被其他元素完全遮挡（通过 `getBoundingClientRect()` 判断）

**视口外元素**：
- 元素不在可视区域内（`getBoundingClientRect()` 判断）

### 3.3 行为过滤策略

#### 3.3.1 时间间隔过滤

**防抖策略**：
```typescript
interface ClickThrottleConfig {
  minInterval: number;  // 最小点击间隔（毫秒），默认 100ms
  maxClicksPerSecond: number;  // 每秒最大点击次数，默认 10 次
}
```

**实现逻辑**：
1. 记录上次点击时间戳
2. 如果距离上次点击时间 < `minInterval`，忽略本次点击
3. 维护最近 1 秒的点击次数，超过 `maxClicksPerSecond` 则忽略

#### 3.2.2 点击区域过滤

**最小点击区域**：
- 元素的可点击区域（`getBoundingClientRect()`）应大于最小阈值
- 默认最小区域：`10px × 10px`
- 避免误触过小的元素

**点击位置验证**：
- 验证点击坐标是否在元素的实际区域内
- 排除点击在元素边缘或外部的情况

### 3.4 上下文过滤策略

#### 3.4.1 页面位置过滤

**首屏优先**：
- 优先采集首屏（视口内）的点击
- 可配置是否采集滚动后的点击

**页面区域过滤**：
- 排除页眉、页脚的装饰性元素（可配置）
- 排除侧边栏的辅助性元素（可配置）

#### 3.4.2 交互链分析

**连续点击模式**：
- 如果用户在短时间内连续点击同一元素，可能是误触或加载问题
- 可配置：连续点击同一元素超过 N 次（如 3 次）后，只记录第一次

**点击序列分析**：
- 分析点击序列的合理性
- 例如：短时间内连续点击多个不相关元素，可能是误触

### 3.5 采样策略

#### 3.5.1 全局采样

**采样率配置**：
```typescript
interface SamplingConfig {
  enabled: boolean;  // 是否启用采样
  sampleRate: number;  // 采样率（0-1），默认 1.0（100%采集）
  strategy: 'random' | 'consistent';  // 采样策略
}
```

**随机采样**：
- 每个点击按 `sampleRate` 概率决定是否采集
- 优点：简单快速
- 缺点：可能丢失重要事件

**一致性采样**：
- 基于 `userId` 或 `sessionId` 的哈希值决定是否采集
- 同一用户在同一会话中的采样结果一致
- 优点：保证用户行为的一致性
- 缺点：实现稍复杂

#### 3.5.2 分层采样

**按元素类型采样**：
```typescript
interface ElementTypeSampling {
  button: number;      // 按钮采样率，默认 1.0
  link: number;        // 链接采样率，默认 1.0
  div: number;         // div 采样率，默认 0.5
  span: number;        // span 采样率，默认 0.3
  other: number;       // 其他元素采样率，默认 0.1
}
```

**按页面区域采样**：
- 首屏区域：100% 采集
- 主要内容区：80% 采集
- 页眉页脚：50% 采集
- 侧边栏：30% 采集

## 4. 配置方案设计

### 4.1 配置结构

```typescript
interface ClickTrackConfig {
  // 基础配置
  enabled: boolean;  // 是否启用点击采集，默认 true
  
  // 元素过滤配置
  elementFilter: {
    excludeTags: string[];  // 排除的标签名
    excludeSelectors: string[];  // 排除的选择器
    includeSelectors: string[];  // 仅采集的选择器（白名单模式）
    requireAttributes: string[];  // 必须的属性（至少满足一个）
    excludeAttributes: string[];  // 排除的属性
  };
  
  // 内容过滤配置
  contentFilter: {
    excludeEmptyText: boolean;  // 排除无文本内容，默认 false
    minTextLength: number;  // 最小文本长度，默认 0
    requireAriaLabel: boolean;  // 要求有 aria-label 或 title，默认 false
  };
  
  // 可见性过滤配置
  visibilityFilter: {
    excludeHidden: boolean;  // 排除隐藏元素，默认 true
    excludeOutOfViewport: boolean;  // 排除视口外元素，默认 false
    minSize: { width: number; height: number };  // 最小尺寸，默认 { width: 10, height: 10 }
  };
  
  // 行为过滤配置
  behaviorFilter: {
    minClickInterval: number;  // 最小点击间隔（毫秒），默认 100
    maxClicksPerSecond: number;  // 每秒最大点击次数，默认 10
    maxConsecutiveClicks: number;  // 同一元素最大连续点击次数，默认 3
    validateClickPosition: boolean;  // 验证点击位置，默认 true
  };
  
  // 采样配置
  sampling: {
    enabled: boolean;  // 是否启用采样，默认 false
    sampleRate: number;  // 采样率（0-1），默认 1.0
    strategy: 'random' | 'consistent';  // 采样策略，默认 'consistent'
    elementTypeRates: Partial<Record<string, number>>;  // 按元素类型采样率
  };
  
  // 上下文过滤配置
  contextFilter: {
    excludeHeaderFooter: boolean;  // 排除页眉页脚，默认 false
    excludeSidebar: boolean;  // 排除侧边栏，默认 false
    onlyFirstScreen: boolean;  // 仅采集首屏，默认 false
  };
  
  // 调试配置
  debug: boolean;  // 是否输出调试信息，默认 false
}
```

### 4.2 默认配置

```typescript
const defaultClickTrackConfig: ClickTrackConfig = {
  enabled: true,
  
  elementFilter: {
    excludeTags: ['script', 'style', 'meta', 'link', 'noscript'],
    excludeSelectors: [
      '[data-no-track]',
      '[data-track-ignore]',
      '.track-ignore',
      '.logo',
      '.icon',
      '.decoration'
    ],
    includeSelectors: [],  // 空数组表示不启用白名单模式
    requireAttributes: [],  // 空数组表示不强制要求属性
    excludeAttributes: ['disabled', 'aria-hidden']
  },
  
  contentFilter: {
    excludeEmptyText: false,  // 不过滤空文本，因为可能有图标按钮
    minTextLength: 0,
    requireAriaLabel: false
  },
  
  visibilityFilter: {
    excludeHidden: true,
    excludeOutOfViewport: false,
    minSize: { width: 10, height: 10 }
  },
  
  behaviorFilter: {
    minClickInterval: 100,
    maxClicksPerSecond: 10,
    maxConsecutiveClicks: 3,
    validateClickPosition: true
  },
  
  sampling: {
    enabled: false,
    sampleRate: 1.0,
    strategy: 'consistent',
    elementTypeRates: {}
  },
  
  contextFilter: {
    excludeHeaderFooter: false,
    excludeSidebar: false,
    onlyFirstScreen: false
  },
  
  debug: false
};
```

### 4.3 预设配置方案

#### 4.3.1 严格模式（高质量数据）

```typescript
const strictConfig: ClickTrackConfig = {
  enabled: true,
  elementFilter: {
    excludeTags: ['script', 'style', 'meta', 'link', 'noscript', 'svg', 'path', 'img'],
    excludeSelectors: [
      '[data-no-track]',
      '.logo', '.icon', '.decoration', '.background',
      '::-webkit-scrollbar'
    ],
    includeSelectors: [
      'button', 'a[href]', '[role="button"]',
      '[data-track]', '[data-track-click]'
    ],
    requireAttributes: ['onclick', 'href', 'role', 'data-track'],
    excludeAttributes: ['disabled', 'aria-hidden', 'data-testid']
  },
  contentFilter: {
    excludeEmptyText: true,
    minTextLength: 1,
    requireAriaLabel: true
  },
  visibilityFilter: {
    excludeHidden: true,
    excludeOutOfViewport: true,
    minSize: { width: 20, height: 20 }
  },
  behaviorFilter: {
    minClickInterval: 200,
    maxClicksPerSecond: 5,
    maxConsecutiveClicks: 2,
    validateClickPosition: true
  },
  sampling: {
    enabled: true,
    sampleRate: 0.8,
    strategy: 'consistent',
    elementTypeRates: {
      button: 1.0,
      a: 1.0,
      div: 0.5,
      span: 0.3
    }
  },
  contextFilter: {
    excludeHeaderFooter: true,
    excludeSidebar: true,
    onlyFirstScreen: false
  }
};
```

#### 4.3.2 平衡模式（推荐）

```typescript
const balancedConfig: ClickTrackConfig = {
  enabled: true,
  elementFilter: {
    excludeTags: ['script', 'style', 'meta', 'link'],
    excludeSelectors: ['[data-no-track]', '.track-ignore'],
    includeSelectors: [],
    requireAttributes: [],
    excludeAttributes: ['disabled']
  },
  contentFilter: {
    excludeEmptyText: false,
    minTextLength: 0,
    requireAriaLabel: false
  },
  visibilityFilter: {
    excludeHidden: true,
    excludeOutOfViewport: false,
    minSize: { width: 10, height: 10 }
  },
  behaviorFilter: {
    minClickInterval: 100,
    maxClicksPerSecond: 10,
    maxConsecutiveClicks: 3,
    validateClickPosition: true
  },
  sampling: {
    enabled: false,
    sampleRate: 1.0,
    strategy: 'consistent',
    elementTypeRates: {}
  },
  contextFilter: {
    excludeHeaderFooter: false,
    excludeSidebar: false,
    onlyFirstScreen: false
  }
};
```

#### 4.3.3 宽松模式（全量采集）

```typescript
const looseConfig: ClickTrackConfig = {
  enabled: true,
  elementFilter: {
    excludeTags: ['script', 'style'],
    excludeSelectors: ['[data-no-track]'],
    includeSelectors: [],
    requireAttributes: [],
    excludeAttributes: []
  },
  contentFilter: {
    excludeEmptyText: false,
    minTextLength: 0,
    requireAriaLabel: false
  },
  visibilityFilter: {
    excludeHidden: true,
    excludeOutOfViewport: false,
    minSize: { width: 5, height: 5 }
  },
  behaviorFilter: {
    minClickInterval: 50,
    maxClicksPerSecond: 20,
    maxConsecutiveClicks: 10,
    validateClickPosition: false
  },
  sampling: {
    enabled: false,
    sampleRate: 1.0,
    strategy: 'consistent',
    elementTypeRates: {}
  },
  contextFilter: {
    excludeHeaderFooter: false,
    excludeSidebar: false,
    onlyFirstScreen: false
  }
};
```

## 5. 实现方案

### 5.1 策略执行流程

```
用户点击事件
    ↓
[1] 基础检查
    - 是否启用点击采集？
    - 元素是否在排除列表中？
    ↓
[2] 元素过滤
    - 标签过滤
    - 选择器过滤
    - 属性过滤
    ↓
[3] 内容过滤
    - 文本内容检查
    - 可访问性文本检查
    ↓
[4] 可见性过滤
    - 元素是否可见？
    - 元素是否在视口内？
    - 元素尺寸是否满足要求？
    ↓
[5] 行为过滤
    - 点击间隔检查
    - 点击频率检查
    - 连续点击检查
    - 点击位置验证
    ↓
[6] 上下文过滤
    - 页面位置检查
    - 交互链分析
    ↓
[7] 采样决策
    - 是否通过采样？
    ↓
[8] 采集事件
    - 构建事件数据
    - 添加到上报队列
```

### 5.2 核心实现代码

```typescript
class ClickTracker {
  private config: ClickTrackConfig;
  private clickHistory: Map<string, number[]> = new Map();  // 点击历史记录
  private lastClickTime: number = 0;
  private lastClickElement: HTMLElement | null = null;
  private consecutiveClickCount: number = 0;
  
  constructor(config: ClickTrackConfig) {
    this.config = { ...defaultClickTrackConfig, ...config };
  }
  
  /**
   * 处理点击事件
   */
  handleClick(event: MouseEvent): boolean {
    const target = event.target as HTMLElement;
    
    // [1] 基础检查
    if (!this.config.enabled) {
      return false;
    }
    
    // [2-7] 执行过滤策略
    if (!this.shouldTrackClick(target, event)) {
      return false;
    }
    
    // [8] 采集事件
    this.trackClick(target, event);
    return true;
  }
  
  /**
   * 判断是否应该采集该点击
   */
  private shouldTrackClick(element: HTMLElement, event: MouseEvent): boolean {
    // [2] 元素过滤
    if (!this.passElementFilter(element)) {
      if (this.config.debug) {
        console.log('[ClickTracker] Filtered by element filter:', element);
      }
      return false;
    }
    
    // [3] 内容过滤
    if (!this.passContentFilter(element)) {
      if (this.config.debug) {
        console.log('[ClickTracker] Filtered by content filter:', element);
      }
      return false;
    }
    
    // [4] 可见性过滤
    if (!this.passVisibilityFilter(element)) {
      if (this.config.debug) {
        console.log('[ClickTracker] Filtered by visibility filter:', element);
      }
      return false;
    }
    
    // [5] 行为过滤
    if (!this.passBehaviorFilter(element, event)) {
      if (this.config.debug) {
        console.log('[ClickTracker] Filtered by behavior filter:', element);
      }
      return false;
    }
    
    // [6] 上下文过滤
    if (!this.passContextFilter(element)) {
      if (this.config.debug) {
        console.log('[ClickTracker] Filtered by context filter:', element);
      }
      return false;
    }
    
    // [7] 采样决策
    if (!this.passSampling(element)) {
      if (this.config.debug) {
        console.log('[ClickTracker] Filtered by sampling:', element);
      }
      return false;
    }
    
    return true;
  }
  
  /**
   * [2] 元素过滤
   */
  private passElementFilter(element: HTMLElement): boolean {
    const filter = this.config.elementFilter;
    
    // 排除标签
    if (filter.excludeTags.includes(element.tagName.toLowerCase())) {
      return false;
    }
    
    // 排除选择器
    for (const selector of filter.excludeSelectors) {
      try {
        if (element.matches(selector) || element.closest(selector)) {
          return false;
        }
      } catch (e) {
        // 选择器语法错误，忽略
      }
    }
    
    // 白名单模式（如果配置了白名单）
    if (filter.includeSelectors.length > 0) {
      const matches = filter.includeSelectors.some(selector => {
        try {
          return element.matches(selector) || element.closest(selector);
        } catch (e) {
          return false;
        }
      });
      if (!matches) {
        return false;
      }
    }
    
    // 必须属性检查
    if (filter.requireAttributes.length > 0) {
      const hasRequiredAttr = filter.requireAttributes.some(attr => {
        if (attr === 'onclick') return element.onclick !== null;
        if (attr === 'href') return element.hasAttribute('href');
        return element.hasAttribute(attr);
      });
      if (!hasRequiredAttr) {
        return false;
      }
    }
    
    // 排除属性检查
    for (const attr of filter.excludeAttributes) {
      if (attr === 'disabled' && (element as any).disabled) {
        return false;
      }
      if (element.getAttribute(attr) !== null) {
        return false;
      }
    }
    
    return true;
  }
  
  /**
   * [3] 内容过滤
   */
  private passContentFilter(element: HTMLElement): boolean {
    const filter = this.config.contentFilter;
    
    if (filter.excludeEmptyText) {
      const text = element.innerText?.trim() || element.textContent?.trim() || '';
      const ariaLabel = element.getAttribute('aria-label') || element.getAttribute('title') || '';
      const hasId = element.id;
      const hasClass = element.className;
      
      if (!text && !ariaLabel && !hasId && !hasClass) {
        return false;
      }
    }
    
    if (filter.minTextLength > 0) {
      const text = element.innerText?.trim() || element.textContent?.trim() || '';
      if (text.length < filter.minTextLength) {
        return false;
      }
    }
    
    if (filter.requireAriaLabel) {
      const ariaLabel = element.getAttribute('aria-label') || element.getAttribute('title');
      if (!ariaLabel) {
        return false;
      }
    }
    
    return true;
  }
  
  /**
   * [4] 可见性过滤
   */
  private passVisibilityFilter(element: HTMLElement): boolean {
    const filter = this.config.visibilityFilter;
    
    if (filter.excludeHidden) {
      const style = window.getComputedStyle(element);
      if (style.display === 'none' || 
          style.visibility === 'hidden' || 
          parseFloat(style.opacity) === 0) {
        return false;
      }
      
      const rect = element.getBoundingClientRect();
      if (rect.width === 0 || rect.height === 0) {
        return false;
      }
    }
    
    if (filter.excludeOutOfViewport) {
      const rect = element.getBoundingClientRect();
      const viewport = {
        width: window.innerWidth,
        height: window.innerHeight
      };
      
      if (rect.right < 0 || rect.left > viewport.width ||
          rect.bottom < 0 || rect.top > viewport.height) {
        return false;
      }
    }
    
    const minSize = filter.minSize;
    if (minSize.width > 0 || minSize.height > 0) {
      const rect = element.getBoundingClientRect();
      if (rect.width < minSize.width || rect.height < minSize.height) {
        return false;
      }
    }
    
    return true;
  }
  
  /**
   * [5] 行为过滤
   */
  private passBehaviorFilter(element: HTMLElement, event: MouseEvent): boolean {
    const filter = this.config.behaviorFilter;
    const now = Date.now();
    
    // 点击间隔检查
    if (now - this.lastClickTime < filter.minClickInterval) {
      return false;
    }
    
    // 点击频率检查
    const elementKey = this.getElementKey(element);
    const clickHistory = this.clickHistory.get(elementKey) || [];
    const recentClicks = clickHistory.filter(time => now - time < 1000);
    if (recentClicks.length >= filter.maxClicksPerSecond) {
      return false;
    }
    
    // 连续点击检查
    if (element === this.lastClickElement) {
      this.consecutiveClickCount++;
      if (this.consecutiveClickCount > filter.maxConsecutiveClicks) {
        return false;
      }
    } else {
      this.consecutiveClickCount = 1;
      this.lastClickElement = element;
    }
    
    // 点击位置验证
    if (filter.validateClickPosition) {
      const rect = element.getBoundingClientRect();
      const clickX = event.clientX;
      const clickY = event.clientY;
      
      if (clickX < rect.left || clickX > rect.right ||
          clickY < rect.top || clickY > rect.bottom) {
        return false;
      }
    }
    
    // 更新历史记录
    this.lastClickTime = now;
    clickHistory.push(now);
    // 只保留最近 5 秒的记录
    this.clickHistory.set(elementKey, clickHistory.filter(time => now - time < 5000));
    
    return true;
  }
  
  /**
   * [6] 上下文过滤
   */
  private passContextFilter(element: HTMLElement): boolean {
    const filter = this.config.contextFilter;
    
    if (filter.onlyFirstScreen) {
      const rect = element.getBoundingClientRect();
      if (rect.top > window.innerHeight) {
        return false;
      }
    }
    
    // 页眉页脚过滤（简化实现，实际需要更复杂的判断）
    if (filter.excludeHeaderFooter) {
      const rect = element.getBoundingClientRect();
      const viewportHeight = window.innerHeight;
      // 假设页眉在顶部 100px，页脚在底部 100px
      if (rect.top < 100 || rect.bottom > viewportHeight - 100) {
        return false;
      }
    }
    
    return true;
  }
  
  /**
   * [7] 采样决策
   */
  private passSampling(element: HTMLElement): boolean {
    const sampling = this.config.sampling;
    
    if (!sampling.enabled) {
      return true;
    }
    
    // 按元素类型采样
    const tagName = element.tagName.toLowerCase();
    const elementTypeRate = sampling.elementTypeRates[tagName];
    if (elementTypeRate !== undefined) {
      return Math.random() < elementTypeRate;
    }
    
    // 全局采样
    if (sampling.strategy === 'random') {
      return Math.random() < sampling.sampleRate;
    } else if (sampling.strategy === 'consistent') {
      // 基于用户 ID 的一致性采样
      const userId = this.getUserId();
      const hash = this.hashCode(userId + element.tagName);
      return (hash % 100) < (sampling.sampleRate * 100);
    }
    
    return true;
  }
  
  /**
   * 获取元素唯一标识
   */
  private getElementKey(element: HTMLElement): string {
    return `${element.tagName}-${element.id || element.className || 'unknown'}`;
  }
  
  /**
   * 获取用户 ID（用于一致性采样）
   */
  private getUserId(): string {
    // 从 SDK 获取用户 ID
    return 'default-user-id';
  }
  
  /**
   * 简单的哈希函数
   */
  private hashCode(str: string): number {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32bit integer
    }
    return Math.abs(hash);
  }
  
  /**
   * [8] 采集点击事件
   */
  private trackClick(element: HTMLElement, event: MouseEvent): void {
    // 构建事件数据并上报
    // ...
  }
}
```

## 6. 使用示例

### 6.1 基础使用

```typescript
import { init, start } from '@track/sdk';

// 使用默认配置
await init({
  appId: 'your-app-id',
  userId: 'user-123'
}, {
  endpoint: 'https://track.yourdomain.com',
  clickTrack: {
    enabled: true
  }
});

start();
```

### 6.2 自定义配置

```typescript
await init({
  appId: 'your-app-id',
  userId: 'user-123'
}, {
  endpoint: 'https://track.yourdomain.com',
  clickTrack: {
    enabled: true,
    elementFilter: {
      excludeTags: ['script', 'style', 'meta', 'link', 'svg'],
      excludeSelectors: [
        '[data-no-track]',
        '.logo',
        '.icon',
        '.decoration'
      ],
      includeSelectors: [
        'button',
        'a[href]',
        '[role="button"]',
        '[data-track]'
      ],
      requireAttributes: [],
      excludeAttributes: ['disabled', 'aria-hidden']
    },
    contentFilter: {
      excludeEmptyText: false,
      minTextLength: 0,
      requireAriaLabel: false
    },
    visibilityFilter: {
      excludeHidden: true,
      excludeOutOfViewport: false,
      minSize: { width: 10, height: 10 }
    },
    behaviorFilter: {
      minClickInterval: 100,
      maxClicksPerSecond: 10,
      maxConsecutiveClicks: 3,
      validateClickPosition: true
    },
    sampling: {
      enabled: true,
      sampleRate: 0.8,
      strategy: 'consistent',
      elementTypeRates: {
        button: 1.0,
        a: 1.0,
        div: 0.5,
        span: 0.3
      }
    },
    contextFilter: {
      excludeHeaderFooter: false,
      excludeSidebar: false,
      onlyFirstScreen: false
    },
    debug: true  // 开启调试模式，查看过滤日志
  }
});
```

### 6.3 使用预设配置

```typescript
import { PRESET_CONFIGS } from '@track/sdk';

// 使用严格模式
await init({
  appId: 'your-app-id',
  userId: 'user-123'
}, {
  endpoint: 'https://track.yourdomain.com',
  clickTrack: PRESET_CONFIGS.CLICK_TRACK_STRICT
});

// 使用平衡模式（推荐）
await init({
  appId: 'your-app-id',
  userId: 'user-123'
}, {
  endpoint: 'https://track.yourdomain.com',
  clickTrack: PRESET_CONFIGS.CLICK_TRACK_BALANCED
});

// 使用宽松模式
await init({
  appId: 'your-app-id',
  userId: 'user-123'
}, {
  endpoint: 'https://track.yourdomain.com',
  clickTrack: PRESET_CONFIGS.CLICK_TRACK_LOOSE
});
```

## 7. 性能优化建议

### 7.1 计算优化

1. **缓存计算结果**：对于静态属性（如标签名、选择器匹配结果），可以缓存
2. **延迟计算**：对于耗时操作（如 `getBoundingClientRect()`），可以延迟到需要时才计算
3. **批量处理**：对于需要遍历 DOM 的操作，尽量批量处理

### 7.2 内存优化

1. **及时清理历史记录**：点击历史记录只保留最近 5 秒的数据
2. **限制 Map 大小**：限制 `clickHistory` Map 的最大条目数
3. **避免内存泄漏**：及时移除事件监听器

### 7.3 网络优化

1. **采样降低数据量**：通过采样减少上报的数据量
2. **批量上报**：将多个点击事件合并上报
3. **压缩传输**：对事件数据进行压缩

## 8. 测试策略

### 8.1 单元测试

```typescript
describe('ClickTracker', () => {
  let tracker: ClickTracker;
  
  beforeEach(() => {
    tracker = new ClickTracker(defaultClickTrackConfig);
  });
  
  it('should filter script tags', () => {
    const script = document.createElement('script');
    const event = new MouseEvent('click');
    expect(tracker.shouldTrackClick(script, event)).toBe(false);
  });
  
  it('should filter elements with data-no-track', () => {
    const element = document.createElement('div');
    element.setAttribute('data-no-track', '');
    const event = new MouseEvent('click');
    expect(tracker.shouldTrackClick(element, event)).toBe(false);
  });
  
  it('should respect click interval', () => {
    const button = document.createElement('button');
    const event1 = new MouseEvent('click');
    const event2 = new MouseEvent('click');
    
    tracker.handleClick(event1);
    expect(tracker.handleClick(event2)).toBe(false);
  });
  
  // 更多测试用例...
});
```

### 8.2 集成测试

- 测试不同配置下的采集效果
- 测试不同页面结构下的采集准确性
- 测试性能影响

### 8.3 数据质量测试

- 统计过滤率：验证过滤策略的有效性
- 数据质量评估：对比过滤前后的数据质量
- A/B 测试：对比不同配置方案的效果

## 9. 监控与调优

### 9.1 监控指标

1. **采集率**：实际采集的点击事件数 / 总点击事件数
2. **过滤率**：被过滤的点击事件数 / 总点击事件数
3. **性能指标**：点击处理的平均耗时
4. **数据质量**：采集的事件中有效事件的比例

### 9.2 调优建议

1. **定期回顾**：根据业务需求调整过滤策略
2. **数据分析**：分析被过滤的事件，优化策略
3. **用户反馈**：收集用户反馈，优化采集准确性

## 10. 总结

本策略设计文档提供了完整的用户点击采集策略方案，包括：

1. **多维度过滤**：元素、内容、可见性、行为、上下文等多维度过滤
2. **灵活配置**：支持详细的配置选项，满足不同业务场景需求
3. **预设方案**：提供严格、平衡、宽松三种预设配置
4. **性能优化**：考虑性能影响，提供优化建议
5. **可扩展性**：策略设计支持未来扩展和优化

通过实施本策略，可以有效提高点击采集的数据质量，降低数据成本，同时保护用户隐私。

