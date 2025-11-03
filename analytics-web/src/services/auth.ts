import { http } from './http'
import type { ApiResponse } from '@/types/api'
import type { User } from '@/types/auth'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  user: User
  role: string
  appId: number
}

export const authApi = {
  // 登录接口 - 用户名全局唯一，从返回的用户信息中获取应用ID
  login: (data: LoginRequest): Promise<ApiResponse<LoginResponse>> => {
    return http.post('/auth/login', data)
  },

  logout: (): Promise<void> => {
    // 清除本地认证信息
    localStorage.clear()
    return Promise.resolve()
  },
}
