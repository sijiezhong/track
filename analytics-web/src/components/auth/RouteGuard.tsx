import { Navigate } from 'react-router-dom'
import { usePermission } from '@/hooks/usePermission'
import { ROUTE_PERMISSIONS } from '@/constants/routes'
import type { ReactNode } from 'react'

interface RouteGuardProps {
  children: ReactNode
  path: string
}

export function RouteGuard({ children, path }: RouteGuardProps) {
  const { isAuthenticated, role } = usePermission()

  // 未认证，跳转登录页
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  // 检查路径权限
  const allowedRoles = ROUTE_PERMISSIONS[path]
  if (allowedRoles && role && !allowedRoles.includes(role)) {
    // 权限不足，跳转403页
    return <Navigate to="/403" replace />
  }

  return <>{children}</>
}
