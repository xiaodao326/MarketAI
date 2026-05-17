<script setup lang="ts">
import { ChatBubbleLeftEllipsisIcon, SparklesIcon } from '@heroicons/vue/24/outline'
import type { Opportunity } from '@/types/insight'
import AppBadge from '@/components/ui/AppBadge.vue'

defineProps<{ opportunities: Opportunity[] }>()
const emit = defineEmits<{ ask: [op: Opportunity] }>()

const TYPE_MAP: Record<string, { label: string; tone: 'success' | 'info' | 'brand' }> = {
  high_value:      { label: '高价值', tone: 'success' },
  blue_ocean:      { label: '蓝海',   tone: 'info' },
  differentiation: { label: '差异化', tone: 'brand' },
}
</script>

<template>
  <div class="rounded-xl bg-[color:var(--color-surface)] border border-[color:var(--color-border)] shadow-card p-6">
    <div class="flex items-center gap-2 mb-4">
      <div class="w-7 h-7 rounded-md bg-emerald-50 dark:bg-emerald-500/12 flex items-center justify-center">
        <SparklesIcon class="w-4 h-4 text-emerald-500" />
      </div>
      <h3 class="text-base font-semibold text-neutral-900 dark:text-neutral-100">市场机会</h3>
      <span class="text-xs text-neutral-400">{{ opportunities?.length || 0 }} 项</span>
    </div>

    <div v-if="!opportunities?.length" class="text-sm text-neutral-400 py-8 text-center">暂无机会数据</div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-3 max-h-[480px] overflow-y-auto pr-1">
      <div
        v-for="(op, i) in opportunities"
        :key="i"
        class="group rounded-lg border border-[color:var(--color-border)] p-4 bg-[color:var(--color-surface)] hover:border-brand-200 dark:hover:border-brand-700 hover:bg-[color:var(--color-surface-muted)]/40 transition-all flex flex-col"
      >
        <div class="flex items-start justify-between gap-2 mb-2">
          <AppBadge :tone="TYPE_MAP[op.type]?.tone || 'neutral'">
            {{ TYPE_MAP[op.type]?.label || op.type }}
          </AppBadge>
          <button
            class="opacity-0 group-hover:opacity-100 text-neutral-400 hover:text-brand-600 transition-all"
            title="追问 AI"
            @click="emit('ask', op)"
          >
            <ChatBubbleLeftEllipsisIcon class="w-4 h-4" />
          </button>
        </div>
        <h4 class="text-sm font-semibold text-neutral-900 dark:text-neutral-100 mb-1.5">{{ op.name }}</h4>
        <p class="text-sm text-neutral-600 dark:text-neutral-300 leading-relaxed flex-1">{{ op.description }}</p>
        <div v-if="op.tags?.length" class="mt-2.5 flex flex-wrap gap-1">
          <span
            v-for="(t, idx) in op.tags"
            :key="idx"
            class="px-2 py-0.5 text-[11px] bg-neutral-100 dark:bg-neutral-800 text-neutral-600 dark:text-neutral-300 rounded"
          >#{{ t }}</span>
        </div>
      </div>
    </div>
  </div>
</template>
