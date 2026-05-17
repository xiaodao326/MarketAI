<script setup lang="ts">
import { CheckCircleIcon } from '@heroicons/vue/24/outline'
import type { ActionItem } from '@/types/insight'

defineProps<{ actions: ActionItem[] }>()
</script>

<template>
  <div class="rounded-xl bg-[color:var(--color-surface)] border border-[color:var(--color-border)] shadow-card p-6 h-full">
    <div class="flex items-center gap-2 mb-4">
      <div class="w-7 h-7 rounded-md bg-brand-50 dark:bg-brand-500/12 flex items-center justify-center">
        <CheckCircleIcon class="w-4 h-4 text-brand-500" />
      </div>
      <h3 class="text-base font-semibold text-neutral-900 dark:text-neutral-100">行动建议</h3>
      <span class="text-xs text-neutral-400">{{ actions?.length || 0 }} 项</span>
    </div>

    <div v-if="!actions?.length" class="text-sm text-neutral-400 py-8 text-center">暂无建议</div>

    <ol class="space-y-3 max-h-[400px] overflow-y-auto pr-1">
      <li
        v-for="(a, i) in actions"
        :key="i"
        class="flex gap-3 rounded-lg border border-[color:var(--color-border)] p-3.5 bg-[color:var(--color-surface)] hover:bg-[color:var(--color-surface-muted)]/40 transition-colors"
      >
        <div class="w-6 h-6 rounded-md bg-brand-50 dark:bg-brand-500/15 text-brand-700 dark:text-brand-300 flex items-center justify-center text-xs font-bold flex-shrink-0 mt-0.5">
          {{ i + 1 }}
        </div>
        <div class="min-w-0 flex-1">
          <h4 class="text-sm font-semibold text-neutral-900 dark:text-neutral-100 mb-1">{{ a.title }}</h4>
          <p class="text-sm text-neutral-600 dark:text-neutral-300 leading-relaxed">{{ a.description }}</p>
        </div>
      </li>
    </ol>
  </div>
</template>
