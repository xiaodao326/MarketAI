<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { insightApi } from '@/api/insight'
import {
  ArrowLeftIcon, PlusIcon, ArrowDownTrayIcon, ArrowPathIcon,
  LightBulbIcon, ClockIcon,
} from '@heroicons/vue/24/outline'
import MarketFitScoreRing from '@/components/insights/MarketFitScoreRing.vue'
import PainPointList from '@/components/insights/PainPointList.vue'
import OpportunityGrid from '@/components/insights/OpportunityGrid.vue'
import RiskList from '@/components/insights/RiskList.vue'
import ActionList from '@/components/insights/ActionList.vue'
import CreateInsightModal from '@/components/insights/CreateInsightModal.vue'
import AskAIModal from '@/components/insights/AskAIModal.vue'
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

// 评分颜色 — 与环形组件保持一致
function scoreColor(score: number) {
  if (score >= 80) return 'text-emerald-500'
  if (score >= 60) return 'text-blue-500'
  if (score >= 40) return 'text-amber-500'
  return 'text-red-500'
}

function formatDate(s: string) {
  if (!s) return ''
  // 后端可能返回 'yyyy-MM-dd HH:mm:ss' 或 ISO,做兼容处理
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
    // 默认选中第一个
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
}

async function handleCompleted(reportId: number) {
  showCreateModal.value = false
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
  <div>
    <!-- 顶部工具栏 -->
    <div class="flex flex-wrap items-center justify-between gap-3 mb-6">
      <div class="flex items-center gap-3">
        <button
          class="p-1.5 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
          title="返回项目"
          @click="router.push(`/projects/${projectId}`)"
        >
          <ArrowLeftIcon class="w-5 h-5" />
        </button>
        <div>
          <h1 class="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
            <LightBulbIcon class="w-5 h-5 text-primary-500" />AI 需求洞察
          </h1>
          <p
            v-if="project"
            class="text-sm text-gray-500 dark:text-gray-400"
          >
            {{ project.name }}
          </p>
        </div>
      </div>

      <div class="flex items-center gap-2">
        <button
          v-if="currentReport"
          class="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm text-gray-600 dark:text-gray-300 border border-gray-200 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
          @click="exportJson"
        >
          <ArrowDownTrayIcon class="w-4 h-4" />导出 JSON
        </button>
        <button
          class="inline-flex items-center gap-1.5 px-4 py-1.5 text-sm bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
          @click="showCreateModal = true"
        >
          <PlusIcon class="w-4 h-4" />新建分析
        </button>
      </div>
    </div>

    <!-- 移动端:历史报告下拉 -->
    <div class="md:hidden mb-4">
      <label class="block text-xs text-gray-500 dark:text-gray-400 mb-1">历史报告 ({{ reports.length }})</label>
      <select
        v-model="selectedId"
        class="w-full px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 dark:text-white"
      >
        <option
          v-if="!reports.length"
          :value="null"
        >
          暂无报告
        </option>
        <option
          v-for="r in reports"
          :key="r.id"
          :value="r.id"
        >
          {{ r.title }} · {{ formatDate(r.createdAt) }}
        </option>
      </select>
    </div>

    <div class="flex flex-col md:flex-row gap-6">
      <!-- 侧边栏(桌面端) -->
      <aside class="hidden md:block w-1/4 flex-shrink-0">
        <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
          <div class="flex items-center justify-between mb-3">
            <h3 class="text-sm font-semibold text-gray-900 dark:text-white">
              历史报告
            </h3>
            <button
              :disabled="listLoading"
              class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
              title="刷新"
              @click="loadList"
            >
              <ArrowPathIcon :class="['w-4 h-4', listLoading && 'animate-spin']" />
            </button>
          </div>

          <p
            v-if="listError"
            class="text-xs text-red-500"
          >
            {{ listError }}
          </p>

          <div
            v-if="!reports.length && !listLoading"
            class="text-center py-8"
          >
            <LightBulbIcon class="w-10 h-10 text-gray-300 dark:text-gray-600 mx-auto mb-2" />
            <p class="text-xs text-gray-400">
              还没有分析报告
            </p>
            <p class="text-xs text-gray-400 mt-0.5">
              点击"新建分析"开始
            </p>
          </div>

          <ul class="space-y-2 max-h-[600px] overflow-y-auto">
            <li
              v-for="r in reports"
              :key="r.id"
              :class="[
                'p-3 rounded-lg cursor-pointer transition-colors border',
                selectedId === r.id
                  ? 'border-primary-300 bg-primary-50 dark:bg-primary-900/20 dark:border-primary-700'
                  : 'border-transparent hover:bg-gray-50 dark:hover:bg-gray-700',
              ]"
              @click="selectReport(r.id)"
            >
              <div class="flex items-start justify-between gap-2 mb-1">
                <h4 class="text-sm font-medium text-gray-900 dark:text-white line-clamp-2 flex-1">
                  {{ r.title }}
                </h4>
                <span :class="['text-sm font-semibold flex-shrink-0', scoreColor(r.marketFitScore || 0)]">
                  {{ r.marketFitScore || '--' }}
                </span>
              </div>
              <div class="flex items-center gap-1 text-xs text-gray-400">
                <ClockIcon class="w-3 h-3" />
                <span>{{ formatDate(r.completedAt || r.createdAt) }}</span>
              </div>
            </li>
          </ul>
        </div>
      </aside>

      <!-- 主内容区 -->
      <main class="flex-1 min-w-0">
        <!-- 空态 -->
        <div
          v-if="!selectedId && !listLoading"
          class="rounded-xl border-2 border-dashed border-gray-300 dark:border-gray-600 p-16 text-center"
        >
          <LightBulbIcon class="w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-3" />
          <p class="text-gray-500 dark:text-gray-400 text-sm">
            还没有分析报告
          </p>
          <button
            class="mt-4 inline-flex items-center gap-1.5 px-4 py-2 text-sm bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
            @click="showCreateModal = true"
          >
            <PlusIcon class="w-4 h-4" />开始第一次分析
          </button>
        </div>

        <!-- 骨架屏 -->
        <div
          v-else-if="detailLoading && !currentReport"
          class="space-y-4"
        >
          <div class="animate-pulse bg-gray-200 dark:bg-gray-700 rounded-xl h-44" />
          <div class="animate-pulse bg-gray-200 dark:bg-gray-700 rounded-xl h-64" />
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="animate-pulse bg-gray-200 dark:bg-gray-700 rounded-xl h-56" />
            <div class="animate-pulse bg-gray-200 dark:bg-gray-700 rounded-xl h-56" />
          </div>
        </div>

        <!-- 错误 -->
        <div
          v-else-if="detailError"
          class="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-5 flex items-center justify-between"
        >
          <p class="text-red-600 dark:text-red-400 text-sm">
            {{ detailError }}
          </p>
          <button
            class="ml-4 px-4 py-1.5 bg-red-500 hover:bg-red-600 text-white text-sm rounded-lg transition-colors flex-shrink-0"
            @click="selectedId && loadDetail(selectedId)"
          >
            重试
          </button>
        </div>

        <!-- 报告详情 -->
        <div
          v-else-if="currentReport"
          class="space-y-4"
        >
          <!-- 标题元数据 -->
          <div class="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 class="text-base font-semibold text-gray-900 dark:text-white">
                {{ currentReport.title }}
              </h2>
              <p class="text-xs text-gray-400 mt-0.5">
                {{ currentReport.aiModel }} · {{ currentReport.tokensUsed?.toLocaleString() }} tokens ·
                {{ formatDate(currentReport.completedAt || currentReport.createdAt) }}
              </p>
            </div>
          </div>

          <!-- 契合度评分环 + 4 维度 -->
          <MarketFitScoreRing
            :score="currentReport.marketFitScore"
            :dimensions="currentReport.dimensions"
          />

          <!-- 痛点 -->
          <PainPointList
            :pain-points="currentReport.painPoints || []"
            @ask="openAskForPain"
          />

          <!-- 机会矩阵 -->
          <OpportunityGrid
            :opportunities="currentReport.opportunities || []"
            @ask="openAskForOpp"
          />

          <!-- 风险 + 行动 -->
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
