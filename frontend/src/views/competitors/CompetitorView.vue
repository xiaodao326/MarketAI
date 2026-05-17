<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { competitorApi } from '@/api/competitor'
import { useCompetitorAnalysis } from '@/composables/useCompetitorAnalysis'
import {
  BuildingOffice2Icon, SparklesIcon, PlusIcon, ArrowPathIcon,
  ExclamationCircleIcon, CheckCircleIcon,
} from '@heroicons/vue/24/outline'
import CompetitorCard from '@/components/competitors/CompetitorCard.vue'
import FeatureMatrix from '@/components/competitors/FeatureMatrix.vue'
import InsightList from '@/components/competitors/InsightList.vue'
import AddCompetitorModal from '@/components/competitors/AddCompetitorModal.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import AppCard from '@/components/ui/AppCard.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppEmpty from '@/components/ui/AppEmpty.vue'
import AppSkeleton from '@/components/ui/AppSkeleton.vue'
import { toast } from '@/utils/feedback'
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
    // 错误已在 composable
  }
}

watch(isCompleted, async (done) => {
  if (done) {
    toast.success('竞品分析完成')
    await loadAll()
    setTimeout(reset, 800)
  }
})

async function handleAdded() {
  showAddModal.value = false
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
  <div class="max-w-[1400px] mx-auto">
    <PageHeader title="竞品分析" backable @back="router.push(`/projects/${projectId}`)">
      <template #subtitle>
        <span v-if="project">
          {{ project.name }} · 配置 {{ competitorNamesFromProject.length }} 个竞品 · 已分析 {{ competitors.length }} 个
        </span>
        <span v-else>AI 自动生成功能对比矩阵与差异化建议</span>
      </template>
      <template #actions>
        <button
          :disabled="loading"
          class="w-9 h-9 inline-flex items-center justify-center rounded-md border border-[color:var(--color-border)] text-neutral-500 hover:text-brand-600 hover:border-brand-300 hover:bg-brand-50 dark:hover:bg-brand-500/10 transition-colors disabled:opacity-40"
          title="刷新"
          @click="loadAll"
        >
          <ArrowPathIcon :class="['w-4 h-4', loading && 'animate-spin']" />
        </button>
        <AppButton variant="secondary" @click="showAddModal = true">
          <PlusIcon class="w-4 h-4" />添加竞品
        </AppButton>
        <AppButton
          variant="gradient"
          :disabled="!canAnalyze"
          :title="competitorNamesFromProject.length === 0 ? '请先添加竞品后再分析' : '触发 AI 分析'"
          @click="startAnalysis"
        >
          <SparklesIcon class="w-4 h-4" />AI 分析
        </AppButton>
      </template>
    </PageHeader>

    <!-- 任务进度横幅 -->
    <transition name="fade">
      <div
        v-if="taskStatus && !taskError"
        class="rounded-xl bg-gradient-to-r from-brand-50 to-purple-50 dark:from-brand-500/10 dark:to-purple-500/10 border border-brand-200 dark:border-brand-700/40 p-4 mb-5 flex items-center gap-4"
      >
        <div class="w-10 h-10 rounded-lg bg-brand-gradient flex items-center justify-center flex-shrink-0 shadow-glow">
          <CheckCircleIcon v-if="isCompleted" class="w-5 h-5 text-white" />
          <ArrowPathIcon v-else class="w-5 h-5 text-white animate-spin" />
        </div>
        <div class="flex-1 min-w-0">
          <p class="text-sm font-semibold text-neutral-900 dark:text-neutral-100">
            {{ isCompleted ? '竞品分析完成' : '正在分析竞品…' }}
          </p>
          <div class="mt-2 h-1.5 bg-white/60 dark:bg-neutral-800 rounded-full overflow-hidden">
            <div class="h-full bg-brand-gradient transition-all duration-500" :style="{ width: `${displayProgress}%` }" />
          </div>
        </div>
        <span class="text-xs font-semibold text-neutral-700 dark:text-neutral-200 tabular-nums flex-shrink-0">{{ displayProgress }}%</span>
      </div>
    </transition>

    <div
      v-if="taskError"
      class="bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-700/40 rounded-lg p-4 mb-5 flex items-center justify-between"
    >
      <div class="flex items-center gap-3">
        <ExclamationCircleIcon class="w-5 h-5 text-red-500 flex-shrink-0" />
        <p class="text-sm text-red-700 dark:text-red-300">{{ taskError }}</p>
      </div>
      <AppButton variant="danger" size="sm" @click="startAnalysis">重新分析</AppButton>
    </div>

    <!-- 加载错误 -->
    <div
      v-if="error"
      class="bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-700/40 rounded-lg p-4 flex items-center justify-between mb-5"
    >
      <p class="text-sm text-red-700 dark:text-red-300">{{ error }}</p>
      <AppButton variant="danger" size="sm" @click="loadAll">重试</AppButton>
    </div>

    <!-- 空态:既无配置又无数据 -->
    <AppCard v-if="!loading && !competitors.length && !competitorNamesFromProject.length" padding="lg">
      <AppEmpty
        :icon="BuildingOffice2Icon"
        size="lg"
        title="项目尚未配置竞品名单"
        description="添加竞品后,可触发 AI 分析自动生成功能对比矩阵和差异化建议"
      >
        <template #action>
          <AppButton variant="gradient" size="lg" @click="showAddModal = true">
            <PlusIcon class="w-4 h-4" />添加第一个竞品
          </AppButton>
        </template>
      </AppEmpty>
    </AppCard>

    <!-- 有配置无数据 -->
    <AppCard v-else-if="!loading && !competitors.length && competitorNamesFromProject.length" padding="lg">
      <AppEmpty
        :icon="SparklesIcon"
        size="lg"
        title="已配置竞品,尚未分析"
        :description="`已配置 ${competitorNamesFromProject.length} 个竞品:${competitorNamesFromProject.join('、')}`"
      >
        <template #action>
          <AppButton variant="gradient" size="lg" @click="startAnalysis">
            <SparklesIcon class="w-4 h-4" />立即 AI 分析
          </AppButton>
        </template>
      </AppEmpty>
    </AppCard>

    <!-- 骨架屏 -->
    <div v-if="loading && !competitors.length && !error" class="space-y-5">
      <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        <AppCard v-for="i in 3" :key="i" padding="lg">
          <div class="flex items-start gap-3">
            <AppSkeleton width="40px" height="40px" rounded="lg" />
            <div class="flex-1 space-y-2">
              <AppSkeleton width="60%" height="14px" />
              <AppSkeleton width="40%" height="10px" />
            </div>
          </div>
          <AppSkeleton class="mt-4" :rows="4" />
        </AppCard>
      </div>
    </div>

    <!-- 主内容 -->
    <template v-else-if="competitors.length">
      <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4 mb-5">
        <CompetitorCard v-for="c in competitors" :key="c.id" :competitor="c" />
      </div>

      <div v-if="matrix?.hasReport" class="mb-5">
        <FeatureMatrix :matrix="matrix" />
      </div>

      <InsightList v-if="insights.length" :insights="insights" />
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
