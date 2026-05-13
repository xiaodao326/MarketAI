<script setup lang="ts">
import { computed } from 'vue'
import type { InsightDimensions } from '@/types/insight'

const props = defineProps<{
  score: number
  dimensions: InsightDimensions
}>()

// 评分颜色分段 — 与设计需求一致
function scoreColor(score: number) {
  if (score >= 80) return { stroke: '#10b981', text: 'text-emerald-500', bar: 'bg-emerald-500' }
  if (score >= 60) return { stroke: '#3b82f6', text: 'text-blue-500', bar: 'bg-blue-500' }
  if (score >= 40) return { stroke: '#f59e0b', text: 'text-amber-500', bar: 'bg-amber-500' }
  return { stroke: '#ef4444', text: 'text-red-500', bar: 'bg-red-500' }
}

const color = computed(() => scoreColor(props.score))

// SVG 环参数:半径 60,周长 ≈ 377
const RADIUS = 60
const CIRCUMFERENCE = 2 * Math.PI * RADIUS
const dashOffset = computed(() => {
  const clamped = Math.max(0, Math.min(props.score, 100))
  return CIRCUMFERENCE * (1 - clamped / 100)
})

const dimensionItems = computed(() => [
  { key: 'demandStrength', label: '需求强度', value: props.dimensions.demandStrength },
  { key: 'competitionSaturation', label: '竞争饱和度', value: props.dimensions.competitionSaturation },
  { key: 'growthPotential', label: '增长潜力', value: props.dimensions.growthPotential },
  { key: 'entryBarrier', label: '进入壁垒', value: props.dimensions.entryBarrier },
])
</script>

<template>
  <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
    <div class="flex flex-col md:flex-row items-center gap-8">
      <!-- 环形进度 -->
      <div class="relative flex-shrink-0">
        <svg
          width="150"
          height="150"
          viewBox="0 0 150 150"
        >
          <circle
            cx="75"
            cy="75"
            :r="RADIUS"
            fill="none"
            stroke="currentColor"
            class="text-gray-100 dark:text-gray-700"
            stroke-width="12"
          />
          <circle
            cx="75"
            cy="75"
            :r="RADIUS"
            fill="none"
            :stroke="color.stroke"
            stroke-width="12"
            stroke-linecap="round"
            :stroke-dasharray="CIRCUMFERENCE"
            :stroke-dashoffset="dashOffset"
            transform="rotate(-90 75 75)"
            style="transition: stroke-dashoffset 0.6s ease"
          />
        </svg>
        <div class="absolute inset-0 flex flex-col items-center justify-center">
          <span :class="['text-3xl font-bold', color.text]">{{ score }}</span>
          <span class="text-xs text-gray-500 dark:text-gray-400">市场契合度</span>
        </div>
      </div>

      <!-- 4 维度进度条 -->
      <div class="flex-1 w-full space-y-3">
        <div
          v-for="dim in dimensionItems"
          :key="dim.key"
        >
          <div class="flex justify-between text-sm mb-1">
            <span class="text-gray-600 dark:text-gray-300">{{ dim.label }}</span>
            <span class="text-gray-900 dark:text-white font-medium">{{ dim.value }}</span>
          </div>
          <div class="h-2 bg-gray-100 dark:bg-gray-700 rounded-full overflow-hidden">
            <div
              :class="['h-full rounded-full transition-all duration-500', scoreColor(dim.value).bar]"
              :style="{ width: `${Math.max(0, Math.min(dim.value, 100))}%` }"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
