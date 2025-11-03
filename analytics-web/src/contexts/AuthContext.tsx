import { createContext, useContext, useState, useEffect, type ReactNode } from 'react'
import type { AuthState, UserRole } from '@/types/auth'
import { getStoredAuth, setStoredAuth, clearStoredAuth, extractRoleFromToken } from '@/lib/auth'

interface AuthContextType extends AuthState {
  login: (token: string, appId: number, user: AuthState['user']) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [authState, setAuthState] = useState<AuthState>(() => {
    const stored = getStoredAuth()
    return {
      user: stored.user || null,
      role: stored.role || null,
      appId: stored.appId || null,
      token: stored.token || null,
      isAuthenticated: stored.isAuthenticated || false,
    }
  })

  useEffect(() => {
    // 从 localStorage 同步状态
    const stored = getStoredAuth()
    setAuthState((prev) => ({
      ...prev,
      ...stored,
    }))
  }, [])

  const login = (token: string, appId: number, user: AuthState['user']) => {
    const role = extractRoleFromToken(token)
    const newState: AuthState = {
      token,
      appId,
      user,
      role,
      isAuthenticated: true,
    }
    setAuthState(newState)
    setStoredAuth(newState)
  }

  const logout = () => {
    setAuthState({
      token: null,
      appId: null,
      user: null,
      role: null,
      isAuthenticated: false,
    })
    clearStoredAuth()
  }

  return (
    <AuthContext.Provider value={{ ...authState, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
