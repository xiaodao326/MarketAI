// 竞品分析相关类型 — 与后端 CompetitorVO / FeatureMatrixVO 字段保持一致

export type CompetitorType = 'direct' | 'indirect' | 'potential'
export type ThreatLevel = 'high' | 'medium' | 'low'
export type FeatureScore = 'yes' | 'partial' | 'no'

export interface Competitor {
  id: number
  projectId: number
  name: string
  logoUrl?: string
  website?: string
  type: CompetitorType
  region: string
  rating: number
  mau?: number
  pricingModel?: string
  fundingStage?: string
  threatLevel: ThreatLevel
  features: Record<string, FeatureScore>
  strengths: string[]
  weaknesses: string[]
  needsUserInput: boolean
  createdAt: string
  updatedAt: string
}

export interface FeatureMatrix {
  features: string[]
  scores: Record<string, FeatureScore[]>
  opportunities: string[]
  hasReport: boolean
}

export type InsightType = 'core_opportunity' | 'pricing_strategy' | 'warning'

export interface DifferentiationInsight {
  type: InsightType
  title: string
  description: string
}

export interface AnalyzeCompetitorsRequest {
  projectId: number
}

export interface AnalyzeCompetitorsResponse {
  taskId: number
  status: string
  estimatedSeconds: number
}

export type TaskStatus = 'pending' | 'running' | 'completed' | 'failed'

export interface CompetitorTaskInfo {
  taskId: number
  status: TaskStatus
  progress: number
  projectId: number
  errorMessage?: string
}

// 部分字段更新
export interface UpdateCompetitorPayload {
  name?: string
  logoUrl?: string
  website?: string
  type?: CompetitorType
  region?: string
  rating?: number
  mau?: number
  pricingModel?: string
  fundingStage?: string
  threatLevel?: ThreatLevel
  features?: Record<string, FeatureScore>
  strengths?: string[]
  weaknesses?: string[]
  needsUserInput?: boolean
}
