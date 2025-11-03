import { useState, useEffect } from 'react'
import { useAuth } from '@/contexts/AuthContext'
import { analyticsApi } from '@/services/analytics'
import { FunnelChart } from '@/components/charts/FunnelChart'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Button } from '@/components/ui/button'
import { Loader2 } from 'lucide-react'
import type { FunnelData } from '@/types/analytics'
import { subDays, format } from 'date-fns'

export default function FunnelAnalysis() {
  const { appId } = useAuth()
  const [data, setData] = useState<FunnelData>({ steps: [], counts: [], conversionRates: [] })
  const [loading, setLoading] = useState(false)
  const [steps, setSteps] = useState('pageview,click,submit,success')
  const [startTime, setStartTime] = useState(
    format(subDays(new Date(), 7), "yyyy-MM-dd'T'00:00:00")
  )
  const [endTime, setEndTime] = useState(format(new Date(), "yyyy-MM-dd'T'23:59:59"))

  const fetchData = async () => {
    if (!appId) return

    setLoading(true)
    try {
      const response = await analyticsApi.getFunnel({
        steps,
        appId,
        startTime,
        endTime,
      })
      setData(response.data)
    } catch (error) {
      console.error('Failed to fetch funnel data:', error)
      setData({ steps: [], counts: [], conversionRates: [] })
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
        <h1 className="text-3xl font-bold">漏斗分析</h1>
        <p className="text-muted-foreground mt-2">分析用户在各个步骤的转化情况</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>筛选条件</CardTitle>
          <CardDescription>设置漏斗步骤和时间范围</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-4 gap-4">
            <div className="space-y-2">
              <Label htmlFor="steps">漏斗步骤</Label>
              <Input
                id="steps"
                value={steps}
                onChange={(e) => setSteps(e.target.value)}
                placeholder="用逗号分隔，如: a,b,c"
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
          <CardTitle>漏斗图</CardTitle>
          <CardDescription>
            {data.steps.length > 0 && data.counts.length > 0
              ? `整体转化率: ${data.counts[0] > 0 ? ((data.counts[data.counts.length - 1] / data.counts[0]) * 100).toFixed(2) : 0}%`
              : '暂无数据'}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <FunnelChart data={data} height={450} />
        </CardContent>
      </Card>
    </div>
  )
}