<script setup lang="ts">
import { ShieldExclamationIcon } from '@heroicons/vue/24/outline'
import type { Risk } from '@/types/insight'
import AppBadge from '@/components/ui/AppBadge.vue'

defineProps<{ risks: Risk[] }>()

const LEVEL_MAP: Record<string, { label: string; tone: 'danger' | 'warning' | 'info' }> = {
  high:   { label: '高风险', tone: 'danger' },
  medium: { label: '中风险', tone: 'warning' },
  low:    { label: '低风险', tone: 'info' },
}
</script>

<template>
  <div class="rounded-xl bg-[color:var(--color-surface)] border border-[color:var(--color-border)] shadow-card p-6 h-full">
    <div class="flex items-center gap-2 mb-4">
      <div class="w-7 h-7 rounded-md bg-amber-50 dark:bg-amber-500/12 flex items-center justify-center">
        <ShieldExclamationIcon class="w-4 h-4 text-amber-500" />
      </div>
      <h3 class="text-base font-semibold text-neutral-900 dark:text-neutral-100">风险评估</h3>
      <span class="text-xs text-neutral-400">{{ risks?.length || 0 }} 项</span>
    </div>

    <div v-if="!risks?.length" class="text-sm text-neutral-400 py-8 text-center">暂无风险数据</div>

    <ul class="space-y-3 max-h-[400px] overflow-y-auto pr-1">
      <li
        v-for="(r, i) in risks"
        :key="i"
        class="rounded-lg border border-[color:var(--color-border)] p-3.5 bg-[color:var(--color-surface)] hover:bg-[color:var(--color-surface-muted)]/40 transition-colors"
      >
        <div class="flex items-center gap-2 mb-1.5 flex-wrap">
          <AppBadge :tone="LEVEL_MAP[r.level]?.tone || 'neutral'">
            {{ LEVEL_MAP[r.level]?.label || r.level }}
          </AppBadge>
          <h4 class="text-sm font-semibold text-neutral-900 dark:text-neutral-100">{{ r.name }}</h4>
        </div>
        <p class="text-sm text-neutral-600 dark:text-neutral-300 leading-relaxed">{{ r.description }}</p>
      </li>
    </ul>
  </div>
</template>
