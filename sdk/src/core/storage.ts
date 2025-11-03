/**
 * 本地存储封装
 * @packageDocumentation
 */

/**
 * 存储接口抽象
 */
export interface IStorage {
  /**
   * 获取值
   * 
   * @param key - 键名
   * @returns 值，如果不存在返回 null
   */
  getItem(key: string): string | null;

  /**
   * 设置值
   * 
   * @param key - 键名
   * @param value - 值
   */
  setItem(key: string, value: string): void;

  /**
   * 删除值
   * 
   * @param key - 键名
   */
  removeItem(key: string): void;

  /**
   * 清空所有值
   */
  clear(): void;
}

/**
 * 内存存储实现（用于降级）
 */
class MemoryStorage implements IStorage {
  private data: Map<string, string> = new Map();

  getItem(key: string): string | null {
    return this.data.get(key) || null;
  }

  setItem(key: string, value: string): void {
    this.data.set(key, value);
  }

  removeItem(key: string): void {
    this.data.delete(key);
  }

  clear(): void {
    this.data.clear();
  }
}

/**
 * 存储管理器
 * 
 * @remarks
 * 自动检测 localStorage 是否可用，不可用时降级到内存存储
 */
export class Storage implements IStorage {
  private storage: IStorage;
  private memoryStorage: MemoryStorage;

  /**
   * 创建存储管理器
   * 
   * @param useMemory - 是否强制使用内存存储
   */
  constructor(useMemory: boolean = false) {
    this.memoryStorage = new MemoryStorage();

    if (useMemory) {
      this.storage = this.memoryStorage;
      return;
    }

    // 检测 localStorage 是否可用
    try {
      const testKey = '__track_storage_test__';
      localStorage.setItem(testKey, 'test');
      localStorage.removeItem(testKey);
      this.storage = localStorage as IStorage;
    } catch (e) {
      // localStorage 不可用，降级到内存存储
      this.storage = this.memoryStorage;
    }
  }

  /**
   * 获取值
   * 
   * @param key - 键名
   * @returns 值，如果不存在返回 null
   */
  getItem(key: string): string | null {
    try {
      return this.storage.getItem(key);
    } catch (e) {
      return null;
    }
  }

  /**
   * 设置值
   * 
   * @param key - 键名
   * @param value - 值
   */
  setItem(key: string, value: string): void {
    try {
      this.storage.setItem(key, value);
    } catch (e) {
      // 存储失败时忽略错误（如存储空间已满）
    }
  }

  /**
   * 获取 JSON 对象
   * 
   * @param key - 键名
   * @returns 解析后的对象，如果不存在或解析失败返回 null
   */
  getJSON<T = unknown>(key: string): T | null {
    const value = this.getItem(key);
    if (!value) {
      return null;
    }

    try {
      return JSON.parse(value) as T;
    } catch (e) {
      return null;
    }
  }

  /**
   * 设置 JSON 对象
   * 
   * @param key - 键名
   * @param value - 要存储的对象
   */
  setJSON(key: string, value: unknown): void {
    try {
      const jsonString = JSON.stringify(value);
      this.setItem(key, jsonString);
    } catch (e) {
      // JSON 序列化失败时忽略
    }
  }

  /**
   * 删除值
   * 
   * @param key - 键名
   */
  removeItem(key: string): void {
    try {
      this.storage.removeItem(key);
    } catch (e) {
      // 忽略错误
    }
  }

  /**
   * 清空所有值
   */
  clear(): void {
    try {
      this.storage.clear();
    } catch (e) {
      // 忽略错误
    }
  }
}

/**
 * 创建默认存储实例
 * 
 * @returns 存储实例
 */
export function createStorage(): Storage {
  return new Storage();
}

