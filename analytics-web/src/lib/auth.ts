import type { AuthState, UserRole } from '@/types/auth'

const TOKEN_KEY = 'token'
const TENANT_ID_KEY = 'appId'
const USER_KEY = 'user'
const ROLE_KEY = 'role'

export function getStoredAuth(): Partial<AuthState> {
  const token = localStorage.getItem(TOKEN_KEY)
  const appIdStr = localStorage.getItem(TENANT_ID_KEY)
  const userStr = localStorage.getItem(USER_KEY)
  const role = localStorage.getItem(ROLE_KEY) as UserRole | null

  return {
    token: token || null,
    appId: appIdStr ? parseInt(appIdStr, 10) : null,
    user: userStr ? JSON.parse(userStr) : null,
    role,
    isAuthenticated: !!token && !!appIdStr && !!role,
  }
}

export function setStoredAuth(auth: Partial<AuthState>) {
  if (auth.token) {
    localStorage.setItem(TOKEN_KEY, auth.token)
  }
  if (auth.appId !== null && auth.appId !== undefined) {
    localStorage.setItem(TENANT_ID_KEY, auth.appId.toString())
  }
  if (auth.user) {
    localStorage.setItem(USER_KEY, JSON.stringify(auth.user))
  }
  if (auth.role) {
    localStorage.setItem(ROLE_KEY, auth.role)
  }
}

export function clearStoredAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(TENANT_ID_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(ROLE_KEY)
}

export function extractRoleFromToken(token: string): UserRole | null {
  // Token格式: "role:ADMIN"
  if (token.startsWith('role:')) {
    const role = token.substring(5).trim() as UserRole
    return role
  }
  return null
}
