<script setup lang="ts">
import { LightBulbIcon, CurrencyDollarIcon, ExclamationTriangleIcon } from '@heroicons/vue/24/outline'
import type { DifferentiationInsight, InsightType } from '@/types/competitor'
import AppBadge from '@/components/ui/AppBadge.vue'

defineProps<{ insights: DifferentiationInsight[] }>()

const TYPE_CFG: Record<InsightType, { label: string; icon: typeof LightBulbIcon; tone: 'info' | 'success' | 'danger'; iconBg: string; iconCls: string }> = {
  core_opportunity:  { label: '核心机会', icon: LightBulbIcon,           tone: 'info',    iconBg: 'bg-sky-50 dark:bg-sky-500/12',         iconCls: 'text-sky-500' },
  pricing_strategy:  { label: '定价策略', icon: CurrencyDollarIcon,      tone: 'success', iconBg: 'bg-emerald-50 dark:bg-emerald-500/12', iconCls: 'text-emerald-500' },
  warning:           { label: '风险预警', icon: ExclamationTriangleIcon, tone: 'danger',  iconBg: 'bg-red-50 dark:bg-red-500/12',         iconCls: 'text-red-500' },
}
</script>

<template>
  <div class="rounded-xl bg-[color:var(--color-surface)] border border-[color:var(--color-border)] shadow-card p-6">
    <div class="flex items-center gap-2 mb-5">
      <div class="w-7 h-7 rounded-md bg-brand-50 dark:bg-brand-500/12 flex items-center justify-center">
        <LightBulbIcon class="w-4 h-4 text-brand-500" />
      </div>
      <h3 class="text-base font-semibold text-neutral-900 dark:text-neutral-100">AI 差异化建议</h3>
      <span class="text-xs text-neutral-400">{{ insights?.length || 0 }} 条</span>
    </div>

    <div v-if="!insights?.length" class="text-sm text-neutral-400 py-8 text-center">暂无差异化建议</div>

    <ul class="space-y-3">
      <li
        v-for="(ins, i) in insights"
        :key="i"
        class="rounded-lg border border-[color:var(--color-border)] p-4 bg-[color:var(--color-surface)] hover:bg-[color:var(--color-surface-muted)]/40 transition-colors"
      >
        <div class="flex items-start gap-3">
          <div :class="['w-9 h-9 rounded-lg flex items-center justify-center flex-shrink-0', TYPE_CFG[ins.type]?.iconBg || 'bg-neutral-100 dark:bg-neutral-800']">
            <component :is="TYPE_CFG[ins.type]?.icon || LightBulbIcon" :class="['w-5 h-5', TYPE_CFG[ins.type]?.iconCls || 'text-neutral-400']" />
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-1.5 flex-wrap">
              <AppBadge :tone="TYPE_CFG[ins.type]?.tone || 'neutral'">
                {{ TYPE_CFG[ins.type]?.label || ins.type }}
              </AppBadge>
              <h4 class="text-sm font-semibold text-neutral-900 dark:text-neutral-100">{{ ins.title }}</h4>
            </div>
            <p class="text-sm text-neutral-600 dark:text-neutral-300 leading-relaxed">{{ ins.description }}</p>
          </div>
        </div>
      </li>
    </ul>
  </div>
</template>
