import { useAuth } from './useAuth'
import type { UserRole } from '@/types/auth'

export function usePermission() {
  const { role, isAuthenticated } = useAuth()

  const hasRole = (requiredRoles: UserRole[]): boolean => {
    if (!isAuthenticated || !role) return false
    return requiredRoles.includes(role)
  }

  const hasAnyRole = (requiredRoles: UserRole[]): boolean => {
    return hasRole(requiredRoles)
  }

  const canAccess = (requiredRoles: UserRole[]): boolean => {
    return hasAnyRole(requiredRoles)
  }

  return {
    role,
    isAuthenticated,
    hasRole,
    hasAnyRole,
    canAccess,
  }
}
