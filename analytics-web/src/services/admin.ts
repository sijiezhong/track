import { http } from './http'
import type { ApiResponse } from '@/types/api'
import type { User } from '@/types/user'
import type { UserCreateInput, UserUpdateInput } from '@/types/user'

export interface Application {
  id: number
  appName: string
  appKey: string
  appId: number
  description?: string
  createTime: string
  updateTime: string
}

export interface ApplicationCreateInput {
  appName: string
  appKey?: string
  description?: string
  appId?: number
}

export const adminApi = {
  // 用户管理
  getUsers: (appId: number): Promise<ApiResponse<User[]>> => {
    return http.get('/admin/users', {
      headers: { 'X-App-Id': appId.toString() },
    })
  },

  createUser: (appId: number, data: UserCreateInput): Promise<ApiResponse<User>> => {
    return http.post('/admin/users', data, {
      headers: { 'X-App-Id': appId.toString() },
    })
  },

  // 应用管理
  getApplications: (appId: number): Promise<ApiResponse<Application[]>> => {
    return http.get('/admin/apps', {
      headers: { 'X-App-Id': appId.toString() },
    })
  },

  createApplication: (
    appId: number,
    data: ApplicationCreateInput
  ): Promise<ApiResponse<Application>> => {
    return http.post('/admin/apps', data, {
      headers: { 'X-App-Id': appId.toString() },
    })
  },
}
