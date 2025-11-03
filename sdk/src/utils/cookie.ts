/**
 * Cookie 操作工具
 * @packageDocumentation
 */

/**
 * Cookie 选项
 */
interface CookieOptions {
  /** 过期天数 */
  expires?: number;
  /** 过期时间（Date 对象） */
  expiresDate?: Date;
  /** 路径 */
  path?: string;
  /** 域名 */
  domain?: string;
  /** 是否安全（HTTPS） */
  secure?: boolean;
  /** SameSite 属性 */
  sameSite?: 'strict' | 'lax' | 'none';
}

/**
 * 设置 Cookie
 * 
 * @param name - Cookie 名称
 * @param value - Cookie 值
 * @param options - Cookie 选项
 * 
 * @example
 * ```ts
 * setCookie('sessionId', 'abc123', { expires: 7 });
 * ```
 */
export function setCookie(
  name: string,
  value: string,
  options: CookieOptions = {}
): void {
  let cookieString = `${encodeURIComponent(name)}=${encodeURIComponent(value)}`;

  if (options.expires !== undefined) {
    const date = new Date();
    date.setTime(date.getTime() + options.expires * 24 * 60 * 60 * 1000);
    cookieString += `; expires=${date.toUTCString()}`;
  } else if (options.expiresDate) {
    cookieString += `; expires=${options.expiresDate.toUTCString()}`;
  }

  if (options.path) {
    cookieString += `; path=${options.path}`;
  }

  if (options.domain) {
    cookieString += `; domain=${options.domain}`;
  }

  if (options.secure) {
    cookieString += '; secure';
  }

  if (options.sameSite) {
    cookieString += `; samesite=${options.sameSite}`;
  }

  document.cookie = cookieString;
}

/**
 * 获取 Cookie
 * 
 * @param name - Cookie 名称
 * @returns Cookie 值，如果不存在返回 null
 * 
 * @example
 * ```ts
 * const sessionId = getCookie('sessionId');
 * ```
 */
export function getCookie(name: string): string | null {
  const nameEQ = `${encodeURIComponent(name)}=`;
  const cookies = document.cookie.split(';');

  for (let i = 0; i < cookies.length; i++) {
    let cookie = cookies[i];
    while (cookie.charAt(0) === ' ') {
      cookie = cookie.substring(1, cookie.length);
    }
    if (cookie.indexOf(nameEQ) === 0) {
      return decodeURIComponent(cookie.substring(nameEQ.length));
    }
  }

  return null;
}

/**
 * 删除 Cookie
 * 
 * @param name - Cookie 名称
 * @param options - Cookie 选项（用于匹配原始设置）
 * 
 * @example
 * ```ts
 * deleteCookie('sessionId', { path: '/' });
 * ```
 */
export function deleteCookie(
  name: string,
  options: Omit<CookieOptions, 'expires' | 'expiresDate'> = {}
): void {
  setCookie(name, '', {
    ...options,
    expires: -1,
  });
}

