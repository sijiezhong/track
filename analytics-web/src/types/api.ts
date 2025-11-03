export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp?: string
}

export interface ApiError {
  code: number
  message: string
  errorCode?: string
}
