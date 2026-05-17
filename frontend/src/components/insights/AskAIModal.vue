<script setup lang="ts">
import { ref } from 'vue'
import { XMarkIcon, ChatBubbleLeftEllipsisIcon } from '@heroicons/vue/24/outline'
import AppButton from '@/components/ui/AppButton.vue'

defineProps<{
  title: string
  context: string
}>()
const emit = defineEmits<{ close: [] }>()

const visible = ref(true)
function close() {
  visible.value = false
  setTimeout(() => emit('close'), 180)
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
            <div class="flex items-center justify-between px-6 py-4 border-b border-[color:var(--color-border)]">
              <div class="flex items-center gap-2.5">
                <div class="w-8 h-8 rounded-lg bg-brand-50 dark:bg-brand-500/15 flex items-center justify-center">
                  <ChatBubbleLeftEllipsisIcon class="w-4 h-4 text-brand-600 dark:text-brand-400" />
                </div>
                <h3 class="text-base font-semibold text-neutral-900 dark:text-neutral-100">追问 AI</h3>
              </div>
              <button
                class="p-1 rounded text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-200 hover:bg-neutral-100 dark:hover:bg-neutral-800"
                @click="close"
              >
                <XMarkIcon class="w-5 h-5" />
              </button>
            </div>

            <div class="px-6 py-5 space-y-4">
              <div class="rounded-lg bg-[color:var(--color-surface-muted)] border border-[color:var(--color-border)] p-3">
                <p class="text-[11px] text-neutral-500 dark:text-neutral-400 uppercase tracking-wider mb-1">针对</p>
                <p class="text-sm font-semibold text-neutral-900 dark:text-neutral-100">{{ title }}</p>
                <p v-if="context" class="text-xs text-neutral-500 dark:text-neutral-400 mt-1.5 line-clamp-3 leading-relaxed">{{ context }}</p>
              </div>

              <div>
                <label class="block text-[13px] font-medium text-neutral-700 dark:text-neutral-300 mb-1.5">你的追问</label>
                <textarea
                  rows="3"
                  disabled
                  placeholder="例如:这个痛点在 25-35 岁人群中表现如何?"
                  class="w-full px-3.5 py-2.5 rounded-md border border-[color:var(--color-border)] bg-neutral-50 dark:bg-neutral-800/50 text-sm text-neutral-500 resize-none cursor-not-allowed"
                />
              </div>

              <div class="rounded-lg bg-brand-50 dark:bg-brand-500/12 border border-brand-100 dark:border-brand-800/50 px-4 py-3 flex items-start gap-2.5">
                <span class="text-base">🎉</span>
                <p class="text-sm text-brand-700 dark:text-brand-300 leading-relaxed">该功能即将上线,敬请期待</p>
              </div>
            </div>

            <div class="flex items-center justify-end gap-2 px-6 py-4 border-t border-[color:var(--color-border)] bg-[color:var(--color-surface-muted)]/40">
              <AppButton variant="ghost" @click="close">关闭</AppButton>
              <AppButton variant="primary" disabled>提交追问</AppButton>
            </div>
          </div>
        </transition>
      </div>
    </transition>
  </Teleport>
</template>

<style scoped>
.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
