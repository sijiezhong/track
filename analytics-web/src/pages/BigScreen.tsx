import { useNavigate } from 'react-router-dom'
import { useAuth } from '@/contexts/AuthContext'
import { useEventStream } from '@/hooks/useEventStream'
import { useAnalyticsData } from '@/hooks/useAnalyticsData'
import { ROUTES } from '@/constants/routes'
import { config } from '@/config'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import {
  TrendingUp,
  Filter,
  Users,
  GitBranch,
  PieChart,
  Activity,
  LogOut,
  Eye,
  Zap,
  Loader2,
} from 'lucide-react'
import { useMemo, useState, useEffect } from 'react'
import { analyticsApi } from '@/services/analytics'
import type { StatsData } from '@/types/analytics'

export default function BigScreen() {
  const navigate = useNavigate()
  const { appId, logout, user } = useAuth()
  const { events, isConnected } = useEventStream(appId)
  const analyticsData = useAnalyticsData(appId)
  const [statsData, setStatsData] = useState<StatsData | null>(null)

  const handleLogout = () => {
    logout()
    navigate(ROUTES.LOGIN)
  }

  // 获取统计数据
  useEffect(() => {
    if (!appId) return

    const fetchStats = async () => {
      try {
        const response = await analyticsApi.getStats()
        setStatsData(response.data)
      } catch (error) {
        console.error('Failed to fetch stats:', error)
      }
    }

    fetchStats()
    // 定期刷新统计数据（每30秒）
    const interval = setInterval(fetchStats, 30000)
    return () => clearInterval(interval)
  }, [appId])

  // 统计数据（使用真实接口数据）
  const stats = useMemo(() => {
    // 实时事件流的计算（用于右侧实时事件面板）
    const realTimeRate = events.length > 0 ? (events.filter((e) => {
        if (!e?.timestamp) return false
        const eventTime = new Date(e.timestamp).getTime()
        const now = Date.now()
        return now - eventTime < 60000 // 1分钟内的事件
    }).length) : 0

    // 如果有真实统计数据，使用真实数据；否则使用默认值
    return {
      totalEvents: statsData?.totalEvents || 0,
      todayEvents: statsData?.todayEvents || 0,
      uniqueUsers: statsData?.uniqueUsers || 0,
      pageViews: statsData?.pageViews || 0,
      clicks: statsData?.clicks || 0,
      onlineUsers: statsData?.onlineUsers || 0,
      todayVsYesterdayPercent: statsData?.todayVsYesterdayPercent || 0,
      realTimeRate,
    }
  }, [events, statsData])

  // 从真实接口获取报表数据
  const analyticsModules = useMemo(() => {
    // 趋势分析数据处理
    const trendData = analyticsData.trend || []
    const trendCurrent = trendData.length > 0 ? trendData[trendData.length - 1]?.count || 0 : 0
    const trendPrev = trendData.length > 1 ? trendData[trendData.length - 2]?.count || 1 : 1
    const trendChange = trendPrev > 0 ? (((trendCurrent - trendPrev) / trendPrev) * 100).toFixed(1) : 0
    const trendTrend = Number(trendChange) >= 0 ? 'up' : 'down'

    // 漏斗分析数据处理
    const funnelData = analyticsData.funnel || { steps: [], counts: [], conversionRates: [] }
    const funnelSteps = funnelData.steps || []
    const funnelValues = funnelData.counts || []
    const funnelConversion =
      funnelValues.length > 1 && funnelValues[0] > 0
        ? ((funnelValues[funnelValues.length - 1] / funnelValues[0]) * 100).toFixed(1)
        : '0'

    // 留存分析数据处理
    const retentionData = analyticsData.retention || []
    const retentionRate =
      retentionData.length > 0 && retentionData[0]?.rate
        ? (retentionData[0].rate * 100).toFixed(1)
        : '0'
    const retentionCohort = retentionData.length > 0 ? retentionData[0]?.cohort || 0 : 0
    const retentionRetained = retentionData.length > 0 ? retentionData[0]?.retained || 0 : 0

    // 路径分析数据处理
    const pathData = analyticsData.path || []
    const totalPaths = pathData.length
    const topPath =
      pathData.length > 0 ? `${pathData[0].from} → ${pathData[0].to}` : '暂无路径数据'
    const uniqueNodes = new Set(pathData.flatMap((p) => [p.from, p.to])).size
    const avgSteps = pathData.length > 0 ? (pathData.reduce((sum, p) => sum + p.count, 0) / pathData.length).toFixed(1) : '0'

    // 分群分析数据处理
    const segmentationRaw = analyticsData.segmentation || {}
    const segmentationEntries = Object.entries(segmentationRaw).sort((a, b) => b[1] - a[1])
    const segmentationTotal = segmentationEntries.reduce((sum, [, val]) => sum + val, 0)
    const segmentationData =
      segmentationTotal > 0
        ? segmentationEntries.map(([label, value]) => ({
            label,
            value: Number(((value / segmentationTotal) * 100).toFixed(1)),
          }))
        : []
    const topSegment = segmentationData.length > 0 ? segmentationData[0].label : 'N/A'
    const topValue = segmentationData.length > 0 ? segmentationData[0].value : 0

    // 热点图数据处理
    const heatmapRaw = analyticsData.heatmap || {}
    const heatmapEntries = Object.entries(heatmapRaw).map(([hour, count]) => ({
      hour: parseInt(hour, 10),
      count,
    }))
    const peakEntry = heatmapEntries.reduce(
      (max, entry) => (entry.count > max.count ? entry : max),
      { hour: 0, count: 0 }
    )
    const avgValue =
      heatmapEntries.length > 0
        ? Math.floor(heatmapEntries.reduce((sum, e) => sum + e.count, 0) / heatmapEntries.length)
        : 0
    const activeHours = heatmapEntries.filter((e) => e.count > avgValue * 0.5).length

    return [
      {
        title: '趋势分析',
        icon: TrendingUp,
        path: ROUTES.ANALYTICS.TREND,
        description: '查看事件趋势变化',
        data: {
          current: trendCurrent,
          change: Number(trendChange),
          trend: trendTrend,
          chartData: trendData.map((point) => ({
            date: point.date.substring(5, 10), // MM-DD
            value: point.count,
          })),
        },
      },
      {
        title: '漏斗分析',
        icon: Filter,
        path: ROUTES.ANALYTICS.FUNNEL,
        description: '分析用户转化流程',
        data: {
          steps: funnelSteps,
          values: funnelValues,
          conversionRate: Number(funnelConversion),
        },
      },
      {
        title: '留存分析',
        icon: Users,
        path: ROUTES.ANALYTICS.RETENTION,
        description: '查看用户留存情况',
        data: {
          rate: Number(retentionRate),
          cohort: retentionCohort,
          retained: retentionRetained,
          change: 0, // 需要对比历史数据才能计算变化
        },
      },
      {
        title: '路径分析',
        icon: GitBranch,
        path: ROUTES.ANALYTICS.PATH,
        description: '分析用户行为路径',
        data: {
          totalPaths,
          topPath,
          pathCount: uniqueNodes,
          avgSteps: Number(avgSteps),
        },
      },
      {
        title: '分群分析',
        icon: PieChart,
        path: ROUTES.ANALYTICS.SEGMENTATION,
        description: '按维度分组统计',
        data: {
          segments: segmentationData.slice(0, 4), // 只显示前4个
          total: segmentationTotal,
          topSegment,
          topValue,
        },
      },
      {
        title: '热点图',
        icon: Activity,
        path: ROUTES.ANALYTICS.HEATMAP,
        description: '查看事件热点分布',
        data: {
          peakHour: peakEntry.hour,
          peakValue: peakEntry.count,
          avgValue,
          activeHours,
          hourlyData: heatmapEntries,
        },
      },
    ]
  }, [analyticsData])

  return (
    <div className="fixed inset-0 overflow-hidden bg-gradient-to-br from-[#0a0e27] via-[#1a1f3a] to-[#0a0e27] text-white">
      {/* 顶部栏 */}
      <header className="flex items-center justify-between border-b border-cyan-500/20 bg-slate-900/30 px-6 py-4 backdrop-blur-md">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-3">
            <div className="rounded-lg bg-gradient-to-br from-cyan-400 to-blue-500 p-2">
              <Activity className="h-5 w-5 text-white" />
            </div>
            <div>
              <h1 className="text-2xl font-bold bg-gradient-to-r from-cyan-400 to-blue-400 bg-clip-text text-transparent">
                {config.appTitle}
              </h1>
              <span className="text-xs text-cyan-400/70">数据大屏实时监控</span>
            </div>
          </div>
        </div>
        <div className="flex items-center gap-6">
          <div className="flex items-center gap-2 rounded-lg bg-cyan-500/10 px-4 py-2 border border-cyan-500/20">
            <div className="h-2 w-2 animate-pulse rounded-full bg-green-400"></div>
            <span className="text-sm text-cyan-300">实时事件:</span>
            <span className="text-lg font-bold text-green-400">{stats.realTimeRate}/分钟</span>
          </div>
          {user && (
            <div className="flex items-center gap-2 text-sm text-slate-300">
              <Users className="h-4 w-4" />
              <span>{user.username}</span>
            </div>
          )}
          <Button
            variant="outline"
            size="sm"
            onClick={handleLogout}
            className="border-red-500/30 bg-red-500/10 text-red-400 hover:bg-red-500/20"
          >
            <LogOut className="mr-2 h-4 w-4" />
            退出
          </Button>
        </div>
      </header>

      <div className="flex h-[calc(100vh-73px)]">
        {/* 主内容区 */}
        <div className="flex flex-1 flex-col gap-6 p-6 overflow-auto">
          {/* 统计卡片 */}
          <div className="grid grid-cols-4 gap-4">
            <Card className="border-cyan-500/20 bg-gradient-to-br from-cyan-500/10 to-cyan-500/5 backdrop-blur-sm">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-cyan-300">今日事件</CardTitle>
                <Zap className="h-4 w-4 text-cyan-400" />
              </CardHeader>
              <CardContent>
                <div className="text-3xl font-bold text-cyan-400">{stats.todayEvents}</div>
                <p className="text-xs text-slate-400 mt-1">
                  较昨日 {stats.todayVsYesterdayPercent >= 0 ? '+' : ''}{stats.todayVsYesterdayPercent.toFixed(1)}%
                </p>
              </CardContent>
            </Card>

            <Card className="border-blue-500/20 bg-gradient-to-br from-blue-500/10 to-blue-500/5 backdrop-blur-sm">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-blue-300">总事件数</CardTitle>
                <Activity className="h-4 w-4 text-blue-400" />
              </CardHeader>
              <CardContent>
                <div className="text-3xl font-bold text-blue-400">{stats.totalEvents}</div>
                <p className="text-xs text-slate-400 mt-1">累计事件总数</p>
              </CardContent>
            </Card>

            <Card className="border-purple-500/20 bg-gradient-to-br from-purple-500/10 to-purple-500/5 backdrop-blur-sm">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-purple-300">独立用户</CardTitle>
                <Users className="h-4 w-4 text-purple-400" />
              </CardHeader>
              <CardContent>
                <div className="text-3xl font-bold text-purple-400">{stats.uniqueUsers}</div>
                <p className="text-xs text-slate-400 mt-1">在线: {stats.onlineUsers}</p>
              </CardContent>
            </Card>

            <Card className="border-green-500/20 bg-gradient-to-br from-green-500/10 to-green-500/5 backdrop-blur-sm">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-green-300">页面访问</CardTitle>
                <Eye className="h-4 w-4 text-green-400" />
              </CardHeader>
              <CardContent>
                <div className="text-3xl font-bold text-green-400">{stats.pageViews}</div>
                <p className="text-xs text-slate-400 mt-1">点击: {stats.clicks}</p>
              </CardContent>
            </Card>
          </div>

          {/* 数据加载状态 */}
          {analyticsData.loading && (
            <div className="flex items-center justify-center gap-2 rounded-lg border border-cyan-500/20 bg-cyan-500/10 p-4">
              <Loader2 className="h-5 w-5 animate-spin text-cyan-400" />
              <span className="text-cyan-300">正在加载分析数据...</span>
            </div>
          )}

          {/* 数据错误状态 */}
          {analyticsData.error && (
            <div className="rounded-lg border border-red-500/20 bg-red-500/10 p-4 text-red-300">
              获取数据失败: {analyticsData.error}
            </div>
          )}

          {/* 分析模块卡片 */}
          <div className="grid grid-cols-3 gap-6">
            {analyticsModules.map((module, index) => {
              const Icon = module.icon
              const colors = [
                'from-cyan-500/20 to-blue-500/10 border-cyan-500/30',
                'from-blue-500/20 to-purple-500/10 border-blue-500/30',
                'from-purple-500/20 to-pink-500/10 border-purple-500/30',
                'from-green-500/20 to-emerald-500/10 border-green-500/30',
                'from-orange-500/20 to-red-500/10 border-orange-500/30',
                'from-indigo-500/20 to-violet-500/10 border-indigo-500/30',
              ]
              const iconColors = [
                'text-cyan-400 bg-cyan-500/20',
                'text-blue-400 bg-blue-500/20',
                'text-purple-400 bg-purple-500/20',
                'text-green-400 bg-green-500/20',
                'text-orange-400 bg-orange-500/20',
                'text-indigo-400 bg-indigo-500/20',
              ]
              const titleColors = [
                'text-cyan-300',
                'text-blue-300',
                'text-purple-300',
                'text-green-300',
                'text-orange-300',
                'text-indigo-300',
              ]

              return (
                <Card
                  key={module.path}
                  className={`cursor-pointer border bg-gradient-to-br ${colors[index]} transition-all hover:scale-[1.02] hover:shadow-xl hover:shadow-cyan-500/20 backdrop-blur-sm group`}
                  onClick={() => navigate(module.path)}
                >
                  <CardHeader className="pb-3">
                    <div className="flex items-center justify-between mb-3">
                      <div className="flex items-center gap-3">
                        <div className={`rounded-xl p-2.5 ${iconColors[index]} transition-transform group-hover:scale-110`}>
                          <Icon className="h-6 w-6" />
                        </div>
                        <div>
                          <CardTitle className={`text-lg font-bold ${titleColors[index]}`}>
                            {module.title}
                          </CardTitle>
                          <p className="text-xs text-slate-400">{module.description}</p>
                        </div>
                      </div>
                      <div className="text-xl font-bold text-slate-600 group-hover:text-cyan-400 transition-colors">
                        →
                      </div>
                    </div>
                  </CardHeader>
                  <CardContent className="pt-0">
                    {/* 趋势分析卡片 */}
                    {module.title === '趋势分析' && (
                      <div className="space-y-3">
                        {module.data.chartData.length === 0 ? (
                          <div className="flex h-32 items-center justify-center text-sm text-slate-500">
                            暂无趋势数据
                          </div>
                        ) : (
                          <>
                            <div className="flex items-baseline gap-2">
                              <span className="text-3xl font-bold text-cyan-400">{module.data.current}</span>
                              {module.data.change !== 0 && (
                                <span
                                  className={`text-sm font-medium ${module.data.trend === 'up' ? 'text-green-400' : 'text-red-400'}`}
                                >
                                  {module.data.trend === 'up' ? '↑' : '↓'} {Math.abs(module.data.change)}%
                                </span>
                              )}
                            </div>
                            <div className="h-20 flex items-end gap-1">
                              {module.data.chartData.map((point, i) => {
                                const maxValue = Math.max(...module.data.chartData.map((p) => p.value))
                                const height = maxValue > 0 ? (point.value / maxValue) * 100 : 0
                                return (
                                  <div
                                    key={i}
                                    className="flex-1 rounded-t bg-gradient-to-t from-cyan-500/40 to-cyan-500/20 transition-all hover:from-cyan-500/60 hover:to-cyan-500/40"
                                    style={{ height: `${height}%` }}
                                    title={`${point.date}: ${point.value}`}
                                  />
                                )
                              })}
                            </div>
                            <div className="text-xs text-slate-400">最近7天趋势</div>
                          </>
                        )}
                      </div>
                    )}

                    {/* 漏斗分析卡片 */}
                    {module.title === '漏斗分析' && (
                      <div className="space-y-3">
                        {module.data.steps.length === 0 ? (
                          <div className="flex h-32 items-center justify-center text-sm text-slate-500">
                            暂无漏斗数据
                          </div>
                        ) : (
                          <>
                            <div className="space-y-2">
                              {module.data.steps.map((step, i) => {
                                const value = module.data.values[i] || 0
                                const prevValue = i > 0 ? module.data.values[i - 1] || 1 : value || 1
                                const maxValue = Math.max(...module.data.values)
                                const width = maxValue > 0 ? (value / maxValue) * 100 : 0
                                const conversion = i > 0 && prevValue > 0 ? ((value / prevValue) * 100).toFixed(1) : 100

                                return (
                                  <div key={i} className="space-y-1">
                                    <div className="flex items-center justify-between text-xs">
                                      <span className="text-slate-300">{step}</span>
                                      <span className="text-blue-400 font-medium">{value.toLocaleString()}</span>
                                    </div>
                                    <div className="h-2 rounded-full bg-slate-700/50 overflow-hidden">
                                      <div
                                        className="h-full bg-gradient-to-r from-blue-500 to-purple-500 transition-all"
                                        style={{ width: `${width}%` }}
                                      />
                                    </div>
                                    {i > 0 && (
                                      <div className="text-xs text-slate-500">转化率: {conversion}%</div>
                                    )}
                                  </div>
                                )
                              })}
                            </div>
                            <div className="pt-2 border-t border-slate-700/50">
                              <div className="text-sm text-slate-300">
                                整体转化率: <span className="text-blue-400 font-bold">{module.data.conversionRate}%</span>
                              </div>
                            </div>
                          </>
                        )}
                      </div>
                    )}

                    {/* 留存分析卡片 */}
                    {module.title === '留存分析' && (
                      <div className="space-y-3">
                        {module.data.cohort === 0 ? (
                          <div className="flex h-32 items-center justify-center text-sm text-slate-500">
                            暂无留存数据
                          </div>
                        ) : (
                          <>
                            <div className="flex items-baseline gap-2">
                              <span className="text-4xl font-bold text-purple-400">{module.data.rate}%</span>
                              {module.data.change !== 0 && (
                                <span className="text-sm text-green-400">↑ {module.data.change}%</span>
                              )}
                            </div>
                            <div className="space-y-2 pt-2">
                              <div className="flex items-center justify-between text-sm">
                                <span className="text-slate-400">初始用户</span>
                                <span className="text-purple-300 font-medium">
                                  {module.data.cohort.toLocaleString()}
                                </span>
                              </div>
                              <div className="flex items-center justify-between text-sm">
                                <span className="text-slate-400">留存用户</span>
                                <span className="text-purple-400 font-bold">
                                  {module.data.retained.toLocaleString()}
                                </span>
                              </div>
                            </div>
                            <div className="pt-2 border-t border-slate-700/50">
                              <div className="h-2 rounded-full bg-slate-700/50 overflow-hidden">
                                <div
                                  className="h-full bg-gradient-to-r from-purple-500 to-pink-500"
                                  style={{ width: `${module.data.rate}%` }}
                                />
                              </div>
                            </div>
                          </>
                        )}
                      </div>
                    )}

                    {/* 路径分析卡片 */}
                    {module.title === '路径分析' && (
                      <div className="space-y-3">
                        {module.data.totalPaths === 0 ? (
                          <div className="flex h-32 items-center justify-center text-sm text-slate-500">
                            暂无路径数据
                          </div>
                        ) : (
                          <>
                            <div className="text-3xl font-bold text-green-400">{module.data.totalPaths}</div>
                            <div className="space-y-2 pt-2">
                              <div className="text-sm text-slate-300">热门路径:</div>
                              <div className="rounded-lg bg-slate-800/50 p-2 text-xs text-green-300 font-mono truncate">
                                {module.data.topPath}
                              </div>
                            </div>
                            <div className="grid grid-cols-2 gap-2 pt-2 border-t border-slate-700/50">
                              <div>
                                <div className="text-xs text-slate-400">路径数量</div>
                                <div className="text-lg font-bold text-green-400">{module.data.pathCount}</div>
                              </div>
                              <div>
                                <div className="text-xs text-slate-400">平均步数</div>
                                <div className="text-lg font-bold text-green-300">{module.data.avgSteps}</div>
                              </div>
                            </div>
                          </>
                        )}
                      </div>
                    )}

                    {/* 分群分析卡片 */}
                    {module.title === '分群分析' && (
                      <div className="space-y-3">
                        {module.data.segments.length === 0 ? (
                          <div className="flex h-32 items-center justify-center text-sm text-slate-500">
                            暂无分群数据
                          </div>
                        ) : (
                          <>
                            <div className="flex items-center justify-between">
                              <span className="text-2xl font-bold text-orange-400">{module.data.topValue}%</span>
                              <span className="text-sm text-slate-400">{module.data.topSegment}</span>
                            </div>
                            <div className="space-y-2 pt-2">
                              {module.data.segments.map((segment, i) => (
                                <div key={i} className="space-y-1">
                                  <div className="flex items-center justify-between text-xs">
                                    <span className="text-slate-300">{segment.label}</span>
                                    <span className="text-orange-400 font-medium">{segment.value}%</span>
                                  </div>
                                  <div className="h-1.5 rounded-full bg-slate-700/50 overflow-hidden">
                                    <div
                                      className="h-full bg-gradient-to-r from-orange-500 to-red-500"
                                      style={{ width: `${segment.value}%` }}
                                    />
                                  </div>
                                </div>
                              ))}
                            </div>
                          </>
                        )}
                      </div>
                    )}

                    {/* 热点图卡片 */}
                    {module.title === '热点图' && (
                      <div className="space-y-3">
                        {module.data.peakValue === 0 ? (
                          <div className="flex h-32 items-center justify-center text-sm text-slate-500">
                            暂无热点数据
                          </div>
                        ) : (
                          <>
                            <div className="flex items-baseline gap-2">
                              <span className="text-3xl font-bold text-indigo-400">{module.data.peakValue}</span>
                              <span className="text-sm text-slate-400">峰值</span>
                            </div>
                            <div className="space-y-2 pt-2">
                              <div className="flex items-center justify-between text-sm">
                                <span className="text-slate-400">高峰时段</span>
                                <span className="text-indigo-400 font-bold">{module.data.peakHour}:00</span>
                              </div>
                              <div className="flex items-center justify-between text-sm">
                                <span className="text-slate-400">平均数值</span>
                                <span className="text-indigo-300">{module.data.avgValue}</span>
                              </div>
                              <div className="flex items-center justify-between text-sm">
                                <span className="text-slate-400">活跃时段</span>
                                <span className="text-indigo-300">{module.data.activeHours}小时</span>
                              </div>
                            </div>
                            <div className="pt-2 border-t border-slate-700/50">
                              <div className="flex gap-1">
                                {Array.from({ length: 24 }, (_, i) => {
                                  const hourData = module.data.hourlyData?.find((h: any) => h.hour === i)
                                  const value = hourData?.count || 0
                                  const maxValue = Math.max(
                                    ...(module.data.hourlyData?.map((h: any) => h.count) || [1])
                                  )
                                  const height = maxValue > 0 ? (value / maxValue) * 100 : 0
                                  const isPeak = i === module.data.peakHour

                                  return (
                                    <div
                                      key={i}
                                      className={`flex-1 rounded-t transition-all ${
                                        isPeak
                                          ? 'bg-gradient-to-t from-indigo-500 to-violet-500'
                                          : value > 0
                                            ? 'bg-indigo-500/30'
                                            : 'bg-slate-700/20'
                                      }`}
                                      style={{ height: `${Math.max(height * 0.6, 4)}px` }}
                                      title={`${i}:00 - ${value}`}
                                    />
                                  )
                                })}
                              </div>
                              <div className="text-xs text-slate-500 mt-1">24小时分布</div>
                            </div>
                          </>
                        )}
                      </div>
                    )}
                  </CardContent>
                </Card>
              )
            })}
          </div>
        </div>

        {/* 右侧事件流面板 */}
        <div className="w-80 border-l border-cyan-500/20 bg-slate-900/30 backdrop-blur-md p-4">
          <div className="mb-4 flex items-center gap-2">
            <div className="h-2 w-2 animate-pulse rounded-full bg-green-400"></div>
            <h2 className="text-lg font-semibold bg-gradient-to-r from-cyan-400 to-blue-400 bg-clip-text text-transparent">
              实时事件流
            </h2>
            <span className="ml-auto rounded-full bg-cyan-500/20 px-2 py-1 text-xs text-cyan-400">
              {events.length}
            </span>
          </div>
          <div className="flex h-[calc(100vh-220px)] flex-col gap-2 overflow-y-auto scrollbar-thin scrollbar-thumb-cyan-500/30 scrollbar-track-transparent">
            {events.length === 0 ? (
              <div className="flex h-full flex-col items-center justify-center text-sm text-slate-500">
                <Activity className={`mb-2 h-8 w-8 ${isConnected ? 'text-green-400' : 'animate-pulse text-slate-600'}`} />
                <p>{isConnected ? '连接成功，等待事件数据...' : '等待事件数据...'}</p>
                <p className="text-xs text-slate-600 mt-1">{isConnected ? '已连接，请触发一些事件' : '连接中...'}</p>
              </div>
            ) : (
              events.map((event, index) => {
                const eventTypeColors: Record<string, string> = {
                  pageview: 'border-blue-500/30 bg-blue-500/10 text-blue-300',
                  click: 'border-green-500/30 bg-green-500/10 text-green-300',
                  error: 'border-red-500/30 bg-red-500/10 text-red-300',
                  custom: 'border-purple-500/30 bg-purple-500/10 text-purple-300',
                }
                const colorClass = eventTypeColors[event.eventType] || 'border-cyan-500/30 bg-cyan-500/10 text-cyan-300'

                return (
                  <div
                    key={`${event.eventId}-${index}`}
                    className={`rounded-lg border ${colorClass} p-3 text-sm backdrop-blur-sm transition-all hover:scale-[1.02] animate-in slide-in-from-right`}
                    style={{ animationDelay: `${index * 50}ms` }}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <span className="font-semibold">{event.eventType}</span>
                      <span className="text-xs text-slate-500">
                        {new Date(event.timestamp).toLocaleTimeString('zh-CN', { 
                          hour: '2-digit', 
                          minute: '2-digit', 
                          second: '2-digit',
                          hour12: false 
                        })}
                      </span>
                    </div>
                    {event.userId && (
                      <div className="mb-1 text-xs text-slate-400">
                        用户: {event.userId.substring(0, 8)}...
                      </div>
                    )}
                    <div className="text-xs text-slate-400 line-clamp-2">
                      {event.eventContent && Object.keys(event.eventContent).length > 0
                        ? JSON.stringify(event.eventContent).substring(0, 60) + '...'
                        : '无附加数据'}
                    </div>
                    {event.pageUrl && (
                      <div className="mt-1 text-xs text-slate-500 truncate">
                        {event.pageUrl}
                      </div>
                    )}
                  </div>
                )
              })
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
