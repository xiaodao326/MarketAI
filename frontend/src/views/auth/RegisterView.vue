<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useFormValidation, validators } from '@/composables/useFormValidation'

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

const rules = {
  nickname: [
    validators.required('请输入昵称'),
    validators.minLength(2, '昵称长度2-20个字符'),
    validators.maxLength(20, '昵称长度2-20个字符'),
  ],
  email: [validators.required('请输入邮箱'), validators.email()],
  password: [
    validators.required('请输入密码'),
    validators.minLength(8, '密码长度至少8位'),
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
  const pwd = form.password
  if (!pwd) return { level: 0, label: '', width: '0%', color: '' }

  let score = 0
  if (pwd.length >= 8) score++
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) score++
  if (/\d/.test(pwd)) score++
  if (/[^a-zA-Z0-9]/.test(pwd)) score++

  switch (score) {
    case 1: return { level: 1, label: '弱', width: '25%', color: 'bg-red-500' }
    case 2: return { level: 2, label: '一般', width: '50%', color: 'bg-yellow-500' }
    case 3: return { level: 3, label: '强', width: '75%', color: 'bg-green-500' }
    case 4: return { level: 4, label: '非常强', width: '100%', color: 'bg-green-600' }
    default: return { level: 0, label: '弱', width: '25%', color: 'bg-red-500' }
  }
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
    serverError.value = (e as Error).message || '注册失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div>
    <h2 class="text-2xl font-bold text-gray-900 mb-2">创建账号</h2>
    <p class="text-gray-500 mb-8">注册以开始使用 MarketAI</p>

    <!-- 服务端错误提示 -->
    <div
      v-if="serverError"
      class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-600"
    >
      {{ serverError }}
    </div>

    <form @submit.prevent="handleSubmit" class="space-y-4">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">昵称</label>
        <input
          v-model="form.nickname"
          type="text"
          autocomplete="name"
          class="w-full px-3 py-2.5 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
          :class="errors.nickname ? 'border-red-300' : 'border-gray-300'"
          placeholder="你的昵称"
        />
        <p v-if="errors.nickname" class="mt-1 text-xs text-red-500">{{ errors.nickname }}</p>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">邮箱</label>
        <input
          v-model="form.email"
          type="email"
          autocomplete="email"
          class="w-full px-3 py-2.5 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
          :class="errors.email ? 'border-red-300' : 'border-gray-300'"
          placeholder="name@example.com"
        />
        <p v-if="errors.email" class="mt-1 text-xs text-red-500">{{ errors.email }}</p>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">密码</label>
        <input
          v-model="form.password"
          type="password"
          autocomplete="new-password"
          class="w-full px-3 py-2.5 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
          :class="errors.password ? 'border-red-300' : 'border-gray-300'"
          placeholder="至少8位，含大小写字母和数字"
        />
        <p v-if="errors.password" class="mt-1 text-xs text-red-500">{{ errors.password }}</p>
        <!-- 密码强度指示器 -->
        <div v-if="form.password" class="mt-2">
          <div class="h-1 bg-gray-200 rounded-full overflow-hidden">
            <div
              :class="['h-full rounded-full transition-all duration-300', passwordStrength.color]"
              :style="{ width: passwordStrength.width }"
            />
          </div>
          <p class="text-xs text-gray-500 mt-1">密码强度：{{ passwordStrength.label }}</p>
        </div>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">确认密码</label>
        <input
          v-model="form.confirmPassword"
          type="password"
          autocomplete="new-password"
          class="w-full px-3 py-2.5 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
          :class="errors.confirmPassword ? 'border-red-300' : 'border-gray-300'"
          placeholder="再次输入密码"
        />
        <p v-if="errors.confirmPassword" class="mt-1 text-xs text-red-500">{{ errors.confirmPassword }}</p>
      </div>

      <div>
        <label class="flex items-start gap-2 text-sm text-gray-600 cursor-pointer">
          <input
            v-model="form.agreeToTerms"
            type="checkbox"
            class="mt-0.5 rounded border-gray-300 text-primary-500 focus:ring-primary-500"
          />
          <span>我已阅读并同意 <a href="#" class="text-primary-500 hover:text-primary-600">服务条款</a> 和 <a href="#" class="text-primary-500 hover:text-primary-600">隐私政策</a></span>
        </label>
        <p v-if="errors.agreeToTerms" class="mt-1 text-xs text-red-500">{{ errors.agreeToTerms }}</p>
      </div>

      <button
        type="submit"
        :disabled="loading"
        class="w-full py-2.5 bg-primary-500 text-white rounded-lg text-sm font-medium hover:bg-primary-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
      >
        <span v-if="loading" class="inline-flex items-center gap-2">
          <svg class="animate-spin h-4 w-4" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          注册中...
        </span>
        <span v-else>注册</span>
      </button>

      <p class="text-center text-sm text-gray-500">
        已有账号？
        <router-link to="/login" class="text-primary-500 hover:text-primary-600 font-medium">去登录</router-link>
      </p>
    </form>
  </div>
</template>
