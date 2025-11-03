import type { Event } from '@/types/event'
import type { ApiResponse } from '@/types/api'

export const eventApi = {
  // SSE 事件流
  // 统一使用相对路径，由 Vite proxy 处理代理
  // 注意：appId 通过 X-App-Id header 传递，不通过 URL 参数
  getEventStreamUrl: (): string => {
    // 生产环境：通过代理或直接访问
    return '/api/v1/events/stream'
  },
}
