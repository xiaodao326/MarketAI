<script setup lang="ts">
import { LightBulbIcon, CurrencyDollarIcon, ExclamationTriangleIcon } from '@heroicons/vue/24/outline'
import type { DifferentiationInsight, InsightType } from '@/types/competitor'

defineProps<{ insights: DifferentiationInsight[] }>()

const TYPE_CFG: Record<InsightType, { label: string; icon: typeof LightBulbIcon; chip: string; iconCls: string }> = {
  core_opportunity:  { label: '核心机会', icon: LightBulbIcon,         chip: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300',     iconCls: 'text-blue-500' },
  pricing_strategy:  { label: '定价策略', icon: CurrencyDollarIcon,    chip: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300', iconCls: 'text-emerald-500' },
  warning:           { label: '风险预警', icon: ExclamationTriangleIcon, chip: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300',         iconCls: 'text-red-500' },
}
</script>

<template>
  <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
    <div class="flex items-center gap-2 mb-4">
      <LightBulbIcon class="w-5 h-5 text-primary-500" />
      <h3 class="text-base font-semibold text-gray-900 dark:text-white">
        AI 差异化建议
      </h3>
      <span class="text-xs text-gray-400">{{ insights?.length || 0 }} 条</span>
    </div>

    <div
      v-if="!insights?.length"
      class="text-sm text-gray-400 py-6 text-center"
    >
      暂无差异化建议
    </div>

    <ul class="space-y-3">
      <li
        v-for="(ins, i) in insights"
        :key="i"
        class="border border-gray-100 dark:border-gray-700 rounded-lg p-4"
      >
        <div class="flex items-start gap-3">
          <component
            :is="TYPE_CFG[ins.type]?.icon || LightBulbIcon"
            :class="['w-5 h-5 flex-shrink-0 mt-0.5', TYPE_CFG[ins.type]?.iconCls || 'text-gray-400']"
          />
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-1">
              <span :class="['px-2 py-0.5 text-xs font-medium rounded-full', TYPE_CFG[ins.type]?.chip || 'bg-gray-100 text-gray-600']">
                {{ TYPE_CFG[ins.type]?.label || ins.type }}
              </span>
              <h4 class="text-sm font-medium text-gray-900 dark:text-white">
                {{ ins.title }}
              </h4>
            </div>
            <p class="text-sm text-gray-600 dark:text-gray-300 leading-relaxed">
              {{ ins.description }}
            </p>
          </div>
        </div>
      </li>
    </ul>
  </div>
</template>
