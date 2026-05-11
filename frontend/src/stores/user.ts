import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { authApi } from '@/api/auth'
import type { UserInfo, LoginRequest, RegisterRequest } from '@/types/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('marketai_token') || '')
  const userInfo = ref<UserInfo | null>(
    JSON.parse(localStorage.getItem('marketai_user') || 'null'),
  )

  const isLoggedIn = computed(() => !!token.value)

  // 持久化到 localStorage
  watch(token, (val) => {
    if (val) {
      localStorage.setItem('marketai_token', val)
    } else {
      localStorage.removeItem('marketai_token')
    }
  })

  watch(userInfo, (val) => {
    if (val) {
      localStorage.setItem('marketai_user', JSON.stringify(val))
    } else {
      localStorage.removeItem('marketai_user')
    }
  }, { deep: true })

  async function login(data: LoginRequest) {
    const res = await authApi.login(data)
    token.value = res.token
    userInfo.value = {
      userId: res.userId,
      email: res.email,
      nickname: res.nickname,
      avatarUrl: res.avatarUrl,
      createdAt: '',
    }
  }

  async function register(data: RegisterRequest) {
    const res = await authApi.register(data)
    token.value = res.token
    await fetchUserInfo()
  }

  async function fetchUserInfo() {
    const info = await authApi.getMe()
    userInfo.value = info
  }

  async function logout() {
    try {
      await authApi.logout()
    } catch {
      // token 可能已失效，响应拦截器会处理清理
    }
    token.value = ''
    userInfo.value = null
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    register,
    fetchUserInfo,
    logout,
  }
})
