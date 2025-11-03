export interface Event {
  eventId: string
  eventType: string
  projectId: string
  userId?: string
  sessionId: string
  anonymousId: string
  eventContent: Record<string, any>
  timestamp: string
  channel?: string
  device?: string
  browser?: string
  os?: string
  ip?: string
  ua?: string
  referrer?: string
  pageUrl?: string
  appId?: number
  extra?: Record<string, any>
}

export interface TrendDataPoint {
  date: string
  count: number
}

export interface PathEdge {
  from: string
  to: string
  count: number
}

export interface RetentionDataPoint {
  cohortDate: string
  cohort: number
  retained: number
  rate: number
}

export interface FunnelData {
  steps: string[]
  counts: number[]
  conversionRates: number[]
}

export interface SegmentationData {
  [key: string]: number
}

export interface HeatmapData {
  [hour: string]: number
}
