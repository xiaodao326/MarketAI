<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useFormValidation, validators } from '@/composables/useFormValidation'
import AppButton from '@/components/ui/AppButton.vue'
import {
  UserIcon, EnvelopeIcon, LockClosedIcon,
  EyeIcon, EyeSlashIcon, ExclamationCircleIcon, CheckCircleIcon,
} from '@heroicons/vue/24/outline'

const router = useRouter()
const userStore = useUserStore()
const { errors, validate, clearErrors } = useFormValidation()

const form = reactive({
  nickname: '',
  email: '',
  password: '',
  confirmPassword: '',
  agreeToTerms: false,
})

const loading = ref(false)
const serverError = ref('')
const showPassword = ref(false)

const rules = {
  nickname: [
    validators.required('请输入昵称'),
    validators.minLength(2, '昵称长度 2-20 个字符'),
    validators.maxLength(20, '昵称长度 2-20 个字符'),
  ],
  email: [validators.required('请输入邮箱'), validators.email()],
  password: [
    validators.required('请输入密码'),
    validators.minLength(8, '密码长度至少 8 位'),
  ],
  confirmPassword: [
    validators.required('请确认密码'),
    validators.match(() => form.password, '两次输入的密码不一致'),
  ],
  agreeToTerms: [
    (v: unknown) => (v === true ? null : '请先同意服务条款'),
  ],
}

const passwordStrength = computed(() => {
  const meta = [
    { label: '',     percent: 0,   color: 'bg-neutral-300', text: 'text-neutral-400' },
    { label: '太弱', percent: 20,  color: 'bg-red-500',     text: 'text-red-600 dark:text-red-400' },
    { label: '弱',   percent: 40,  color: 'bg-orange-500',  text: 'text-orange-600 dark:text-orange-400' },
    { label: '一般', percent: 65,  color: 'bg-amber-500',   text: 'text-amber-600 dark:text-amber-400' },
    { label: '强',   percent: 85,  color: 'bg-emerald-500', text: 'text-emerald-600 dark:text-emerald-400' },
    { label: '极强', percent: 100, color: 'bg-emerald-600', text: 'text-emerald-700 dark:text-emerald-300' },
  ] as const
  const pwd = form.password
  if (!pwd) return { level: 0, ...meta[0] }
  let score = 0
  if (pwd.length >= 8) score++
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) score++
  if (/\d/.test(pwd)) score++
  if (/[^a-zA-Z0-9]/.test(pwd)) score++
  const idx = Math.min(score + 1, meta.length - 1)
  return { level: score, ...meta[idx] }
})

async function handleSubmit() {
  clearErrors()
  serverError.value = ''
  if (!validate(rules, form)) return

  loading.value = true
  try {
    await userStore.register({
      email: form.email,
      password: form.password,
      nickname: form.nickname,
    })
    router.push('/dashboard')
  } catch (e: unknown) {
    serverError.value = (e as Error).message || '注册失败,请重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="w-full max-w-md mx-auto">
    <div class="mb-6">
      <h2 class="text-[26px] font-bold tracking-tight text-neutral-900 dark:text-neutral-100">创建账号</h2>
      <p class="text-sm text-neutral-500 dark:text-neutral-400 mt-1.5">注册以开始使用 MarketAI</p>
    </div>

    <transition name="fade">
      <div
        v-if="serverError"
        class="mb-4 p-3 rounded-lg bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-900 flex items-start gap-2 text-sm text-red-700 dark:text-red-300"
      >
        <ExclamationCircleIcon class="w-4 h-4 mt-0.5 flex-shrink-0" />
        <span>{{ serverError }}</span>
      </div>
    </transition>

    <form @submit.prevent="handleSubmit" class="space-y-3.5" novalidate>
      <!-- 昵称 -->
      <div>
        <label class="block text-[13px] font-medium text-neutral-700 dark:text-neutral-300 mb-1.5">昵称</label>
        <div class="relative">
          <UserIcon class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400 pointer-events-none" />
          <input
            v-model="form.nickname"
            type="text"
            autocomplete="name"
            placeholder="你的昵称"
            :class="[
              'w-full pl-10 pr-3 py-2.5 rounded-md border bg-white dark:bg-[color:var(--color-surface-muted)]',
              'text-sm text-neutral-900 dark:text-neutral-100 placeholder:text-neutral-400',
              'transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-brand-500/20',
              errors.nickname ? 'border-red-400 focus:border-red-500' : 'border-[color:var(--color-border)] focus:border-brand-500',
            ]"
          />
        </div>
        <p v-if="errors.nickname" class="mt-1.5 text-xs text-red-500 flex items-center gap-1">
          <ExclamationCircleIcon class="w-3 h-3" />{{ errors.nickname }}
        </p>
      </div>

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
              errors.email ? 'border-red-400 focus:border-red-500' : 'border-[color:var(--color-border)] focus:border-brand-500',
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
            autocomplete="new-password"
            placeholder="至少 8 位,建议含大小写字母与数字"
            :class="[
              'w-full pl-10 pr-10 py-2.5 rounded-md border bg-white dark:bg-[color:var(--color-surface-muted)]',
              'text-sm text-neutral-900 dark:text-neutral-100 placeholder:text-neutral-400',
              'transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-brand-500/20',
              errors.password ? 'border-red-400 focus:border-red-500' : 'border-[color:var(--color-border)] focus:border-brand-500',
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
        <!-- 密码强度 -->
        <div v-if="form.password" class="mt-2 flex items-center gap-2.5">
          <div class="flex-1 h-1 bg-neutral-200 dark:bg-neutral-700 rounded-full overflow-hidden">
            <div
              :class="['h-full transition-all duration-300', passwordStrength.color]"
              :style="{ width: passwordStrength.percent + '%' }"
            />
          </div>
          <span :class="['text-xs font-medium whitespace-nowrap', passwordStrength.text]">
            {{ passwordStrength.label }}
          </span>
        </div>
      </div>

      <!-- 确认密码 -->
      <div>
        <label class="block text-[13px] font-medium text-neutral-700 dark:text-neutral-300 mb-1.5">确认密码</label>
        <div class="relative">
          <LockClosedIcon class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400 pointer-events-none" />
          <input
            v-model="form.confirmPassword"
            :type="showPassword ? 'text' : 'password'"
            autocomplete="new-password"
            placeholder="再次输入密码"
            :class="[
              'w-full pl-10 pr-10 py-2.5 rounded-md border bg-white dark:bg-[color:var(--color-surface-muted)]',
              'text-sm text-neutral-900 dark:text-neutral-100 placeholder:text-neutral-400',
              'transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-brand-500/20',
              errors.confirmPassword ? 'border-red-400 focus:border-red-500' : 'border-[color:var(--color-border)] focus:border-brand-500',
            ]"
          />
          <CheckCircleIcon
            v-if="form.confirmPassword && form.confirmPassword === form.password"
            class="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-emerald-500"
          />
        </div>
        <p v-if="errors.confirmPassword" class="mt-1.5 text-xs text-red-500 flex items-center gap-1">
          <ExclamationCircleIcon class="w-3 h-3" />{{ errors.confirmPassword }}
        </p>
      </div>

      <!-- 服务条款 -->
      <div>
        <label class="flex items-start gap-2.5 text-sm text-neutral-600 dark:text-neutral-400 cursor-pointer select-none">
          <input
            v-model="form.agreeToTerms"
            type="checkbox"
            class="mt-0.5 w-4 h-4 rounded border-neutral-300 dark:border-neutral-600 text-brand-500 focus:ring-brand-500 focus:ring-offset-0"
          />
          <span class="leading-snug">
            我已阅读并同意
            <a href="#" class="text-brand-600 dark:text-brand-400 hover:underline">服务条款</a>
            和
            <a href="#" class="text-brand-600 dark:text-brand-400 hover:underline">隐私政策</a>
          </span>
        </label>
        <p v-if="errors.agreeToTerms" class="mt-1.5 text-xs text-red-500 flex items-center gap-1">
          <ExclamationCircleIcon class="w-3 h-3" />{{ errors.agreeToTerms }}
        </p>
      </div>

      <AppButton type="submit" :loading="loading" block size="lg" variant="gradient" class="mt-1">
        {{ loading ? '创建中…' : '创建账号' }}
      </AppButton>

      <p class="text-center text-sm text-neutral-500 dark:text-neutral-400">
        已有账号?
        <router-link to="/login" class="text-brand-600 dark:text-brand-400 hover:text-brand-700 dark:hover:text-brand-300 font-medium transition-colors">立即登录</router-link>
      </p>
    </form>
  </div>
</template>
