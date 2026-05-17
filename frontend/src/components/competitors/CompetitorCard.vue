<script setup lang="ts">
import { computed } from 'vue'
import { ExclamationCircleIcon, ArrowTopRightOnSquareIcon, StarIcon } from '@heroicons/vue/24/outline'
import type { Competitor, CompetitorType, ThreatLevel } from '@/types/competitor'
import AppBadge from '@/components/ui/AppBadge.vue'

defineProps<{ competitor: Competitor }>()

const TYPE_MAP: Record<CompetitorType, { label: string; tone: 'danger' | 'warning' | 'neutral' }> = {
  direct:    { label: '直接竞品', tone: 'danger' },
  indirect:  { label: '间接竞品', tone: 'warning' },
  potential: { label: '潜在竞品', tone: 'neutral' },
}

const THREAT_MAP: Record<ThreatLevel, { label: string; tone: 'danger' | 'warning' | 'success' }> = {
  high:   { label: '高威胁', tone: 'danger' },
  medium: { label: '中威胁', tone: 'warning' },
  low:    { label: '低威胁', tone: 'success' },
}

const AVATAR_PALETTE = [
  'from-brand-500 to-indigo-600',
  'from-emerald-500 to-teal-600',
  'from-purple-500 to-fuchsia-600',
  'from-amber-500 to-orange-600',
  'from-rose-500 to-pink-600',
  'from-cyan-500 to-blue-600',
  'from-violet-500 to-purple-600',
  'from-lime-500 to-emerald-600',
]
function hashCode(s: string) {
  let h = 5381
  for (let i = 0; i < s.length; i++) h = ((h << 5) + h + s.charCodeAt(i)) >>> 0
  return h
}
const avatarColor = computed(() => (c: string) => AVATAR_PALETTE[hashCode(c) % AVATAR_PALETTE.length])
</script>

<template>
  <div class="rounded-xl bg-[color:var(--color-surface)] border border-[color:var(--color-border)] shadow-card p-5 hover:shadow-card-hover hover:-translate-y-px transition-all duration-200 flex flex-col">
    <!-- 顶部 -->
    <div class="flex items-start gap-3 mb-4">
      <img
        v-if="competitor.logoUrl"
        :src="competitor.logoUrl"
        :alt="competitor.name"
        class="w-10 h-10 rounded-lg object-cover flex-shrink-0 ring-1 ring-[color:var(--color-border)]"
      />
      <div
        v-else
        :class="['w-10 h-10 rounded-lg flex items-center justify-center text-white text-sm font-bold flex-shrink-0 bg-gradient-to-br shadow-sm', avatarColor(competitor.name)]"
      >
        {{ (competitor.name?.[0] || '?').toUpperCase() }}
      </div>

      <div class="flex-1 min-w-0">
        <div class="flex items-center gap-1.5">
          <h3 class="text-[15px] font-semibold text-neutral-900 dark:text-neutral-100 truncate">{{ competitor.name }}</h3>
          <a
            v-if="competitor.website"
            :href="competitor.website"
            target="_blank"
            rel="noopener"
            class="text-neutral-400 hover:text-brand-600 transition-colors flex-shrink-0"
            title="访问官网"
          >
            <ArrowTopRightOnSquareIcon class="w-3.5 h-3.5" />
          </a>
        </div>
        <div class="mt-1.5 flex flex-wrap gap-1">
          <AppBadge :tone="TYPE_MAP[competitor.type]?.tone || 'neutral'">
            {{ TYPE_MAP[competitor.type]?.label || competitor.type }}
          </AppBadge>
          <AppBadge :tone="THREAT_MAP[competitor.threatLevel]?.tone || 'neutral'" dot>
            {{ THREAT_MAP[competitor.threatLevel]?.label || competitor.threatLevel }}
          </AppBadge>
        </div>
      </div>

      <ExclamationCircleIcon
        v-if="competitor.needsUserInput"
        class="w-5 h-5 text-amber-500 flex-shrink-0"
        title="AI 不熟悉该竞品,需要你补充信息"
      />
    </div>

    <!-- 关键指标 -->
    <div class="grid grid-cols-2 gap-x-3 gap-y-2 text-xs mb-4">
      <div class="flex items-center justify-between">
        <span class="text-neutral-500 dark:text-neutral-400">评分</span>
        <span class="text-neutral-900 dark:text-neutral-100 font-semibold inline-flex items-center gap-0.5">
          <StarIcon class="w-3 h-3 text-amber-500" />{{ Number(competitor.rating ?? 0).toFixed(1) }}
        </span>
      </div>
      <div class="flex items-center justify-between">
        <span class="text-neutral-500 dark:text-neutral-400">区域</span>
        <span class="text-neutral-700 dark:text-neutral-300 truncate ml-2">{{ competitor.region || '—' }}</span>
      </div>
      <div class="flex items-center justify-between col-span-2 gap-2">
        <span class="text-neutral-500 dark:text-neutral-400 flex-shrink-0">定价</span>
        <span class="text-neutral-700 dark:text-neutral-300 truncate text-right">{{ competitor.pricingModel || '—' }}</span>
      </div>
      <div class="flex items-center justify-between col-span-2">
        <span class="text-neutral-500 dark:text-neutral-400">融资阶段</span>
        <span class="text-neutral-700 dark:text-neutral-300">{{ competitor.fundingStage || '—' }}</span>
      </div>
    </div>

    <!-- 优势 / 劣势 -->
    <div
      v-if="competitor.strengths?.length || competitor.weaknesses?.length"
      class="space-y-3 mt-auto pt-3 border-t border-[color:var(--color-border)]"
    >
      <div v-if="competitor.strengths?.length">
        <p class="text-[10px] font-semibold uppercase tracking-wider text-emerald-600 dark:text-emerald-400 mb-1.5">优势</p>
        <ul class="text-xs text-neutral-600 dark:text-neutral-300 space-y-1">
          <li v-for="(s, i) in competitor.strengths.slice(0, 3)" :key="i" class="flex items-start gap-1.5">
            <span class="text-emerald-500 mt-0.5">+</span>
            <span class="truncate">{{ s }}</span>
          </li>
        </ul>
      </div>
      <div v-if="competitor.weaknesses?.length">
        <p class="text-[10px] font-semibold uppercase tracking-wider text-red-600 dark:text-red-400 mb-1.5">劣势</p>
        <ul class="text-xs text-neutral-600 dark:text-neutral-300 space-y-1">
          <li v-for="(w, i) in competitor.weaknesses.slice(0, 3)" :key="i" class="flex items-start gap-1.5">
            <span class="text-red-500 mt-0.5">−</span>
            <span class="truncate">{{ w }}</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>
