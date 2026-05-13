// 用户画像相关类型 — 与后端 PersonaVO / PersonaTaskStatusVO 字段保持一致

export interface PersonaPainPoint {
  title: string
  description: string
}

export type PaymentLevel = 'high' | 'medium' | 'low'

export interface PersonaDecisionParams {
  paymentWillingness: PaymentLevel
  decisionCycle: string   // "1-2 周" / "2-4 周" / "2-6 月"
  budgetRange: string     // "0.5-2 万/年" / "3-8 万/年" / "10-50 万/年"
  techCapability: PaymentLevel
}

export interface Persona {
  id: number
  projectId: number
  name: string
  role: string
  ageRange: string
  marketShare: number
  isPrimary: boolean
  goals: string[]
  painPoints: PersonaPainPoint[]
  quote: string
  decisionParams: PersonaDecisionParams
  createdAt: string
  updatedAt: string
}

export interface GeneratePersonaRequest {
  projectId: number
  count: number       // 3-5
  context?: string
}

export interface CreatePersonaTaskResponse {
  taskId: number
  status: string
  estimatedSeconds: number
}

export type PersonaTaskStatus = 'pending' | 'running' | 'completed' | 'failed'

export interface PersonaTaskInfo {
  taskId: number
  status: PersonaTaskStatus
  progress: number
  projectId: number
  errorMessage?: string
}

// 手动创建画像
export interface CreatePersonaPayload {
  projectId: number
  name: string
  role: string
  ageRange: string
  marketShare?: number
  isPrimary?: boolean
  goals?: string[]
  painPoints?: PersonaPainPoint[]
  quote?: string
  decisionParams?: PersonaDecisionParams
}

// 更新画像 (所有字段可选)
export type UpdatePersonaPayload = Partial<Omit<CreatePersonaPayload, 'projectId'>>
