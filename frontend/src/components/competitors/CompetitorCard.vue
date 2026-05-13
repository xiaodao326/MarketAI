<script setup lang="ts">
import { computed } from 'vue'
import { ExclamationCircleIcon, ArrowTopRightOnSquareIcon, StarIcon } from '@heroicons/vue/24/outline'
import type { Competitor, CompetitorType, ThreatLevel } from '@/types/competitor'

defineProps<{ competitor: Competitor }>()

const TYPE_MAP: Record<CompetitorType, { label: string; cls: string }> = {
  direct:    { label: '直接竞品', cls: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300' },
  indirect:  { label: '间接竞品', cls: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300' },
  potential: { label: '潜在竞品', cls: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300' },
}

const THREAT_MAP: Record<ThreatLevel, { label: string; cls: string }> = {
  high:   { label: '高威胁', cls: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300' },
  medium: { label: '中威胁', cls: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300' },
  low:    { label: '低威胁', cls: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300' },
}

// 头像色基于名称哈希,与 PersonaCard 同模式
const AVATAR_PALETTE = [
  'bg-blue-500', 'bg-emerald-500', 'bg-purple-500', 'bg-amber-500',
  'bg-rose-500', 'bg-indigo-500', 'bg-teal-500', 'bg-pink-500',
]
function hashCode(s: string) {
  let h = 5381
  for (let i = 0; i < s.length; i++) h = ((h << 5) + h + s.charCodeAt(i)) >>> 0
  return h
}
const avatarColor = computed(() => (c: string) => AVATAR_PALETTE[hashCode(c) % AVATAR_PALETTE.length])
</script>

<template>
  <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5 hover:shadow-md transition-shadow flex flex-col">
    <!-- 顶部:头像 + 名称 + 类型徽章 -->
    <div class="flex items-start gap-3 mb-3">
      <img
        v-if="competitor.logoUrl"
        :src="competitor.logoUrl"
        :alt="competitor.name"
        class="w-10 h-10 rounded-lg object-cover flex-shrink-0"
      >
      <div
        v-else
        :class="['w-10 h-10 rounded-lg flex items-center justify-center text-white text-sm font-semibold flex-shrink-0', avatarColor(competitor.name)]"
      >
        {{ (competitor.name?.[0] || '?').toUpperCase() }}
      </div>

      <div class="flex-1 min-w-0">
        <div class="flex items-center gap-1.5">
          <h3 class="text-base font-semibold text-gray-900 dark:text-white truncate">
            {{ competitor.name }}
          </h3>
          <a
            v-if="competitor.website"
            :href="competitor.website"
            target="_blank"
            rel="noopener"
            class="text-gray-400 hover:text-primary-600 transition-colors"
            title="访问官网"
          >
            <ArrowTopRightOnSquareIcon class="w-3.5 h-3.5" />
          </a>
        </div>
        <div class="mt-1 flex flex-wrap gap-1">
          <span :class="['px-1.5 py-0.5 text-xs font-medium rounded-full', TYPE_MAP[competitor.type]?.cls || TYPE_MAP.direct.cls]">
            {{ TYPE_MAP[competitor.type]?.label || competitor.type }}
          </span>
          <span :class="['px-1.5 py-0.5 text-xs font-medium rounded-full', THREAT_MAP[competitor.threatLevel]?.cls || THREAT_MAP.medium.cls]">
            {{ THREAT_MAP[competitor.threatLevel]?.label || competitor.threatLevel }}
          </span>
        </div>
      </div>

      <ExclamationCircleIcon
        v-if="competitor.needsUserInput"
        class="w-5 h-5 text-amber-500 flex-shrink-0"
        title="AI 不熟悉该竞品,需要你补充信息"
      />
    </div>

    <!-- 关键指标 -->
    <div class="grid grid-cols-2 gap-x-3 gap-y-1.5 text-xs mb-3">
      <div class="flex items-center justify-between">
        <span class="text-gray-500 dark:text-gray-400">评分</span>
        <span class="text-gray-900 dark:text-white font-medium inline-flex items-center gap-0.5">
          <StarIcon class="w-3 h-3 text-amber-500" />{{ Number(competitor.rating ?? 0).toFixed(1) }}
        </span>
      </div>
      <div class="flex items-center justify-between">
        <span class="text-gray-500 dark:text-gray-400">区域</span>
        <span class="text-gray-700 dark:text-gray-300">{{ competitor.region || '—' }}</span>
      </div>
      <div class="flex items-center justify-between col-span-2 gap-2">
        <span class="text-gray-500 dark:text-gray-400 flex-shrink-0">定价</span>
        <span class="text-gray-700 dark:text-gray-300 truncate text-right">{{ competitor.pricingModel || '—' }}</span>
      </div>
      <div class="flex items-center justify-between col-span-2">
        <span class="text-gray-500 dark:text-gray-400">融资阶段</span>
        <span class="text-gray-700 dark:text-gray-300">{{ competitor.fundingStage || '—' }}</span>
      </div>
    </div>

    <!-- 优势 / 劣势 -->
    <div
      v-if="competitor.strengths?.length || competitor.weaknesses?.length"
      class="space-y-2 mt-auto pt-3 border-t border-gray-100 dark:border-gray-700"
    >
      <div v-if="competitor.strengths?.length">
        <p class="text-xs font-medium text-emerald-600 dark:text-emerald-400 mb-1">
          优势
        </p>
        <ul class="text-xs text-gray-600 dark:text-gray-300 space-y-0.5">
          <li
            v-for="(s, i) in competitor.strengths.slice(0, 3)"
            :key="i"
            class="truncate"
          >
            · {{ s }}
          </li>
        </ul>
      </div>
      <div v-if="competitor.weaknesses?.length">
        <p class="text-xs font-medium text-red-600 dark:text-red-400 mb-1">
          劣势
        </p>
        <ul class="text-xs text-gray-600 dark:text-gray-300 space-y-0.5">
          <li
            v-for="(w, i) in competitor.weaknesses.slice(0, 3)"
            :key="i"
            class="truncate"
          >
            · {{ w }}
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>
