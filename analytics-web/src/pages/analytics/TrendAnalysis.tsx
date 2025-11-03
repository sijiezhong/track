import { useState, useEffect } from 'react'
import { useAuth } from '@/contexts/AuthContext'
import { analyticsApi } from '@/services/analytics'
import { TrendChart } from '@/components/charts/TrendChart'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Button } from '@/components/ui/button'
import { Loader2 } from 'lucide-react'
import type { TrendDataPoint } from '@/types/analytics'
import { subDays, format } from 'date-fns'

export default function TrendAnalysis() {
  const { appId } = useAuth()
  const [data, setData] = useState<TrendDataPoint[]>([])
  const [loading, setLoading] = useState(false)
  const [eventName, setEventName] = useState('pageview')
  const [startTime, setStartTime] = useState(
    format(subDays(new Date(), 7), "yyyy-MM-dd'T'00:00:00")
  )
  const [endTime, setEndTime] = useState(format(new Date(), "yyyy-MM-dd'T'23:59:59"))

  const fetchData = async () => {
    if (!appId) return

    setLoading(true)
    try {
      const response = await analyticsApi.getTrend({
        eventName,
        appId,
        startTime,
        endTime,
        interval: 'daily',
      })
      setData(response.data)
    } catch (error) {
      console.error('Failed to fetch trend data:', error)
      setData([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [appId])

  return (
    <div className="p-6 space-y-6">
      <div>
        <h1 className="text-3xl font-bold">趋势分析</h1>
        <p className="text-muted-foreground mt-2">查看事件随时间变化的趋势</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>筛选条件</CardTitle>
          <CardDescription>设置分析参数并查询数据</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-4 gap-4">
            <div className="space-y-2">
              <Label htmlFor="eventName">事件名称</Label>
              <Input
                id="eventName"
                value={eventName}
                onChange={(e) => setEventName(e.target.value)}
                placeholder="请输入事件名称"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="startTime">开始时间</Label>
              <Input
                id="startTime"
                type="datetime-local"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="endTime">结束时间</Label>
              <Input
                id="endTime"
                type="datetime-local"
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
              />
            </div>
            <div className="flex items-end">
              <Button onClick={fetchData} disabled={loading} className="w-full">
                {loading ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    查询中...
                  </>
                ) : (
                  '查询'
                )}
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>趋势图</CardTitle>
          <CardDescription>
            共 {data.length} 个数据点
            {data.length > 0 &&
              ` · 总计 ${data.reduce((sum, d) => sum + d.count, 0).toLocaleString()} 个事件`}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <TrendChart data={data} height={450} />
        </CardContent>
      </Card>
    </div>
  )
}