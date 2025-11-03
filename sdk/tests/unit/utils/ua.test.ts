/**
 * User-Agent 解析工具测试
 */

import { describe, it, expect } from 'vitest';
import { parseUA } from '../../../src/utils/ua';

describe('parseUA', () => {
  it('应该解析 Chrome User-Agent', () => {
    const ua =
      'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36';
    const result = parseUA(ua);
    expect(result.browser).toBe('Chrome');
    expect(result.os).toBe('Windows');
    expect(result.device).toBe('Desktop');
  });

  it('应该解析 Firefox User-Agent', () => {
    const ua =
      'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0';
    const result = parseUA(ua);
    expect(result.browser).toBe('Firefox');
    expect(result.os).toBe('Windows');
    expect(result.device).toBe('Desktop');
  });

  it('应该解析 Safari User-Agent', () => {
    const ua =
      'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15';
    const result = parseUA(ua);
    expect(result.browser).toBe('Safari');
    expect(result.os).toBe('macOS');
    expect(result.device).toBe('Desktop');
  });

  it('应该解析移动设备 User-Agent', () => {
    const ua =
      'Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1';
    const result = parseUA(ua);
    expect(result.os).toBe('iOS');
    expect(result.device).toBe('Mobile');
  });

  it('应该解析 Android 设备', () => {
    const ua =
      'Mozilla/5.0 (Linux; Android 11; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36';
    const result = parseUA(ua);
    expect(result.os).toBe('Android');
    expect(result.device).toBe('Mobile');
  });

  it('应该解析 iPad', () => {
    const ua =
      'Mozilla/5.0 (iPad; CPU OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1';
    const result = parseUA(ua);
    expect(result.device).toBe('Tablet');
  });

  it('空 UA 应返回 null 值', () => {
    const result = parseUA('');
    expect(result.browser).toBeNull();
    expect(result.os).toBeNull();
    // 空 UA 时 device 默认返回 'Desktop'
    expect(result.device).toBeTruthy();
  });

  it('应该处理未提供的 UA（使用当前浏览器）', () => {
    const result = parseUA();
    expect(result).toBeTruthy();
    // 在 jsdom 环境中，browser 可能是 null 或 string
    expect(result.browser === null || typeof result.browser === 'string').toBe(true);
    expect(typeof result.device === 'string').toBe(true);
  });
});

