<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import {
  ArrowLeftIcon, PencilIcon, TrashIcon,
  ChartBarIcon, LightBulbIcon, UserGroupIcon, BuildingOffice2Icon,
  ArrowRightIcon,
} from '@heroicons/vue/24/outline'
import ProjectEditModal from './projects/ProjectEditModal.vue'
import type { Project } from '@/types/project'
import type { Component } from 'vue'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()

const project = ref<Project | null>(null)
const loading = ref(true)
const showEditModal = ref(false)

const id = computed(() => Number(route.params.id))

// 功能卡片配置 — available=true 才可点击进入,false 仅展示"敬请期待"
interface FeatureCard {
  key: string
  label: string
  desc: string
  icon: Component
  available: boolean
  route?: string
  // tailwind 颜色对 — 用于 icon 背景与悬停效果区分
  bg: string
  iconCls: string
}

const features = computed<FeatureCard[]>(() => [
  {
    key: 'dashboard', label: '趋势仪表盘',
    desc: '搜索热度、社媒讨论、竞品活跃度等核心指标的实时趋势',
    icon: ChartBarIcon, available: true,
    route: `/projects/${id.value}/dashboard`,
    bg: 'bg-blue-50 dark:bg-blue-900/20', iconCls: 'text-blue-500',
  },
  {
    key: 'insight', label: 'AI 需求洞察',
    desc: '一键生成痛点、机会、风险、行动建议的结构化分析报告',
    icon: LightBulbIcon, available: true,
    route: `/projects/${id.value}/insights`,
    bg: 'bg-amber-50 dark:bg-amber-900/20', iconCls: 'text-amber-500',
  },
  {
    key: 'persona', label: '用户画像',
    desc: 'AI 自动生成 3-5 个目标用户画像,含决策参数与代表性引言',
    icon: UserGroupIcon, available: true,
    route: `/projects/${id.value}/personas`,
    bg: 'bg-purple-50 dark:bg-purple-900/20', iconCls: 'text-purple-500',
  },
  {
    key: 'competitor', label: '竞品分析',
    desc: 'AI 生成功能对比矩阵 + 差异化建议,帮你找到市场切入点',
    icon: BuildingOffice2Icon, available: true,
    route: `/projects/${id.value}/competitors`,
    bg: 'bg-emerald-50 dark:bg-emerald-900/20', iconCls: 'text-emerald-500',
  },
])

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

function openFeature(f: FeatureCard) {
  if (f.available && f.route) router.push(f.route)
}

onMounted(() => loadProject())
</script>

<template>
  <div
    v-if="loading"
    class="flex justify-center py-20"
  >
    <div class="w-8 h-8 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
  </div>

  <template v-else-if="project">
    <!-- 顶部信息卡片 -->
    <div class="bg-white rounded-lg border border-gray-200 p-6 mb-6">
      <div class="flex items-start justify-between">
        <div class="flex items-start gap-4">
          <button
            class="mt-0.5 p-1 text-gray-400 hover:text-gray-600 transition-colors"
            @click="router.push('/projects')"
          >
            <ArrowLeftIcon class="w-5 h-5" />
          </button>
          <div>
            <div class="flex items-center gap-3 mb-1">
              <h1 class="text-xl font-semibold text-gray-900">{{ project.name }}</h1>
              <span :class="['px-2.5 py-0.5 text-xs font-medium rounded-full', statusBadge(project.status).cls]">{{ statusBadge(project.status).label }}</span>
            </div>
            <p
              v-if="project.description"
              class="text-sm text-gray-500 mb-3"
            >{{ project.description }}</p>
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
          <button
            class="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
            title="编辑"
            @click="showEditModal = true"
          ><PencilIcon class="w-4 h-4" /></button>
          <button
            class="p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
            title="归档"
            @click="handleArchive"
          ><TrashIcon class="w-4 h-4" /></button>
        </div>
      </div>
      <!-- 关键词标签 -->
      <div class="flex flex-wrap gap-1.5 mt-4 ml-9">
        <span
          v-for="kw in project.keywords"
          :key="kw"
          class="px-2.5 py-1 bg-primary-50 text-primary-700 text-xs rounded-full"
        >{{ kw }}</span>
      </div>
    </div>

    <!-- 功能卡片网格 -->
    <div class="mb-3">
      <h2 class="text-sm font-medium text-gray-500 dark:text-gray-400">功能模块</h2>
    </div>
    <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
      <button
        v-for="f in features"
        :key="f.key"
        :disabled="!f.available"
        :class="[
          'group flex items-start gap-4 p-5 rounded-xl border bg-white dark:bg-gray-800 text-left transition-all',
          f.available
            ? 'border-gray-200 dark:border-gray-700 hover:border-primary-300 dark:hover:border-primary-700 hover:shadow-md cursor-pointer'
            : 'border-gray-200 dark:border-gray-700 opacity-60 cursor-not-allowed',
        ]"
        @click="openFeature(f)"
      >
        <div :class="['w-11 h-11 rounded-lg flex items-center justify-center flex-shrink-0', f.bg]">
          <component
            :is="f.icon"
            :class="['w-6 h-6', f.iconCls]"
          />
        </div>
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2 mb-1">
            <h3 class="text-base font-medium text-gray-900 dark:text-white">{{ f.label }}</h3>
            <span
              v-if="!f.available"
              class="px-1.5 py-0.5 text-[10px] bg-gray-100 dark:bg-gray-700 text-gray-500 rounded"
            >敬请期待</span>
          </div>
          <p class="text-sm text-gray-500 dark:text-gray-400 leading-relaxed">{{ f.desc }}</p>
        </div>
        <ArrowRightIcon
          v-if="f.available"
          class="w-4 h-4 text-gray-300 group-hover:text-primary-500 group-hover:translate-x-0.5 transition-all flex-shrink-0 mt-1"
        />
      </button>
    </div>

    <ProjectEditModal
      v-if="showEditModal && project"
      :project="project"
      @close="showEditModal = false"
      @updated="handleUpdated"
    />
  </template>
</template>
