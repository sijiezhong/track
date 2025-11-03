/**
 * User-Agent 解析工具
 * @packageDocumentation
 */

/**
 * 解析后的 UA 信息
 */
export interface UAParseResult {
  /** 浏览器名称 */
  browser: string | null;
  /** 操作系统 */
  os: string | null;
  /** 设备类型 */
  device: string | null;
}

/**
 * 解析 User-Agent 字符串
 * 
 * @param ua - User-Agent 字符串，默认为当前浏览器的 UA
 * @returns 解析结果
 * 
 * @example
 * ```ts
 * const result = parseUA();
 * console.log(result.browser); // "Chrome"
 * console.log(result.os); // "Windows"
 * console.log(result.device); // "Desktop"
 * ```
 */
export function parseUA(ua?: string): UAParseResult {
  const userAgent = ua || (typeof navigator !== 'undefined' ? navigator.userAgent : '');
  
  const result: UAParseResult = {
    browser: null,
    os: null,
    device: null,
  };

  if (!userAgent) {
    return result;
  }

  // 浏览器检测
  if (userAgent.indexOf('Firefox') > -1) {
    result.browser = 'Firefox';
  } else if (userAgent.indexOf('Chrome') > -1 && userAgent.indexOf('Edge') === -1) {
    result.browser = 'Chrome';
  } else if (userAgent.indexOf('Safari') > -1 && userAgent.indexOf('Chrome') === -1) {
    result.browser = 'Safari';
  } else if (userAgent.indexOf('Edge') > -1) {
    result.browser = 'Edge';
  } else if (userAgent.indexOf('Opera') > -1 || userAgent.indexOf('OPR') > -1) {
    result.browser = 'Opera';
  } else if (userAgent.indexOf('MSIE') > -1 || userAgent.indexOf('Trident') > -1) {
    result.browser = 'IE';
  }

  // 操作系统检测
  if (userAgent.indexOf('Windows') > -1) {
    result.os = 'Windows';
  } else if (userAgent.indexOf('Mac') > -1 && userAgent.indexOf('iPhone') === -1 && userAgent.indexOf('iPad') === -1) {
    result.os = 'macOS';
  } else if (userAgent.indexOf('Linux') > -1 && userAgent.indexOf('Android') === -1) {
    result.os = 'Linux';
  } else if (userAgent.indexOf('Android') > -1) {
    result.os = 'Android';
  } else if (userAgent.indexOf('iOS') > -1 || userAgent.indexOf('iPhone') > -1 || userAgent.indexOf('iPad') > -1) {
    result.os = 'iOS';
  }

  // 设备类型检测
  if (userAgent.indexOf('Mobile') > -1 || userAgent.indexOf('Android') > -1 || userAgent.indexOf('iPhone') > -1) {
    if (userAgent.indexOf('iPad') > -1) {
      result.device = 'Tablet';
    } else {
      result.device = 'Mobile';
    }
  } else if (userAgent.indexOf('Tablet') > -1 || userAgent.indexOf('iPad') > -1) {
    result.device = 'Tablet';
  } else {
    result.device = 'Desktop';
  }

  return result;
}

