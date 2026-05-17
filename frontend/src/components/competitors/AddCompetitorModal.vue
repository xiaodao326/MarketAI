<script setup lang="ts">
import { ref, computed } from 'vue'
import { XMarkIcon, PlusIcon, BuildingOffice2Icon } from '@heroicons/vue/24/outline'
import { useProjectStore } from '@/stores/project'
import AppButton from '@/components/ui/AppButton.vue'
import { toast } from '@/utils/feedback'

const props = defineProps<{
  projectId: number
  existing: string[]
}>()

const emit = defineEmits<{
  close: []
  added: [names: string[]]
}>()

const projectStore = useProjectStore()

const input = ref('')
const newNames = ref<string[]>([])
const submitting = ref(false)
const errorMsg = ref('')

const allNames = computed(() => [...props.existing, ...newNames.value])

const visible = ref(true)
function close() {
  visible.value = false
  setTimeout(() => emit('close'), 180)
}

function addOne() {
  const v = input.value.trim()
  if (!v) return
  if (allNames.value.includes(v)) {
    errorMsg.value = `「${v}」已存在`
    return
  }
  newNames.value.push(v)
  input.value = ''
  errorMsg.value = ''
}

function removeNew(idx: number) {
  newNames.value.splice(idx, 1)
}

async function submit() {
  if (!newNames.value.length || submitting.value) return
  submitting.value = true
  errorMsg.value = ''
  try {
    const merged = [...props.existing, ...newNames.value]
    await projectStore.update(props.projectId, { competitors: merged })
    toast.success(`已添加 ${newNames.value.length} 个竞品`)
    emit('added', newNames.value)
  } catch (e: unknown) {
    errorMsg.value = e instanceof Error ? e.message : '添加失败'
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
          <div class="relative w-full max-w-md rounded-2xl bg-[color:var(--color-surface-elevated)] shadow-overlay border border-[color:var(--color-border)] overflow-hidden">
            <!-- Header -->
            <div class="flex items-start justify-between px-6 py-5 border-b border-[color:var(--color-border)]">
              <div class="flex items-start gap-3">
                <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-emerald-500 to-teal-600 flex items-center justify-center flex-shrink-0">
                  <BuildingOffice2Icon class="w-5 h-5 text-white" />
                </div>
                <div>
                  <h3 class="text-base font-semibold text-neutral-900 dark:text-neutral-100">添加竞品</h3>
                  <p class="text-xs text-neutral-500 mt-0.5">添加后需点击『AI 分析』重新生成对比矩阵</p>
                </div>
              </div>
              <button
                class="p-1 rounded text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-200 hover:bg-neutral-100 dark:hover:bg-neutral-800"
                @click="close"
              >
                <XMarkIcon class="w-5 h-5" />
              </button>
            </div>

            <div class="px-6 py-5 space-y-4">
              <div>
                <label class="block text-[13px] font-medium text-neutral-700 dark:text-neutral-300 mb-1.5">竞品名称</label>
                <div class="flex items-center gap-2">
                  <input
                    v-model="input"
                    type="text"
                    placeholder="输入后按回车添加"
                    maxlength="100"
                    class="flex-1 px-3.5 py-2.5 rounded-md border border-[color:var(--color-border)] bg-[color:var(--color-surface)] text-sm focus:outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/15"
                    @keyup.enter.prevent="addOne"
                  />
                  <button
                    type="button"
                    class="p-2.5 rounded-md text-brand-600 dark:text-brand-400 hover:bg-brand-50 dark:hover:bg-brand-500/10 transition-colors border border-[color:var(--color-border)]"
                    @click="addOne"
                  >
                    <PlusIcon class="w-4 h-4" />
                  </button>
                </div>
              </div>

              <!-- 本次新增 -->
              <div v-if="newNames.length" class="space-y-1.5">
                <p class="text-[11px] font-semibold uppercase tracking-wider text-neutral-500 dark:text-neutral-400">本次新增 ({{ newNames.length }})</p>
                <div class="flex flex-wrap gap-1.5">
                  <span
                    v-for="(n, idx) in newNames"
                    :key="n"
                    class="inline-flex items-center gap-1 px-2.5 py-1 bg-brand-50 dark:bg-brand-500/12 text-brand-700 dark:text-brand-300 text-xs rounded-full"
                  >
                    {{ n }}
                    <button class="text-brand-400 hover:text-brand-700 dark:hover:text-brand-200" @click="removeNew(idx)">
                      <XMarkIcon class="w-3 h-3" />
                    </button>
                  </span>
                </div>
              </div>

              <!-- 已配置 -->
              <div v-if="existing.length" class="space-y-1.5">
                <p class="text-[11px] font-semibold uppercase tracking-wider text-neutral-500 dark:text-neutral-400">已配置 ({{ existing.length }})</p>
                <div class="flex flex-wrap gap-1.5">
                  <span
                    v-for="e in existing"
                    :key="e"
                    class="px-2.5 py-1 bg-neutral-100 dark:bg-neutral-800 text-neutral-700 dark:text-neutral-300 text-xs rounded-full"
                  >{{ e }}</span>
                </div>
              </div>

              <p v-if="errorMsg" class="text-sm text-red-500">{{ errorMsg }}</p>
            </div>

            <div class="flex items-center justify-end gap-2 px-6 py-4 border-t border-[color:var(--color-border)] bg-[color:var(--color-surface-muted)]/40">
              <AppButton variant="ghost" @click="close">取消</AppButton>
              <AppButton
                variant="primary"
                :disabled="!newNames.length"
                :loading="submitting"
                @click="submit"
              >
                {{ submitting ? '保存中…' : `保存 ${newNames.length} 个` }}
              </AppButton>
            </div>
          </div>
        </transition>
      </div>
    </transition>
  </Teleport>
</template>
