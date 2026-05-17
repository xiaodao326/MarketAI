<script setup lang="ts">
import { computed } from 'vue'
import type { InsightDimensions } from '@/types/insight'

const props = defineProps<{
  score: number
  dimensions: InsightDimensions
}>()

function scoreColor(score: number) {
  if (score >= 80) return { stroke: '#10B981', text: 'text-emerald-600 dark:text-emerald-400', bar: 'bg-emerald-500', glow: 'shadow-[0_0_24px_rgba(16,185,129,0.25)]', label: '极佳' }
  if (score >= 60) return { stroke: '#6366F1', text: 'text-brand-600 dark:text-brand-400',     bar: 'bg-brand-500',   glow: 'shadow-[0_0_24px_rgba(99,102,241,0.25)]', label: '良好' }
  if (score >= 40) return { stroke: '#F59E0B', text: 'text-amber-600 dark:text-amber-400',     bar: 'bg-amber-500',   glow: 'shadow-[0_0_24px_rgba(245,158,11,0.25)]', label: '一般' }
  return                  { stroke: '#EF4444', text: 'text-red-600 dark:text-red-400',         bar: 'bg-red-500',     glow: 'shadow-[0_0_24px_rgba(239,68,68,0.25)]', label: '需谨慎' }
}

const color = computed(() => scoreColor(props.score))

const RADIUS = 64
const CIRCUMFERENCE = 2 * Math.PI * RADIUS
const dashOffset = computed(() => {
  const clamped = Math.max(0, Math.min(props.score, 100))
  return CIRCUMFERENCE * (1 - clamped / 100)
})

const dimensionItems = computed(() => [
  { key: 'demandStrength',         label: '需求强度',   value: props.dimensions.demandStrength },
  { key: 'competitionSaturation',  label: '竞争饱和度', value: props.dimensions.competitionSaturation },
  { key: 'growthPotential',        label: '增长潜力',   value: props.dimensions.growthPotential },
  { key: 'entryBarrier',           label: '进入壁垒',   value: props.dimensions.entryBarrier },
])
</script>

<template>
  <div class="rounded-xl bg-[color:var(--color-surface)] border border-[color:var(--color-border)] shadow-card p-6 relative overflow-hidden">
    <!-- 装饰光晕 -->
    <div class="absolute -top-20 -right-20 w-56 h-56 rounded-full bg-brand-500/8 blur-3xl pointer-events-none"></div>

    <div class="relative flex flex-col lg:flex-row items-center gap-8">
      <!-- 环形进度 -->
      <div class="relative flex-shrink-0">
        <svg width="160" height="160" viewBox="0 0 160 160">
          <circle
            cx="80" cy="80" :r="RADIUS"
            fill="none"
            stroke="currentColor"
            class="text-neutral-100 dark:text-neutral-800"
            stroke-width="10"
          />
          <circle
            cx="80" cy="80" :r="RADIUS"
            fill="none"
            :stroke="color.stroke"
            stroke-width="10"
            stroke-linecap="round"
            :stroke-dasharray="CIRCUMFERENCE"
            :stroke-dashoffset="dashOffset"
            transform="rotate(-90 80 80)"
            style="transition: stroke-dashoffset 0.8s cubic-bezier(0.22, 1, 0.36, 1)"
          />
        </svg>
        <div class="absolute inset-0 flex flex-col items-center justify-center">
          <span :class="['text-[40px] font-bold leading-none tabular-nums', color.text]">{{ score }}</span>
          <span class="text-[11px] text-neutral-500 dark:text-neutral-400 mt-1">市场契合度</span>
          <span :class="['text-[10px] mt-0.5 font-medium', color.text]">{{ color.label }}</span>
        </div>
      </div>

      <!-- 4 维度进度条 -->
      <div class="flex-1 w-full space-y-3.5">
        <div v-for="dim in dimensionItems" :key="dim.key">
          <div class="flex justify-between items-baseline text-sm mb-1.5">
            <span class="text-neutral-600 dark:text-neutral-300">{{ dim.label }}</span>
            <span class="text-neutral-900 dark:text-neutral-100 font-semibold tabular-nums">{{ dim.value }}</span>
          </div>
          <div class="h-2 bg-neutral-100 dark:bg-neutral-800 rounded-full overflow-hidden">
            <div
              :class="['h-full rounded-full transition-all duration-700 ease-out', scoreColor(dim.value).bar]"
              :style="{ width: `${Math.max(0, Math.min(dim.value, 100))}%` }"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
