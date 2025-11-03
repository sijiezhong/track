import { useState, useEffect } from 'react'
import { useAuth } from '@/contexts/AuthContext'
import { analyticsApi } from '@/services/analytics'
import { PieChart } from '@/components/charts/PieChart'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Button } from '@/components/ui/button'
import { Loader2 } from 'lucide-react'
import type { SegmentationData } from '@/types/analytics'

export default function SegmentationAnalysis() {
  const { appId } = useAuth()
  const [data, setData] = useState<SegmentationData>({})
  const [loading, setLoading] = useState(false)
  const [eventName, setEventName] = useState('pageview')
  const [by, setBy] = useState('browser')

  const fetchData = async () => {
    if (!appId) return

    setLoading(true)
    try {
      const response = await analyticsApi.getSegmentation({
        eventName,
        by,
        appId,
      })
      setData(response.data)
    } catch (error) {
      console.error('Failed to fetch segmentation data:', error)
      setData({})
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [appId])

  const chartData = Object.entries(data).map(([label, value]) => ({ label, value }))
  const total = chartData.reduce((sum, item) => sum + item.value, 0)

  return (
    <div className="p-6 space-y-6">
      <div>
        <h1 className="text-3xl font-bold">分群分析</h1>
        <p className="text-muted-foreground mt-2">按不同维度对用户进行分组统计</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>筛选条件</CardTitle>
          <CardDescription>设置事件名称和分组维度</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-3 gap-4">
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
              <Label htmlFor="by">分组维度</Label>
              <Input
                id="by"
                value={by}
                onChange={(e) => setBy(e.target.value)}
                placeholder="browser/device/os/referrer"
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

      <div className="grid grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>分布饼图</CardTitle>
            <CardDescription>总计: {total.toLocaleString()} 个事件</CardDescription>
          </CardHeader>
          <CardContent>
            <PieChart data={chartData} height={400} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>分组详情</CardTitle>
          </CardHeader>
          <CardContent>
            {chartData.length === 0 ? (
              <div className="flex h-96 items-center justify-center text-muted-foreground">
                暂无分群数据
              </div>
            ) : (
              <div className="space-y-3">
                {chartData
                  .sort((a, b) => b.value - a.value)
                  .map((item, index) => (
                    <div key={index} className="space-y-2">
                      <div className="flex items-center justify-between text-sm">
                        <span className="font-medium">{item.label}</span>
                        <span className="text-muted-foreground">
                          {item.value.toLocaleString()} ({((item.value / total) * 100).toFixed(2)}%)
                        </span>
                      </div>
                      <div className="h-2 rounded-full bg-muted overflow-hidden">
                        <div
                          className="h-full bg-primary transition-all"
                          style={{ width: `${(item.value / total) * 100}%` }}
                        />
                      </div>
                    </div>
                  ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}