<script setup lang="ts">
import { ref, computed } from 'vue'
import { XMarkIcon, PlusIcon } from '@heroicons/vue/24/outline'
import { useProjectStore } from '@/stores/project'

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
    <div class="fixed inset-0 z-50 flex items-center justify-center">
      <div
        class="absolute inset-0 bg-black/50"
        @click="emit('close')"
      />
      <div class="relative bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-md mx-4">
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-100 dark:border-gray-700">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
            添加竞品
          </h3>
          <button
            class="p-1 text-gray-400 hover:text-gray-600 transition-colors"
            @click="emit('close')"
          >
            <XMarkIcon class="w-5 h-5" />
          </button>
        </div>

        <div class="px-6 py-5 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              竞品名称
            </label>
            <div class="flex items-center gap-2">
              <input
                v-model="input"
                type="text"
                placeholder="输入后按回车添加"
                maxlength="100"
                class="flex-1 px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500"
                @keyup.enter.prevent="addOne"
              >
              <button
                type="button"
                class="p-2 text-primary-500 hover:bg-primary-50 dark:hover:bg-primary-900/20 rounded-lg transition-colors"
                @click="addOne"
              >
                <PlusIcon class="w-4 h-4" />
              </button>
            </div>
            <p class="text-xs text-gray-400 mt-1">
              添加后需点击"AI 分析"重新生成对比矩阵
            </p>
          </div>

          <!-- 新增标签 -->
          <div
            v-if="newNames.length"
            class="space-y-1.5"
          >
            <p class="text-xs text-gray-500 dark:text-gray-400">
              本次新增:
            </p>
            <div class="flex flex-wrap gap-1.5">
              <span
                v-for="(n, idx) in newNames"
                :key="n"
                class="inline-flex items-center gap-1 px-2.5 py-1 bg-primary-50 dark:bg-primary-900/20 text-primary-700 dark:text-primary-300 text-xs rounded-full"
              >
                {{ n }}
                <button
                  class="text-primary-400 hover:text-primary-600"
                  @click="removeNew(idx)"
                >
                  <XMarkIcon class="w-3 h-3" />
                </button>
              </span>
            </div>
          </div>

          <!-- 已有 -->
          <div
            v-if="existing.length"
            class="space-y-1.5"
          >
            <p class="text-xs text-gray-500 dark:text-gray-400">
              已配置:
            </p>
            <div class="flex flex-wrap gap-1.5">
              <span
                v-for="e in existing"
                :key="e"
                class="px-2.5 py-1 bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 text-xs rounded-full"
              >{{ e }}</span>
            </div>
          </div>

          <p
            v-if="errorMsg"
            class="text-sm text-red-500"
          >
            {{ errorMsg }}
          </p>
        </div>

        <div class="flex items-center justify-end gap-2 px-6 py-4 border-t border-gray-100 dark:border-gray-700">
          <button
            class="px-4 py-2 text-sm text-gray-500 hover:text-gray-700 transition-colors"
            @click="emit('close')"
          >
            取消
          </button>
          <button
            :disabled="!newNames.length || submitting"
            class="px-5 py-2 bg-primary-500 text-white rounded-lg text-sm font-medium hover:bg-primary-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            @click="submit"
          >
            {{ submitting ? '保存中…' : `保存 ${newNames.length} 个` }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
