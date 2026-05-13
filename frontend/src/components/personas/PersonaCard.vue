<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  StarIcon, EllipsisHorizontalIcon, PencilIcon, TrashIcon,
  MapIcon, CheckIcon, ChatBubbleBottomCenterTextIcon, FlagIcon,
  ExclamationTriangleIcon, AdjustmentsHorizontalIcon,
} from '@heroicons/vue/24/outline'
import type { Persona, UpdatePersonaPayload, PaymentLevel } from '@/types/persona'

const props = defineProps<{
  persona: Persona
  editable?: boolean
}>()

const emit = defineEmits<{
  update: [id: number, payload: UpdatePersonaPayload]
  delete: [id: number]
}>()

const editMode = ref(false)
const menuOpen = ref(false)

// 头像背景色:基于姓名的稳定哈希,从调色板中选一个
const AVATAR_PALETTE = [
  'bg-blue-500', 'bg-emerald-500', 'bg-purple-500', 'bg-amber-500',
  'bg-rose-500', 'bg-indigo-500', 'bg-teal-500', 'bg-pink-500',
]
function hashCode(s: string) {
  let h = 5381
  for (let i = 0; i < s.length; i++) h = ((h << 5) + h + s.charCodeAt(i)) >>> 0
  return h
}
const avatarColor = computed(() => AVATAR_PALETTE[hashCode(props.persona.name) % AVATAR_PALETTE.length])
const avatarLetter = computed(() => (props.persona.name?.[0] || '?').toUpperCase())

// ── 决策参数徽章配置 ─────────────────────────────────────
const LEVEL_LABEL: Record<PaymentLevel, string> = { high: '高', medium: '中', low: '低' }
const LEVEL_CLS: Record<PaymentLevel, string> = {
  high:   'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300',
  medium: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300',
  low:    'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400',
}

// ── 失焦保存 ─────────────────────────────────────────────
function saveField<K extends keyof UpdatePersonaPayload>(field: K, value: UpdatePersonaPayload[K]) {
  // 跳过未变化的值 — 防止失焦时无意义请求
  // 单层比较即可,goals/decisionParams 也是常被 emit 的对象
  const current = (props.persona as unknown as Record<string, unknown>)[field as string]
  if (JSON.stringify(current) === JSON.stringify(value)) return
  emit('update', props.persona.id, { [field]: value } as UpdatePersonaPayload)
}

function onBlurText(field: 'name' | 'role' | 'ageRange' | 'quote', e: FocusEvent) {
  const target = e.target as HTMLInputElement | HTMLTextAreaElement
  const v = target.value.trim()
  if (!v) return
  saveField(field, v)
}

function onBlurShare(e: FocusEvent) {
  const target = e.target as HTMLInputElement
  const num = Number(target.value)
  if (Number.isNaN(num) || num < 0 || num > 100) return
  saveField('marketShare', num)
}

// goals 以逗号分隔 string 编辑
const goalsEditing = ref('')
function startEditingGoals() {
  goalsEditing.value = (props.persona.goals || []).join('、')
}
function onBlurGoals() {
  const arr = goalsEditing.value
    .split(/[、,，]/)
    .map(s => s.trim())
    .filter(Boolean)
  saveField('goals', arr)
}

function changeDecision(field: 'paymentWillingness' | 'techCapability', val: PaymentLevel) {
  const next = { ...props.persona.decisionParams, [field]: val }
  saveField('decisionParams', next)
}

function changeDecisionText(field: 'decisionCycle' | 'budgetRange', val: string) {
  const next = { ...props.persona.decisionParams, [field]: val.trim() }
  saveField('decisionParams', next)
}

function togglePrimary() {
  if (props.persona.isPrimary) return // 当前已是核心用户,取消标记的责任交给后端兼容(避免误操作)
  saveField('isPrimary', true)
}

// ── 菜单 / 删除 ─────────────────────────────────────────
function confirmDelete() {
  menuOpen.value = false
  if (confirm(`确定要删除画像「${props.persona.name}」吗?此操作无法撤销。`)) {
    emit('delete', props.persona.id)
  }
}

function toggleEdit() {
  editMode.value = !editMode.value
  menuOpen.value = false
  if (editMode.value) startEditingGoals()
}

// 点击卡片外关闭菜单
function handleClickOutside(e: MouseEvent) {
  const root = (e.target as HTMLElement).closest('[data-persona-menu]')
  if (!root) menuOpen.value = false
}
onMounted(() => document.addEventListener('click', handleClickOutside))
onUnmounted(() => document.removeEventListener('click', handleClickOutside))
</script>

<template>
  <div
    :class="[
      'relative rounded-xl bg-white dark:bg-gray-800 p-5 transition-all',
      persona.isPrimary
        ? 'border-2 border-primary-400 dark:border-primary-600 shadow-sm'
        : 'border border-gray-200 dark:border-gray-700',
    ]"
  >
    <!-- 核心用户徽章 -->
    <span
      v-if="persona.isPrimary"
      class="absolute -top-2.5 left-5 inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium rounded-full bg-primary-500 text-white"
    >
      <StarIcon class="w-3 h-3" />核心用户
    </span>

    <!-- Header: 头像 + 姓名/职位 + 操作菜单 -->
    <div class="flex items-start gap-3 mb-4">
      <div
        :class="['w-12 h-12 rounded-full flex items-center justify-center text-white text-lg font-semibold flex-shrink-0', avatarColor]"
      >
        {{ avatarLetter }}
      </div>

      <div class="flex-1 min-w-0">
        <input
          v-if="editMode"
          :value="persona.name"
          maxlength="50"
          class="block w-full px-2 py-0.5 text-base font-semibold border border-gray-200 dark:border-gray-600 rounded bg-white dark:bg-gray-700 dark:text-white focus:outline-none focus:ring-1 focus:ring-primary-500"
          @blur="onBlurText('name', $event)"
        >
        <h3
          v-else
          class="text-base font-semibold text-gray-900 dark:text-white truncate"
        >
          {{ persona.name }}
        </h3>

        <input
          v-if="editMode"
          :value="persona.role"
          maxlength="100"
          class="mt-1 block w-full px-2 py-0.5 text-sm text-gray-500 border border-gray-200 dark:border-gray-600 rounded bg-white dark:bg-gray-700 dark:text-gray-300 focus:outline-none focus:ring-1 focus:ring-primary-500"
          @blur="onBlurText('role', $event)"
        >
        <p
          v-else
          class="text-sm text-gray-500 dark:text-gray-400 truncate"
        >
          {{ persona.role }}
        </p>

        <div class="mt-1 flex items-center gap-2 text-xs text-gray-400">
          <input
            v-if="editMode"
            :value="persona.ageRange"
            maxlength="20"
            class="w-20 px-1.5 py-0.5 border border-gray-200 dark:border-gray-600 rounded bg-white dark:bg-gray-700 focus:outline-none focus:ring-1 focus:ring-primary-500"
            @blur="onBlurText('ageRange', $event)"
          >
          <span v-else>{{ persona.ageRange }}</span>
          <span>·</span>
          <span class="inline-flex items-center gap-1">
            市场份额
            <input
              v-if="editMode"
              :value="persona.marketShare"
              type="number"
              min="0"
              max="100"
              step="0.1"
              class="w-14 px-1.5 py-0.5 border border-gray-200 dark:border-gray-600 rounded bg-white dark:bg-gray-700 focus:outline-none focus:ring-1 focus:ring-primary-500"
              @blur="onBlurShare"
            >
            <span v-else>{{ Number(persona.marketShare).toFixed(0) }}%</span>
          </span>
        </div>
      </div>

      <div
        v-if="editable"
        class="relative flex-shrink-0"
        data-persona-menu
      >
        <button
          class="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md transition-colors"
          @click.stop="menuOpen = !menuOpen"
        >
          <EllipsisHorizontalIcon class="w-5 h-5" />
        </button>
        <div
          v-if="menuOpen"
          class="absolute right-0 top-7 z-10 w-40 py-1 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg"
        >
          <button
            class="w-full px-3 py-1.5 text-left text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700 inline-flex items-center gap-2"
            @click="toggleEdit"
          >
            <PencilIcon class="w-4 h-4" />{{ editMode ? '完成编辑' : '编辑' }}
          </button>
          <button
            class="w-full px-3 py-1.5 text-left text-sm text-gray-400 cursor-not-allowed inline-flex items-center gap-2"
            disabled
            title="即将上线"
          >
            <MapIcon class="w-4 h-4" />生成用户旅程
          </button>
          <button
            class="w-full px-3 py-1.5 text-left text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 inline-flex items-center gap-2"
            @click="confirmDelete"
          >
            <TrashIcon class="w-4 h-4" />删除画像
          </button>
        </div>
      </div>
    </div>

    <!-- 4 个信息块 -->
    <div class="space-y-3.5">
      <!-- 目标 -->
      <div>
        <div class="flex items-center gap-1.5 mb-1.5">
          <FlagIcon class="w-4 h-4 text-blue-500" />
          <h4 class="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">
            目标
          </h4>
        </div>
        <input
          v-if="editMode"
          v-model="goalsEditing"
          placeholder="用、号分隔多个目标"
          class="w-full px-2 py-1 text-sm border border-gray-200 dark:border-gray-600 rounded bg-white dark:bg-gray-700 dark:text-gray-200 focus:outline-none focus:ring-1 focus:ring-primary-500"
          @blur="onBlurGoals"
        >
        <ul
          v-else-if="persona.goals?.length"
          class="space-y-1"
        >
          <li
            v-for="(g, i) in persona.goals"
            :key="i"
            class="flex items-start gap-1.5 text-sm text-gray-700 dark:text-gray-300"
          >
            <CheckIcon class="w-3.5 h-3.5 text-emerald-500 mt-0.5 flex-shrink-0" />{{ g }}
          </li>
        </ul>
        <p
          v-else
          class="text-sm text-gray-400 italic"
        >
          暂无目标
        </p>
      </div>

      <!-- 痛点 -->
      <div>
        <div class="flex items-center gap-1.5 mb-1.5">
          <ExclamationTriangleIcon class="w-4 h-4 text-red-500" />
          <h4 class="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">
            痛点
          </h4>
        </div>
        <ul
          v-if="persona.painPoints?.length"
          class="space-y-1.5"
        >
          <li
            v-for="(pp, i) in persona.painPoints"
            :key="i"
            class="text-sm"
          >
            <span class="font-medium text-gray-800 dark:text-gray-200">{{ pp.title }}:</span>
            <span class="text-gray-600 dark:text-gray-400 ml-1">{{ pp.description }}</span>
          </li>
        </ul>
        <p
          v-else
          class="text-sm text-gray-400 italic"
        >
          暂无痛点
        </p>
      </div>

      <!-- 引言 -->
      <div>
        <div class="flex items-center gap-1.5 mb-1.5">
          <ChatBubbleBottomCenterTextIcon class="w-4 h-4 text-purple-500" />
          <h4 class="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">
            引言
          </h4>
        </div>
        <textarea
          v-if="editMode"
          :value="persona.quote"
          rows="2"
          maxlength="500"
          class="w-full px-2 py-1 text-sm italic border border-gray-200 dark:border-gray-600 rounded bg-white dark:bg-gray-700 dark:text-gray-200 focus:outline-none focus:ring-1 focus:ring-primary-500 resize-none"
          @blur="onBlurText('quote', $event)"
        />
        <blockquote
          v-else-if="persona.quote"
          class="border-l-2 border-purple-300 pl-3 text-sm italic text-gray-600 dark:text-gray-300"
        >
          "{{ persona.quote }}"
        </blockquote>
        <p
          v-else
          class="text-sm text-gray-400 italic"
        >
          暂无引言
        </p>
      </div>

      <!-- 决策参数 -->
      <div>
        <div class="flex items-center gap-1.5 mb-1.5">
          <AdjustmentsHorizontalIcon class="w-4 h-4 text-amber-500" />
          <h4 class="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">
            决策参数
          </h4>
        </div>
        <div class="grid grid-cols-2 gap-x-3 gap-y-1.5 text-xs">
          <!-- 付费意愿 -->
          <div class="flex items-center justify-between">
            <span class="text-gray-500 dark:text-gray-400">付费意愿</span>
            <select
              v-if="editMode"
              :value="persona.decisionParams?.paymentWillingness"
              class="px-1.5 py-0.5 border border-gray-200 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-xs focus:outline-none focus:ring-1 focus:ring-primary-500"
              @change="changeDecision('paymentWillingness', ($event.target as HTMLSelectElement).value as PaymentLevel)"
            >
              <option value="high">
                高
              </option>
              <option value="medium">
                中
              </option>
              <option value="low">
                低
              </option>
            </select>
            <span
              v-else
              :class="['px-1.5 py-0.5 rounded-full font-medium', LEVEL_CLS[persona.decisionParams?.paymentWillingness] || LEVEL_CLS.medium]"
            >{{ LEVEL_LABEL[persona.decisionParams?.paymentWillingness] || '中' }}</span>
          </div>

          <!-- 技术能力 -->
          <div class="flex items-center justify-between">
            <span class="text-gray-500 dark:text-gray-400">技术能力</span>
            <select
              v-if="editMode"
              :value="persona.decisionParams?.techCapability"
              class="px-1.5 py-0.5 border border-gray-200 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-xs focus:outline-none focus:ring-1 focus:ring-primary-500"
              @change="changeDecision('techCapability', ($event.target as HTMLSelectElement).value as PaymentLevel)"
            >
              <option value="high">
                高
              </option>
              <option value="medium">
                中
              </option>
              <option value="low">
                低
              </option>
            </select>
            <span
              v-else
              :class="['px-1.5 py-0.5 rounded-full font-medium', LEVEL_CLS[persona.decisionParams?.techCapability] || LEVEL_CLS.medium]"
            >{{ LEVEL_LABEL[persona.decisionParams?.techCapability] || '中' }}</span>
          </div>

          <!-- 决策周期 -->
          <div class="flex items-center justify-between col-span-2 gap-2">
            <span class="text-gray-500 dark:text-gray-400">决策周期</span>
            <input
              v-if="editMode"
              :value="persona.decisionParams?.decisionCycle"
              maxlength="30"
              class="flex-1 px-1.5 py-0.5 border border-gray-200 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-xs focus:outline-none focus:ring-1 focus:ring-primary-500"
              @blur="changeDecisionText('decisionCycle', ($event.target as HTMLInputElement).value)"
            >
            <span
              v-else
              class="text-gray-700 dark:text-gray-300 font-medium"
            >{{ persona.decisionParams?.decisionCycle || '—' }}</span>
          </div>

          <!-- 预算 -->
          <div class="flex items-center justify-between col-span-2 gap-2">
            <span class="text-gray-500 dark:text-gray-400">预算范围</span>
            <input
              v-if="editMode"
              :value="persona.decisionParams?.budgetRange"
              maxlength="30"
              class="flex-1 px-1.5 py-0.5 border border-gray-200 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-xs focus:outline-none focus:ring-1 focus:ring-primary-500"
              @blur="changeDecisionText('budgetRange', ($event.target as HTMLInputElement).value)"
            >
            <span
              v-else
              class="text-gray-700 dark:text-gray-300 font-medium"
            >{{ persona.decisionParams?.budgetRange || '—' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部:标记核心用户(仅非核心可点击) -->
    <button
      v-if="editable && !persona.isPrimary"
      class="mt-4 w-full inline-flex items-center justify-center gap-1.5 px-3 py-1.5 text-xs text-gray-500 dark:text-gray-400 border border-dashed border-gray-200 dark:border-gray-600 rounded-lg hover:border-primary-300 hover:text-primary-600 transition-colors"
      @click="togglePrimary"
    >
      <StarIcon class="w-3.5 h-3.5" />标记为核心用户
    </button>
  </div>
</template>
