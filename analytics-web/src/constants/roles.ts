import type { UserRole } from '@/types/auth'

export const ROLES: Record<string, UserRole> = {
  ADMIN: 'ADMIN',
  ANALYST: 'ANALYST',
  DEVELOPER: 'DEVELOPER',
  READONLY: 'READONLY',
} as const

export const ROLE_LABELS: Record<UserRole, string> = {
  ADMIN: '管理员',
  ANALYST: '分析师',
  DEVELOPER: '开发者',
  READONLY: '只读用户',
}

export const ROLE_PERMISSIONS: Record<UserRole, string[]> = {
  ADMIN: ['*'], // 所有权限
  ANALYST: ['analytics.*', 'bigscreen.view'],
  DEVELOPER: ['events.query', 'bigscreen.view'],
  READONLY: ['analytics.view', 'bigscreen.view'],
}
