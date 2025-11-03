import { useState, useEffect } from 'react'
import { useAuth } from '@/contexts/AuthContext'
import { analyticsApi } from '@/services/analytics'
import { PathGraph } from '@/components/charts/PathGraph'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Button } from '@/components/ui/button'
import { Loader2 } from 'lucide-react'
import type { PathEdge } from '@/types/analytics'
import { subDays, format } from 'date-fns'

export default function PathAnalysis() {
  const { appId } = useAuth()
  const [data, setData] = useState<PathEdge[]>([])
  const [loading, setLoading] = useState(false)
  const [startTime, setStartTime] = useState(
    format(subDays(new Date(), 7), "yyyy-MM-dd'T'00:00:00")
  )
  const [endTime, setEndTime] = useState(format(new Date(), "yyyy-MM-dd'T'23:59:59"))

  const fetchData = async () => {
    if (!appId) return

    setLoading(true)
    try {
      const response = await analyticsApi.getPath({
        appId,
        startTime,
        endTime,
      })
      setData(response.data)
    } catch (error) {
      console.error('Failed to fetch path data:', error)
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
        <h1 className="text-3xl font-bold">路径分析</h1>
        <p className="text-muted-foreground mt-2">分析用户在不同事件之间的流转路径</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>筛选条件</CardTitle>
          <CardDescription>设置时间范围查询路径数据</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-3 gap-4">
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
          <CardTitle>路径关系图</CardTitle>
          <CardDescription>共 {data.length} 条路径</CardDescription>
        </CardHeader>
        <CardContent>
          <PathGraph data={data} height={500} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>路径详情</CardTitle>
        </CardHeader>
        <CardContent>
          {data.length === 0 ? (
            <div className="flex h-32 items-center justify-center text-muted-foreground">
              暂无路径数据
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b">
                    <th className="px-4 py-2 text-left text-sm font-medium">起点</th>
                    <th className="px-4 py-2 text-left text-sm font-medium">终点</th>
                    <th className="px-4 py-2 text-right text-sm font-medium">流转次数</th>
                  </tr>
                </thead>
                <tbody>
                  {data.map((edge, index) => (
                    <tr key={index} className="border-b hover:bg-muted/50">
                      <td className="px-4 py-3 text-sm">{edge.from}</td>
                      <td className="px-4 py-3 text-sm">{edge.to}</td>
                      <td className="px-4 py-3 text-right text-sm font-medium text-primary">
                        {edge.count.toLocaleString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}