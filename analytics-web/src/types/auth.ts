export type UserRole = 'ADMIN' | 'ANALYST' | 'DEVELOPER' | 'READONLY'

export interface User {
  id: number
  username: string
  realName?: string
  email?: string
  phone?: string
  isAnonymous: boolean
  appId?: number
  createTime: string
  updateTime: string
}

export interface AuthState {
  user: User | null
  role: UserRole | null
  appId: number | null
  token: string | null
  isAuthenticated: boolean
}
