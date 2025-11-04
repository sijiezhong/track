import { Track } from "../core/tracker";
import {
  EventData,
  EventType,
  ClickTrackConfig,
  defaultClickTrackConfig,
} from "../types";
import { getDomPath } from "../utils/dom";
import { hashCode } from "../utils";
import { isBrowser } from "../utils";

/**
 * 点击采集器类
 * 实现多维度过滤策略，只采集有价值的点击事件
 */
export class ClickTracker {
  private tracker: Track;
  private config: ClickTrackConfig;
  private clickHistory: Map<string, number[]> = new Map(); // 点击历史记录
  private lastClickTime: number = 0;
  private lastClickElement: HTMLElement | null = null;
  private consecutiveClickCount: number = 0;
  private clickHandler: ((event: MouseEvent) => void) | null = null;

  constructor(tracker: Track, config?: Partial<ClickTrackConfig>) {
    this.tracker = tracker;
    // 合并配置，使用默认配置作为基础
    this.config = {
      ...defaultClickTrackConfig,
      ...config,
      elementFilter: {
        ...defaultClickTrackConfig.elementFilter,
        ...config?.elementFilter,
      },
      contentFilter: {
        ...defaultClickTrackConfig.contentFilter,
        ...config?.contentFilter,
      },
      visibilityFilter: {
        ...defaultClickTrackConfig.visibilityFilter,
        ...config?.visibilityFilter,
      },
      behaviorFilter: {
        ...defaultClickTrackConfig.behaviorFilter,
        ...config?.behaviorFilter,
      },
      sampling: {
        ...defaultClickTrackConfig.sampling,
        ...config?.sampling,
        elementTypeRates: {
          ...defaultClickTrackConfig.sampling.elementTypeRates,
          ...config?.sampling?.elementTypeRates,
        },
      },
      contextFilter: {
        ...defaultClickTrackConfig.contextFilter,
        ...config?.contextFilter,
      },
    };
  }

  /**
   * 设置点击采集
   */
  setup(): void {
    if (!isBrowser() || !this.config.enabled) {
      return;
    }

    // 使用捕获阶段，确保能捕获所有点击
    this.clickHandler = (event: MouseEvent) => {
      this.handleClick(event);
    };

    document.addEventListener("click", this.clickHandler, true);
  }

  /**
   * 移除点击采集
   */
  remove(): void {
    if (!isBrowser() || !this.clickHandler) {
      return;
    }

    document.removeEventListener("click", this.clickHandler, true);
    this.clickHandler = null;

    // 清理历史记录
    this.clickHistory.clear();
    this.lastClickTime = 0;
    this.lastClickElement = null;
    this.consecutiveClickCount = 0;
  }

  /**
   * 处理点击事件
   */
  private handleClick(event: MouseEvent): boolean {
    const target = event.target as HTMLElement;

    if (!target) {
      return false;
    }

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
        console.log("[ClickTracker] Filtered by element filter:", element);
      }
      return false;
    }

    // [3] 内容过滤
    if (!this.passContentFilter(element)) {
      if (this.config.debug) {
        console.log("[ClickTracker] Filtered by content filter:", element);
      }
      return false;
    }

    // [4] 可见性过滤
    if (!this.passVisibilityFilter(element)) {
      if (this.config.debug) {
        console.log("[ClickTracker] Filtered by visibility filter:", element);
      }
      return false;
    }

    // [5] 行为过滤
    if (!this.passBehaviorFilter(element, event)) {
      if (this.config.debug) {
        console.log("[ClickTracker] Filtered by behavior filter:", element);
      }
      return false;
    }

    // [6] 上下文过滤
    if (!this.passContextFilter(element)) {
      if (this.config.debug) {
        console.log("[ClickTracker] Filtered by context filter:", element);
      }
      return false;
    }

    // [7] 采样决策
    if (!this.passSampling(element)) {
      if (this.config.debug) {
        console.log("[ClickTracker] Filtered by sampling:", element);
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
      const matches = filter.includeSelectors.some((selector) => {
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
      const hasRequiredAttr = filter.requireAttributes.some((attr) => {
        if (attr === "onclick") return (element as any).onclick !== null;
        if (attr === "href") return element.hasAttribute("href");
        return element.hasAttribute(attr);
      });
      if (!hasRequiredAttr) {
        return false;
      }
    }

    // 排除属性检查
    for (const attr of filter.excludeAttributes) {
      if (
        attr === "disabled" &&
        (element as HTMLButtonElement | HTMLInputElement).disabled
      ) {
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
      const text =
        element.innerText?.trim() || element.textContent?.trim() || "";
      const ariaLabel =
        element.getAttribute("aria-label") ||
        element.getAttribute("title") ||
        "";
      const hasId = !!element.id;
      const hasClass = !!element.className;

      if (!text && !ariaLabel && !hasId && !hasClass) {
        return false;
      }
    }

    if (filter.minTextLength > 0) {
      const text =
        element.innerText?.trim() || element.textContent?.trim() || "";
      if (text.length < filter.minTextLength) {
        return false;
      }
    }

    if (filter.requireAriaLabel) {
      const ariaLabel =
        element.getAttribute("aria-label") || element.getAttribute("title");
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
      if (
        style.display === "none" ||
        style.visibility === "hidden" ||
        parseFloat(style.opacity) === 0
      ) {
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
        height: window.innerHeight,
      };

      if (
        rect.right < 0 ||
        rect.left > viewport.width ||
        rect.bottom < 0 ||
        rect.top > viewport.height
      ) {
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
    const recentClicks = clickHistory.filter((time) => now - time < 1000);
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

      if (
        clickX < rect.left ||
        clickX > rect.right ||
        clickY < rect.top ||
        clickY > rect.bottom
      ) {
        return false;
      }
    }

    // 更新历史记录
    this.lastClickTime = now;
    clickHistory.push(now);
    // 只保留最近 5 秒的记录
    this.clickHistory.set(
      elementKey,
      clickHistory.filter((time) => now - time < 5000),
    );

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
    if (sampling.strategy === "random") {
      return Math.random() < sampling.sampleRate;
    } else if (sampling.strategy === "consistent") {
      // 基于用户 ID 的一致性采样
      const userId = this.getUserId();
      const hash = hashCode(userId + element.tagName);
      return hash % 100 < sampling.sampleRate * 100;
    }

    return true;
  }

  /**
   * 获取元素唯一标识
   */
  private getElementKey(element: HTMLElement): string {
    return `${element.tagName}-${element.id || element.className || "unknown"}`;
  }

  /**
   * 获取用户 ID（用于一致性采样）
   */
  private getUserId(): string {
    const userConfig = this.tracker.getUserConfig();
    return userConfig?.userId || "default-user-id";
  }

  /**
   * [8] 采集点击事件
   */
  private trackClick(element: HTMLElement, event: MouseEvent): void {
    if (!this.tracker.isStarted()) {
      return;
    }

    const domPath = getDomPath(element);

    const clickEvent: EventData = {
      type: EventType.CLICK,
      properties: {
        domPath: domPath,
        tagName: element.tagName,
        className: element.className,
        id: element.id,
        innerText: element.innerText?.slice(0, 100), // 限制长度
        x: event.clientX,
        y: event.clientY,
      },
    };

    this.tracker.addEvent(clickEvent);
  }
}
