import { useState, useEffect, useRef } from 'react'
import type { Event } from '@/types/event'
import { eventApi } from '@/services/event'
import { getStoredAuth } from '@/lib/auth'

export function useEventStream(appId: number | null) {
  const [events, setEvents] = useState<Event[]>([])
  const [isConnected, setIsConnected] = useState(false)
  const abortControllerRef = useRef<AbortController | null>(null)

  useEffect(() => {
    if (!appId) {
      console.warn('[SSE] ⚠️  appId 为空，跳过连接')
      return
    }

    const url = eventApi.getEventStreamUrl()
    const auth = getStoredAuth()
    
    console.log(`[SSE] 🔄 准备连接事件流 | URL: ${url} | appId: ${auth.appId} | hasToken: ${!!auth.token}`)
    
    if (!auth.appId) {
      console.error('[SSE] ❌ 致命错误: appId 为空！请检查登录状态')
      return
    }
    
    if (!auth.token) {
      console.error('[SSE] ❌ 致命错误: token 为空！请检查登录状态')
      return
    }

    // 创建 AbortController 用于取消请求
    const abortController = new AbortController()
    abortControllerRef.current = abortController

    // 使用原生 fetch API 处理 SSE 流式响应
    const fetchSSE = async () => {
      try {
        const headers: HeadersInit = {
          Accept: 'text/event-stream',
          'Cache-Control': 'no-cache',
        }
        
        if (auth.token) {
          headers['Authorization'] = `Bearer ${auth.token}`
        }
        
        if (auth.appId) {
          headers['X-App-Id'] = auth.appId.toString()
        }
        
        console.log(`[SSE] 📡 正在发起 SSE 连接... | headers: ${JSON.stringify(headers)}`)
        
        const response = await fetch(url, {
          method: 'GET',
          headers,
          signal: abortController.signal,
        }).catch(err => {
          console.error(`[SSE] ❌ fetch 请求失败 | error: ${err}`)
          throw err
        })

        console.log(`[SSE] 📨 收到响应 | status: ${response.status} | contentType: ${response.headers.get('content-type')}`)

        if (!response.ok) {
          const errorText = await response.text().catch(() => 'unknown error')
          console.error(`[SSE] ❌ HTTP 错误 | status: ${response.status} | statusText: ${response.statusText} | body: ${errorText}`)
          throw new Error(`HTTP error! status: ${response.status}, body: ${errorText}`)
        }

        if (!response.body) {
          console.error('[SSE] Response body is null')
          return
        }

        const textDecoder = new TextDecoder()
        const reader = response.body.getReader()
        let buffer = ''
        let chunkCount = 0

        console.log('[SSE] 📖 开始读取数据流...')

        while (true) {
          const { done, value } = await reader.read()

          if (done) {
            console.log('[SSE] 📕 数据流读取完成')
            break
          }

          // 解码数据块
          const chunk = textDecoder.decode(value, { stream: true })
          chunkCount++
          console.log(`[SSE] 📦 收到数据块 #${chunkCount} | 大小: ${chunk.length}字节 | 内容: ${chunk.substring(0, 200)}`)
          buffer += chunk

          // 处理 SSE 格式：以 \n\n 分隔事件
          const events = buffer.split('\n\n')
          buffer = events.pop() || '' // 保留最后一个不完整的事件

          for (const event of events) {
            if (event.trim() === '') continue

            console.log(`[SSE] 🔍 解析事件 | 原始内容: ${JSON.stringify(event)}`)

            // SSE 格式处理（兼容有无空格的情况）
            // - 标准格式：event: init\ndata: ok\n\n
            // - 可能格式：event:init\ndata:ok\n\n
            const lines = event.split('\n')
            let eventType = ''
            let dataLine = ''

            for (const line of lines) {
              const trimmedLine = line.trim()
              
              // 处理 event: 行（支持 "event: " 和 "event:" 两种格式）
              if (trimmedLine.startsWith('event:')) {
                const colonIndex = trimmedLine.indexOf(':')
                if (colonIndex >= 0) {
                  eventType = trimmedLine.slice(colonIndex + 1).trim()
                }
              }
              
              // 处理 data: 行（支持 "data: " 和 "data:" 两种格式）
              if (trimmedLine.startsWith('data:')) {
                const colonIndex = trimmedLine.indexOf(':')
                if (colonIndex >= 0) {
                  // 如果已经有数据行，追加（支持多行数据）
                  const newData = trimmedLine.slice(colonIndex + 1).trim()
                  if (dataLine) {
                    dataLine += '\n' + newData
                  } else {
                    dataLine = newData
                  }
                }
              }
            }

            console.log(`[SSE] 🔍 解析结果 | eventType: "${eventType}" | dataLine: "${dataLine}"`)

            // 处理初始化消息
            if (eventType === 'init') {
              console.log('[SSE] ✅ 连接成功！正在等待事件数据...')
              setIsConnected(true) // 标记连接已建立
            }
            
            // 处理事件类型为 'event' 的消息
            if (eventType === 'event' && dataLine) {
              try {
                const rawEvent = JSON.parse(dataLine)
                console.log(`[SSE] 📥 收到新事件 | 原始数据: ${dataLine}`)
                
                // 映射后端字段到前端 Event 类型
                // 后端返回: { id, eventName, eventTime }
                // 前端期望: { eventId, eventType, eventName, timestamp, ... }
                // 注意：后端的 eventName 实际上就是事件类型（如 pageview, click, error）
                const newEvent: Event = {
                  eventId: String(rawEvent.id || Date.now()),
                  eventType: rawEvent.eventName || 'custom', // 使用 eventName 作为 eventType
                  eventName: rawEvent.eventName || 'unknown',
                  projectId: '',
                  sessionId: '',
                  anonymousId: '',
                  eventContent: {},
                  timestamp: rawEvent.eventTime || new Date().toISOString(),
                }
                
                console.log(`[SSE] 📥 事件已映射 | eventId: ${newEvent.eventId} | eventType: ${newEvent.eventType} | timestamp: ${newEvent.timestamp}`)
                
                setEvents((prev) => {
                  // 保留最近 50 条事件
                  return [newEvent, ...prev].slice(0, 50)
                })
              } catch (err) {
                console.error(`[SSE] ❌ 解析事件数据失败 | error: ${err} | data: ${dataLine}`)
              }
            }
          }
        }
      } catch (err) {
        // 如果是主动取消，不输出错误
        if (err instanceof Error && err.name === 'AbortError') {
          return
        }
        console.error(`[SSE] ❌ Stream 错误 | error: ${err}`)
      } finally {
        console.log('[SSE] 🔌 连接已断开')
      }
    }

    fetchSSE()

    return () => {
      // 清理：取消请求
      if (abortControllerRef.current) {
        abortControllerRef.current.abort()
        abortControllerRef.current = null
      }
      setIsConnected(false) // 重置连接状态
    }
  }, [appId])

  return { events, isConnected }
}
