import { get, post, put, del } from './request'
import type {
  Persona,
  GeneratePersonaRequest,
  CreatePersonaTaskResponse,
  PersonaTaskInfo,
  CreatePersonaPayload,
  UpdatePersonaPayload,
} from '@/types/persona'

export const personaApi = {
  generate: (data: GeneratePersonaRequest) =>
    post<CreatePersonaTaskResponse>('/personas/generate', data),

  getTaskStatus: (taskId: number) =>
    get<PersonaTaskInfo>(`/personas/tasks/${taskId}`),

  listByProject: (projectId: number) =>
    get<Persona[]>(`/personas/project/${projectId}`),

  getById: (id: number) =>
    get<Persona>(`/personas/${id}`),

  create: (data: CreatePersonaPayload) =>
    post<Persona>('/personas', data),

  update: (id: number, data: UpdatePersonaPayload) =>
    put<Persona>(`/personas/${id}`, data),

  remove: (id: number) =>
    del<void>(`/personas/${id}`),
}
