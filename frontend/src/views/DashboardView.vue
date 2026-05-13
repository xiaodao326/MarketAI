<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useProjectStore } from '@/stores/project'
import {
  PlusIcon, ArrowRightIcon, FolderIcon,
  ChartBarIcon, LightBulbIcon, UserGroupIcon, BuildingOffice2Icon,
} from '@heroicons/vue/24/outline'
import type { Project } from '@/types/project'

const router = useRouter()
const userStore = useUserStore()
const projectStore = useProjectStore()

// 加载最近 6 个项目用于首页展示
onMounted(() => {
  if (projectStore.projectList.length === 0) {
    projectStore.fetchList({ size: 6 }).catch(() => {})
  }
})

const recentProjects = computed(() => projectStore.projectList.slice(0, 6))

const stats = computed(() => {
  const list = projectStore.projectList
  return {
    total:    projectStore.total || list.length,
    active:   list.filter(p => p.status === 1).length,
    draft:    list.filter(p => p.status === 0).length,
    archived: list.filter(p => p.status === 2).length,
  }
})

const statusBadge = (status: number) => {
  switch (status) {
    case 0: return { label: '草稿', cls: 'bg-gray-100 text-gray-600' }
    case 1: return { label: '活跃', cls: 'bg-green-100 text-green-700' }
    case 2: return { label: '归档', cls: 'bg-yellow-100 text-yellow-700' }
    default: return { label: '未知', cls: 'bg-gray-100 text-gray-600' }
  }
}

const industryClass = (industry: string) => {
  const map: Record<string, string> = {
    '企业服务': 'bg-blue-100 text-blue-700',
    '电商零售': 'bg-orange-100 text-orange-700',
    '金融':     'bg-purple-100 text-purple-700',
    '医疗':     'bg-green-100 text-green-700',
    '教育':     'bg-yellow-100 text-yellow-700',
    '制造':     'bg-gray-100 text-gray-700',
    SaaS:       'bg-blue-100 text-blue-700',
  }
  return map[industry] || 'bg-gray-100 text-gray-700'
}

function goToDetail(project: Project) {
  projectStore.switchProject(project)
  router.push(`/projects/${project.id}`)
}

// 4 个能力快捷入口 — 引导用户认识可用功能
const FEATURE_SHORTCUTS = [
  { label: '趋势仪表盘', icon: ChartBarIcon,         iconCls: 'text-blue-500',    desc: '搜索热度 / 社媒讨论' },
  { label: 'AI 需求洞察', icon: LightBulbIcon,       iconCls: 'text-amber-500',   desc: '痛点 / 机会 / 风险' },
  { label: '用户画像',   icon: UserGroupIcon,        iconCls: 'text-purple-500',  desc: '3-5 个目标画像' },
  { label: '竞品分析',   icon: BuildingOffice2Icon,  iconCls: 'text-emerald-500', desc: '对比矩阵 / 差异化' },
]
</script>

<template>
  <div>
    <!-- 欢迎横栏 -->
    <div class="flex flex-wrap items-end justify-between gap-3 mb-6">
      <div>
        <h2 class="text-2xl font-semibold text-gray-900 dark:text-white">
          欢迎，{{ userStore.userInfo?.nickname || '用户' }}
        </h2>
        <p class="text-gray-500 dark:text-gray-400 mt-1">AI 驱动的市场需求分析平台</p>
      </div>
      <button
        v-if="projectStore.hasProjects"
        class="inline-flex items-center gap-1.5 px-4 py-2 bg-primary-500 text-white rounded-lg text-sm font-medium hover:bg-primary-600 transition-colors"
        @click="router.push('/projects')"
      >
        <PlusIcon class="w-4 h-4" />新建项目
      </button>
    </div>

    <!-- 加载态 -->
    <div
      v-if="projectStore.loading && !projectStore.projectList.length"
      class="flex justify-center py-20"
    >
      <div class="w-8 h-8 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
    </div>

    <!-- 空态:无项目 -->
    <div
      v-else-if="!projectStore.hasProjects"
      class="rounded-lg border-2 border-dashed border-gray-300 dark:border-gray-600 p-12 text-center"
    >
      <FolderIcon class="w-12 h-12 text-gray-400 mx-auto mb-4" />
      <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-2">
        创建第一个分析项目
      </h3>
      <p class="text-sm text-gray-500 dark:text-gray-400 mb-6 max-w-md mx-auto">
        选择一个市场关键词，AI 将自动分析市场趋势、竞争格局和目标用户画像，生成深度洞察报告。
      </p>
      <button
        class="px-6 py-2.5 bg-primary-500 text-white rounded-lg text-sm font-medium hover:bg-primary-600 transition-colors"
        @click="router.push('/projects')"
      >
        开始新项目
      </button>
    </div>

    <!-- 主内容:有项目 -->
    <template v-else>
      <!-- 统计卡片 -->
      <div class="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
        <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
          <p class="text-xs text-gray-500 dark:text-gray-400">
            全部项目
          </p>
          <p class="text-2xl font-semibold text-gray-900 dark:text-white mt-1">
            {{ stats.total }}
          </p>
        </div>
        <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
          <p class="text-xs text-gray-500 dark:text-gray-400">
            活跃中
          </p>
          <p class="text-2xl font-semibold text-emerald-600 dark:text-emerald-400 mt-1">
            {{ stats.active }}
          </p>
        </div>
        <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
          <p class="text-xs text-gray-500 dark:text-gray-400">
            草稿
          </p>
          <p class="text-2xl font-semibold text-gray-700 dark:text-gray-300 mt-1">
            {{ stats.draft }}
          </p>
        </div>
        <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
          <p class="text-xs text-gray-500 dark:text-gray-400">
            已归档
          </p>
          <p class="text-2xl font-semibold text-yellow-600 dark:text-yellow-400 mt-1">
            {{ stats.archived }}
          </p>
        </div>
      </div>

      <!-- 最近项目 -->
      <div class="mb-6">
        <div class="flex items-baseline justify-between mb-3">
          <h3 class="text-base font-semibold text-gray-900 dark:text-white">
            最近项目
          </h3>
          <button
            class="inline-flex items-center gap-0.5 text-sm text-primary-600 hover:text-primary-700 transition-colors"
            @click="router.push('/projects')"
          >
            查看全部<ArrowRightIcon class="w-3.5 h-3.5" />
          </button>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div
            v-for="project in recentProjects"
            :key="project.id"
            class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-5 cursor-pointer hover:shadow-md hover:border-primary-200 dark:hover:border-primary-700 transition-all"
            @click="goToDetail(project)"
          >
            <div class="flex items-start justify-between mb-3 gap-2">
              <h4 class="font-medium text-gray-900 dark:text-white truncate flex-1">
                {{ project.name }}
              </h4>
              <span :class="['px-2 py-0.5 text-xs font-medium rounded-full flex-shrink-0', statusBadge(project.status).cls]">{{ statusBadge(project.status).label }}</span>
            </div>
            <div class="flex items-center gap-2 mb-3 flex-wrap">
              <span :class="['px-2 py-0.5 text-xs rounded-full', industryClass(project.industry)]">{{ project.industry }}</span>
              <span class="text-xs text-gray-400">{{ project.targetMarket }}</span>
            </div>
            <div class="flex items-center justify-between text-xs text-gray-400">
              <span>{{ project.keywords?.length || 0 }} 个关键词 · {{ project.competitors?.length || 0 }} 个竞品</span>
              <span>{{ new Date(project.createdAt).toLocaleDateString('zh-CN') }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 能力快捷入口 -->
      <div>
        <h3 class="text-base font-semibold text-gray-900 dark:text-white mb-3">
          可用能力
        </h3>
        <div class="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <div
            v-for="f in FEATURE_SHORTCUTS"
            :key="f.label"
            class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4 flex items-center gap-3"
          >
            <component
              :is="f.icon"
              :class="['w-8 h-8 flex-shrink-0', f.iconCls]"
            />
            <div class="min-w-0">
              <p class="text-sm font-medium text-gray-900 dark:text-white truncate">
                {{ f.label }}
              </p>
              <p class="text-xs text-gray-500 dark:text-gray-400 truncate">
                {{ f.desc }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
