import { useState, useEffect } from 'react'
import { useAuth } from '@/contexts/AuthContext'
import { analyticsApi } from '@/services/analytics'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Button } from '@/components/ui/button'
import { Loader2 } from 'lucide-react'
import type { RetentionDataPoint } from '@/types/analytics'
import { subDays, format } from 'date-fns'

export default function RetentionAnalysis() {
  const { appId } = useAuth()
  const [data, setData] = useState<RetentionDataPoint[]>([])
  const [loading, setLoading] = useState(false)
  const [cohortEvent, setCohortEvent] = useState('pageview')
  const [returnEvent, setReturnEvent] = useState('pageview')
  const [day, setDay] = useState(1)
  const [startTime, setStartTime] = useState(
    format(subDays(new Date(), 30), "yyyy-MM-dd'T'00:00:00")
  )
  const [endTime, setEndTime] = useState(format(new Date(), "yyyy-MM-dd'T'23:59:59"))

  const fetchData = async () => {
    if (!appId) return

    setLoading(true)
    try {
      const response = await analyticsApi.getRetention({
        cohortEvent,
        returnEvent,
        day,
        appId,
        startTime,
        endTime,
      })
      setData(response.data)
    } catch (error) {
      console.error('Failed to fetch retention data:', error)
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
        <h1 className="text-3xl font-bold">留存分析</h1>
        <p className="text-muted-foreground mt-2">分析用户在指定天数后的留存情况</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>筛选条件</CardTitle>
          <CardDescription>设置留存事件和时间范围</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-5 gap-4">
            <div className="space-y-2">
              <Label htmlFor="cohortEvent">初始事件</Label>
              <Input
                id="cohortEvent"
                value={cohortEvent}
                onChange={(e) => setCohortEvent(e.target.value)}
                placeholder="如: pageview"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="returnEvent">回访事件</Label>
              <Input
                id="returnEvent"
                value={returnEvent}
                onChange={(e) => setReturnEvent(e.target.value)}
                placeholder="如: pageview"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="day">留存天数</Label>
              <Input
                id="day"
                type="number"
                value={day}
                onChange={(e) => setDay(parseInt(e.target.value))}
                min="1"
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
          <CardTitle>留存数据</CardTitle>
          <CardDescription>
            {data.length > 0
              ? `共 ${data.length} 个队列 · 平均留存率 ${((data.reduce((sum, d) => sum + d.rate, 0) / data.length) * 100).toFixed(2)}%`
              : '暂无数据'}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {data.length === 0 ? (
            <div className="flex h-64 items-center justify-center text-muted-foreground">
              暂无留存数据
            </div>
          ) : (
            <div className="space-y-4">
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b">
                      <th className="px-4 py-2 text-left text-sm font-medium">队列日期</th>
                      <th className="px-4 py-2 text-right text-sm font-medium">初始用户</th>
                      <th className="px-4 py-2 text-right text-sm font-medium">留存用户</th>
                      <th className="px-4 py-2 text-right text-sm font-medium">留存率</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.map((row, index) => (
                      <tr key={index} className="border-b hover:bg-muted/50">
                        <td className="px-4 py-3 text-sm">{row.cohortDate}</td>
                        <td className="px-4 py-3 text-right text-sm">{row.cohort.toLocaleString()}</td>
                        <td className="px-4 py-3 text-right text-sm">{row.retained.toLocaleString()}</td>
                        <td className="px-4 py-3 text-right text-sm font-medium text-primary">
                          {(row.rate * 100).toFixed(2)}%
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}