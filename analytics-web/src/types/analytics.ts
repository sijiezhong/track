import type {
  TrendDataPoint,
  PathEdge,
  RetentionDataPoint,
  FunnelData,
  SegmentationData,
  HeatmapData,
} from './event'

export type { TrendDataPoint, PathEdge, RetentionDataPoint, FunnelData, SegmentationData, HeatmapData }

export interface AnalyticsQueryParams {
  eventName?: string
  startTime?: string
  endTime?: string
  appId: number
}

export interface TrendQueryParams extends AnalyticsQueryParams {
  eventName: string
  interval?: string
}

export interface PathQueryParams extends AnalyticsQueryParams {}

export interface RetentionQueryParams extends AnalyticsQueryParams {
  cohortEvent: string
  returnEvent: string
  day?: number
}

export interface FunnelQueryParams extends AnalyticsQueryParams {
  steps: string
  windowDays?: number
}

export interface SegmentationQueryParams extends AnalyticsQueryParams {
  eventName: string
  by: string
}

export interface HeatmapQueryParams extends AnalyticsQueryParams {
  eventName: string
  bucket?: string
}

export interface StatsData {
  totalEvents: number
  todayEvents: number
  yesterdayEvents: number
  uniqueUsers: number
  onlineUsers: number
  pageViews: number
  clicks: number
  todayVsYesterdayPercent: number
}
