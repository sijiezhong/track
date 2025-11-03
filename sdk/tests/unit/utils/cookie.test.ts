/**
 * Cookie 工具函数测试
 */

import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { setCookie, getCookie, deleteCookie } from '../../../src/utils/cookie';

describe('Cookie 工具', () => {
  beforeEach(() => {
    // 清空所有 cookie
    document.cookie.split(';').forEach((cookie) => {
      const [name] = cookie.split('=');
      document.cookie = `${name.trim()}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/`;
    });
  });

  afterEach(() => {
    // 清理测试 cookie
    deleteCookie('test', { path: '/' });
  });

  describe('setCookie', () => {
    it('应该设置 cookie', () => {
      setCookie('test', 'value', { path: '/' });
      expect(getCookie('test')).toBe('value');
    });

    it('应该处理特殊字符', () => {
      setCookie('test', 'value with spaces', { path: '/' });
      expect(getCookie('test')).toBe('value with spaces');
    });

    it('应该设置带过期时间的 cookie', () => {
      setCookie('test', 'value', { expires: 1, path: '/' });
      expect(getCookie('test')).toBe('value');
    });

    it('应该设置带过期日期的 cookie', () => {
      const date = new Date();
      date.setTime(date.getTime() + 24 * 60 * 60 * 1000);
      setCookie('test', 'value', { expiresDate: date, path: '/' });
      expect(getCookie('test')).toBe('value');
    });
  });

  describe('getCookie', () => {
    it('应该获取已设置的 cookie', () => {
      setCookie('test', 'value', { path: '/' });
      expect(getCookie('test')).toBe('value');
    });

    it('不存在的 cookie 应返回 null', () => {
      expect(getCookie('nonexistent')).toBeNull();
    });

    it('应该正确解码 cookie 值', () => {
      setCookie('test', 'value%20with%20spaces', { path: '/' });
      const value = getCookie('test');
      expect(value).toBeTruthy();
    });
  });

  describe('deleteCookie', () => {
    it('应该删除 cookie', () => {
      setCookie('test', 'value', { path: '/' });
      expect(getCookie('test')).toBe('value');
      deleteCookie('test', { path: '/' });
      expect(getCookie('test')).toBeNull();
    });
  });
});

