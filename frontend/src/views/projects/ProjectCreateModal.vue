<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useProjectStore } from '@/stores/project'
import { XMarkIcon, PlusIcon, ChevronLeftIcon, ChevronRightIcon } from '@heroicons/vue/24/outline'
import type { CreateProjectRequest } from '@/types/project'

const emit = defineEmits<{ close: []; created: [project: import('@/types/project').Project] }>()
const projectStore = useProjectStore()
const submitting = ref(false)
const errorMsg = ref('')

const step = ref(1)
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

function addKeyword() {
  const val = keywordInput.value.trim()
  if (!val) return
  if (form.keywords.length >= 20) { errorMsg.value = '关键词最多20个'; return }
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

function canNext() {
  if (step.value === 1) return form.name.trim() && form.industry && form.targetMarket
  if (step.value === 2) return form.keywords.length >= 1
  return true
}

async function submit() {
  if (!canNext() || submitting.value) return
  submitting.value = true
  errorMsg.value = ''
  try {
    const project = await projectStore.create({ ...form })
    emit('created', project)
  } catch (e: unknown) {
    errorMsg.value = e instanceof Error ? e.message : '创建失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div class="fixed inset-0 z-50 flex items-center justify-center">
      <div class="absolute inset-0 bg-black/50" @click="emit('close')" />
      <div class="relative bg-white rounded-xl shadow-xl w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto">
        <!-- Header -->
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <h3 class="text-lg font-semibold text-gray-900">新建分析项目</h3>
          <button @click="emit('close')" class="p-1 text-gray-400 hover:text-gray-600 transition-colors"><XMarkIcon class="w-5 h-5" /></button>
        </div>

        <!-- Step indicator -->
        <div class="flex items-center px-6 py-3 border-b border-gray-50">
          <div class="flex items-center gap-2" :class="step === 1 ? 'text-primary-600' : 'text-gray-400'">
            <span class="w-6 h-6 rounded-full flex items-center justify-center text-xs font-medium border" :class="step === 1 ? 'bg-primary-500 text-white border-primary-500' : 'border-gray-300'">1</span>
            <span class="text-sm font-medium">基础信息</span>
          </div>
          <div class="w-8 h-px bg-gray-200 mx-3" />
          <div class="flex items-center gap-2" :class="step === 2 ? 'text-primary-600' : 'text-gray-400'">
            <span class="w-6 h-6 rounded-full flex items-center justify-center text-xs font-medium border" :class="step === 2 ? 'bg-primary-500 text-white border-primary-500' : 'border-gray-300'">2</span>
            <span class="text-sm font-medium">监控配置</span>
          </div>
        </div>

        <!-- Step 1: Basic Info -->
        <div v-if="step === 1" class="px-6 py-4 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">项目名称 <span class="text-red-500">*</span></label>
            <input v-model="form.name" type="text" maxlength="50" placeholder="例如：AI客服市场分析" class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500" />
            <p class="text-xs text-gray-400 mt-1">{{ form.name.length }}/50</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">项目描述</label>
            <textarea v-model="form.description" rows="2" placeholder="简要描述分析目标..." class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 resize-none" />
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">行业 <span class="text-red-500">*</span></label>
              <select v-model="form.industry" class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500">
                <option value="" disabled>请选择行业</option>
                <option v-for="ind in industries" :key="ind" :value="ind">{{ ind }}</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">目标市场 <span class="text-red-500">*</span></label>
              <select v-model="form.targetMarket" class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500">
                <option value="" disabled>请选择市场</option>
                <option v-for="m in markets" :key="m" :value="m">{{ m }}</option>
              </select>
            </div>
          </div>
        </div>

        <!-- Step 2: Monitor Config -->
        <div v-if="step === 2" class="px-6 py-4 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">监控关键词 <span class="text-red-500">*</span></label>
            <div class="flex items-center gap-2">
              <input v-model="keywordInput" type="text" placeholder="输入后按回车添加" class="flex-1 px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500" @keyup.enter.prevent="addKeyword" />
              <button @click="addKeyword" type="button" class="p-2 text-primary-500 hover:bg-primary-50 rounded-lg transition-colors"><PlusIcon class="w-4 h-4" /></button>
            </div>
            <div class="flex flex-wrap gap-1.5 mt-2">
              <span v-for="(kw, idx) in form.keywords" :key="idx" class="inline-flex items-center gap-1 px-2.5 py-1 bg-primary-50 text-primary-700 text-xs rounded-full">
                {{ kw }}
                <button @click="removeKeyword(idx)" class="text-primary-400 hover:text-primary-600"><XMarkIcon class="w-3 h-3" /></button>
              </span>
              <span v-if="form.keywords.length === 0" class="text-xs text-gray-400">至少添加 1 个关键词</span>
            </div>
            <p class="text-xs text-gray-400 mt-1">{{ form.keywords.length }}/20</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">竞品名称（选填）</label>
            <div class="flex items-center gap-2">
              <input v-model="competitorInput" type="text" placeholder="输入后按回车添加" class="flex-1 px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500" @keyup.enter.prevent="addCompetitor" />
              <button @click="addCompetitor" type="button" class="p-2 text-primary-500 hover:bg-primary-50 rounded-lg transition-colors"><PlusIcon class="w-4 h-4" /></button>
            </div>
            <div class="flex flex-wrap gap-1.5 mt-2">
              <span v-for="(cp, idx) in form.competitors" :key="idx" class="inline-flex items-center gap-1 px-2.5 py-1 bg-gray-100 text-gray-600 text-xs rounded-full">
                {{ cp }}
                <button @click="removeCompetitor(idx)" class="text-gray-400 hover:text-gray-600"><XMarkIcon class="w-3 h-3" /></button>
              </span>
            </div>
          </div>
        </div>

        <!-- Error -->
        <p v-if="errorMsg" class="px-6 text-sm text-red-500">{{ errorMsg }}</p>

        <!-- Footer -->
        <div class="flex items-center justify-between px-6 py-4 border-t border-gray-100">
          <button v-if="step === 2" @click="step = 1" class="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 transition-colors">
            <ChevronLeftIcon class="w-4 h-4" />上一步
          </button>
          <div v-else />

          <div class="flex items-center gap-2">
            <button @click="emit('close')" class="px-4 py-2 text-sm text-gray-500 hover:text-gray-700 transition-colors">取消</button>
            <button v-if="step === 1" @click="step = 2" :disabled="!canNext()" class="inline-flex items-center gap-1 px-4 py-2 bg-primary-500 text-white rounded-lg text-sm font-medium hover:bg-primary-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors">
              下一步<ChevronRightIcon class="w-4 h-4" />
            </button>
            <button v-else @click="submit" :disabled="!canNext() || submitting" class="px-6 py-2 bg-primary-500 text-white rounded-lg text-sm font-medium hover:bg-primary-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors">
              {{ submitting ? '创建中...' : '创建项目' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>
