<script setup lang="ts">
import type { KeywordRank } from '@/types/dashboard'
import { ArrowTrendingUpIcon, ArrowTrendingDownIcon, MinusIcon } from '@heroicons/vue/24/outline'

defineProps<{
  keywords: KeywordRank[]
}>()

function rankClass(rank: number) {
  if (rank === 1) return 'bg-amber-100 text-amber-700 dark:bg-amber-500/20 dark:text-amber-300 ring-1 ring-amber-200 dark:ring-amber-700/50'
  if (rank === 2) return 'bg-neutral-100 text-neutral-700 dark:bg-neutral-700/40 dark:text-neutral-300'
  if (rank === 3) return 'bg-orange-100 text-orange-700 dark:bg-orange-500/15 dark:text-orange-300'
  return 'bg-neutral-50 text-neutral-500 dark:bg-neutral-800/50 dark:text-neutral-400'
}
</script>

<template>
  <div class="rounded-xl bg-[color:var(--color-surface)] border border-[color:var(--color-border)] shadow-card p-5">
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-sm font-semibold text-neutral-700 dark:text-neutral-200">热门关键词</h3>
      <span class="text-xs text-neutral-400">Top {{ keywords.length }}</span>
    </div>
    <ul v-if="keywords.length" class="space-y-1.5">
      <li
        v-for="kw in keywords"
        :key="kw.rank"
        class="flex items-center gap-3 py-2 px-2 rounded-lg hover:bg-neutral-50 dark:hover:bg-neutral-800/50 transition-colors"
      >
        <span :class="['w-6 h-6 rounded-md text-[11px] font-bold flex items-center justify-center flex-shrink-0 tabular-nums', rankClass(kw.rank)]">
          {{ kw.rank }}
        </span>
        <span class="flex-1 text-sm text-neutral-800 dark:text-neutral-200 truncate">{{ kw.keyword }}</span>
        <span class="text-sm font-semibold text-neutral-900 dark:text-neutral-100 tabular-nums">{{ kw.volume.toLocaleString() }}</span>
        <span
          :class="[
            'inline-flex items-center gap-0.5 text-[11px] font-medium w-16 justify-end',
            kw.deltaPercent > 0 ? 'text-emerald-600 dark:text-emerald-400' :
            kw.deltaPercent < 0 ? 'text-red-600 dark:text-red-400' : 'text-neutral-400',
          ]"
        >
          <ArrowTrendingUpIcon v-if="kw.deltaPercent > 0" class="w-3 h-3" />
          <ArrowTrendingDownIcon v-else-if="kw.deltaPercent < 0" class="w-3 h-3" />
          <MinusIcon v-else class="w-3 h-3" />
          {{ Math.abs(kw.deltaPercent).toFixed(1) }}%
        </span>
      </li>
    </ul>
    <div v-else class="py-10 text-center text-sm text-neutral-400">暂无关键词数据</div>
  </div>
</template>
