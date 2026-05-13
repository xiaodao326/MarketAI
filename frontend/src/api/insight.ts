import { get, post } from './request'
import type {
  CreateInsightRequest,
  CreateInsightResponse,
  InsightReport,
  InsightReportSummary,
  MpPage,
  TaskStatusInfo,
} from '@/types/insight'

export const insightApi = {
  createInsight: (data: CreateInsightRequest) =>
    post<CreateInsightResponse>('/insights', data),

  getTaskStatus: (taskId: number) =>
    get<TaskStatusInfo>(`/insights/tasks/${taskId}`),

  getReport: (reportId: number) =>
    get<InsightReport>(`/insights/${reportId}`),

  getReportList: (projectId: number, page = 1, size = 20) =>
    get<MpPage<InsightReportSummary>>(`/insights/project/${projectId}`, { page, size }),
}
