import { http } from './http'
import type { ApiResponse } from '@/types/api'
import type {
  TrendDataPoint,
  PathEdge,
  RetentionDataPoint,
  FunnelData,
  SegmentationData,
  HeatmapData,
  TrendQueryParams,
  PathQueryParams,
  RetentionQueryParams,
  FunnelQueryParams,
  SegmentationQueryParams,
  HeatmapQueryParams,
  StatsData,
} from '@/types/analytics'

export const analyticsApi = {
  // 趋势分析
  getTrend: (params: TrendQueryParams): Promise<ApiResponse<TrendDataPoint[]>> => {
    return http.get('/events/trend', { params })
  },

  // 路径分析
  getPath: (params: PathQueryParams): Promise<ApiResponse<PathEdge[]>> => {
    return http.get('/events/path', { params })
  },

  // 留存分析
  getRetention: (params: RetentionQueryParams): Promise<ApiResponse<RetentionDataPoint[]>> => {
    return http.get('/events/retention', { params })
  },

  // 漏斗分析
  getFunnel: (params: FunnelQueryParams): Promise<ApiResponse<FunnelData>> => {
    return http.get('/events/funnel', { params })
  },

  // 分群分析
  getSegmentation: (params: SegmentationQueryParams): Promise<ApiResponse<SegmentationData>> => {
    return http.get('/events/segmentation', { params })
  },

  // 热点图
  getHeatmap: (params: HeatmapQueryParams): Promise<ApiResponse<HeatmapData>> => {
    return http.get('/events/heatmap', { params })
  },

  // 统计汇总
  getStats: (): Promise<ApiResponse<StatsData>> => {
    return http.get('/events/stats')
  },
}
