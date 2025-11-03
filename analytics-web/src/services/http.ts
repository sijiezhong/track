import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import type { ApiResponse, ApiError } from '@/types/api'
import { config } from '@/config'
import { getStoredAuth, clearStoredAuth } from '@/lib/auth'

const http: AxiosInstance = axios.create({
  baseURL: config.apiBaseUrl,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
http.interceptors.request.use(
  (config: AxiosRequestConfig) => {
    const auth = getStoredAuth()
    if (auth.token) {
      config.headers = config.headers || {}
      config.headers['Authorization'] = `Bearer ${auth.token}`
    }
    if (auth.appId) {
      config.headers = config.headers || {}
      config.headers['X-App-Id'] = auth.appId.toString()
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
http.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    // 直接返回 data 字段
    return response.data
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        // 未授权，清除认证信息并跳转登录页
        clearStoredAuth()
        window.location.href = '/login'
      } else if (status === 403) {
        // 权限不足
        const errorData: ApiError = error.response.data || {
          code: 403,
          message: '访问被拒绝',
          errorCode: 'FORBIDDEN',
        }
        return Promise.reject(errorData)
      } else {
        // 其他错误
        const errorData: ApiError = error.response.data || {
          code: status,
          message: error.response.statusText || '请求失败',
        }
        return Promise.reject(errorData)
      }
    }
    return Promise.reject(error)
  }
)

export { http }
