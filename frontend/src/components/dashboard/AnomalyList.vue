<script setup lang="ts">
import type { Anomaly } from '@/types/dashboard'

defineProps<{
  anomalies: Anomaly[]
}>()

const SEVERITY_DOT: Record<string, string> = {
  critical: 'bg-red-500',
  warning: 'bg-amber-500',
  opportunity: 'bg-green-500',
}

const SEVERITY_BADGE: Record<string, string> = {
  critical: 'bg-red-50 text-red-600 dark:bg-red-900/20 dark:text-red-400',
  warning: 'bg-amber-50 text-amber-600 dark:bg-amber-900/20 dark:text-amber-400',
  opportunity: 'bg-green-50 text-green-600 dark:bg-green-900/20 dark:text-green-400',
}

const SEVERITY_LABELS: Record<string, string> = {
  critical: '严重',
  warning: '警告',
  opportunity: '机会',
}
</script>

<template>
  <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-5">
    <h3 class="text-sm font-semibold text-gray-700 dark:text-gray-200 mb-4">市场异动</h3>
    <ul class="space-y-4">
      <li v-for="a in anomalies" :key="a.id" class="flex gap-3">
        <div class="mt-1.5 flex-shrink-0">
          <span :class="['w-2.5 h-2.5 rounded-full block', SEVERITY_DOT[a.severity]]" />
        </div>
        <div class="min-w-0">
          <div class="flex items-center gap-2 mb-0.5">
            <span class="text-sm font-medium text-gray-800 dark:text-gray-100 truncate">{{ a.title }}</span>
            <span :class="['text-xs px-1.5 py-0.5 rounded font-medium flex-shrink-0', SEVERITY_BADGE[a.severity]]">
              {{ SEVERITY_LABELS[a.severity] }}
            </span>
          </div>
          <p class="text-xs text-gray-500 dark:text-gray-400 line-clamp-2">{{ a.description }}</p>
          <p class="text-xs text-gray-400 dark:text-gray-500 mt-1">{{ a.occurredAt }} · {{ a.source }}</p>
        </div>
      </li>
    </ul>
    <div v-if="!anomalies.length" class="py-8 text-center text-sm text-gray-400 dark:text-gray-500">暂无异动事件</div>
  </div>
</template>
