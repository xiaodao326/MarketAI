<script setup lang="ts">
import { ShieldExclamationIcon } from '@heroicons/vue/24/outline'
import type { Risk } from '@/types/insight'

defineProps<{ risks: Risk[] }>()

const LEVEL_MAP: Record<string, { label: string; cls: string }> = {
  high:   { label: '高风险', cls: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300' },
  medium: { label: '中风险', cls: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300' },
  low:    { label: '低风险', cls: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300' },
}
</script>

<template>
  <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
    <div class="flex items-center gap-2 mb-4">
      <ShieldExclamationIcon class="w-5 h-5 text-amber-500" />
      <h3 class="text-base font-semibold text-gray-900 dark:text-white">
        风险评估
      </h3>
      <span class="text-xs text-gray-400">{{ risks?.length || 0 }} 项</span>
    </div>

    <div
      v-if="!risks?.length"
      class="text-sm text-gray-400 py-6 text-center"
    >
      暂无风险数据
    </div>

    <ul class="space-y-3 max-h-[360px] overflow-y-auto pr-1">
      <li
        v-for="(r, i) in risks"
        :key="i"
        class="border border-gray-100 dark:border-gray-700 rounded-lg p-3"
      >
        <div class="flex items-center gap-2 mb-1.5">
          <span :class="['px-2 py-0.5 text-xs font-medium rounded-full', LEVEL_MAP[r.level]?.cls || 'bg-gray-100 text-gray-600']">
            {{ LEVEL_MAP[r.level]?.label || r.level }}
          </span>
          <h4 class="text-sm font-medium text-gray-900 dark:text-white">
            {{ r.name }}
          </h4>
        </div>
        <p class="text-sm text-gray-600 dark:text-gray-300 leading-relaxed">
          {{ r.description }}
        </p>
      </li>
    </ul>
  </div>
</template>
