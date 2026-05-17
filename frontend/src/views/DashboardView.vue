<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useProjectStore } from '@/stores/project'
import {
  PlusIcon, ArrowRightIcon, FolderIcon, FolderOpenIcon, ArchiveBoxIcon, PencilSquareIcon,
  ChartBarIcon, LightBulbIcon, UserGroupIcon, BuildingOffice2Icon, SparklesIcon,
} from '@heroicons/vue/24/outline'
import type { Project } from '@/types/project'
import AppCard from '@/components/ui/AppCard.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppEmpty from '@/components/ui/AppEmpty.vue'
import AppSkeleton from '@/components/ui/AppSkeleton.vue'

const router = useRouter()
const userStore = useUserStore()
const projectStore = useProjectStore()

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

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6)  return '夜深了'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const statusBadge = (status: number): { label: string; tone: 'neutral' | 'success' | 'warning' } => {
  switch (status) {
    case 0: return { label: '草稿', tone: 'neutral' }
    case 1: return { label: '活跃', tone: 'success' }
    case 2: return { label: '归档', tone: 'warning' }
    default: return { label: '未知', tone: 'neutral' }
  }
}

function goToDetail(project: Project) {
  projectStore.switchProject(project)
  router.push(`/projects/${project.id}`)
}

const STAT_CARDS = computed(() => [
  { key: 'total',    label: '全部项目', value: stats.value.total,    icon: FolderIcon,      iconBg: 'from-brand-500/15 to-brand-500/5',     iconColor: 'text-brand-600' },
  { key: 'active',   label: '活跃中',   value: stats.value.active,   icon: FolderOpenIcon,  iconBg: 'from-emerald-500/15 to-emerald-500/5', iconColor: 'text-emerald-600' },
  { key: 'draft',    label: '草稿',     value: stats.value.draft,    icon: PencilSquareIcon, iconBg: 'from-sky-500/15 to-sky-500/5',         iconColor: 'text-sky-600' },
  { key: 'archived', label: '已归档',   value: stats.value.archived, icon: ArchiveBoxIcon,  iconBg: 'from-amber-500/15 to-amber-500/5',     iconColor: 'text-amber-600' },
])

const FEATURE_SHORTCUTS = [
  { label: '趋势仪表盘',  icon: ChartBarIcon,         iconColor: 'text-sky-500',     iconBg: 'bg-sky-50 dark:bg-sky-500/10',         desc: '搜索热度 / 社媒讨论' },
  { label: 'AI 需求洞察', icon: LightBulbIcon,        iconColor: 'text-amber-500',   iconBg: 'bg-amber-50 dark:bg-amber-500/10',     desc: '痛点 / 机会 / 风险' },
  { label: '用户画像',    icon: UserGroupIcon,        iconColor: 'text-purple-500',  iconBg: 'bg-purple-50 dark:bg-purple-500/10',   desc: '3–5 个目标画像' },
  { label: '竞品分析',    icon: BuildingOffice2Icon,  iconColor: 'text-emerald-500', iconBg: 'bg-emerald-50 dark:bg-emerald-500/10', desc: '对比矩阵 / 差异化' },
]
</script>

<template>
  <div class="max-w-[1400px] mx-auto">
    <!-- 欢迎横栏 -->
    <div class="flex flex-wrap items-end justify-between gap-4 mb-7">
      <div class="min-w-0">
        <div class="flex items-center gap-2 text-sm text-brand-600 dark:text-brand-400 font-medium mb-1.5">
          <SparklesIcon class="w-4 h-4" />
          <span>{{ greeting }}</span>
        </div>
        <h1 class="text-[28px] font-bold tracking-tight text-neutral-900 dark:text-neutral-100 leading-tight">
          {{ userStore.userInfo?.nickname || '用户' }},欢迎回到 MarketAI
        </h1>
        <p class="text-sm text-neutral-500 dark:text-neutral-400 mt-2">
          管理你的市场分析项目,获取 AI 驱动的需求洞察与竞品情报。
        </p>
      </div>
      <AppButton
        v-if="projectStore.hasProjects"
        variant="gradient"
        size="lg"
        @click="router.push('/projects')"
      >
        <PlusIcon class="w-4 h-4" />新建项目
      </AppButton>
    </div>

    <!-- 加载态 -->
    <div v-if="projectStore.loading && !projectStore.projectList.length" class="space-y-4">
      <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <AppCard v-for="i in 4" :key="i">
          <AppSkeleton height="12px" width="40%" />
          <AppSkeleton class="mt-3" height="28px" width="60%" />
        </AppCard>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <AppCard v-for="i in 3" :key="i">
          <AppSkeleton height="16px" width="70%" />
          <AppSkeleton class="mt-3" height="12px" width="40%" />
          <AppSkeleton class="mt-4" :rows="2" height="10px" />
        </AppCard>
      </div>
    </div>

    <!-- 空态 -->
    <AppCard v-else-if="!projectStore.hasProjects" padding="lg" class="text-center">
      <AppEmpty
        :icon="FolderOpenIcon"
        size="lg"
        title="创建你的第一个分析项目"
        description="输入目标市场关键词,AI 将自动分析市场趋势、竞争格局和目标用户画像,生成深度洞察报告。"
      >
        <template #action>
          <AppButton variant="gradient" size="lg" @click="router.push('/projects')">
            <PlusIcon class="w-4 h-4" />开始第一个项目
          </AppButton>
        </template>
      </AppEmpty>
    </AppCard>

    <!-- 主内容 -->
    <template v-else>
      <!-- 统计卡片 -->
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-7">
        <AppCard v-for="s in STAT_CARDS" :key="s.key" class="overflow-hidden relative">
          <div :class="['absolute -right-4 -top-4 w-24 h-24 rounded-full bg-gradient-to-br opacity-60', s.iconBg]"></div>
          <div class="relative">
            <div class="flex items-center justify-between">
              <p class="text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">{{ s.label }}</p>
              <component :is="s.icon" :class="['w-4 h-4', s.iconColor]" />
            </div>
            <p class="text-3xl font-bold text-neutral-900 dark:text-neutral-100 mt-3 tabular-nums">{{ s.value }}</p>
          </div>
        </AppCard>
      </div>

      <!-- 最近项目 -->
      <div class="mb-7">
        <div class="flex items-end justify-between mb-4">
          <div>
            <h2 class="text-lg font-semibold text-neutral-900 dark:text-neutral-100 tracking-tight">最近项目</h2>
            <p class="text-sm text-neutral-500 dark:text-neutral-400 mt-1">点击卡片进入项目详情</p>
          </div>
          <button
            class="inline-flex items-center gap-1 text-sm text-brand-600 dark:text-brand-400 hover:text-brand-700 dark:hover:text-brand-300 transition-colors"
            @click="router.push('/projects')"
          >
            查看全部<ArrowRightIcon class="w-3.5 h-3.5" />
          </button>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          <AppCard
            v-for="project in recentProjects"
            :key="project.id"
            hoverable
            @click="goToDetail(project)"
          >
            <div class="flex items-start justify-between mb-3 gap-2">
              <h3 class="font-semibold text-neutral-900 dark:text-neutral-100 truncate flex-1 text-[15px]">{{ project.name }}</h3>
              <AppBadge :tone="statusBadge(project.status).tone" dot>{{ statusBadge(project.status).label }}</AppBadge>
            </div>
            <div class="flex items-center gap-2 mb-4 flex-wrap">
              <AppBadge tone="brand" outline>{{ project.industry }}</AppBadge>
              <span class="text-xs text-neutral-400 truncate">{{ project.targetMarket }}</span>
            </div>
            <div class="pt-3 border-t border-[color:var(--color-border)] flex items-center justify-between text-xs text-neutral-500 dark:text-neutral-400">
              <span>{{ project.keywords?.length || 0 }} 关键词 · {{ project.competitors?.length || 0 }} 竞品</span>
              <span>{{ new Date(project.createdAt).toLocaleDateString('zh-CN') }}</span>
            </div>
          </AppCard>
        </div>
      </div>

      <!-- 能力快捷入口 -->
      <div>
        <div class="mb-4">
          <h2 class="text-lg font-semibold text-neutral-900 dark:text-neutral-100 tracking-tight">可用能力</h2>
          <p class="text-sm text-neutral-500 dark:text-neutral-400 mt-1">每个项目都可以使用以下分析能力</p>
        </div>
        <div class="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <AppCard v-for="f in FEATURE_SHORTCUTS" :key="f.label" hoverable padding="sm">
            <div class="flex items-center gap-3">
              <div :class="['w-11 h-11 rounded-xl flex items-center justify-center flex-shrink-0', f.iconBg]">
                <component :is="f.icon" :class="['w-5 h-5', f.iconColor]" />
              </div>
              <div class="min-w-0">
                <p class="text-sm font-semibold text-neutral-900 dark:text-neutral-100 truncate">{{ f.label }}</p>
                <p class="text-xs text-neutral-500 dark:text-neutral-400 truncate mt-0.5">{{ f.desc }}</p>
              </div>
            </div>
          </AppCard>
        </div>
      </div>
    </template>
  </div>
</template>
