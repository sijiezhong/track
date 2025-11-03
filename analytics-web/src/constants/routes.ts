import type { UserRole } from '@/types/auth'

export const ROUTES = {
  LOGIN: '/login',
  HOME: '/',
  FORBIDDEN: '/403',
  ANALYTICS: {
    TREND: '/analytics/trend',
    FUNNEL: '/analytics/funnel',
    RETENTION: '/analytics/retention',
    PATH: '/analytics/path',
    SEGMENTATION: '/analytics/segmentation',
    HEATMAP: '/analytics/heatmap',
  },
  ADMIN: {
    USERS: '/admin/users',
    APPLICATIONS: '/admin/applications',
    WEBHOOKS: '/admin/webhooks',
    AUDIT_LOG: '/admin/audit-log',
  },
} as const

export const ROUTE_PERMISSIONS: Record<string, UserRole[]> = {
  '/': ['ADMIN', 'ANALYST', 'DEVELOPER', 'READONLY'], // 大屏首页
  '/analytics/trend': ['ADMIN', 'ANALYST'],
  '/analytics/funnel': ['ADMIN', 'ANALYST'],
  '/analytics/retention': ['ADMIN', 'ANALYST'],
  '/analytics/path': ['ADMIN', 'ANALYST'],
  '/analytics/segmentation': ['ADMIN', 'ANALYST'],
  '/analytics/heatmap': ['ADMIN', 'ANALYST'],
  '/admin/users': ['ADMIN'],
  '/admin/applications': ['ADMIN'],
  '/admin/webhooks': ['ADMIN'],
  '/admin/audit-log': ['ADMIN'],
}
