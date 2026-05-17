<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  StarIcon, EllipsisHorizontalIcon, PencilIcon, TrashIcon,
  MapIcon, CheckIcon, ChatBubbleBottomCenterTextIcon, FlagIcon,
  ExclamationTriangleIcon, AdjustmentsHorizontalIcon,
} from '@heroicons/vue/24/outline'
import { StarIcon as StarSolid } from '@heroicons/vue/24/solid'
import type { Persona, UpdatePersonaPayload, PaymentLevel } from '@/types/persona'
import { confirm } from '@/utils/feedback'

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

const AVATAR_PALETTE = [
  'from-brand-500 to-indigo-600',
  'from-emerald-500 to-teal-600',
  'from-purple-500 to-fuchsia-600',
  'from-amber-500 to-orange-600',
  'from-rose-500 to-pink-600',
  'from-cyan-500 to-blue-600',
  'from-violet-500 to-purple-600',
  'from-lime-500 to-emerald-600',
]
function hashCode(s: string) {
  let h = 5381
  for (let i = 0; i < s.length; i++) h = ((h << 5) + h + s.charCodeAt(i)) >>> 0
  return h
}
const avatarColor = computed(() => AVATAR_PALETTE[hashCode(props.persona.name) % AVATAR_PALETTE.length])
const avatarLetter = computed(() => (props.persona.name?.[0] || '?').toUpperCase())

const LEVEL_LABEL: Record<PaymentLevel, string> = { high: '高', medium: '中', low: '低' }
const LEVEL_CLS: Record<PaymentLevel, string> = {
  high:   'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300',
  medium: 'bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300',
  low:    'bg-neutral-100 text-neutral-600 dark:bg-neutral-800 dark:text-neutral-400',
}

function saveField<K extends keyof UpdatePersonaPayload>(field: K, value: UpdatePersonaPayload[K]) {
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
  if (props.persona.isPrimary) return
  saveField('isPrimary', true)
}

async function confirmDelete() {
  menuOpen.value = false
  const ok = await confirm(
    `画像「${props.persona.name}」将被永久删除,且无法恢复。`,
    `删除画像`,
    { type: 'warning', confirmText: '删除', danger: true },
  )
  if (ok) emit('delete', props.persona.id)
}

function toggleEdit() {
  editMode.value = !editMode.value
  menuOpen.value = false
  if (editMode.value) startEditingGoals()
}

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
      'relative rounded-xl bg-[color:var(--color-surface)] p-5 transition-all duration-200',
      'shadow-card hover:shadow-card-hover',
      persona.isPrimary
        ? 'ring-2 ring-brand-400 dark:ring-brand-500/60 border border-brand-200 dark:border-brand-700'
        : 'border border-[color:var(--color-border)]',
    ]"
  >
    <!-- 核心用户徽章 -->
    <span
      v-if="persona.isPrimary"
      class="absolute -top-2.5 left-5 inline-flex items-center gap-1 px-2.5 py-0.5 text-xs font-semibold rounded-full bg-brand-gradient text-white shadow-glow"
    >
      <StarSolid class="w-3 h-3" />核心用户
    </span>

    <!-- Header -->
    <div class="flex items-start gap-3 mb-4">
      <div
        :class="['w-12 h-12 rounded-xl bg-gradient-to-br flex items-center justify-center text-white text-lg font-bold flex-shrink-0 shadow-sm', avatarColor]"
      >
        {{ avatarLetter }}
      </div>

      <div class="flex-1 min-w-0">
        <input
          v-if="editMode"
          :value="persona.name"
          maxlength="50"
          class="block w-full px-2 py-0.5 text-base font-semibold rounded border border-[color:var(--color-border)] bg-[color:var(--color-surface-muted)] focus:outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-500/30"
          @blur="onBlurText('name', $event)"
        />
        <h3 v-else class="text-base font-semibold text-neutral-900 dark:text-neutral-100 truncate">{{ persona.name }}</h3>

        <input
          v-if="editMode"
          :value="persona.role"
          maxlength="100"
          class="mt-1 block w-full px-2 py-0.5 text-sm rounded border border-[color:var(--color-border)] bg-[color:var(--color-surface-muted)] text-neutral-600 focus:outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-500/30"
          @blur="onBlurText('role', $event)"
        />
        <p v-else class="text-sm text-neutral-500 dark:text-neutral-400 truncate">{{ persona.role }}</p>

        <div class="mt-1.5 flex items-center gap-2 text-xs text-neutral-400 flex-wrap">
          <input
            v-if="editMode"
            :value="persona.ageRange"
            maxlength="20"
            class="w-20 px-1.5 py-0.5 rounded border border-[color:var(--color-border)] bg-[color:var(--color-surface-muted)] focus:outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-500/30"
            @blur="onBlurText('ageRange', $event)"
          />
          <span v-else>{{ persona.ageRange }}</span>
          <span>·</span>
          <span class="inline-flex items-center gap-1">
            市场份额
            <input
              v-if="editMode"
              :value="persona.marketShare"
              type="number" min="0" max="100" step="0.1"
              class="w-14 px-1.5 py-0.5 rounded border border-[color:var(--color-border)] bg-[color:var(--color-surface-muted)] focus:outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-500/30"
              @blur="onBlurShare"
            />
            <span v-else class="font-semibold text-neutral-700 dark:text-neutral-300">{{ Number(persona.marketShare).toFixed(0) }}%</span>
          </span>
        </div>
      </div>

      <div v-if="editable" class="relative flex-shrink-0" data-persona-menu>
        <button
          class="p-1 rounded-md text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-200 hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors"
          @click.stop="menuOpen = !menuOpen"
        >
          <EllipsisHorizontalIcon class="w-5 h-5" />
        </button>
        <div
          v-if="menuOpen"
          class="absolute right-0 top-8 z-20 w-44 py-1 rounded-lg shadow-pop border border-[color:var(--color-border)] bg-[color:var(--color-surface-elevated)]"
        >
          <button
            class="w-full px-3 py-2 text-left text-sm text-neutral-700 dark:text-neutral-200 hover:bg-neutral-100 dark:hover:bg-neutral-800 inline-flex items-center gap-2"
            @click="toggleEdit"
          >
            <PencilIcon class="w-4 h-4" />{{ editMode ? '完成编辑' : '编辑' }}
          </button>
          <button
            class="w-full px-3 py-2 text-left text-sm text-neutral-400 cursor-not-allowed inline-flex items-center gap-2"
            disabled
            title="即将上线"
          >
            <MapIcon class="w-4 h-4" />生成用户旅程
          </button>
          <div class="my-1 border-t border-[color:var(--color-border)]"></div>
          <button
            class="w-full px-3 py-2 text-left text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-500/10 inline-flex items-center gap-2"
            @click="confirmDelete"
          >
            <TrashIcon class="w-4 h-4" />删除画像
          </button>
        </div>
      </div>
    </div>

    <!-- 4 个信息块 -->
    <div class="space-y-4">
      <!-- 目标 -->
      <div>
        <div class="flex items-center gap-1.5 mb-1.5">
          <FlagIcon class="w-3.5 h-3.5 text-sky-500" />
          <h4 class="text-[10px] font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">目标</h4>
        </div>
        <input
          v-if="editMode"
          v-model="goalsEditing"
          placeholder="用、号分隔多个目标"
          class="w-full px-2 py-1.5 text-sm rounded border border-[color:var(--color-border)] bg-[color:var(--color-surface-muted)] focus:outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-500/30"
          @blur="onBlurGoals"
        />
        <ul v-else-if="persona.goals?.length" class="space-y-1">
          <li v-for="(g, i) in persona.goals" :key="i" class="flex items-start gap-1.5 text-sm text-neutral-700 dark:text-neutral-300">
            <CheckIcon class="w-3.5 h-3.5 text-emerald-500 mt-0.5 flex-shrink-0" />{{ g }}
          </li>
        </ul>
        <p v-else class="text-sm text-neutral-400 italic">暂无目标</p>
      </div>

      <!-- 痛点 -->
      <div>
        <div class="flex items-center gap-1.5 mb-1.5">
          <ExclamationTriangleIcon class="w-3.5 h-3.5 text-red-500" />
          <h4 class="text-[10px] font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">痛点</h4>
        </div>
        <ul v-if="persona.painPoints?.length" class="space-y-1.5">
          <li v-for="(pp, i) in persona.painPoints" :key="i" class="text-sm">
            <span class="font-semibold text-neutral-800 dark:text-neutral-200">{{ pp.title }}:</span>
            <span class="text-neutral-600 dark:text-neutral-400 ml-1">{{ pp.description }}</span>
          </li>
        </ul>
        <p v-else class="text-sm text-neutral-400 italic">暂无痛点</p>
      </div>

      <!-- 引言 -->
      <div>
        <div class="flex items-center gap-1.5 mb-1.5">
          <ChatBubbleBottomCenterTextIcon class="w-3.5 h-3.5 text-purple-500" />
          <h4 class="text-[10px] font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">引言</h4>
        </div>
        <textarea
          v-if="editMode"
          :value="persona.quote"
          rows="2"
          maxlength="500"
          class="w-full px-2 py-1.5 text-sm italic rounded border border-[color:var(--color-border)] bg-[color:var(--color-surface-muted)] focus:outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-500/30 resize-none"
          @blur="onBlurText('quote', $event)"
        />
        <blockquote
          v-else-if="persona.quote"
          class="border-l-2 border-purple-400 pl-3 text-sm italic text-neutral-600 dark:text-neutral-300"
        >“{{ persona.quote }}”</blockquote>
        <p v-else class="text-sm text-neutral-400 italic">暂无引言</p>
      </div>

      <!-- 决策参数 -->
      <div>
        <div class="flex items-center gap-1.5 mb-2">
          <AdjustmentsHorizontalIcon class="w-3.5 h-3.5 text-amber-500" />
          <h4 class="text-[10px] font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">决策参数</h4>
        </div>
        <div class="grid grid-cols-2 gap-x-4 gap-y-2 text-xs">
          <div class="flex items-center justify-between">
            <span class="text-neutral-500 dark:text-neutral-400">付费意愿</span>
            <select
              v-if="editMode"
              :value="persona.decisionParams?.paymentWillingness"
              class="px-1.5 py-0.5 rounded border border-[color:var(--color-border)] bg-[color:var(--color-surface-muted)] text-xs focus:outline-none focus:border-brand-500"
              @change="changeDecision('paymentWillingness', ($event.target as HTMLSelectElement).value as PaymentLevel)"
            >
              <option value="high">高</option>
              <option value="medium">中</option>
              <option value="low">低</option>
            </select>
            <span v-else :class="['px-2 py-0.5 rounded-full font-medium', LEVEL_CLS[persona.decisionParams?.paymentWillingness] || LEVEL_CLS.medium]">
              {{ LEVEL_LABEL[persona.decisionParams?.paymentWillingness] || '中' }}
            </span>
          </div>

          <div class="flex items-center justify-between">
            <span class="text-neutral-500 dark:text-neutral-400">技术能力</span>
            <select
              v-if="editMode"
              :value="persona.decisionParams?.techCapability"
              class="px-1.5 py-0.5 rounded border border-[color:var(--color-border)] bg-[color:var(--color-surface-muted)] text-xs focus:outline-none focus:border-brand-500"
              @change="changeDecision('techCapability', ($event.target as HTMLSelectElement).value as PaymentLevel)"
            >
              <option value="high">高</option>
              <option value="medium">中</option>
              <option value="low">低</option>
            </select>
            <span v-else :class="['px-2 py-0.5 rounded-full font-medium', LEVEL_CLS[persona.decisionParams?.techCapability] || LEVEL_CLS.medium]">
              {{ LEVEL_LABEL[persona.decisionParams?.techCapability] || '中' }}
            </span>
          </div>

          <div class="flex items-center justify-between col-span-2 gap-2">
            <span class="text-neutral-500 dark:text-neutral-400 flex-shrink-0">决策周期</span>
            <input
              v-if="editMode"
              :value="persona.decisionParams?.decisionCycle"
              maxlength="30"
              class="flex-1 px-1.5 py-0.5 rounded border border-[color:var(--color-border)] bg-[color:var(--color-surface-muted)] text-xs focus:outline-none focus:border-brand-500"
              @blur="changeDecisionText('decisionCycle', ($event.target as HTMLInputElement).value)"
            />
            <span v-else class="text-neutral-700 dark:text-neutral-300 font-medium">{{ persona.decisionParams?.decisionCycle || '—' }}</span>
          </div>

          <div class="flex items-center justify-between col-span-2 gap-2">
            <span class="text-neutral-500 dark:text-neutral-400 flex-shrink-0">预算范围</span>
            <input
              v-if="editMode"
              :value="persona.decisionParams?.budgetRange"
              maxlength="30"
              class="flex-1 px-1.5 py-0.5 rounded border border-[color:var(--color-border)] bg-[color:var(--color-surface-muted)] text-xs focus:outline-none focus:border-brand-500"
              @blur="changeDecisionText('budgetRange', ($event.target as HTMLInputElement).value)"
            />
            <span v-else class="text-neutral-700 dark:text-neutral-300 font-medium">{{ persona.decisionParams?.budgetRange || '—' }}</span>
          </div>
        </div>
      </div>
    </div>

    <button
      v-if="editable && !persona.isPrimary"
      class="mt-5 w-full inline-flex items-center justify-center gap-1.5 px-3 py-1.5 text-xs text-neutral-500 dark:text-neutral-400 border border-dashed border-[color:var(--color-border)] rounded-lg hover:border-brand-400 hover:text-brand-600 hover:bg-brand-50 dark:hover:bg-brand-500/10 transition-all"
      @click="togglePrimary"
    >
      <StarIcon class="w-3.5 h-3.5" />标记为核心用户
    </button>
  </div>
</template>
