export interface MetricCard {
  current: number
  delta: number
  trend: 'up' | 'down' | 'stable'
}

export interface Metrics {
  searchHeatIndex: MetricCard
  socialMentions: MetricCard
  competitorActiveCount: MetricCard
  sentimentScore: MetricCard
}

export interface TrendSeries {
  name: string
  data: number[]
  yaxisIndex: number
}

export interface TrendData {
  dates: string[]
  series: TrendSeries[]
}

export interface KeywordRank {
  rank: number
  keyword: string
  volume: number
  deltaPercent: number
}

export interface Anomaly {
  id: string
  severity: 'critical' | 'warning' | 'opportunity'
  title: string
  description: string
  source: string
  occurredAt: string
}
