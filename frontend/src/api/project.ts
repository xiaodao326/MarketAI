import { get, post, put, del } from './request'
import type {
  Project,
  CreateProjectRequest,
  UpdateProjectRequest,
  AddKeywordsRequest,
  ProjectListResponse,
} from '@/types/project'

const BASE = '/projects'

export const projectApi = {
  create(data: CreateProjectRequest) {
    return post<Project>(BASE, data)
  },

  list(params: { page?: number; size?: number; status?: number; keyword?: string }) {
    return get<ProjectListResponse>(BASE, params as Record<string, unknown>)
  },

  detail(id: number) {
    return get<Project>(`${BASE}/${id}`)
  },

  update(id: number, data: UpdateProjectRequest) {
    return put<Project>(`${BASE}/${id}`, data)
  },

  remove(id: number) {
    return del<void>(`${BASE}/${id}`)
  },

  addKeywords(id: number, data: AddKeywordsRequest) {
    return post<Project>(`${BASE}/${id}/keywords`, data)
  },

  removeKeyword(id: number, keyword: string) {
    return del<Project>(`${BASE}/${id}/keywords/${encodeURIComponent(keyword)}`)
  },
}
