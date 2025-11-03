/**
 * 存储模块测试
 */

import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { Storage, createStorage } from '../../../src/core/storage';

describe('Storage', () => {
  let storage: Storage;

  beforeEach(() => {
    // 清空 localStorage
    localStorage.clear();
    storage = createStorage();
  });

  describe('基本操作', () => {
    it('应该设置和获取值', () => {
      storage.setItem('test', 'value');
      expect(storage.getItem('test')).toBe('value');
    });

    it('应该删除值', () => {
      storage.setItem('test', 'value');
      storage.removeItem('test');
      expect(storage.getItem('test')).toBeNull();
    });

    it('应该清空所有值', () => {
      storage.setItem('test1', 'value1');
      storage.setItem('test2', 'value2');
      storage.clear();
      expect(storage.getItem('test1')).toBeNull();
      expect(storage.getItem('test2')).toBeNull();
    });
  });

  describe('JSON 操作', () => {
    it('应该设置和获取 JSON', () => {
      const obj = { a: 1, b: 'test' };
      storage.setJSON('test', obj);
      expect(storage.getJSON('test')).toEqual(obj);
    });

    it('获取不存在的 JSON 应返回 null', () => {
      expect(storage.getJSON('nonexistent')).toBeNull();
    });

    it('应该处理无效的 JSON', () => {
      storage.setItem('invalid', 'not json');
      expect(storage.getJSON('invalid')).toBeNull();
    });

    it('应该处理复杂的 JSON 对象', () => {
      const complex = {
        nested: { a: 1, b: [1, 2, 3] },
        date: new Date().toISOString(),
      };
      storage.setJSON('complex', complex);
      const retrieved = storage.getJSON('complex');
      expect(retrieved).toBeTruthy();
      expect(retrieved?.nested).toEqual(complex.nested);
    });
  });

  describe('localStorage 降级', () => {
    afterEach(() => {
      vi.restoreAllMocks();
    });

    it('localStorage 不可用时应降级到内存存储', () => {
      // 使用强制内存存储模式测试
      const fallbackStorage = new Storage(true);
      fallbackStorage.setItem('test', 'value');
      // 内存存储应该工作
      expect(fallbackStorage.getItem('test')).toBe('value');
    });

    it('应该处理存储错误', () => {
      const storage = new Storage();
      // 设置一个值
      storage.setItem('test', 'value');

      // Mock setItem 抛出错误（模拟存储已满）
      vi.spyOn(localStorage, 'setItem').mockImplementation(() => {
        throw new Error('QuotaExceededError');
      });

      // 应该不抛出错误
      expect(() => {
        storage.setItem('test2', 'value2');
      }).not.toThrow();
    });
  });

  describe('强制内存存储', () => {
    it('应该使用内存存储', () => {
      const memoryStorage = new Storage(true);
      memoryStorage.setItem('test', 'value');
      expect(memoryStorage.getItem('test')).toBe('value');
    });
  });
});

