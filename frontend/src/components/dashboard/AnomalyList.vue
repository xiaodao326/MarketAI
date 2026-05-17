<script setup lang="ts">
import type { Anomaly } from '@/types/dashboard'
import { ExclamationTriangleIcon, ShieldExclamationIcon, SparklesIcon } from '@heroicons/vue/24/outline'

defineProps<{
  anomalies: Anomaly[]
}>()

const SEVERITY_META: Record<string, { icon: typeof ExclamationTriangleIcon; label: string; iconCls: string; bgCls: string; badgeCls: string }> = {
  critical: {
    icon: ShieldExclamationIcon,
    label: '严重',
    iconCls: 'text-red-500',
    bgCls: 'bg-red-50 dark:bg-red-500/12 ring-red-200 dark:ring-red-700/40',
    badgeCls: 'bg-red-100 dark:bg-red-500/20 text-red-700 dark:text-red-300',
  },
  warning: {
    icon: ExclamationTriangleIcon,
    label: '警告',
    iconCls: 'text-amber-500',
    bgCls: 'bg-amber-50 dark:bg-amber-500/12 ring-amber-200 dark:ring-amber-700/40',
    badgeCls: 'bg-amber-100 dark:bg-amber-500/20 text-amber-700 dark:text-amber-300',
  },
  opportunity: {
    icon: SparklesIcon,
    label: '机会',
    iconCls: 'text-emerald-500',
    bgCls: 'bg-emerald-50 dark:bg-emerald-500/12 ring-emerald-200 dark:ring-emerald-700/40',
    badgeCls: 'bg-emerald-100 dark:bg-emerald-500/20 text-emerald-700 dark:text-emerald-300',
  },
}
</script>

<template>
  <div class="rounded-xl bg-[color:var(--color-surface)] border border-[color:var(--color-border)] shadow-card p-5">
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-sm font-semibold text-neutral-700 dark:text-neutral-200">市场异动</h3>
      <span class="text-xs text-neutral-400">最近 {{ anomalies.length }} 条</span>
    </div>
    <ul v-if="anomalies.length" class="space-y-3">
      <li
        v-for="a in anomalies"
        :key="a.id"
        class="flex gap-3 p-3 rounded-lg border border-[color:var(--color-border)] bg-[color:var(--color-surface-muted)]/40 hover:bg-[color:var(--color-surface-muted)] transition-colors"
      >
        <div :class="['w-8 h-8 rounded-lg ring-1 flex items-center justify-center flex-shrink-0', SEVERITY_META[a.severity]?.bgCls]">
          <component :is="SEVERITY_META[a.severity]?.icon" :class="['w-4 h-4', SEVERITY_META[a.severity]?.iconCls]" />
        </div>
        <div class="min-w-0 flex-1">
          <div class="flex items-center gap-2 mb-0.5 flex-wrap">
            <span class="text-sm font-semibold text-neutral-800 dark:text-neutral-100 truncate">{{ a.title }}</span>
            <span :class="['text-[10px] px-1.5 py-0.5 rounded font-medium flex-shrink-0', SEVERITY_META[a.severity]?.badgeCls]">
              {{ SEVERITY_META[a.severity]?.label }}
            </span>
          </div>
          <p class="text-xs text-neutral-500 dark:text-neutral-400 line-clamp-2 leading-relaxed">{{ a.description }}</p>
          <p class="text-[11px] text-neutral-400 mt-1.5">{{ a.occurredAt }} · {{ a.source }}</p>
        </div>
      </li>
    </ul>
    <div v-else class="py-10 text-center text-sm text-neutral-400">暂无异动事件</div>
  </div>
</template>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
