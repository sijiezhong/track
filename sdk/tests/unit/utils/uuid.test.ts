/**
 * UUID 工具函数测试
 */

import { describe, it, expect } from 'vitest';
import { generateUUID, generateShortId } from '../../../src/utils/uuid';

describe('generateUUID', () => {
  it('应该生成有效的 UUID 格式', () => {
    const uuid = generateUUID();
    expect(uuid).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
    );
  });

  it('每次调用应生成不同的 UUID', () => {
    const uuid1 = generateUUID();
    const uuid2 = generateUUID();
    expect(uuid1).not.toBe(uuid2);
  });

  it('应生成多个唯一的 UUID', () => {
    const uuids = new Set();
    for (let i = 0; i < 100; i++) {
      uuids.add(generateUUID());
    }
    expect(uuids.size).toBe(100);
  });
});

describe('generateShortId', () => {
  it('应该生成有效的短 ID', () => {
    const id = generateShortId();
    expect(id).toBeTruthy();
    expect(typeof id).toBe('string');
    expect(id.length).toBeGreaterThan(0);
  });

  it('带前缀时应包含前缀', () => {
    const id = generateShortId('track');
    expect(id).toContain('track_');
  });

  it('每次调用应生成不同的 ID', () => {
    const id1 = generateShortId();
    const id2 = generateShortId();
    expect(id1).not.toBe(id2);
  });

  it('相同前缀应生成不同的 ID', () => {
    const id1 = generateShortId('prefix');
    const id2 = generateShortId('prefix');
    expect(id1).not.toBe(id2);
  });
});

