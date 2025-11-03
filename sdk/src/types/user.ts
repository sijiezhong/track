/**
 * 用户相关类型定义
 * @packageDocumentation
 */

/**
 * 用户模式枚举
 */
export enum UserMode {
  /** 实名模式（已设置 userId） */
  IDENTIFIED = 'identified',
  /** 匿名模式（未设置 userId） */
  ANONYMOUS = 'anonymous',
}

/**
 * 用户信息接口
 */
export interface UserInfo {
  /** 用户 ID（必填） */
  userId: string | number;
  /** 用户名（可选） */
  userName?: string;
  /** 用户邮箱（可选） */
  email?: string;
  /** 用户手机号（可选） */
  phone?: string;
  /** 其他自定义属性 */
  [key: string]: unknown;
}

