<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useFormValidation, validators } from '@/composables/useFormValidation'
import AppButton from '@/components/ui/AppButton.vue'
import { EnvelopeIcon, LockClosedIcon, EyeIcon, EyeSlashIcon, ExclamationCircleIcon } from '@heroicons/vue/24/outline'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { errors, validate, clearErrors } = useFormValidation()

const form = reactive({
  email: '',
  password: '',
  remember: false,
})

const loading = ref(false)
const serverError = ref('')
const showPassword = ref(false)

const rules = {
  email: [validators.required('请输入邮箱'), validators.email()],
  password: [validators.required('请输入密码')],
}

async function handleSubmit() {
  clearErrors()
  serverError.value = ''
  if (!validate(rules, form)) return

  loading.value = true
  try {
    await userStore.login({ email: form.email, password: form.password })
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch (e: unknown) {
    serverError.value = (e as Error).message || '登录失败,请重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="w-full max-w-md mx-auto">
    <div class="mb-8">
      <h2 class="text-[26px] font-bold tracking-tight text-neutral-900 dark:text-neutral-100">欢迎回来</h2>
      <p class="text-sm text-neutral-500 dark:text-neutral-400 mt-1.5">登录账号以继续使用 MarketAI</p>
    </div>

    <!-- 服务端错误 -->
    <transition name="fade">
      <div
        v-if="serverError"
        class="mb-5 p-3 rounded-lg bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-900 flex items-start gap-2 text-sm text-red-700 dark:text-red-300"
      >
        <ExclamationCircleIcon class="w-4 h-4 mt-0.5 flex-shrink-0" />
        <span>{{ serverError }}</span>
      </div>
    </transition>

    <form @submit.prevent="handleSubmit" class="space-y-4" novalidate>
      <!-- 邮箱 -->
      <div>
        <label class="block text-[13px] font-medium text-neutral-700 dark:text-neutral-300 mb-1.5">邮箱地址</label>
        <div class="relative">
          <EnvelopeIcon class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400 pointer-events-none" />
          <input
            v-model="form.email"
            type="email"
            autocomplete="email"
            placeholder="name@example.com"
            :class="[
              'w-full pl-10 pr-3 py-2.5 rounded-md border bg-white dark:bg-[color:var(--color-surface-muted)]',
              'text-sm text-neutral-900 dark:text-neutral-100 placeholder:text-neutral-400',
              'transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-brand-500/20',
              errors.email
                ? 'border-red-400 focus:border-red-500'
                : 'border-[color:var(--color-border)] focus:border-brand-500',
            ]"
          />
        </div>
        <p v-if="errors.email" class="mt-1.5 text-xs text-red-500 flex items-center gap-1">
          <ExclamationCircleIcon class="w-3 h-3" />{{ errors.email }}
        </p>
      </div>

      <!-- 密码 -->
      <div>
        <label class="block text-[13px] font-medium text-neutral-700 dark:text-neutral-300 mb-1.5">密码</label>
        <div class="relative">
          <LockClosedIcon class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400 pointer-events-none" />
          <input
            v-model="form.password"
            :type="showPassword ? 'text' : 'password'"
            autocomplete="current-password"
            placeholder="输入密码"
            :class="[
              'w-full pl-10 pr-10 py-2.5 rounded-md border bg-white dark:bg-[color:var(--color-surface-muted)]',
              'text-sm text-neutral-900 dark:text-neutral-100 placeholder:text-neutral-400',
              'transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-brand-500/20',
              errors.password
                ? 'border-red-400 focus:border-red-500'
                : 'border-[color:var(--color-border)] focus:border-brand-500',
            ]"
          />
          <button
            type="button"
            class="absolute right-3 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-neutral-600 transition-colors"
            @click="showPassword = !showPassword"
            tabindex="-1"
          >
            <EyeSlashIcon v-if="showPassword" class="w-4 h-4" />
            <EyeIcon v-else class="w-4 h-4" />
          </button>
        </div>
        <p v-if="errors.password" class="mt-1.5 text-xs text-red-500 flex items-center gap-1">
          <ExclamationCircleIcon class="w-3 h-3" />{{ errors.password }}
        </p>
      </div>

      <!-- 记住我 / 忘记密码 -->
      <div class="flex items-center justify-between pt-1">
        <label class="flex items-center gap-2 text-sm text-neutral-600 dark:text-neutral-400 cursor-pointer select-none">
          <input
            v-model="form.remember"
            type="checkbox"
            class="w-4 h-4 rounded border-neutral-300 dark:border-neutral-600 text-brand-500 focus:ring-brand-500 focus:ring-offset-0"
          />
          <span>记住我</span>
        </label>
        <a href="#" class="text-sm text-brand-600 dark:text-brand-400 hover:text-brand-700 dark:hover:text-brand-300 transition-colors">忘记密码?</a>
      </div>

      <!-- 提交 -->
      <AppButton type="submit" :loading="loading" block size="lg" variant="gradient" class="mt-2">
        {{ loading ? '登录中…' : '登 录' }}
      </AppButton>

      <p class="text-center text-sm text-neutral-500 dark:text-neutral-400">
        还没有账号?
        <router-link to="/register" class="text-brand-600 dark:text-brand-400 hover:text-brand-700 dark:hover:text-brand-300 font-medium transition-colors">立即注册</router-link>
      </p>
    </form>
  </div>
</template>
