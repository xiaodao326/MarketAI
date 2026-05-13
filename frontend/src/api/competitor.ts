import { get, post, put } from './request'
import type {
  Competitor,
  FeatureMatrix,
  DifferentiationInsight,
  AnalyzeCompetitorsRequest,
  AnalyzeCompetitorsResponse,
  CompetitorTaskInfo,
  UpdateCompetitorPayload,
} from '@/types/competitor'

export const competitorApi = {
  analyze: (data: AnalyzeCompetitorsRequest) =>
    post<AnalyzeCompetitorsResponse>('/competitors/analyze', data),

  getTaskStatus: (taskId: number) =>
    get<CompetitorTaskInfo>(`/competitors/tasks/${taskId}`),

  listByProject: (projectId: number) =>
    get<Competitor[]>(`/competitors/project/${projectId}`),

  getById: (id: number) =>
    get<Competitor>(`/competitors/${id}`),

  update: (id: number, data: UpdateCompetitorPayload) =>
    put<Competitor>(`/competitors/${id}`, data),

  getMatrix: (projectId: number) =>
    get<FeatureMatrix>(`/competitors/project/${projectId}/matrix`),

  getInsights: (projectId: number) =>
    get<DifferentiationInsight[]>(`/competitors/project/${projectId}/insights`),
}
