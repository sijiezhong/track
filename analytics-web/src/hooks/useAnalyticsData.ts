import { useState, useEffect } from 'react'
import { analyticsApi } from '@/services/analytics'
import type {
  TrendDataPoint,
  PathEdge,
  RetentionDataPoint,
  FunnelData,
  SegmentationData,
  HeatmapData,
} from '@/types/analytics'
import { subDays, format } from 'date-fns'

interface AnalyticsData {
  trend: TrendDataPoint[] | null
  funnel: FunnelData | null
  retention: RetentionDataPoint[] | null
  path: PathEdge[] | null
  segmentation: SegmentationData | null
  heatmap: HeatmapData | null
  loading: boolean
  error: string | null
}

export function useAnalyticsData(appId: number | null) {
  const [data, setData] = useState<AnalyticsData>({
    trend: null,
    funnel: null,
    retention: null,
    path: null,
    segmentation: null,
    heatmap: null,
    loading: false,
    error: null,
  })

  useEffect(() => {
    if (!appId) return

    const fetchAllAnalytics = async () => {
      setData((prev) => ({ ...prev, loading: true, error: null }))

      try {
        // 计算时间范围（最近7天）
        const endTime = format(new Date(), "yyyy-MM-dd'T'HH:mm:ss")
        const startTime = format(subDays(new Date(), 7), "yyyy-MM-dd'T'HH:mm:ss")

        // 并发请求所有分析接口
        const [trendRes, funnelRes, retentionRes, pathRes, segmentationRes, heatmapRes] =
          await Promise.all([
            // 趋势分析
            analyticsApi.getTrend({
              eventName: 'pageview',
              appId,
              startTime,
              endTime,
              interval: 'daily',
            }),

            // 漏斗分析
            analyticsApi.getFunnel({
              steps: 'pageview,click,submit,success',
              appId,
              startTime,
              endTime,
            }),

            // 留存分析
            analyticsApi.getRetention({
              cohortEvent: 'pageview',
              returnEvent: 'pageview',
              day: 1,
              appId,
              startTime,
              endTime,
            }),

            // 路径分析
            analyticsApi.getPath({
              appId,
              startTime,
              endTime,
            }),

            // 分群分析
            analyticsApi.getSegmentation({
              eventName: 'pageview',
              by: 'browser',
              appId,
            }),

            // 热点图
            analyticsApi.getHeatmap({
              eventName: 'click',
              bucket: 'hour',
              appId,
            }),
          ])

        setData({
          trend: trendRes.data,
          funnel: funnelRes.data,
          retention: retentionRes.data,
          path: pathRes.data,
          segmentation: segmentationRes.data,
          heatmap: heatmapRes.data,
          loading: false,
          error: null,
        })
      } catch (err: any) {
        setData((prev) => ({
          ...prev,
          loading: false,
          error: err.message || '获取分析数据失败',
        }))
      }
    }

    fetchAllAnalytics()

    // 每分钟刷新一次数据
    const interval = setInterval(fetchAllAnalytics, 60000)

    return () => clearInterval(interval)
  }, [appId])

  return data
}
