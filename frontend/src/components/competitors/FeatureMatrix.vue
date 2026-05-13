<script setup lang="ts">
import { computed } from 'vue'
import { CheckIcon, MinusIcon, XMarkIcon } from '@heroicons/vue/24/outline'
import type { FeatureMatrix, FeatureScore } from '@/types/competitor'

const props = defineProps<{ matrix: FeatureMatrix }>()

const competitorNames = computed(() => Object.keys(props.matrix?.scores || {}))

const SCORE_CFG: Record<FeatureScore, { icon: typeof CheckIcon; cls: string; label: string }> = {
  yes:     { icon: CheckIcon,   cls: 'text-emerald-500',           label: '完全支持' },
  partial: { icon: MinusIcon,   cls: 'text-amber-500',             label: '部分支持' },
  no:      { icon: XMarkIcon,   cls: 'text-gray-300 dark:text-gray-600', label: '不支持' },
}

// 解析机会标记 "功能名: 空白" / "功能名: 红海" 成 Map<功能名, 类型>
const opportunityMap = computed(() => {
  const map: Record<string, '空白' | '红海' | string> = {}
  ;(props.matrix?.opportunities || []).forEach(s => {
    const [feat, kind] = s.split(/[::]/).map(x => x.trim())
    if (feat) map[feat] = kind || ''
  })
  return map
})

function oppCls(kind: string) {
  if (kind === '空白') return 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300'
  if (kind === '红海') return 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300'
  return 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300'
}
</script>

<template>
  <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
    <div class="flex items-baseline justify-between mb-4">
      <h3 class="text-base font-semibold text-gray-900 dark:text-white">
        功能对比矩阵
      </h3>
      <span class="text-xs text-gray-400">{{ matrix.features?.length || 0 }} 个维度 × {{ competitorNames.length }} 个竞品</span>
    </div>

    <!-- 窄屏横向滚动而非压缩 -->
    <div class="overflow-x-auto">
      <table class="min-w-full text-sm">
        <thead>
          <tr class="border-b border-gray-200 dark:border-gray-700">
            <th class="text-left py-2 pr-4 font-medium text-gray-600 dark:text-gray-300 sticky left-0 bg-white dark:bg-gray-800 z-10 min-w-[140px]">
              功能维度
            </th>
            <th
              v-for="name in competitorNames"
              :key="name"
              class="text-center py-2 px-3 font-medium text-gray-600 dark:text-gray-300 min-w-[100px]"
            >
              {{ name }}
            </th>
            <th class="text-center py-2 px-3 font-medium text-gray-600 dark:text-gray-300 min-w-[80px]">
              机会
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(feat, idx) in matrix.features"
            :key="feat"
            class="border-b border-gray-100 dark:border-gray-700/50"
          >
            <td class="py-2.5 pr-4 text-gray-900 dark:text-white sticky left-0 bg-white dark:bg-gray-800">
              {{ feat }}
            </td>
            <td
              v-for="name in competitorNames"
              :key="name"
              class="text-center py-2.5 px-3"
            >
              <component
                :is="SCORE_CFG[matrix.scores[name]?.[idx]]?.icon || SCORE_CFG.no.icon"
                :class="['w-5 h-5 mx-auto', SCORE_CFG[matrix.scores[name]?.[idx]]?.cls || SCORE_CFG.no.cls]"
                :title="SCORE_CFG[matrix.scores[name]?.[idx]]?.label || '未知'"
              />
            </td>
            <td class="text-center py-2.5 px-3">
              <span
                v-if="opportunityMap[feat]"
                :class="['inline-block px-2 py-0.5 text-xs font-medium rounded-full', oppCls(opportunityMap[feat])]"
              >{{ opportunityMap[feat] }}</span>
              <span
                v-else
                class="text-gray-300 dark:text-gray-600 text-xs"
              >—</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 图例 -->
    <div class="mt-4 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-gray-500 dark:text-gray-400">
      <span class="inline-flex items-center gap-1"><CheckIcon class="w-3.5 h-3.5 text-emerald-500" /> 完全支持</span>
      <span class="inline-flex items-center gap-1"><MinusIcon class="w-3.5 h-3.5 text-amber-500" /> 部分支持</span>
      <span class="inline-flex items-center gap-1"><XMarkIcon class="w-3.5 h-3.5 text-gray-400" /> 不支持</span>
      <span class="inline-flex items-center gap-1"><span class="w-3.5 h-3.5 rounded bg-blue-100" /> 空白机会</span>
      <span class="inline-flex items-center gap-1"><span class="w-3.5 h-3.5 rounded bg-purple-100" /> 红海竞争</span>
    </div>
  </div>
</template>
