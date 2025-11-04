/**
 * 移除对象中的 null 和 undefined 值
 * 用于压缩上报数据，减少 payload 大小
 *
 * @param obj - 原始对象
 * @returns 清理后的对象
 */
export function removeNulls<T extends Record<string, any>>(obj: T): Partial<T> {
  const result: Partial<T> = {};

  for (const key in obj) {
    if (obj.hasOwnProperty(key)) {
      const value = obj[key];
      if (value !== null && value !== undefined) {
        if (
          typeof value === "object" &&
          !Array.isArray(value) &&
          value.constructor === Object
        ) {
          // 递归处理嵌套对象
          const cleaned = removeNulls(value);
          if (Object.keys(cleaned).length > 0) {
            result[key] = cleaned as T[Extract<keyof T, string>];
          }
        } else {
          result[key] = value;
        }
      }
    }
  }

  return result;
}

/**
 * 简单的哈希函数
 * 用于一致性采样
 *
 * @param str - 输入字符串
 * @returns 哈希值
 */
export function hashCode(str: string): number {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i);
    hash = (hash << 5) - hash + char;
    hash = hash & hash; // Convert to 32bit integer
  }
  return Math.abs(hash);
}

/**
 * 检查是否为浏览器环境
 */
export function isBrowser(): boolean {
  return typeof window !== "undefined" && typeof document !== "undefined";
}

/**
 * 检查是否支持 LocalStorage
 */
export function isLocalStorageAvailable(): boolean {
  if (!isBrowser()) {
    return false;
  }

  try {
    const test = "__track_test__";
    localStorage.setItem(test, test);
    localStorage.removeItem(test);
    return true;
  } catch {
    return false;
  }
}

/**
 * 安全地获取 Cookie 值
 *
 * @param name - Cookie 名称
 * @returns Cookie 值，如果不存在则返回 null
 */
export function getCookie(name: string): string | null {
  if (!isBrowser()) {
    return null;
  }

  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) {
    return parts.pop()?.split(";").shift() || null;
  }
  return null;
}

export function debounce<T extends (...args: any[]) => any>(
  fn: T,
  delay: number,
): (...args: Parameters<T>) => void {
  let timeoutId: ReturnType<typeof setTimeout> | null = null;

  return function (this: any, ...args: Parameters<T>) {
    if (timeoutId !== null) {
      clearTimeout(timeoutId);
    }
    timeoutId = setTimeout(() => {
      fn.apply(this, args);
    }, delay);
  };
}

/**
 * 节流函数
 *
 * @param fn - 要节流的函数
 * @param delay - 延迟时间（毫秒）
 * @returns 节流后的函数
 */
export function throttle<T extends (...args: any[]) => any>(
  fn: T,
  delay: number,
): (...args: Parameters<T>) => void {
  let lastCallTime = 0;

  return function (this: any, ...args: Parameters<T>) {
    const now = Date.now();
    if (now - lastCallTime >= delay) {
      lastCallTime = now;
      fn.apply(this, args);
    }
  };
}
