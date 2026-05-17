<script setup lang="ts">
import { ChatBubbleLeftEllipsisIcon, ExclamationTriangleIcon } from '@heroicons/vue/24/outline'
import type { PainPoint } from '@/types/insight'
import AppBadge from '@/components/ui/AppBadge.vue'

defineProps<{ painPoints: PainPoint[] }>()

const emit = defineEmits<{ ask: [pp: PainPoint] }>()

const SEVERITY_MAP: Record<string, { label: string; tone: 'danger' | 'warning' | 'info' }> = {
  high:   { label: '严重', tone: 'danger' },
  medium: { label: '中等', tone: 'warning' },
  low:    { label: '轻微', tone: 'info' },
}
</script>

<template>
  <div class="rounded-xl bg-[color:var(--color-surface)] border border-[color:var(--color-border)] shadow-card p-6">
    <div class="flex items-center gap-2 mb-4">
      <div class="w-7 h-7 rounded-md bg-red-50 dark:bg-red-500/12 flex items-center justify-center">
        <ExclamationTriangleIcon class="w-4 h-4 text-red-500" />
      </div>
      <h3 class="text-base font-semibold text-neutral-900 dark:text-neutral-100">用户痛点</h3>
      <span class="text-xs text-neutral-400">{{ painPoints?.length || 0 }} 项</span>
    </div>

    <div v-if="!painPoints?.length" class="text-sm text-neutral-400 py-8 text-center">暂无痛点数据</div>

    <ul class="space-y-3 max-h-[480px] overflow-y-auto pr-1">
      <li
        v-for="(pp, i) in painPoints"
        :key="i"
        class="group rounded-lg border border-[color:var(--color-border)] p-4 bg-[color:var(--color-surface)] hover:border-brand-200 dark:hover:border-brand-700 hover:bg-[color:var(--color-surface-muted)]/40 transition-all"
      >
        <div class="flex items-start justify-between gap-3 mb-2">
          <div class="flex items-center gap-2 flex-wrap min-w-0">
            <AppBadge :tone="SEVERITY_MAP[pp.severity]?.tone || 'neutral'">
              {{ SEVERITY_MAP[pp.severity]?.label || pp.severity }}
            </AppBadge>
            <h4 class="text-sm font-semibold text-neutral-900 dark:text-neutral-100">{{ pp.title }}</h4>
          </div>
          <button
            class="opacity-0 group-hover:opacity-100 inline-flex items-center gap-1 px-2 py-1 text-xs text-brand-600 dark:text-brand-400 hover:bg-brand-50 dark:hover:bg-brand-500/12 rounded-md transition-all flex-shrink-0"
            @click="emit('ask', pp)"
          >
            <ChatBubbleLeftEllipsisIcon class="w-3.5 h-3.5" />追问
          </button>
        </div>
        <p class="text-sm text-neutral-600 dark:text-neutral-300 leading-relaxed">{{ pp.description }}</p>
        <div v-if="pp.evidence?.length" class="mt-2 flex flex-wrap gap-1">
          <span
            v-for="(ev, idx) in pp.evidence"
            :key="idx"
            class="px-2 py-0.5 text-[11px] bg-neutral-100 dark:bg-neutral-800 text-neutral-600 dark:text-neutral-300 rounded"
          >{{ ev }}</span>
        </div>
      </li>
    </ul>
  </div>
</template>
