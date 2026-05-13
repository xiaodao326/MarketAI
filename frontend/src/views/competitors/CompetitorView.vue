<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { competitorApi } from '@/api/competitor'
import { useCompetitorAnalysis } from '@/composables/useCompetitorAnalysis'
import {
  ArrowLeftIcon, BuildingOffice2Icon, SparklesIcon, PlusIcon, ArrowPathIcon,
  ExclamationCircleIcon, CheckCircleIcon,
} from '@heroicons/vue/24/outline'
import CompetitorCard from '@/components/competitors/CompetitorCard.vue'
import FeatureMatrix from '@/components/competitors/FeatureMatrix.vue'
import InsightList from '@/components/competitors/InsightList.vue'
import AddCompetitorModal from '@/components/competitors/AddCompetitorModal.vue'
import type { Competitor, FeatureMatrix as FeatureMatrixT, DifferentiationInsight } from '@/types/competitor'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()

const projectId = computed(() => Number(route.params.id))
const project = computed(() => projectStore.currentProject)

const competitors = ref<Competitor[]>([])
const matrix = ref<FeatureMatrixT | null>(null)
const insights = ref<DifferentiationInsight[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

const showAddModal = ref(false)

const {
  createTask, taskStatus, isPolling, isCompleted,
  error: taskError, displayProgress, reset,
} = useCompetitorAnalysis()

// 项目尚未配置竞品名单则禁用 AI 分析按钮
const competitorNamesFromProject = computed(() => project.value?.competitors || [])
const canAnalyze = computed(() => competitorNamesFromProject.value.length > 0 && !isPolling.value)

async function loadAll() {
  loading.value = true
  error.value = null
  try {
    const [c, m, i] = await Promise.all([
      competitorApi.listByProject(projectId.value),
      competitorApi.getMatrix(projectId.value),
      competitorApi.getInsights(projectId.value),
    ])
    competitors.value = c
    matrix.value = m
    insights.value = i
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : '加载竞品数据失败'
  } finally {
    loading.value = false
  }
}

async function startAnalysis() {
  if (!canAnalyze.value) return
  try {
    await createTask({ projectId: projectId.value })
  } catch {
    // 错误已在 composable 中
  }
}

// 分析完成后,刷新数据 + 延迟关闭进度条
watch(isCompleted, async (done) => {
  if (done) {
    await loadAll()
    setTimeout(reset, 800)
  }
})

async function handleAdded() {
  showAddModal.value = false
  // 刷新项目以拿到最新 competitors 列表
  await projectStore.fetchDetail(projectId.value)
}

onMounted(async () => {
  if (!project.value || project.value.id !== projectId.value) {
    try {
      await projectStore.fetchDetail(projectId.value)
    } catch {
      router.replace('/projects')
      return
    }
  }
  await loadAll()
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
            <BuildingOffice2Icon class="w-5 h-5 text-emerald-500" />竞品分析
          </h1>
          <p
            v-if="project"
            class="text-sm text-gray-500 dark:text-gray-400"
          >
            {{ project.name }} · 配置 {{ competitorNamesFromProject.length }} 个竞品 · 已分析 {{ competitors.length }} 个
          </p>
        </div>
      </div>

      <div class="flex items-center gap-2">
        <button
          :disabled="loading"
          class="p-1.5 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors disabled:opacity-40"
          title="刷新"
          @click="loadAll"
        >
          <ArrowPathIcon :class="['w-5 h-5', loading && 'animate-spin']" />
        </button>
        <button
          class="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm text-gray-600 dark:text-gray-300 border border-gray-200 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
          @click="showAddModal = true"
        >
          <PlusIcon class="w-4 h-4" />添加竞品
        </button>
        <button
          :disabled="!canAnalyze"
          :title="competitorNamesFromProject.length === 0 ? '请先添加竞品后再分析' : '触发 AI 分析'"
          class="inline-flex items-center gap-1.5 px-4 py-1.5 text-sm bg-primary-500 text-white rounded-lg hover:bg-primary-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          @click="startAnalysis"
        >
          <SparklesIcon class="w-4 h-4" />AI 分析
        </button>
      </div>
    </div>

    <!-- 任务进度横幅 -->
    <div
      v-if="taskStatus && !taskError"
      class="bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800 rounded-lg p-4 mb-6 flex items-center gap-4"
    >
      <CheckCircleIcon
        v-if="isCompleted"
        class="w-6 h-6 text-emerald-500 flex-shrink-0"
      />
      <ArrowPathIcon
        v-else
        class="w-6 h-6 text-primary-500 animate-spin flex-shrink-0"
      />
      <div class="flex-1 min-w-0">
        <p class="text-sm font-medium text-gray-900 dark:text-white">
          {{ isCompleted ? '竞品分析完成' : '正在分析竞品…' }}
        </p>
        <div class="mt-1.5 h-1.5 bg-white dark:bg-gray-700 rounded-full overflow-hidden">
          <div
            class="h-full bg-primary-500 transition-all duration-500"
            :style="{ width: `${displayProgress}%` }"
          />
        </div>
      </div>
      <span class="text-xs text-gray-500 dark:text-gray-400 flex-shrink-0">{{ displayProgress }}%</span>
    </div>

    <div
      v-else-if="taskError"
      class="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4 mb-6 flex items-center justify-between"
    >
      <div class="flex items-center gap-3">
        <ExclamationCircleIcon class="w-5 h-5 text-red-500" />
        <p class="text-sm text-red-600 dark:text-red-400">
          {{ taskError }}
        </p>
      </div>
      <button
        class="px-3 py-1 bg-red-500 hover:bg-red-600 text-white text-xs rounded transition-colors"
        @click="startAnalysis"
      >
        重新分析
      </button>
    </div>

    <!-- 加载错误 -->
    <div
      v-if="error"
      class="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-5 flex items-center justify-between mb-6"
    >
      <p class="text-red-600 dark:text-red-400 text-sm">
        {{ error }}
      </p>
      <button
        class="ml-4 px-4 py-1.5 bg-red-500 hover:bg-red-600 text-white text-sm rounded-lg transition-colors flex-shrink-0"
        @click="loadAll"
      >
        重试
      </button>
    </div>

    <!-- 空态:既无配置又无数据 -->
    <div
      v-if="!loading && !competitors.length && !competitorNamesFromProject.length"
      class="rounded-xl border-2 border-dashed border-gray-300 dark:border-gray-600 p-16 text-center"
    >
      <BuildingOffice2Icon class="w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-3" />
      <p class="text-gray-500 dark:text-gray-400 text-sm">
        项目尚未配置竞品名单
      </p>
      <button
        class="mt-4 inline-flex items-center gap-1.5 px-4 py-2 text-sm bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
        @click="showAddModal = true"
      >
        <PlusIcon class="w-4 h-4" />添加第一个竞品
      </button>
    </div>

    <!-- 有配置无数据态 -->
    <div
      v-else-if="!loading && !competitors.length && competitorNamesFromProject.length"
      class="rounded-xl border-2 border-dashed border-gray-300 dark:border-gray-600 p-16 text-center"
    >
      <SparklesIcon class="w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-3" />
      <p class="text-gray-500 dark:text-gray-400 text-sm">
        已配置 {{ competitorNamesFromProject.length }} 个竞品,点击 "AI 分析" 生成对比矩阵
      </p>
      <p class="text-xs text-gray-400 mt-1">
        {{ competitorNamesFromProject.join('、') }}
      </p>
    </div>

    <!-- 骨架屏 -->
    <div
      v-if="loading && !competitors.length && !error"
      class="space-y-4"
    >
      <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        <div
          v-for="i in 3"
          :key="i"
          class="animate-pulse bg-gray-200 dark:bg-gray-700 rounded-xl h-56"
        />
      </div>
      <div class="animate-pulse bg-gray-200 dark:bg-gray-700 rounded-xl h-64" />
    </div>

    <!-- 主内容 -->
    <template v-else-if="competitors.length">
      <!-- 竞品卡片网格 -->
      <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4 mb-6">
        <CompetitorCard
          v-for="c in competitors"
          :key="c.id"
          :competitor="c"
        />
      </div>

      <!-- 功能对比矩阵 -->
      <div
        v-if="matrix?.hasReport"
        class="mb-6"
      >
        <FeatureMatrix :matrix="matrix" />
      </div>

      <!-- AI 差异化建议 -->
      <InsightList
        v-if="insights.length"
        :insights="insights"
      />
    </template>

    <AddCompetitorModal
      v-if="showAddModal"
      :project-id="projectId"
      :existing="competitorNamesFromProject"
      @close="showAddModal = false"
      @added="handleAdded"
    />
  </div>
</template>
