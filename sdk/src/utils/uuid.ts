/**
 * UUID 生成工具
 * @packageDocumentation
 */

/**
 * 生成 UUID v4
 * 
 * @returns 生成的 UUID 字符串
 * 
 * @example
 * ```ts
 * const id = generateUUID();
 * console.log(id); // "550e8400-e29b-41d4-a716-446655440000"
 * ```
 */
export function generateUUID(): string {
  // 简化的 UUID v4 实现，兼容性更好
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

/**
 * 生成简短的唯一 ID（用于匿名 ID 等场景）
 * 
 * @param prefix - 可选前缀
 * @returns 生成的唯一 ID
 * 
 * @example
 * ```ts
 * const id = generateShortId('track');
 * console.log(id); // "track_abc123..."
 * ```
 */
export function generateShortId(prefix?: string): string {
  const timestamp = Date.now().toString(36);
  const randomPart = Math.random().toString(36).substring(2, 9);
  const id = `${timestamp}_${randomPart}`;
  return prefix ? `${prefix}_${id}` : id;
}

