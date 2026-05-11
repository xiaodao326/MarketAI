<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { PlusIcon, MagnifyingGlassIcon, FolderIcon, TrashIcon } from '@heroicons/vue/24/outline'
import ProjectCreateModal from './ProjectCreateModal.vue'
import type { Project } from '@/types/project'

const router = useRouter()
const projectStore = useProjectStore()

const searchKeyword = ref('')
const statusFilter = ref<number | undefined>(undefined)
const showCreateModal = ref(false)
const deletingId = ref<number | null>(null)
const currentPage = ref(1)
const pageSize = 12

const statusOptions = [
  { value: undefined, label: '全部' },
  { value: 0, label: '草稿' },
  { value: 1, label: '活跃' },
  { value: 2, label: '归档' },
]

const industryLabels: Record<string, string> = {
  '企业服务': 'bg-blue-100 text-blue-700',
  '电商零售': 'bg-orange-100 text-orange-700',
  '金融': 'bg-purple-100 text-purple-700',
  '医疗': 'bg-green-100 text-green-700',
  '教育': 'bg-yellow-100 text-yellow-700',
  '制造': 'bg-gray-100 text-gray-700',
}

function statusBadge(status: number) {
  switch (status) {
    case 0: return { label: '草稿', cls: 'bg-gray-100 text-gray-600' }
    case 1: return { label: '活跃', cls: 'bg-green-100 text-green-700' }
    case 2: return { label: '归档', cls: 'bg-yellow-100 text-yellow-700' }
    default: return { label: '未知', cls: 'bg-gray-100 text-gray-600' }
  }
}

function industryClass(industry: string) {
  return industryLabels[industry] || 'bg-gray-100 text-gray-700'
}

const totalPages = computed(() => Math.ceil(projectStore.total / pageSize))

async function loadProjects() {
  await projectStore.fetchList({ page: currentPage.value, size: pageSize, status: statusFilter.value, keyword: searchKeyword.value || undefined })
}

function onSearch() {
  currentPage.value = 1
  loadProjects()
}

function onStatusChange(status: number | undefined) {
  statusFilter.value = status
  currentPage.value = 1
  loadProjects()
}

function goToDetail(project: Project) {
  projectStore.switchProject(project)
  router.push(`/projects/${project.id}`)
}

async function handleDelete(project: Project) {
  if (!confirm(`确定要归档项目「${project.name}」吗？`)) return
  deletingId.value = project.id
  try { await projectStore.remove(project.id) } finally { deletingId.value = null }
}

function handleCreated(project: Project) {
  showCreateModal.value = false
  projectStore.switchProject(project)
  router.push(`/projects/${project.id}`)
}

function goToPage(page: number) { currentPage.value = page; loadProjects() }

onMounted(() => loadProjects().catch(() => {}))
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl font-semibold text-gray-900">分析项目</h2>
      <button @click="showCreateModal = true" class="inline-flex items-center gap-2 px-4 py-2 bg-primary-500 text-white rounded-lg text-sm font-medium hover:bg-primary-600 transition-colors">
        <PlusIcon class="w-4 h-4" />新建项目
      </button>
    </div>

    <div class="flex items-center gap-3 mb-6">
      <div class="relative flex-1 max-w-xs">
        <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input v-model="searchKeyword" type="text" placeholder="搜索项目名称..." class="w-full pl-9 pr-4 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500" @keyup.enter="onSearch" />
      </div>
      <div class="flex items-center gap-1 bg-gray-100 rounded-lg p-0.5">
        <button v-for="opt in statusOptions" :key="opt.value ?? '_all'" @click="onStatusChange(opt.value)" :class="['px-3 py-1.5 text-xs font-medium rounded-md transition-colors', statusFilter === opt.value ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700']">
          {{ opt.label }}
        </button>
      </div>
    </div>

    <div v-if="projectStore.loading" class="flex justify-center py-20">
      <div class="w-8 h-8 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
    </div>

    <div v-else-if="!projectStore.hasProjects" class="rounded-lg border-2 border-dashed border-gray-300 p-16 text-center">
      <FolderIcon class="w-12 h-12 text-gray-400 mx-auto mb-4" />
      <h3 class="text-lg font-medium text-gray-900 mb-2">创建您的第一个分析项目</h3>
      <p class="text-sm text-gray-500 mb-6 max-w-md mx-auto">输入您关注的市场关键词，AI 将自动分析市场趋势、竞争格局和目标用户画像，生成深度洞察报告。</p>
      <button @click="showCreateModal = true" class="inline-flex items-center gap-2 px-6 py-2.5 bg-primary-500 text-white rounded-lg text-sm font-medium hover:bg-primary-600 transition-colors">
        <PlusIcon class="w-4 h-4" />开始新项目
      </button>
    </div>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <div v-for="project in projectStore.projectList" :key="project.id" @click="goToDetail(project)" class="bg-white rounded-lg border border-gray-200 p-5 cursor-pointer hover:shadow-md hover:border-primary-200 transition-all">
        <div class="flex items-start justify-between mb-3">
          <h3 class="font-medium text-gray-900 truncate flex-1 mr-2">{{ project.name }}</h3>
          <span :class="['px-2 py-0.5 text-xs font-medium rounded-full flex-shrink-0', statusBadge(project.status).cls]">{{ statusBadge(project.status).label }}</span>
        </div>
        <div class="flex items-center gap-2 mb-3">
          <span :class="['px-2 py-0.5 text-xs rounded-full', industryClass(project.industry)]">{{ project.industry }}</span>
          <span class="text-xs text-gray-400">{{ project.targetMarket }}</span>
        </div>
        <div class="flex items-center justify-between text-xs text-gray-400">
          <span>{{ project.keywords?.length || 0 }} 个关键词</span>
          <span>{{ new Date(project.createdAt).toLocaleDateString('zh-CN') }}</span>
        </div>
        <div class="mt-4 pt-3 border-t border-gray-100 flex justify-end">
          <button @click.stop="handleDelete(project)" :disabled="deletingId === project.id" class="inline-flex items-center gap-1 text-xs text-gray-400 hover:text-red-500 transition-colors">
            <TrashIcon class="w-3.5 h-3.5" />归档
          </button>
        </div>
      </div>
    </div>

    <div v-if="totalPages > 1" class="flex items-center justify-center gap-2 mt-8">
      <button v-for="p in totalPages" :key="p" @click="goToPage(p)" :class="['w-8 h-8 text-sm rounded-lg transition-colors', p === currentPage ? 'bg-primary-500 text-white' : 'text-gray-600 hover:bg-gray-100']">{{ p }}</button>
    </div>

    <ProjectCreateModal v-if="showCreateModal" @close="showCreateModal = false" @created="handleCreated" />
  </div>
</template>
