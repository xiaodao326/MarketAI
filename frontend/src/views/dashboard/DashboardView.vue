<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { useDashboard } from '@/composables/useDashboard'
import MetricCard from '@/components/dashboard/MetricCard.vue'
import TrendChart from '@/components/dashboard/TrendChart.vue'
import KeywordRankList from '@/components/dashboard/KeywordRankList.vue'
import AnomalyList from '@/components/dashboard/AnomalyList.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppSkeleton from '@/components/ui/AppSkeleton.vue'
import {
  MagnifyingGlassIcon,
  ChatBubbleLeftRightIcon,
  BuildingOffice2Icon,
  FaceSmileIcon,
  ArrowPathIcon,
  ExclamationCircleIcon,
} from '@heroicons/vue/24/outline'
import type { Metrics } from '@/types/dashboard'
import type { Component } from 'vue'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()

const projectId = computed(() => Number(route.params.id))
const project = computed(() => projectStore.currentProject)

const { metrics, trendData, topKeywords, anomalies, timeRange, loading, error, refresh } = useDashboard(projectId)

interface MetricConfig {
  key: keyof Metrics
  label: string
  icon: Component
  format: (v: number) => string
  tone: 'brand' | 'accent' | 'warning' | 'success'
}

const METRIC_CONFIG: MetricConfig[] = [
  { key: 'searchHeatIndex',     label: '搜索热度', icon: MagnifyingGlassIcon,     format: v => v.toLocaleString(), tone: 'brand' },
  { key: 'socialMentions',      label: '社媒讨论', icon: ChatBubbleLeftRightIcon, format: v => v.toLocaleString(), tone: 'accent' },
  { key: 'competitorActiveCount', label: '竞品活跃', icon: BuildingOffice2Icon,   format: v => String(v),          tone: 'success' },
  { key: 'sentimentScore',      label: '情感值',   icon: FaceSmileIcon,           format: v => `${v}%`,            tone: 'warning' },
]

const TIME_RANGES = [
  { label: '7 天',  value: '7d' as const },
  { label: '30 天', value: '30d' as const },
  { label: '90 天', value: '90d' as const },
]

const metricCards = computed(() => {
  if (!metrics.value) return []
  return METRIC_CONFIG.map((cfg) => ({
    key: cfg.key,
    label: cfg.label,
    icon: cfg.icon,
    value: cfg.format(metrics.value![cfg.key].current),
    delta: metrics.value![cfg.key].delta,
    trend: metrics.value![cfg.key].trend,
    tone: cfg.tone,
  }))
})

onMounted(async () => {
  if (!project.value || project.value.id !== projectId.value) {
    try {
      await projectStore.fetchDetail(projectId.value)
    } catch {
      router.replace('/projects')
    }
  }
})
</script>

<template>
  <div class="max-w-[1400px] mx-auto">
    <PageHeader title="趋势仪表盘" backable @back="router.push(`/projects/${projectId}`)">
      <template #subtitle>
        <span v-if="project">{{ project.name }} · 实时市场指标与趋势</span>
        <span v-else>实时市场指标与趋势</span>
      </template>
      <template #actions>
        <div class="inline-flex items-center p-0.5 rounded-md bg-[color:var(--color-surface-muted)] border border-[color:var(--color-border)]">
          <button
            v-for="r in TIME_RANGES"
            :key="r.value"
            type="button"
            :class="[
              'px-3 py-1.5 text-xs font-medium rounded transition-all',
              timeRange === r.value
                ? 'bg-[color:var(--color-surface)] text-neutral-900 dark:text-neutral-100 shadow-sm'
                : 'text-neutral-500 hover:text-neutral-700 dark:hover:text-neutral-200',
            ]"
            @click="timeRange = r.value"
          >{{ r.label }}</button>
        </div>
        <AppBadge v-if="project?.industry" tone="neutral" outline>{{ project.industry }}</AppBadge>
        <button
          class="w-9 h-9 inline-flex items-center justify-center rounded-md border border-[color:var(--color-border)] text-neutral-500 hover:text-brand-600 hover:border-brand-300 hover:bg-brand-50 dark:hover:bg-brand-500/10 transition-colors disabled:opacity-40"
          :disabled="loading"
          title="刷新"
          @click="refresh"
        >
          <ArrowPathIcon :class="['w-4 h-4', loading && 'animate-spin']" />
        </button>
      </template>
    </PageHeader>

    <!-- 错误提示 -->
    <div
      v-if="error"
      class="bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-700/40 rounded-lg p-4 flex items-center justify-between mb-6"
    >
      <p class="text-sm text-red-700 dark:text-red-300 flex items-center gap-2">
        <ExclamationCircleIcon class="w-4 h-4 flex-shrink-0" />{{ error }}
      </p>
      <button
        class="ml-4 px-3 py-1.5 bg-red-500 hover:bg-red-600 text-white text-xs font-medium rounded-md transition-colors flex-shrink-0"
        @click="refresh"
      >重试</button>
    </div>

    <!-- 4 指标卡 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
      <template v-if="loading && !metrics">
        <div v-for="i in 4" :key="i" class="rounded-xl bg-[color:var(--color-surface)] border border-[color:var(--color-border)] p-5">
          <AppSkeleton height="12px" width="40%" />
          <AppSkeleton class="mt-4" height="28px" width="60%" />
        </div>
      </template>
      <MetricCard
        v-for="card in metricCards"
        :key="card.key"
        :icon="card.icon"
        :label="card.label"
        :value="card.value"
        :delta="card.delta"
        :trend="card.trend"
        :tone="card.tone"
      />
    </div>

    <!-- 趋势图 -->
    <div class="mb-6">
      <div v-if="loading && !trendData" class="rounded-xl bg-[color:var(--color-surface)] border border-[color:var(--color-border)] p-5">
        <AppSkeleton height="16px" width="20%" />
        <AppSkeleton class="mt-4" height="280px" />
      </div>
      <TrendChart v-else-if="trendData" :trend-data="trendData" :time-range="timeRange" />
      <div
        v-else-if="!loading"
        class="rounded-xl bg-[color:var(--color-surface)] border border-[color:var(--color-border)] h-80 flex items-center justify-center text-sm text-neutral-400"
      >暂无趋势数据</div>
    </div>

    <!-- 双栏 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
      <div v-if="loading && !topKeywords.length" class="rounded-xl bg-[color:var(--color-surface)] border border-[color:var(--color-border)] p-5">
        <AppSkeleton height="16px" width="30%" />
        <AppSkeleton class="mt-4" :rows="5" />
      </div>
      <KeywordRankList v-else :keywords="topKeywords" />

      <div v-if="loading && !anomalies.length" class="rounded-xl bg-[color:var(--color-surface)] border border-[color:var(--color-border)] p-5">
        <AppSkeleton height="16px" width="30%" />
        <AppSkeleton class="mt-4" :rows="5" height="40px" />
      </div>
      <AnomalyList v-else :anomalies="anomalies" />
    </div>
  </div>
</template>
