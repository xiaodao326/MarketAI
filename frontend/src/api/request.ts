import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import type { ApiResponse } from '@/types'
import { toast } from '@/utils/feedback'

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求拦截器 — 自动附加 token
instance.interceptors.request.use((config) => {
  const token = localStorage.getItem('marketai_token')
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let sessionExpiredNotified = false

// 响应拦截器 — 统一处理业务错误和 401/403
instance.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const { code, message } = response.data
    if (code !== 0) {
      return Promise.reject(new Error(message || '请求失败'))
    }
    return response
  },
  (error) => {
    const status = error.response?.status

    if (status === 401 || status === 403) {
      localStorage.removeItem('marketai_token')
      localStorage.removeItem('marketai_user')
      // 防止登录页内的接口请求触发重定向循环;同时避免连续刷新出现多条 toast
      if (window.location.pathname !== '/login') {
        if (!sessionExpiredNotified) {
          sessionExpiredNotified = true
          toast.warning('登录状态已失效,请重新登录')
          setTimeout(() => { sessionExpiredNotified = false }, 3000)
        }
        window.location.href = '/login'
      }
    } else if (!error.response) {
      // 网络层错误(超时、断网)主动提示;业务错误留给调用方 catch
      toast.error(error.message || '网络异常,请检查连接')
    }

    return Promise.reject(error)
  },
)

export async function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const res = await instance.get<ApiResponse<T>>(url, { params })
  return res.data.data
}

export async function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const res = await instance.post<ApiResponse<T>>(url, data, config)
  return res.data.data
}

export async function put<T>(url: string, data?: unknown): Promise<T> {
  const res = await instance.put<ApiResponse<T>>(url, data)
  return res.data.data
}

export async function del<T>(url: string): Promise<T> {
  const res = await instance.delete<ApiResponse<T>>(url)
  return res.data.data
}

export default instance
