<script setup lang="ts">
import { computed } from 'vue'
import type { Component } from 'vue'
import { ArrowTrendingUpIcon, ArrowTrendingDownIcon, MinusIcon } from '@heroicons/vue/24/outline'

const props = defineProps<{
  icon: Component
  label: string
  value: string | number
  delta: number
  trend: 'up' | 'down' | 'stable'
  /** 主题色调,默认 brand */
  tone?: 'brand' | 'success' | 'warning' | 'accent'
}>()

const tone = computed(() => props.tone || 'brand')

const toneCls = computed(() => {
  const map = {
    brand:   { bg: 'from-brand-500/12 to-brand-500/3',      icon: 'text-brand-600 dark:text-brand-400' },
    success: { bg: 'from-emerald-500/12 to-emerald-500/3',  icon: 'text-emerald-600 dark:text-emerald-400' },
    warning: { bg: 'from-amber-500/12 to-amber-500/3',      icon: 'text-amber-600 dark:text-amber-400' },
    accent:  { bg: 'from-cyan-500/12 to-cyan-500/3',        icon: 'text-cyan-600 dark:text-cyan-400' },
  }
  return map[tone.value]
})

const trendCls = computed(() => {
  if (props.trend === 'up')   return 'text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-500/10'
  if (props.trend === 'down') return 'text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-500/10'
  return 'text-neutral-500 dark:text-neutral-400 bg-neutral-100 dark:bg-neutral-800'
})

const TrendIcon = computed(() => {
  if (props.trend === 'up')   return ArrowTrendingUpIcon
  if (props.trend === 'down') return ArrowTrendingDownIcon
  return MinusIcon
})
</script>

<template>
  <div class="relative rounded-xl bg-[color:var(--color-surface)] border border-[color:var(--color-border)] shadow-card p-5 overflow-hidden transition-all duration-200 hover:shadow-card-hover">
    <!-- 渐变装饰 -->
    <div :class="['absolute -top-12 -right-12 w-32 h-32 rounded-full bg-gradient-to-br opacity-80', toneCls.bg]"></div>

    <div class="relative">
      <div class="flex items-center justify-between mb-4">
        <span class="text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">{{ label }}</span>
        <div :class="['w-9 h-9 rounded-lg bg-white dark:bg-neutral-900 ring-1 ring-[color:var(--color-border)] flex items-center justify-center']">
          <component :is="icon" :class="['w-[18px] h-[18px]', toneCls.icon]" />
        </div>
      </div>
      <div class="flex items-end justify-between gap-2">
        <span class="text-[28px] font-bold text-neutral-900 dark:text-neutral-100 tabular-nums leading-none">{{ value }}</span>
        <span :class="['inline-flex items-center gap-0.5 px-1.5 py-0.5 text-[11px] font-semibold rounded-md', trendCls]">
          <component :is="TrendIcon" class="w-3 h-3" />
          {{ Math.abs(delta).toFixed(1) }}%
        </span>
      </div>
    </div>
  </div>
</template>
