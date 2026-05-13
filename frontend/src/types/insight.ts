// AI 需求洞察相关类型 — 与后端 InsightReportVO / TaskStatusVO 保持字段一致

export interface InsightDimensions {
  demandStrength: number
  competitionSaturation: number
  growthPotential: number
  entryBarrier: number
}

export interface PainPoint {
  title: string
  severity: 'high' | 'medium' | 'low'
  description: string
  evidence?: string[]
}

export interface Opportunity {
  name: string
  type: 'high_value' | 'blue_ocean' | 'differentiation'
  description: string
  tags?: string[]
}

export interface Risk {
  name: string
  level: 'high' | 'medium' | 'low'
  description: string
}

export interface ActionItem {
  title: string
  description: string
}

export interface InsightReport {
  id: number
  projectId: number
  title: string
  marketFitScore: number
  dimensions: InsightDimensions
  painPoints: PainPoint[]
  opportunities: Opportunity[]
  risks: Risk[]
  actions: ActionItem[]
  aiModel: string
  tokensUsed: number
  createdAt: string
  completedAt: string
}

export interface InsightReportSummary {
  id: number
  title: string
  marketFitScore: number
  aiModel: string
  tokensUsed: number
  createdAt: string
  completedAt: string
}

export type InsightDepth = 'lite' | 'standard' | 'full'

export interface CreateInsightRequest {
  projectId: number
  productDescription: string
  depth?: InsightDepth
}

export interface CreateInsightResponse {
  taskId: number
  status: string
  estimatedSeconds: number
}

export type TaskStatus = 'pending' | 'running' | 'completed' | 'failed'

export interface TaskStatusInfo {
  taskId: number
  status: TaskStatus
  progress: number
  resultId?: number
  errorMessage?: string
}

// MyBatis-Plus Page 响应结构
export interface MpPage<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
