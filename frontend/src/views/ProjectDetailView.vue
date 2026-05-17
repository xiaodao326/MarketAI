<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import {
  PencilIcon, TrashIcon,
  ChartBarIcon, LightBulbIcon, UserGroupIcon, BuildingOffice2Icon,
  ArrowRightIcon, TagIcon, CalendarDaysIcon,
} from '@heroicons/vue/24/outline'
import ProjectEditModal from './projects/ProjectEditModal.vue'
import type { Project } from '@/types/project'
import type { Component } from 'vue'
import AppCard from '@/components/ui/AppCard.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppSkeleton from '@/components/ui/AppSkeleton.vue'
import { confirm, toast } from '@/utils/feedback'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()

const project = ref<Project | null>(null)
const loading = ref(true)
const showEditModal = ref(false)

const id = computed(() => Number(route.params.id))

interface FeatureCard {
  key: string
  label: string
  desc: string
  icon: Component
  available: boolean
  route?: string
  gradient: string
  iconColor: string
  ring: string
}

const features = computed<FeatureCard[]>(() => [
  {
    key: 'dashboard', label: '趋势仪表盘',
    desc: '搜索热度、社媒讨论、竞品活跃度等核心指标的实时趋势',
    icon: ChartBarIcon, available: true,
    route: `/projects/${id.value}/dashboard`,
    gradient: 'from-sky-500/15 via-sky-500/5 to-transparent',
    iconColor: 'text-sky-600 dark:text-sky-400',
    ring: 'hover:ring-sky-300/50 dark:hover:ring-sky-700/50',
  },
  {
    key: 'insight', label: 'AI 需求洞察',
    desc: '一键生成痛点、机会、风险、行动建议的结构化分析报告',
    icon: LightBulbIcon, available: true,
    route: `/projects/${id.value}/insights`,
    gradient: 'from-amber-500/15 via-amber-500/5 to-transparent',
    iconColor: 'text-amber-600 dark:text-amber-400',
    ring: 'hover:ring-amber-300/50 dark:hover:ring-amber-700/50',
  },
  {
    key: 'persona', label: '用户画像',
    desc: 'AI 自动生成 3–5 个目标用户画像,含决策参数与代表性引言',
    icon: UserGroupIcon, available: true,
    route: `/projects/${id.value}/personas`,
    gradient: 'from-purple-500/15 via-purple-500/5 to-transparent',
    iconColor: 'text-purple-600 dark:text-purple-400',
    ring: 'hover:ring-purple-300/50 dark:hover:ring-purple-700/50',
  },
  {
    key: 'competitor', label: '竞品分析',
    desc: 'AI 生成功能对比矩阵 + 差异化建议,帮你找到市场切入点',
    icon: BuildingOffice2Icon, available: true,
    route: `/projects/${id.value}/competitors`,
    gradient: 'from-emerald-500/15 via-emerald-500/5 to-transparent',
    iconColor: 'text-emerald-600 dark:text-emerald-400',
    ring: 'hover:ring-emerald-300/50 dark:hover:ring-emerald-700/50',
  },
])

const statusBadge = (status: number): { label: string; tone: 'neutral' | 'success' | 'warning' } => {
  switch (status) {
    case 0: return { label: '草稿', tone: 'neutral' }
    case 1: return { label: '活跃', tone: 'success' }
    case 2: return { label: '归档', tone: 'warning' }
    default: return { label: '未知', tone: 'neutral' }
  }
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
  if (!project.value) return
  const ok = await confirm(
    `项目「${project.value.name}」归档后不再展示在活跃列表,可随时通过筛选查看。`,
    `归档项目`,
    { type: 'warning', confirmText: '归档', danger: true },
  )
  if (!ok) return
  try {
    await projectStore.remove(project.value.id)
    toast.success('项目已归档')
    router.push('/projects')
  } catch (e) {
    toast.error((e as Error).message || '归档失败')
  }
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
  <div class="max-w-[1400px] mx-auto">
    <!-- 加载态 -->
    <div v-if="loading" class="space-y-4">
      <AppCard padding="lg">
        <AppSkeleton height="24px" width="40%" />
        <AppSkeleton class="mt-4" height="14px" width="80%" />
        <AppSkeleton class="mt-2" height="14px" width="60%" />
      </AppCard>
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <AppCard v-for="i in 4" :key="i" padding="lg">
          <AppSkeleton height="20px" width="50%" />
          <AppSkeleton class="mt-3" :rows="2" height="12px" />
        </AppCard>
      </div>
    </div>

    <template v-else-if="project">
      <!-- 项目信息卡 -->
      <AppCard padding="lg" class="mb-6 relative overflow-hidden">
        <!-- 顶部装饰 -->
        <div class="absolute inset-x-0 top-0 h-1 bg-brand-gradient"></div>
        <div class="absolute -top-20 -right-20 w-64 h-64 rounded-full bg-brand-500/8 blur-3xl pointer-events-none"></div>

        <div class="relative flex items-start justify-between gap-4 flex-wrap">
          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-3 flex-wrap mb-2">
              <h1 class="text-[22px] font-semibold text-neutral-900 dark:text-neutral-100 tracking-tight">
                {{ project.name }}
              </h1>
              <AppBadge :tone="statusBadge(project.status).tone" dot>
                {{ statusBadge(project.status).label }}
              </AppBadge>
            </div>
            <p
              v-if="project.description"
              class="text-sm text-neutral-600 dark:text-neutral-400 mb-3 leading-relaxed max-w-3xl"
            >{{ project.description }}</p>
            <div class="flex items-center gap-3 flex-wrap text-xs text-neutral-500 dark:text-neutral-400">
              <AppBadge tone="brand" outline>
                <BuildingOffice2Icon class="w-3 h-3" />{{ project.industry }}
              </AppBadge>
              <span>· 目标市场:{{ project.targetMarket }}</span>
              <span class="inline-flex items-center gap-1">
                <TagIcon class="w-3 h-3" />{{ project.keywords?.length || 0 }} 个关键词
              </span>
              <span class="inline-flex items-center gap-1">
                <CalendarDaysIcon class="w-3 h-3" />创建于 {{ new Date(project.createdAt).toLocaleDateString('zh-CN') }}
              </span>
            </div>
          </div>
          <div class="flex items-center gap-2 flex-shrink-0">
            <AppButton variant="secondary" size="sm" @click="showEditModal = true">
              <PencilIcon class="w-3.5 h-3.5" />编辑
            </AppButton>
            <AppButton variant="ghost" size="sm" @click="handleArchive">
              <TrashIcon class="w-3.5 h-3.5" />归档
            </AppButton>
          </div>
        </div>

        <!-- 关键词标签 -->
        <div v-if="project.keywords?.length" class="relative mt-5 pt-5 border-t border-[color:var(--color-border)]">
          <p class="text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-2">监控关键词</p>
          <div class="flex flex-wrap gap-1.5">
            <span
              v-for="kw in project.keywords"
              :key="kw"
              class="px-2.5 py-1 bg-brand-50 dark:bg-brand-500/10 text-brand-700 dark:text-brand-300 text-xs rounded-full"
            >{{ kw }}</span>
          </div>
        </div>
      </AppCard>

      <!-- 功能模块 -->
      <div class="mb-3 flex items-baseline justify-between">
        <h2 class="text-base font-semibold text-neutral-900 dark:text-neutral-100">功能模块</h2>
        <p class="text-xs text-neutral-500 dark:text-neutral-400">点击进入对应分析能力</p>
      </div>
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <button
          v-for="f in features"
          :key="f.key"
          :disabled="!f.available"
          :class="[
            'group relative text-left rounded-xl bg-[color:var(--color-surface)] border border-[color:var(--color-border)] p-5 overflow-hidden',
            'transition-all duration-200 ease-out ring-1 ring-transparent',
            f.available
              ? `cursor-pointer hover:-translate-y-0.5 hover:shadow-card-hover ${f.ring}`
              : 'opacity-60 cursor-not-allowed',
          ]"
          @click="openFeature(f)"
        >
          <!-- 渐变装饰 -->
          <div :class="['absolute -right-12 -top-12 w-40 h-40 rounded-full bg-gradient-to-br opacity-80', f.gradient]"></div>

          <div class="relative flex items-start gap-4">
            <div :class="['w-12 h-12 rounded-xl bg-white dark:bg-neutral-900 ring-1 ring-[color:var(--color-border)] flex items-center justify-center flex-shrink-0 shadow-sm']">
              <component :is="f.icon" :class="['w-6 h-6', f.iconColor]" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <h3 class="text-[15px] font-semibold text-neutral-900 dark:text-neutral-100">{{ f.label }}</h3>
                <span
                  v-if="!f.available"
                  class="px-1.5 py-0.5 text-[10px] bg-neutral-100 dark:bg-neutral-800 text-neutral-500 rounded"
                >敬请期待</span>
              </div>
              <p class="text-sm text-neutral-500 dark:text-neutral-400 leading-relaxed">{{ f.desc }}</p>
            </div>
            <ArrowRightIcon
              v-if="f.available"
              class="w-4 h-4 text-neutral-300 dark:text-neutral-600 group-hover:text-brand-500 group-hover:translate-x-1 transition-all flex-shrink-0 mt-1"
            />
          </div>
        </button>
      </div>

      <ProjectEditModal
        v-if="showEditModal && project"
        :project="project"
        @close="showEditModal = false"
        @updated="handleUpdated"
      />
    </template>
  </div>
</template>
