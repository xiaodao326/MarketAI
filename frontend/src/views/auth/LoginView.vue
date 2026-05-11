<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useFormValidation, validators } from '@/composables/useFormValidation'

const router = useRouter()
const userStore = useUserStore()
const { errors, validate, clearErrors } = useFormValidation()

const form = reactive({
  email: '',
  password: '',
  remember: false,
})

const loading = ref(false)
const serverError = ref('')

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
    await userStore.login({
      email: form.email,
      password: form.password,
    })
    router.push('/dashboard')
  } catch (e: unknown) {
    serverError.value = (e as Error).message || '登录失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div>
    <h2 class="text-2xl font-bold text-gray-900 mb-2">欢迎回来</h2>
    <p class="text-gray-500 mb-8">登录您的账号以继续</p>

    <!-- 服务端错误提示 -->
    <div
      v-if="serverError"
      class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-600"
    >
      {{ serverError }}
    </div>

    <form @submit.prevent="handleSubmit" class="space-y-5">
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
          autocomplete="current-password"
          class="w-full px-3 py-2.5 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
          :class="errors.password ? 'border-red-300' : 'border-gray-300'"
          placeholder="输入密码"
        />
        <p v-if="errors.password" class="mt-1 text-xs text-red-500">{{ errors.password }}</p>
      </div>

      <div class="flex items-center justify-between">
        <label class="flex items-center gap-2 text-sm text-gray-600 cursor-pointer">
          <input v-model="form.remember" type="checkbox" class="rounded border-gray-300 text-primary-500 focus:ring-primary-500" />
          记住我
        </label>
        <a href="#" class="text-sm text-primary-500 hover:text-primary-600">忘记密码？</a>
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
          登录中...
        </span>
        <span v-else>登录</span>
      </button>

      <p class="text-center text-sm text-gray-500">
        没有账号？
        <router-link to="/register" class="text-primary-500 hover:text-primary-600 font-medium">去注册</router-link>
      </p>
    </form>
  </div>
</template>
