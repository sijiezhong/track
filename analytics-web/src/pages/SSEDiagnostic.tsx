import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { getStoredAuth } from '@/lib/auth'

/**
 * SSE 诊断页面
 * 访问路径: /sse-diagnostic
 * 
 * 用于诊断实时事件流连接问题
 */
export default function SSEDiagnostic() {
  const [logs, setLogs] = useState<string[]>([])
  const [isConnecting, setIsConnecting] = useState(false)

  const addLog = (message: string) => {
    const timestamp = new Date().toLocaleTimeString()
    setLogs((prev) => [...prev, `[${timestamp}] ${message}`])
  }

  const clearLogs = () => {
    setLogs([])
  }

  const testAuth = () => {
    addLog('=== 测试认证信息 ===')
    const auth = getStoredAuth()
    addLog(`Token: ${auth.token ? '✅ 已设置' : '❌ 未设置'}`)
    addLog(`TenantId: ${auth.appId || '❌ 未设置'}`)
    if (auth.token) {
      addLog(`Token 值: ${auth.token}`)
    }
  }

  const testSSEConnection = async () => {
    setIsConnecting(true)
    addLog('=== 开始测试 SSE 连接 ===')

    const auth = getStoredAuth()
    if (!auth.token || !auth.appId) {
      addLog('❌ 认证信息不完整，测试终止')
      setIsConnecting(false)
      return
    }

    const url = '/api/v1/events/stream'
    addLog(`URL: ${url}`)

    const headers: HeadersInit = {
      Accept: 'text/event-stream',
      'Cache-Control': 'no-cache',
      Authorization: `Bearer ${auth.token}`,
      'X-App-Id': auth.appId.toString(),
    }

    addLog(`Headers: ${JSON.stringify(headers, null, 2)}`)

    try {
      addLog('📡 正在发起请求...')
      const response = await fetch(url, {
        method: 'GET',
        headers,
      })

      addLog(`📨 收到响应 | status: ${response.status} | statusText: ${response.statusText}`)
      addLog(`Content-Type: ${response.headers.get('content-type')}`)

      if (!response.ok) {
        const errorText = await response.text()
        addLog(`❌ HTTP 错误 | body: ${errorText}`)
        setIsConnecting(false)
        return
      }

      if (!response.body) {
        addLog('❌ Response body 为空')
        setIsConnecting(false)
        return
      }

      addLog('✅ 连接成功！开始读取事件流...')
      addLog('⏳ 等待事件数据（最多等待 30 秒）...')

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let eventCount = 0
      
      // 设置超时
      const timeout = setTimeout(() => {
        reader.cancel()
        addLog('⏱️  30秒超时，停止连接')
        addLog(`共收到 ${eventCount} 个事件`)
        setIsConnecting(false)
      }, 30000)

      try {
        while (true) {
          const { done, value } = await reader.read()

          if (done) {
            addLog('🔌 连接关闭')
            break
          }

          const chunk = decoder.decode(value, { stream: true })
          buffer += chunk

          const events = buffer.split('\n\n')
          buffer = events.pop() || ''

          for (const event of events) {
            if (event.trim() === '') continue

            const lines = event.split('\n')
            let eventType = ''
            let dataLine = ''

            for (const line of lines) {
              if (line.startsWith('event: ')) {
                eventType = line.slice(7).trim()
              } else if (line.startsWith('data: ')) {
                dataLine = line.slice(6).trim()
              }
            }

            eventCount++

            if (eventType === 'init') {
              addLog(`✅ [事件 ${eventCount}] 初始化消息: ${dataLine}`)
            } else if (eventType === 'event') {
              try {
                const eventData = JSON.parse(dataLine)
                addLog(`📥 [事件 ${eventCount}] ${eventData.eventType} | ${eventData.eventName} | ${dataLine.substring(0, 100)}...`)
              } catch {
                addLog(`📥 [事件 ${eventCount}] ${eventType}: ${dataLine.substring(0, 100)}...`)
              }
            } else {
              addLog(`📩 [事件 ${eventCount}] ${eventType}: ${dataLine.substring(0, 100)}...`)
            }
          }
        }
      } finally {
        clearTimeout(timeout)
      }
    } catch (err) {
      addLog(`❌ 错误: ${err}`)
    } finally {
      setIsConnecting(false)
      addLog('=== 测试结束 ===')
    }
  }

  const sendTestEvent = async () => {
    addLog('=== 发送测试事件 ===')
    
    const auth = getStoredAuth()
    if (!auth.token || !auth.appId) {
      addLog('❌ 认证信息不完整')
      return
    }

    try {
      const response = await fetch('/api/v1/events/collect', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${auth.token}`,
          'X-App-Id': auth.appId.toString(),
        },
        body: JSON.stringify({
          eventName: 'sse_diagnostic_test',
          eventType: 'click',
          sessionId: `diagnostic-${Date.now()}`,
          properties: {
            source: 'SSE诊断工具',
            timestamp: new Date().toISOString(),
          },
        }),
      })

      if (response.ok) {
        addLog('✅ 测试事件发送成功')
      } else {
        const errorText = await response.text()
        addLog(`❌ 发送失败 | status: ${response.status} | body: ${errorText}`)
      }
    } catch (err) {
      addLog(`❌ 发送失败 | error: ${err}`)
    }
  }

  const copyLogs = () => {
    const logsText = logs.join('\n')
    navigator.clipboard.writeText(logsText)
    addLog('📋 日志已复制到剪贴板')
  }

  return (
    <div className="container mx-auto p-6">
      <Card>
        <CardHeader>
          <CardTitle>SSE 事件流诊断工具</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div className="flex flex-wrap gap-2">
              <Button onClick={testAuth} variant="outline">
                1. 测试认证信息
              </Button>
              <Button onClick={testSSEConnection} disabled={isConnecting}>
                {isConnecting ? '连接中...' : '2. 测试 SSE 连接'}
              </Button>
              <Button onClick={sendTestEvent} variant="secondary">
                3. 发送测试事件
              </Button>
              <Button onClick={copyLogs} variant="outline">
                📋 复制日志
              </Button>
              <Button onClick={clearLogs} variant="destructive">
                清空日志
              </Button>
            </div>

            <div className="rounded-md border bg-slate-950 p-4">
              <div className="font-mono text-xs space-y-1 max-h-[600px] overflow-y-auto">
                {logs.length === 0 ? (
                  <div className="text-slate-500">暂无日志</div>
                ) : (
                  logs.map((log, index) => (
                    <div key={index} className="text-slate-200">
                      {log}
                    </div>
                  ))
                )}
              </div>
            </div>

            <div className="text-sm text-slate-600 space-y-2">
              <p>
                <strong>使用说明：</strong>
              </p>
              <ol className="list-decimal list-inside space-y-1">
                <li>先点击"测试认证信息"确认 token 和 appId 存在</li>
                <li>点击"测试 SSE 连接"建立连接，最多等待 30 秒</li>
                <li>如果连接成功但没有事件，点击"发送测试事件"触发一个事件</li>
                <li>使用"复制日志"按钮将结果复制分享给开发者</li>
              </ol>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

