import { useState, useEffect } from 'react'
import { useAuth } from '@/contexts/AuthContext'
import { analyticsApi } from '@/services/analytics'
import { HeatmapChart } from '@/components/charts/HeatmapChart'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Button } from '@/components/ui/button'
import { Loader2 } from 'lucide-react'
import type { HeatmapData } from '@/types/analytics'

export default function HeatmapAnalysis() {
  const { appId } = useAuth()
  const [data, setData] = useState<HeatmapData>({})
  const [loading, setLoading] = useState(false)
  const [eventName, setEventName] = useState('click')

  const fetchData = async () => {
    if (!appId) return

    setLoading(true)
    try {
      const response = await analyticsApi.getHeatmap({
        eventName,
        bucket: 'hour',
        appId,
      })
      setData(response.data)
    } catch (error) {
      console.error('Failed to fetch heatmap data:', error)
      setData({})
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [appId])

  const total = Object.values(data).reduce((sum, count) => sum + count, 0)
  const peak = Object.entries(data).reduce(
    (max, [hour, count]) => (count > max.count ? { hour, count } : max),
    { hour: '0', count: 0 }
  )

  return (
    <div className="p-6 space-y-6">
      <div>
        <h1 className="text-3xl font-bold">热点图分析</h1>
        <p className="text-muted-foreground mt-2">查看事件在不同时段的热度分布</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>筛选条件</CardTitle>
          <CardDescription>设置事件名称</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="eventName">事件名称</Label>
              <Input
                id="eventName"
                value={eventName}
                onChange={(e) => setEventName(e.target.value)}
                placeholder="请输入事件名称"
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

      <div className="grid grid-cols-3 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>总事件数</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-4xl font-bold text-primary">{total.toLocaleString()}</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>峰值时段</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-4xl font-bold text-primary">{peak.hour}:00</div>
            <p className="text-sm text-muted-foreground mt-2">{peak.count.toLocaleString()} 个事件</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>平均值</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-4xl font-bold text-primary">
              {Object.keys(data).length > 0 ? Math.floor(total / Object.keys(data).length).toLocaleString() : 0}
            </div>
            <p className="text-sm text-muted-foreground mt-2">每小时平均</p>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>24小时热力分布</CardTitle>
          <CardDescription>按小时统计事件数量</CardDescription>
        </CardHeader>
        <CardContent>
          <HeatmapChart data={data} height={400} />
        </CardContent>
      </Card>
    </div>
  )
}