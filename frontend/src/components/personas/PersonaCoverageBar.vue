<script setup lang="ts">
import { computed } from 'vue'
import type { Persona } from '@/types/persona'

// Phase 1 简化覆盖度:基于已有画像汇总 4 个维度的"覆盖度"
// - 市场份额覆盖: 所有画像 market_share 总和 / 100
// - 高付费意愿覆盖: high 付费意愿画像 market_share 总和 / 100
// - 高技术能力覆盖: high 技术能力画像 market_share 总和 / 100
// - 短决策周期覆盖: 决策周期含"周"字的画像 market_share 总和 / 100

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
    { key: 'total', label: '市场覆盖度', pct: total, hint: '所有画像市场份额之和', bar: 'bg-blue-500' },
    { key: 'pay', label: '高付费意愿', pct: highPay, hint: '付费意愿=high 的画像份额', bar: 'bg-emerald-500' },
    { key: 'tech', label: '高技术能力', pct: highTech, hint: '技术能力=high 的画像份额', bar: 'bg-purple-500' },
    { key: 'cycle', label: '短决策周期', pct: shortCycle, hint: '决策周期为"周"的画像份额', bar: 'bg-amber-500' },
  ]
})
</script>

<template>
  <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
    <div class="flex items-baseline justify-between mb-4">
      <h3 class="text-base font-semibold text-gray-900 dark:text-white">
        画像覆盖度
      </h3>
      <span class="text-xs text-gray-400">基于 {{ personas.length }} 个画像汇总</span>
    </div>
    <div class="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-4">
      <div
        v-for="item in items"
        :key="item.key"
      >
        <div class="flex justify-between items-baseline mb-1.5">
          <div>
            <span class="text-sm text-gray-700 dark:text-gray-300">{{ item.label }}</span>
            <span class="text-xs text-gray-400 ml-2">{{ item.hint }}</span>
          </div>
          <span class="text-sm font-medium text-gray-900 dark:text-white">{{ item.pct }}%</span>
        </div>
        <div class="h-2 bg-gray-100 dark:bg-gray-700 rounded-full overflow-hidden">
          <div
            :class="['h-full rounded-full transition-all duration-500', item.bar]"
            :style="{ width: `${item.pct}%` }"
          />
        </div>
      </div>
    </div>
  </div>
</template>
