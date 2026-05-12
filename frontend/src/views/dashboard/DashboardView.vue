<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { useDashboard } from '@/composables/useDashboard'
import MetricCard from '@/components/dashboard/MetricCard.vue'
import TrendChart from '@/components/dashboard/TrendChart.vue'
import KeywordRankList from '@/components/dashboard/KeywordRankList.vue'
import AnomalyList from '@/components/dashboard/AnomalyList.vue'
import {
  MagnifyingGlassIcon,
  ChatBubbleLeftRightIcon,
  BuildingOffice2Icon,
  FaceSmileIcon,
  ArrowLeftIcon,
  ArrowPathIcon,
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
}

const METRIC_CONFIG: MetricConfig[] = [
  { key: 'searchHeatIndex', label: '搜索热度', icon: MagnifyingGlassIcon, format: (v) => v.toLocaleString() },
  { key: 'socialMentions', label: '社媒讨论', icon: ChatBubbleLeftRightIcon, format: (v) => v.toLocaleString() },
  { key: 'competitorActiveCount', label: '竞品活跃', icon: BuildingOffice2Icon, format: (v) => String(v) },
  { key: 'sentimentScore', label: '情感值', icon: FaceSmileIcon, format: (v) => `${v}%` },
]

const TIME_RANGES = [
  { label: '7天', value: '7d' as const },
  { label: '30天', value: '30d' as const },
  { label: '90天', value: '90d' as const },
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
  <div>
    <!-- 顶部工具栏 -->
    <div class="flex flex-wrap items-center justify-between gap-3 mb-6">
      <div class="flex items-center gap-3">
        <button
          @click="router.push(`/projects/${projectId}`)"
          class="p-1.5 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
          title="返回项目"
        >
          <ArrowLeftIcon class="w-5 h-5" />
        </button>
        <div>
          <h1 class="text-lg font-semibold text-gray-900 dark:text-white">趋势仪表盘</h1>
          <p v-if="project" class="text-sm text-gray-500 dark:text-gray-400">{{ project.name }}</p>
        </div>
      </div>

      <div class="flex items-center gap-2">
        <!-- 时间范围切换 -->
        <div class="flex rounded-lg border border-gray-200 dark:border-gray-600 overflow-hidden">
          <button
            v-for="r in TIME_RANGES"
            :key="r.value"
            @click="timeRange = r.value"
            :class="[
              'px-3 py-1.5 text-sm transition-colors',
              timeRange === r.value
                ? 'bg-primary-500 text-white'
                : 'bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700',
            ]"
          >{{ r.label }}</button>
        </div>
        <!-- 行业标签（只读展示） -->
        <span
          v-if="project?.industry"
          class="px-3 py-1.5 text-sm bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 rounded-lg"
        >{{ project.industry }}</span>
        <!-- 手动刷新 -->
        <button
          @click="refresh"
          :disabled="loading"
          class="p-1.5 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors disabled:opacity-40"
          title="刷新数据"
        >
          <ArrowPathIcon :class="['w-5 h-5', loading && 'animate-spin']" />
        </button>
      </div>
    </div>

    <!-- 错误提示 -->
    <div
      v-if="error"
      class="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-5 flex items-center justify-between mb-6"
    >
      <p class="text-red-600 dark:text-red-400 text-sm">{{ error }}</p>
      <button
        @click="refresh"
        class="ml-4 px-4 py-1.5 bg-red-500 hover:bg-red-600 text-white text-sm rounded-lg transition-colors flex-shrink-0"
      >重试</button>
    </div>

    <!-- 4 个指标卡 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
      <template v-if="loading && !metrics">
        <div
          v-for="i in 4"
          :key="i"
          class="animate-pulse bg-gray-200 dark:bg-gray-700 rounded-lg h-24"
        />
      </template>
      <MetricCard
        v-for="card in metricCards"
        :key="card.key"
        :icon="card.icon"
        :label="card.label"
        :value="card.value"
        :delta="card.delta"
        :trend="card.trend"
      />
    </div>

    <!-- 趋势走势图 -->
    <div class="mb-6">
      <div v-if="loading && !trendData" class="animate-pulse bg-gray-200 dark:bg-gray-700 rounded-lg h-80" />
      <TrendChart v-else-if="trendData" :trend-data="trendData" :time-range="timeRange" />
      <div
        v-else-if="!loading"
        class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 h-80 flex items-center justify-center text-sm text-gray-400 dark:text-gray-500"
      >暂无趋势数据</div>
    </div>

    <!-- 底部双栏：关键词 + 异动 -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <div v-if="loading && !topKeywords.length" class="animate-pulse bg-gray-200 dark:bg-gray-700 rounded-lg h-64" />
      <KeywordRankList v-else :keywords="topKeywords" />

      <div v-if="loading && !anomalies.length" class="animate-pulse bg-gray-200 dark:bg-gray-700 rounded-lg h-64" />
      <AnomalyList v-else :anomalies="anomalies" />
    </div>
  </div>
</template>
