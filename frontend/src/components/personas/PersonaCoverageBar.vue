<script setup lang="ts">
import { computed } from 'vue'
import type { Persona } from '@/types/persona'

const props = defineProps<{ personas: Persona[] }>()

interface CoverageItem {
  key: string
  label: string
  pct: number
  hint: string
  bar: string
}

const items = computed<CoverageItem[]>(() => {
  const sum = (filter: (p: Persona) => boolean) =>
    Math.min(100, Math.round(
      props.personas.filter(filter).reduce((acc, p) => acc + (p.marketShare || 0), 0),
    ))

  const total = sum(() => true)
  const highPay = sum(p => p.decisionParams?.paymentWillingness === 'high')
  const highTech = sum(p => p.decisionParams?.techCapability === 'high')
  const shortCycle = sum(p => /周/.test(p.decisionParams?.decisionCycle || ''))

  return [
    { key: 'total', label: '市场覆盖度', pct: total,       hint: '所有画像市场份额之和',     bar: 'bg-brand-500' },
    { key: 'pay',   label: '高付费意愿', pct: highPay,     hint: '付费意愿 = 高的画像份额',  bar: 'bg-emerald-500' },
    { key: 'tech',  label: '高技术能力', pct: highTech,    hint: '技术能力 = 高的画像份额',  bar: 'bg-purple-500' },
    { key: 'cycle', label: '短决策周期', pct: shortCycle,  hint: '决策周期为"周"的画像份额', bar: 'bg-amber-500' },
  ]
})
</script>

<template>
  <div class="rounded-xl bg-[color:var(--color-surface)] border border-[color:var(--color-border)] shadow-card p-6">
    <div class="flex items-baseline justify-between mb-5">
      <div>
        <h3 class="text-base font-semibold text-neutral-900 dark:text-neutral-100">画像覆盖度</h3>
        <p class="text-xs text-neutral-500 dark:text-neutral-400 mt-1">基于 {{ personas.length }} 个画像汇总</p>
      </div>
    </div>
    <div class="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-5">
      <div v-for="item in items" :key="item.key">
        <div class="flex justify-between items-baseline mb-2">
          <div class="min-w-0 flex-1">
            <span class="text-sm font-medium text-neutral-700 dark:text-neutral-200">{{ item.label }}</span>
            <span class="text-xs text-neutral-400 ml-2">{{ item.hint }}</span>
          </div>
          <span class="text-base font-bold text-neutral-900 dark:text-neutral-100 tabular-nums">{{ item.pct }}%</span>
        </div>
        <div class="h-2 bg-neutral-100 dark:bg-neutral-800 rounded-full overflow-hidden">
          <div
            :class="['h-full rounded-full transition-all duration-700 ease-out', item.bar]"
            :style="{ width: `${item.pct}%` }"
          />
        </div>
      </div>
    </div>
  </div>
</template>
