import { get } from './request'
import type { Metrics, TrendData, KeywordRank, Anomaly } from '@/types/dashboard'

export const dashboardApi = {
  getMetrics: (projectId: number, timeRange: string) =>
    get<Metrics>(`/dashboard/${projectId}/metrics`, { timeRange }),

  getTrend: (projectId: number, timeRange: string) =>
    get<TrendData>(`/dashboard/${projectId}/trend`, { timeRange }),

  getTopKeywords: (projectId: number, limit = 10) =>
    get<KeywordRank[]>(`/dashboard/${projectId}/keywords/top`, { limit }),

  getAnomalies: (projectId: number, limit = 10) =>
    get<Anomaly[]>(`/dashboard/${projectId}/anomalies`, { limit }),
}
