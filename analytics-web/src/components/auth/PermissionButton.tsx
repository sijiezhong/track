import { usePermission } from '@/hooks/usePermission'
import type { UserRole } from '@/types/auth'
import type { ReactNode } from 'react'
import { Button } from '@/components/ui/button'
import type { ButtonProps } from '@/components/ui/button'

interface PermissionButtonProps extends Omit<ButtonProps, 'disabled'> {
  requiredRoles: UserRole[]
  fallback?: ReactNode
}

export function PermissionButton({ requiredRoles, fallback = null, children, ...props }: PermissionButtonProps) {
  const { canAccess } = usePermission()

  if (!canAccess(requiredRoles)) {
    return <>{fallback}</>
  }

  return <Button {...props}>{children}</Button>
}
