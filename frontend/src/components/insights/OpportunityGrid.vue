<script setup lang="ts">
import { ChatBubbleLeftEllipsisIcon, SparklesIcon } from '@heroicons/vue/24/outline'
import type { Opportunity } from '@/types/insight'

defineProps<{ opportunities: Opportunity[] }>()
const emit = defineEmits<{ ask: [op: Opportunity] }>()

const TYPE_MAP: Record<string, { label: string; cls: string }> = {
  high_value:      { label: '高价值', cls: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300' },
  blue_ocean:      { label: '蓝海', cls: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300' },
  differentiation: { label: '差异化', cls: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300' },
}
</script>

<template>
  <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
    <div class="flex items-center gap-2 mb-4">
      <SparklesIcon class="w-5 h-5 text-emerald-500" />
      <h3 class="text-base font-semibold text-gray-900 dark:text-white">
        市场机会
      </h3>
      <span class="text-xs text-gray-400">{{ opportunities?.length || 0 }} 项</span>
    </div>

    <div
      v-if="!opportunities?.length"
      class="text-sm text-gray-400 py-6 text-center"
    >
      暂无机会数据
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-3 max-h-[420px] overflow-y-auto pr-1">
      <div
        v-for="(op, i) in opportunities"
        :key="i"
        class="border border-gray-100 dark:border-gray-700 rounded-lg p-4 hover:border-primary-200 dark:hover:border-primary-700 transition-colors flex flex-col"
      >
        <div class="flex items-start justify-between gap-2 mb-2">
          <span :class="['px-2 py-0.5 text-xs font-medium rounded-full', TYPE_MAP[op.type]?.cls || 'bg-gray-100 text-gray-600']">
            {{ TYPE_MAP[op.type]?.label || op.type }}
          </span>
          <button
            class="text-gray-400 hover:text-primary-600 transition-colors"
            title="追问 AI"
            @click="emit('ask', op)"
          >
            <ChatBubbleLeftEllipsisIcon class="w-4 h-4" />
          </button>
        </div>
        <h4 class="text-sm font-medium text-gray-900 dark:text-white mb-1.5">
          {{ op.name }}
        </h4>
        <p class="text-sm text-gray-600 dark:text-gray-300 leading-relaxed flex-1">
          {{ op.description }}
        </p>
        <div
          v-if="op.tags?.length"
          class="mt-2 flex flex-wrap gap-1"
        >
          <span
            v-for="(t, idx) in op.tags"
            :key="idx"
            class="px-2 py-0.5 text-xs bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400 rounded"
          >
            #{{ t }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>
