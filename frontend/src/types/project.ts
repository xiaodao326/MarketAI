import type { PageResult } from './index'

export interface Project {
  id: number
  userId: number
  name: string
  description: string | null
  industry: string
  targetMarket: string
  keywords: string[]
  competitors: string[]
  status: number // 0=草稿, 1=活跃, 2=归档
  createdAt: string
  updatedAt: string
}

export interface CreateProjectRequest {
  name: string
  description?: string
  industry: string
  targetMarket: string
  keywords: string[]
  competitors?: string[]
}

export interface UpdateProjectRequest {
  name?: string
  description?: string
  industry?: string
  targetMarket?: string
  keywords?: string[]
  competitors?: string[]
}

export interface AddKeywordsRequest {
  keywords: string[]
}

export type ProjectListResponse = PageResult<Project>
