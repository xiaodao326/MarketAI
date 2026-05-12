<script setup lang="ts">
import type { KeywordRank } from '@/types/dashboard'

defineProps<{
  keywords: KeywordRank[]
}>()
</script>

<template>
  <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-5">
    <h3 class="text-sm font-semibold text-gray-700 dark:text-gray-200 mb-4">热门关键词</h3>
    <ul class="space-y-3">
      <li v-for="kw in keywords" :key="kw.rank" class="flex items-center gap-3">
        <span
          :class="[
            'w-6 h-6 rounded-full text-xs font-bold flex items-center justify-center flex-shrink-0',
            kw.rank <= 3
              ? 'bg-primary-500 text-white'
              : 'bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400',
          ]"
        >{{ kw.rank }}</span>
        <span class="flex-1 text-sm text-gray-700 dark:text-gray-200 truncate">{{ kw.keyword }}</span>
        <span class="text-sm font-medium text-gray-900 dark:text-white tabular-nums">
          {{ kw.volume.toLocaleString() }}
        </span>
        <span
          :class="[
            'text-xs font-medium w-14 text-right',
            kw.deltaPercent > 0 ? 'text-green-500' : kw.deltaPercent < 0 ? 'text-red-500' : 'text-gray-400',
          ]"
        >
          {{ kw.deltaPercent > 0 ? '↑' : kw.deltaPercent < 0 ? '↓' : '—' }}
          {{ Math.abs(kw.deltaPercent).toFixed(1) }}%
        </span>
      </li>
    </ul>
    <div v-if="!keywords.length" class="py-8 text-center text-sm text-gray-400 dark:text-gray-500">暂无数据</div>
  </div>
</template>
