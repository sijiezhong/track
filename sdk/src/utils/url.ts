/**
 * URL 处理工具
 * @packageDocumentation
 */

/**
 * 解析 URL 参数
 * 
 * @param url - URL 字符串，默认为当前页面 URL
 * @returns 参数对象
 * 
 * @example
 * ```ts
 * const params = parseUrlParams('?a=1&b=2');
 * console.log(params); // { a: '1', b: '2' }
 * ```
 */
export function parseUrlParams(url?: string): Record<string, string> {
  const params: Record<string, string> = {};
  const queryString = url
    ? url.split('?')[1] || ''
    : window.location.search.substring(1);

  if (!queryString) {
    return params;
  }

  const pairs = queryString.split('&');
  for (const pair of pairs) {
    const [key, value] = pair.split('=');
    if (key) {
      params[decodeURIComponent(key)] = decodeURIComponent(value || '');
    }
  }

  return params;
}

/**
 * 构建 URL 查询字符串
 * 
 * @param params - 参数对象
 * @returns 查询字符串（不含 ?）
 * 
 * @example
 * ```ts
 * const query = buildQueryString({ a: 1, b: 'hello' });
 * console.log(query); // "a=1&b=hello"
 * ```
 */
export function buildQueryString(params: Record<string, unknown>): string {
  const pairs: string[] = [];

  for (const key in params) {
    if (Object.prototype.hasOwnProperty.call(params, key)) {
      const value = params[key];
      if (value !== null && value !== undefined) {
        pairs.push(
          `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`
        );
      }
    }
  }

  return pairs.join('&');
}

/**
 * 获取当前页面完整 URL
 * 
 * @returns 完整 URL
 */
export function getCurrentUrl(): string {
  return window.location.href;
}

/**
 * 获取当前页面路径（不含查询参数和哈希）
 * 
 * @returns 页面路径
 */
export function getCurrentPath(): string {
  return window.location.pathname;
}

/**
 * 获取来源页面（referrer）
 * 
 * @returns 来源页面 URL，如果没有则返回空字符串
 */
export function getReferrer(): string {
  return document.referrer || '';
}

