import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { projectApi } from '@/api/project'
import type { Project, CreateProjectRequest, UpdateProjectRequest, AddKeywordsRequest } from '@/types/project'

export const useProjectStore = defineStore('project', () => {
  const currentProject = ref<Project | null>(null)
  const projectList = ref<Project[]>([])
  const total = ref(0)
  const loading = ref(false)

  const hasProjects = computed(() => projectList.value.length > 0)

  async function fetchList(params: { page?: number; size?: number; status?: number; keyword?: string } = {}) {
    loading.value = true
    try {
      const res = await projectApi.list({ page: 1, size: 12, ...params })
      projectList.value = res.records
      total.value = res.total
    } finally {
      loading.value = false
    }
  }

  async function fetchDetail(id: number) {
    const project = await projectApi.detail(id)
    currentProject.value = project
    return project
  }

  async function create(data: CreateProjectRequest) {
    const project = await projectApi.create(data)
    return project
  }

  async function update(id: number, data: UpdateProjectRequest) {
    const project = await projectApi.update(id, data)
    if (currentProject.value?.id === id) {
      currentProject.value = project
    }
    return project
  }

  async function remove(id: number) {
    await projectApi.remove(id)
    if (currentProject.value?.id === id) {
      currentProject.value = null
    }
    projectList.value = projectList.value.filter((p) => p.id !== id)
  }

  async function addKeywords(id: number, data: AddKeywordsRequest) {
    const project = await projectApi.addKeywords(id, data)
    if (currentProject.value?.id === id) {
      currentProject.value = project
    }
    return project
  }

  async function removeKeyword(id: number, keyword: string) {
    const project = await projectApi.removeKeyword(id, keyword)
    if (currentProject.value?.id === id) {
      currentProject.value = project
    }
    return project
  }

  function switchProject(project: Project) {
    currentProject.value = project
  }

  return {
    currentProject,
    projectList,
    total,
    loading,
    hasProjects,
    fetchList,
    fetchDetail,
    create,
    update,
    remove,
    addKeywords,
    removeKeyword,
    switchProject,
  }
})
