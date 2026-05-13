<script setup lang="ts">
import { ChatBubbleLeftEllipsisIcon, ExclamationTriangleIcon } from '@heroicons/vue/24/outline'
import type { PainPoint } from '@/types/insight'

defineProps<{ painPoints: PainPoint[] }>()

const emit = defineEmits<{ ask: [pp: PainPoint] }>()

const SEVERITY_MAP: Record<string, { label: string; cls: string }> = {
  high:   { label: '严重', cls: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300' },
  medium: { label: '中等', cls: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300' },
  low:    { label: '轻微', cls: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300' },
}
</script>

<template>
  <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
    <div class="flex items-center gap-2 mb-4">
      <ExclamationTriangleIcon class="w-5 h-5 text-red-500" />
      <h3 class="text-base font-semibold text-gray-900 dark:text-white">
        用户痛点
      </h3>
      <span class="text-xs text-gray-400">{{ painPoints?.length || 0 }} 项</span>
    </div>

    <div
      v-if="!painPoints?.length"
      class="text-sm text-gray-400 py-6 text-center"
    >
      暂无痛点数据
    </div>

    <ul class="space-y-3 max-h-[420px] overflow-y-auto pr-1">
      <li
        v-for="(pp, i) in painPoints"
        :key="i"
        class="border border-gray-100 dark:border-gray-700 rounded-lg p-4 hover:border-primary-200 dark:hover:border-primary-700 transition-colors"
      >
        <div class="flex items-start justify-between gap-3 mb-2">
          <div class="flex items-center gap-2">
            <span :class="['px-2 py-0.5 text-xs font-medium rounded-full', SEVERITY_MAP[pp.severity]?.cls || 'bg-gray-100 text-gray-600']">
              {{ SEVERITY_MAP[pp.severity]?.label || pp.severity }}
            </span>
            <h4 class="text-sm font-medium text-gray-900 dark:text-white">
              {{ pp.title }}
            </h4>
          </div>
          <button
            class="inline-flex items-center gap-1 px-2.5 py-1 text-xs text-primary-600 hover:bg-primary-50 dark:hover:bg-primary-900/20 rounded-md transition-colors flex-shrink-0"
            @click="emit('ask', pp)"
          >
            <ChatBubbleLeftEllipsisIcon class="w-3.5 h-3.5" />追问 AI
          </button>
        </div>
        <p class="text-sm text-gray-600 dark:text-gray-300 leading-relaxed">
          {{ pp.description }}
        </p>
        <div
          v-if="pp.evidence?.length"
          class="mt-2 flex flex-wrap gap-1"
        >
          <span
            v-for="(ev, idx) in pp.evidence"
            :key="idx"
            class="px-2 py-0.5 text-xs bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400 rounded"
          >
            {{ ev }}
          </span>
        </div>
      </li>
    </ul>
  </div>
</template>
