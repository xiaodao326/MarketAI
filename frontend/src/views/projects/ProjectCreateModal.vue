<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useProjectStore } from '@/stores/project'
import { XMarkIcon, PlusIcon, CheckIcon, ChevronLeftIcon, ChevronRightIcon, SparklesIcon } from '@heroicons/vue/24/outline'
import type { CreateProjectRequest, Project } from '@/types/project'
import AppButton from '@/components/ui/AppButton.vue'
import { toast } from '@/utils/feedback'

const emit = defineEmits<{ close: []; created: [project: Project] }>()
const projectStore = useProjectStore()
const submitting = ref(false)
const errorMsg = ref('')

const step = ref<1 | 2>(1)
const industries = ['企业服务', '电商零售', '金融', '医疗', '教育', '制造', '其他']
const markets = ['中国', '美国', '欧洲', '东南亚', '全球']

const form = reactive<CreateProjectRequest>({
  name: '',
  description: '',
  industry: '',
  targetMarket: '',
  keywords: [],
  competitors: [],
})

const competitorInput = ref('')
const keywordInput = ref('')

const visible = ref(true)
function close() {
  visible.value = false
  setTimeout(() => emit('close'), 180)
}

function addKeyword() {
  const val = keywordInput.value.trim()
  if (!val) return
  if (form.keywords.length >= 20) { errorMsg.value = '关键词最多 20 个'; return }
  if (!form.keywords.includes(val)) form.keywords.push(val)
  keywordInput.value = ''
  errorMsg.value = ''
}

function removeKeyword(idx: number) { form.keywords.splice(idx, 1) }

function addCompetitor() {
  const val = competitorInput.value.trim()
  if (!val) return
  if (!form.competitors!.includes(val)) form.competitors!.push(val)
  competitorInput.value = ''
}

function removeCompetitor(idx: number) { form.competitors?.splice(idx, 1) }

const canNext = computed(() => {
  if (step.value === 1) return !!(form.name.trim() && form.industry && form.targetMarket)
  if (step.value === 2) return form.keywords.length >= 1
  return true
})

async function submit() {
  if (!canNext.value || submitting.value) return
  submitting.value = true
  errorMsg.value = ''
  try {
    const project = await projectStore.create({ ...form })
    emit('created', project)
  } catch (e: unknown) {
    errorMsg.value = e instanceof Error ? e.message : '创建失败'
    toast.error(errorMsg.value)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <transition name="fade">
      <div v-if="visible" class="fixed inset-0 z-[1000] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-neutral-900/60 backdrop-blur-sm" @click="close" />

        <transition
          appear
          enter-active-class="transition duration-200 ease-out"
          enter-from-class="opacity-0 scale-95 translate-y-2"
          enter-to-class="opacity-100 scale-100 translate-y-0"
        >
          <div class="relative w-full max-w-xl rounded-2xl bg-[color:var(--color-surface-elevated)] shadow-overlay border border-[color:var(--color-border)] overflow-hidden max-h-[92vh] flex flex-col">
            <!-- Header -->
            <div class="flex items-start justify-between px-6 py-5 border-b border-[color:var(--color-border)]">
              <div class="flex items-start gap-3">
                <div class="w-10 h-10 rounded-lg bg-brand-gradient flex items-center justify-center flex-shrink-0">
                  <SparklesIcon class="w-5 h-5 text-white" />
                </div>
                <div>
                  <h3 class="text-base font-semibold text-neutral-900 dark:text-neutral-100">新建分析项目</h3>
                  <p class="text-xs text-neutral-500 mt-0.5">配置项目基础信息与监控关键词,即可启动 AI 分析</p>
                </div>
              </div>
              <button
                class="p-1 rounded text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-200 hover:bg-neutral-100 dark:hover:bg-neutral-800"
                @click="close"
              >
                <XMarkIcon class="w-5 h-5" />
              </button>
            </div>

            <!-- Step indicator -->
            <div class="flex items-center justify-center gap-3 px-6 py-4 border-b border-[color:var(--color-border)] bg-[color:var(--color-surface-muted)]/40">
              <div class="flex items-center gap-2">
                <span
                  :class="[
                    'w-7 h-7 rounded-full flex items-center justify-center text-xs font-semibold transition-all',
                    step >= 1
                      ? 'bg-brand-500 text-white shadow-sm'
                      : 'bg-neutral-200 dark:bg-neutral-700 text-neutral-500',
                  ]"
                >
                  <CheckIcon v-if="step > 1" class="w-3.5 h-3.5" />
                  <span v-else>1</span>
                </span>
                <span :class="['text-sm font-medium', step === 1 ? 'text-brand-600 dark:text-brand-400' : 'text-neutral-500']">
                  基础信息
                </span>
              </div>
              <div class="w-10 h-px bg-neutral-200 dark:bg-neutral-700"></div>
              <div class="flex items-center gap-2">
                <span
                  :class="[
                    'w-7 h-7 rounded-full flex items-center justify-center text-xs font-semibold transition-all',
                    step >= 2
                      ? 'bg-brand-500 text-white shadow-sm'
                      : 'bg-neutral-200 dark:bg-neutral-700 text-neutral-500',
                  ]"
                >
                  2
                </span>
                <span :class="['text-sm font-medium', step === 2 ? 'text-brand-600 dark:text-brand-400' : 'text-neutral-500']">
                  监控配置
                </span>
              </div>
            </div>

            <!-- Body -->
            <div class="flex-1 overflow-y-auto px-6 py-5">
              <!-- Step 1 -->
              <div v-if="step === 1" class="space-y-4">
                <div>
                  <label class="block text-[13px] font-medium text-neutral-700 dark:text-neutral-300 mb-1.5">
                    项目名称 <span class="text-red-500">*</span>
                  </label>
                  <input
                    v-model="form.name"
                    type="text"
                    maxlength="50"
                    placeholder="例如:AI 客服市场分析"
                    class="w-full px-3.5 py-2.5 rounded-md border border-[color:var(--color-border)] bg-[color:var(--color-surface)] text-sm focus:outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/15"
                  />
                  <p class="text-xs text-neutral-400 mt-1.5">{{ form.name.length }} / 50</p>
                </div>
                <div>
                  <label class="block text-[13px] font-medium text-neutral-700 dark:text-neutral-300 mb-1.5">项目描述</label>
                  <textarea
                    v-model="form.description"
                    rows="3"
                    placeholder="简要描述分析目标与背景…"
                    class="w-full px-3.5 py-2.5 rounded-md border border-[color:var(--color-border)] bg-[color:var(--color-surface)] text-sm focus:outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/15 resize-none"
                  />
                </div>
                <div class="grid grid-cols-2 gap-3">
                  <div>
                    <label class="block text-[13px] font-medium text-neutral-700 dark:text-neutral-300 mb-1.5">
                      行业 <span class="text-red-500">*</span>
                    </label>
                    <select
                      v-model="form.industry"
                      class="w-full px-3.5 py-2.5 rounded-md border border-[color:var(--color-border)] bg-[color:var(--color-surface)] text-sm focus:outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/15"
                    >
                      <option value="" disabled>请选择行业</option>
                      <option v-for="ind in industries" :key="ind" :value="ind">{{ ind }}</option>
                    </select>
                  </div>
                  <div>
                    <label class="block text-[13px] font-medium text-neutral-700 dark:text-neutral-300 mb-1.5">
                      目标市场 <span class="text-red-500">*</span>
                    </label>
                    <select
                      v-model="form.targetMarket"
                      class="w-full px-3.5 py-2.5 rounded-md border border-[color:var(--color-border)] bg-[color:var(--color-surface)] text-sm focus:outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/15"
                    >
                      <option value="" disabled>请选择市场</option>
                      <option v-for="m in markets" :key="m" :value="m">{{ m }}</option>
                    </select>
                  </div>
                </div>
              </div>

              <!-- Step 2 -->
              <div v-else class="space-y-5">
                <div>
                  <div class="flex items-center justify-between mb-1.5">
                    <label class="text-[13px] font-medium text-neutral-700 dark:text-neutral-300">
                      监控关键词 <span class="text-red-500">*</span>
                    </label>
                    <span class="text-xs text-neutral-400">{{ form.keywords.length }} / 20</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <input
                      v-model="keywordInput"
                      type="text"
                      placeholder="输入关键词后回车添加"
                      class="flex-1 px-3.5 py-2.5 rounded-md border border-[color:var(--color-border)] bg-[color:var(--color-surface)] text-sm focus:outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/15"
                      @keyup.enter.prevent="addKeyword"
                    />
                    <button
                      type="button"
                      class="p-2.5 rounded-md text-brand-600 dark:text-brand-400 hover:bg-brand-50 dark:hover:bg-brand-500/10 transition-colors border border-[color:var(--color-border)]"
                      @click="addKeyword"
                    >
                      <PlusIcon class="w-4 h-4" />
                    </button>
                  </div>
                  <div class="flex flex-wrap gap-1.5 mt-3 min-h-[28px]">
                    <span
                      v-for="(kw, idx) in form.keywords"
                      :key="idx"
                      class="inline-flex items-center gap-1 px-2.5 py-1 bg-brand-50 dark:bg-brand-500/12 text-brand-700 dark:text-brand-300 text-xs rounded-full"
                    >
                      {{ kw }}
                      <button class="text-brand-400 hover:text-brand-700 dark:hover:text-brand-200" @click="removeKeyword(idx)">
                        <XMarkIcon class="w-3 h-3" />
                      </button>
                    </span>
                    <span v-if="form.keywords.length === 0" class="text-xs text-neutral-400 self-center">至少添加 1 个关键词</span>
                  </div>
                </div>

                <div>
                  <label class="text-[13px] font-medium text-neutral-700 dark:text-neutral-300 mb-1.5 block">竞品名称(选填)</label>
                  <div class="flex items-center gap-2">
                    <input
                      v-model="competitorInput"
                      type="text"
                      placeholder="输入竞品名称后回车添加"
                      class="flex-1 px-3.5 py-2.5 rounded-md border border-[color:var(--color-border)] bg-[color:var(--color-surface)] text-sm focus:outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/15"
                      @keyup.enter.prevent="addCompetitor"
                    />
                    <button
                      type="button"
                      class="p-2.5 rounded-md text-brand-600 dark:text-brand-400 hover:bg-brand-50 dark:hover:bg-brand-500/10 transition-colors border border-[color:var(--color-border)]"
                      @click="addCompetitor"
                    >
                      <PlusIcon class="w-4 h-4" />
                    </button>
                  </div>
                  <div class="flex flex-wrap gap-1.5 mt-3 min-h-[28px]">
                    <span
                      v-for="(cp, idx) in form.competitors"
                      :key="idx"
                      class="inline-flex items-center gap-1 px-2.5 py-1 bg-neutral-100 dark:bg-neutral-800 text-neutral-700 dark:text-neutral-300 text-xs rounded-full"
                    >
                      {{ cp }}
                      <button class="text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-100" @click="removeCompetitor(idx)">
                        <XMarkIcon class="w-3 h-3" />
                      </button>
                    </span>
                  </div>
                </div>
              </div>

              <p v-if="errorMsg" class="mt-3 text-sm text-red-500">{{ errorMsg }}</p>
            </div>

            <!-- Footer -->
            <div class="flex items-center justify-between px-6 py-4 border-t border-[color:var(--color-border)] bg-[color:var(--color-surface-muted)]/40">
              <button
                v-if="step === 2"
                type="button"
                class="inline-flex items-center gap-1 text-sm text-neutral-500 hover:text-neutral-800 dark:hover:text-neutral-100 transition-colors"
                @click="step = 1"
              >
                <ChevronLeftIcon class="w-4 h-4" />上一步
              </button>
              <div v-else />

              <div class="flex items-center gap-2">
                <AppButton variant="ghost" @click="close">取消</AppButton>
                <AppButton
                  v-if="step === 1"
                  variant="primary"
                  :disabled="!canNext"
                  @click="step = 2"
                >
                  下一步<ChevronRightIcon class="w-4 h-4" />
                </AppButton>
                <AppButton
                  v-else
                  variant="gradient"
                  :loading="submitting"
                  :disabled="!canNext"
                  @click="submit"
                >
                  {{ submitting ? '创建中…' : '创建项目' }}
                </AppButton>
              </div>
            </div>
          </div>
        </transition>
      </div>
    </transition>
  </Teleport>
</template>
