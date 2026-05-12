<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { ArrowLeftIcon, PencilIcon, TrashIcon, Cog6ToothIcon, ChartBarIcon, LightBulbIcon, UserGroupIcon, BuildingOffice2Icon } from '@heroicons/vue/24/outline'
import ProjectEditModal from './projects/ProjectEditModal.vue'
import type { Project } from '@/types/project'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()

const project = ref<Project | null>(null)
const loading = ref(true)
const activeTab = ref('insight')
const showEditModal = ref(false)

const id = computed(() => Number(route.params.id))

const tabs = [
  { key: 'dashboard', label: '趋势仪表盘', icon: ChartBarIcon },
  { key: 'insight', label: 'AI 洞察', icon: LightBulbIcon },
  { key: 'persona', label: '用户画像', icon: UserGroupIcon },
  { key: 'competitor', label: '竞品分析', icon: BuildingOffice2Icon },
]

const statusBadge = (status: number) => {
  switch (status) {
    case 0: return { label: '草稿', cls: 'bg-gray-100 text-gray-600' }
    case 1: return { label: '活跃', cls: 'bg-green-100 text-green-700' }
    case 2: return { label: '归档', cls: 'bg-yellow-100 text-yellow-700' }
    default: return { label: '未知', cls: 'bg-gray-100 text-gray-600' }
  }
}

const industryLabels: Record<string, string> = {
  '企业服务': 'bg-blue-100 text-blue-700',
  '电商零售': 'bg-orange-100 text-orange-700',
  '金融': 'bg-purple-100 text-purple-700',
  '医疗': 'bg-green-100 text-green-700',
  '教育': 'bg-yellow-100 text-yellow-700',
  '制造': 'bg-gray-100 text-gray-700',
}

async function loadProject() {
  loading.value = true
  try {
    project.value = await projectStore.fetchDetail(id.value)
  } catch {
    router.replace('/projects')
  } finally {
    loading.value = false
  }
}

async function handleArchive() {
  if (!project.value || !confirm(`确定要归档项目「${project.value.name}」吗？`)) return
  await projectStore.remove(project.value.id)
  router.push('/projects')
}

function handleUpdated(updated: Project) {
  project.value = updated
  showEditModal.value = false
}

onMounted(() => loadProject())
</script>

<template>
  <div v-if="loading" class="flex justify-center py-20">
    <div class="w-8 h-8 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
  </div>

  <template v-else-if="project">
    <!-- 顶部信息卡片 -->
    <div class="bg-white rounded-lg border border-gray-200 p-6 mb-6">
      <div class="flex items-start justify-between">
        <div class="flex items-start gap-4">
          <button @click="router.push('/projects')" class="mt-0.5 p-1 text-gray-400 hover:text-gray-600 transition-colors">
            <ArrowLeftIcon class="w-5 h-5" />
          </button>
          <div>
            <div class="flex items-center gap-3 mb-1">
              <h1 class="text-xl font-semibold text-gray-900">{{ project.name }}</h1>
              <span :class="['px-2.5 py-0.5 text-xs font-medium rounded-full', statusBadge(project.status).cls]">{{ statusBadge(project.status).label }}</span>
            </div>
            <p v-if="project.description" class="text-sm text-gray-500 mb-3">{{ project.description }}</p>
            <div class="flex items-center gap-2 flex-wrap">
              <span :class="['px-2.5 py-0.5 text-xs rounded-full', industryLabels[project.industry] || 'bg-gray-100 text-gray-700']">{{ project.industry }}</span>
              <span class="text-xs text-gray-400">{{ project.targetMarket }}</span>
              <span class="text-xs text-gray-400">·</span>
              <span class="text-xs text-gray-400">{{ project.keywords?.length || 0 }} 个关键词</span>
              <span class="text-xs text-gray-400">·</span>
              <span class="text-xs text-gray-400">创建于 {{ new Date(project.createdAt).toLocaleDateString('zh-CN') }}</span>
            </div>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <button @click="showEditModal = true" class="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition-colors" title="编辑"><PencilIcon class="w-4 h-4" /></button>
          <button @click="handleArchive" class="p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors" title="归档"><TrashIcon class="w-4 h-4" /></button>
        </div>
      </div>
      <!-- 关键词标签 -->
      <div class="flex flex-wrap gap-1.5 mt-4 ml-9">
        <span v-for="kw in project.keywords" :key="kw" class="px-2.5 py-1 bg-primary-50 text-primary-700 text-xs rounded-full">{{ kw }}</span>
      </div>
    </div>

    <!-- Tab 导航 -->
    <div class="border-b border-gray-200 mb-6">
      <nav class="flex gap-6 -mb-px">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          @click="tab.key === 'dashboard' ? router.push(`/projects/${id}/dashboard`) : (activeTab = tab.key)"
          :class="[
            'inline-flex items-center gap-2 px-1 py-3 text-sm font-medium border-b-2 transition-colors',
            activeTab === tab.key
              ? 'border-primary-500 text-primary-600'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300',
          ]"
        >
          <component :is="tab.icon" class="w-4 h-4" />
          {{ tab.label }}
        </button>
      </nav>
    </div>

    <!-- Tab 内容 -->
    <div class="rounded-lg border-2 border-dashed border-gray-300 p-16 text-center">
      <component :is="tabs.find(t => t.key === activeTab)?.icon || Cog6ToothIcon" class="w-10 h-10 text-gray-400 mx-auto mb-3" />
      <p class="text-gray-500 text-sm">{{ tabs.find(t => t.key === activeTab)?.label }}功能开发中，敬请期待</p>
    </div>
    <ProjectEditModal v-if="showEditModal && project" :project="project" @close="showEditModal = false" @updated="handleUpdated" />
  </template>
</template>
