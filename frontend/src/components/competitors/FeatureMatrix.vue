<script setup lang="ts">
import { computed } from 'vue'
import { CheckIcon, MinusIcon, XMarkIcon } from '@heroicons/vue/24/outline'
import type { FeatureMatrix, FeatureScore } from '@/types/competitor'

const props = defineProps<{ matrix: FeatureMatrix }>()

const competitorNames = computed(() => Object.keys(props.matrix?.scores || {}))

const SCORE_CFG: Record<FeatureScore, { icon: typeof CheckIcon; cls: string; label: string }> = {
  yes:     { icon: CheckIcon, cls: 'text-emerald-500',                       label: '完全支持' },
  partial: { icon: MinusIcon, cls: 'text-amber-500',                         label: '部分支持' },
  no:      { icon: XMarkIcon, cls: 'text-neutral-300 dark:text-neutral-600', label: '不支持' },
}

const opportunityMap = computed(() => {
  const map: Record<string, '空白' | '红海' | string> = {}
  ;(props.matrix?.opportunities || []).forEach(s => {
    const [feat, kind] = s.split(/[::]/).map(x => x.trim())
    if (feat) map[feat] = kind || ''
  })
  return map
})

function oppCls(kind: string) {
  if (kind === '空白') return 'bg-sky-100 text-sky-700 dark:bg-sky-500/15 dark:text-sky-300'
  if (kind === '红海') return 'bg-purple-100 text-purple-700 dark:bg-purple-500/15 dark:text-purple-300'
  return 'bg-neutral-100 text-neutral-600 dark:bg-neutral-800 dark:text-neutral-300'
}
</script>

<template>
  <div class="rounded-xl bg-[color:var(--color-surface)] border border-[color:var(--color-border)] shadow-card p-6">
    <div class="flex items-baseline justify-between mb-5">
      <div>
        <h3 class="text-base font-semibold text-neutral-900 dark:text-neutral-100">功能对比矩阵</h3>
        <p class="text-xs text-neutral-500 dark:text-neutral-400 mt-1">{{ matrix.features?.length || 0 }} 个维度 × {{ competitorNames.length }} 个竞品</p>
      </div>
    </div>

    <div class="overflow-x-auto -mx-2 px-2">
      <table class="min-w-full text-sm">
        <thead>
          <tr class="border-b-2 border-[color:var(--color-border)]">
            <th class="text-left py-3 pr-4 pl-2 text-xs font-semibold uppercase tracking-wider text-neutral-500 sticky left-0 bg-[color:var(--color-surface)] z-10 min-w-[140px]">
              功能维度
            </th>
            <th
              v-for="name in competitorNames"
              :key="name"
              class="text-center py-3 px-3 text-xs font-semibold uppercase tracking-wider text-neutral-500 min-w-[100px]"
            >
              {{ name }}
            </th>
            <th class="text-center py-3 px-3 text-xs font-semibold uppercase tracking-wider text-neutral-500 min-w-[80px]">
              机会
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(feat, idx) in matrix.features"
            :key="feat"
            class="border-b border-[color:var(--color-border)] hover:bg-[color:var(--color-surface-muted)]/40 transition-colors"
          >
            <td class="py-3 pr-4 pl-2 text-sm font-medium text-neutral-900 dark:text-neutral-100 sticky left-0 bg-[color:var(--color-surface)] z-10">
              {{ feat }}
            </td>
            <td v-for="name in competitorNames" :key="name" class="text-center py-3 px-3">
              <div class="inline-flex items-center justify-center w-7 h-7 rounded-md" :class="(matrix.scores[name]?.[idx] === 'yes') ? 'bg-emerald-50 dark:bg-emerald-500/10' : (matrix.scores[name]?.[idx] === 'partial') ? 'bg-amber-50 dark:bg-amber-500/10' : 'bg-neutral-50 dark:bg-neutral-800/50'">
                <component
                  :is="SCORE_CFG[matrix.scores[name]?.[idx]]?.icon || SCORE_CFG.no.icon"
                  :class="['w-4 h-4', SCORE_CFG[matrix.scores[name]?.[idx]]?.cls || SCORE_CFG.no.cls]"
                  :title="SCORE_CFG[matrix.scores[name]?.[idx]]?.label || '未知'"
                />
              </div>
            </td>
            <td class="text-center py-3 px-3">
              <span
                v-if="opportunityMap[feat]"
                :class="['inline-block px-2 py-0.5 text-xs font-semibold rounded-full', oppCls(opportunityMap[feat])]"
              >{{ opportunityMap[feat] }}</span>
              <span v-else class="text-neutral-300 dark:text-neutral-600 text-xs">—</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 图例 -->
    <div class="mt-5 flex flex-wrap items-center gap-x-5 gap-y-1.5 text-xs text-neutral-500 dark:text-neutral-400">
      <span class="inline-flex items-center gap-1.5"><CheckIcon class="w-3.5 h-3.5 text-emerald-500" /> 完全支持</span>
      <span class="inline-flex items-center gap-1.5"><MinusIcon class="w-3.5 h-3.5 text-amber-500" /> 部分支持</span>
      <span class="inline-flex items-center gap-1.5"><XMarkIcon class="w-3.5 h-3.5 text-neutral-400" /> 不支持</span>
      <span class="inline-flex items-center gap-1.5"><span class="w-2 h-2 rounded-full bg-sky-500" /> 空白机会</span>
      <span class="inline-flex items-center gap-1.5"><span class="w-2 h-2 rounded-full bg-purple-500" /> 红海竞争</span>
    </div>
  </div>
</template>
