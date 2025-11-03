/**
 * URL 工具函数测试
 */

import { describe, it, expect } from 'vitest';
import {
  parseUrlParams,
  buildQueryString,
  getCurrentUrl,
  getCurrentPath,
  getReferrer,
} from '../../../src/utils/url';

describe('parseUrlParams', () => {
  it('应该解析 URL 参数', () => {
    const params = parseUrlParams('?a=1&b=2');
    expect(params).toEqual({ a: '1', b: '2' });
  });

  it('应该处理空参数', () => {
    const params = parseUrlParams('');
    expect(params).toEqual({});
  });

  it('应该处理没有值的参数', () => {
    const params = parseUrlParams('?a=');
    expect(params).toEqual({ a: '' });
  });

  it('应该处理编码的参数', () => {
    const params = parseUrlParams('?a=hello%20world');
    expect(params).toEqual({ a: 'hello world' });
  });

  it('应该处理多个同名参数（取最后一个）', () => {
    const params = parseUrlParams('?a=1&a=2');
    expect(params.a).toBe('2');
  });
});

describe('buildQueryString', () => {
  it('应该构建查询字符串', () => {
    const query = buildQueryString({ a: 1, b: 'hello' });
    expect(query).toContain('a=1');
    expect(query).toContain('b=hello');
  });

  it('应该处理空对象', () => {
    const query = buildQueryString({});
    expect(query).toBe('');
  });

  it('应该跳过 null 和 undefined', () => {
    const query = buildQueryString({ a: 1, b: null, c: undefined });
    expect(query).not.toContain('b=');
    expect(query).not.toContain('c=');
    expect(query).toContain('a=1');
  });

  it('应该编码特殊字符', () => {
    const query = buildQueryString({ a: 'hello world' });
    expect(query).toBe('a=hello%20world');
  });
});

describe('getCurrentUrl', () => {
  it('应该返回当前页面 URL', () => {
    const url = getCurrentUrl();
    expect(url).toBeTruthy();
    expect(typeof url).toBe('string');
  });
});

describe('getCurrentPath', () => {
  it('应该返回当前页面路径', () => {
    const path = getCurrentPath();
    expect(path).toBeTruthy();
    expect(typeof path).toBe('string');
  });
});

describe('getReferrer', () => {
  it('应该返回来源页面', () => {
    const referrer = getReferrer();
    expect(typeof referrer).toBe('string');
  });
});

