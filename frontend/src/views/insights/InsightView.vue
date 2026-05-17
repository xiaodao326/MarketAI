<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { insightApi } from '@/api/insight'
import {
  PlusIcon, ArrowDownTrayIcon, ArrowPathIcon,
  LightBulbIcon, ClockIcon,
} from '@heroicons/vue/24/outline'
import MarketFitScoreRing from '@/components/insights/MarketFitScoreRing.vue'
import PainPointList from '@/components/insights/PainPointList.vue'
import OpportunityGrid from '@/components/insights/OpportunityGrid.vue'
import RiskList from '@/components/insights/RiskList.vue'
import ActionList from '@/components/insights/ActionList.vue'
import CreateInsightModal from '@/components/insights/CreateInsightModal.vue'
import AskAIModal from '@/components/insights/AskAIModal.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import AppEmpty from '@/components/ui/AppEmpty.vue'
import AppSkeleton from '@/components/ui/AppSkeleton.vue'
import { toast } from '@/utils/feedback'
import type { InsightReport, InsightReportSummary, PainPoint, Opportunity } from '@/types/insight'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()

const projectId = computed(() => Number(route.params.id))
const project = computed(() => projectStore.currentProject)

const reports = ref<InsightReportSummary[]>([])
const currentReport = ref<InsightReport | null>(null)
const selectedId = ref<number | null>(null)
const listLoading = ref(false)
const detailLoading = ref(false)
const listError = ref<string | null>(null)
const detailError = ref<string | null>(null)

const showCreateModal = ref(false)
const askContext = ref<{ title: string; context: string } | null>(null)

function scoreColor(score: number) {
  if (score >= 80) return 'text-emerald-600 dark:text-emerald-400'
  if (score >= 60) return 'text-brand-600 dark:text-brand-400'
  if (score >= 40) return 'text-amber-600 dark:text-amber-400'
  return 'text-red-600 dark:text-red-400'
}

function formatDate(s: string) {
  if (!s) return ''
  const d = new Date(s.replace(' ', 'T'))
  if (isNaN(d.getTime())) return s
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

async function loadList() {
  listLoading.value = true
  listError.value = null
  try {
    const page = await insightApi.getReportList(projectId.value, 1, 50)
    reports.value = page.records || []
    if (reports.value.length && !selectedId.value) {
      selectedId.value = reports.value[0].id
    } else if (!reports.value.length) {
      currentReport.value = null
      selectedId.value = null
    }
  } catch (e: unknown) {
    listError.value = e instanceof Error ? e.message : '加载报告列表失败'
  } finally {
    listLoading.value = false
  }
}

async function loadDetail(reportId: number) {
  detailLoading.value = true
  detailError.value = null
  try {
    currentReport.value = await insightApi.getReport(reportId)
  } catch (e: unknown) {
    detailError.value = e instanceof Error ? e.message : '加载报告详情失败'
    currentReport.value = null
  } finally {
    detailLoading.value = false
  }
}

function selectReport(id: number) {
  selectedId.value = id
}

function exportJson() {
  if (!currentReport.value) return
  const blob = new Blob([JSON.stringify(currentReport.value, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `insight-report-${currentReport.value.id}.json`
  a.click()
  URL.revokeObjectURL(url)
  toast.success('报告已导出')
}

async function handleCompleted(reportId: number) {
  showCreateModal.value = false
  toast.success('分析报告已生成')
  await loadList()
  selectedId.value = reportId
}

function openAskForPain(pp: PainPoint) {
  askContext.value = { title: pp.title, context: pp.description }
}
function openAskForOpp(op: Opportunity) {
  askContext.value = { title: op.name, context: op.description }
}

watch(selectedId, (id) => {
  if (id) loadDetail(id)
})

onMounted(async () => {
  if (!project.value || project.value.id !== projectId.value) {
    try {
      await projectStore.fetchDetail(projectId.value)
    } catch {
      router.replace('/projects')
      return
    }
  }
  await loadList()
})
</script>

<template>
  <div class="max-w-[1400px] mx-auto">
    <PageHeader title="AI 需求洞察" backable @back="router.push(`/projects/${projectId}`)">
      <template #subtitle>
        <span v-if="project">{{ project.name }} · 一键生成结构化市场分析报告</span>
        <span v-else>一键生成结构化市场分析报告</span>
      </template>
      <template #actions>
        <AppButton v-if="currentReport" variant="secondary" @click="exportJson">
          <ArrowDownTrayIcon class="w-4 h-4" />导出 JSON
        </AppButton>
        <AppButton variant="gradient" @click="showCreateModal = true">
          <PlusIcon class="w-4 h-4" />新建分析
        </AppButton>
      </template>
    </PageHeader>

    <!-- 移动端:历史报告下拉 -->
    <div class="md:hidden mb-4">
      <label class="block text-xs text-neutral-500 dark:text-neutral-400 mb-1.5">历史报告 ({{ reports.length }})</label>
      <select
        v-model="selectedId"
        class="w-full px-3.5 py-2.5 rounded-md border border-[color:var(--color-border)] bg-[color:var(--color-surface)] text-sm focus:outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/15"
      >
        <option v-if="!reports.length" :value="null">暂无报告</option>
        <option v-for="r in reports" :key="r.id" :value="r.id">
          {{ r.title }} · {{ formatDate(r.createdAt) }}
        </option>
      </select>
    </div>

    <div class="flex flex-col md:flex-row gap-5">
      <!-- 侧边栏(桌面) -->
      <aside class="hidden md:block w-72 flex-shrink-0">
        <AppCard padding="sm">
          <div class="flex items-center justify-between mb-3 px-1">
            <h3 class="text-sm font-semibold text-neutral-900 dark:text-neutral-100">历史报告</h3>
            <button
              :disabled="listLoading"
              class="text-neutral-400 hover:text-brand-600 transition-colors"
              title="刷新"
              @click="loadList"
            >
              <ArrowPathIcon :class="['w-4 h-4', listLoading && 'animate-spin']" />
            </button>
          </div>

          <p v-if="listError" class="text-xs text-red-500 px-1">{{ listError }}</p>

          <div v-if="!reports.length && !listLoading" class="text-center py-8">
            <LightBulbIcon class="w-10 h-10 text-neutral-300 dark:text-neutral-600 mx-auto mb-2" />
            <p class="text-xs text-neutral-400">还没有分析报告</p>
            <p class="text-xs text-neutral-400 mt-0.5">点击"新建分析"开始</p>
          </div>

          <ul class="space-y-1 max-h-[640px] overflow-y-auto">
            <li
              v-for="r in reports"
              :key="r.id"
              :class="[
                'relative pl-3 pr-3 py-2.5 rounded-lg cursor-pointer transition-all',
                selectedId === r.id
                  ? 'bg-brand-50 dark:bg-brand-500/12 text-brand-900 dark:text-brand-100'
                  : 'hover:bg-[color:var(--color-surface-muted)]',
              ]"
              @click="selectReport(r.id)"
            >
              <span v-if="selectedId === r.id" class="absolute left-0 top-1/2 -translate-y-1/2 w-0.5 h-6 bg-brand-500 rounded-r"></span>
              <div class="flex items-start justify-between gap-2 mb-1">
                <h4 class="text-sm font-medium text-neutral-900 dark:text-neutral-100 line-clamp-2 flex-1">{{ r.title }}</h4>
                <span :class="['text-sm font-bold tabular-nums flex-shrink-0', scoreColor(r.marketFitScore || 0)]">
                  {{ r.marketFitScore || '--' }}
                </span>
              </div>
              <div class="flex items-center gap-1 text-xs text-neutral-400">
                <ClockIcon class="w-3 h-3" />
                <span>{{ formatDate(r.completedAt || r.createdAt) }}</span>
              </div>
            </li>
          </ul>
        </AppCard>
      </aside>

      <!-- 主内容 -->
      <main class="flex-1 min-w-0">
        <!-- 空态 -->
        <AppCard v-if="!selectedId && !listLoading" padding="lg">
          <AppEmpty
            :icon="LightBulbIcon"
            size="lg"
            title="还没有分析报告"
            description="点击右上角『新建分析』,让 AI 为你生成结构化的市场洞察报告"
          >
            <template #action>
              <AppButton variant="gradient" size="lg" @click="showCreateModal = true">
                <PlusIcon class="w-4 h-4" />开始第一次分析
              </AppButton>
            </template>
          </AppEmpty>
        </AppCard>

        <!-- 骨架 -->
        <div v-else-if="detailLoading && !currentReport" class="space-y-4">
          <AppCard padding="lg">
            <div class="flex items-center gap-8">
              <AppSkeleton width="160px" height="160px" rounded="full" />
              <div class="flex-1 space-y-3">
                <AppSkeleton :rows="4" height="14px" />
              </div>
            </div>
          </AppCard>
          <AppCard padding="lg"><AppSkeleton :rows="4" height="20px" /></AppCard>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <AppCard padding="lg"><AppSkeleton :rows="3" /></AppCard>
            <AppCard padding="lg"><AppSkeleton :rows="3" /></AppCard>
          </div>
        </div>

        <!-- 错误 -->
        <div
          v-else-if="detailError"
          class="bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-700/40 rounded-lg p-4 flex items-center justify-between"
        >
          <p class="text-sm text-red-700 dark:text-red-300">{{ detailError }}</p>
          <AppButton variant="danger" size="sm" @click="selectedId && loadDetail(selectedId)">重试</AppButton>
        </div>

        <!-- 报告详情 -->
        <div v-else-if="currentReport" class="space-y-4">
          <div class="flex flex-wrap items-end justify-between gap-2">
            <div>
              <h2 class="text-lg font-semibold text-neutral-900 dark:text-neutral-100">{{ currentReport.title }}</h2>
              <p class="text-xs text-neutral-500 dark:text-neutral-400 mt-1">
                {{ currentReport.aiModel }} · {{ currentReport.tokensUsed?.toLocaleString() }} tokens ·
                {{ formatDate(currentReport.completedAt || currentReport.createdAt) }}
              </p>
            </div>
          </div>

          <MarketFitScoreRing :score="currentReport.marketFitScore" :dimensions="currentReport.dimensions" />
          <PainPointList :pain-points="currentReport.painPoints || []" @ask="openAskForPain" />
          <OpportunityGrid :opportunities="currentReport.opportunities || []" @ask="openAskForOpp" />
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <RiskList :risks="currentReport.risks || []" />
            <ActionList :actions="currentReport.actions || []" />
          </div>
        </div>
      </main>
    </div>

    <CreateInsightModal
      v-if="showCreateModal"
      :project-id="projectId"
      @close="showCreateModal = false"
      @completed="handleCompleted"
    />

    <AskAIModal
      v-if="askContext"
      :title="askContext.title"
      :context="askContext.context"
      @close="askContext = null"
    />
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
