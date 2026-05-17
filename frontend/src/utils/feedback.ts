import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import type { MessageBoxData } from 'element-plus'

/**
 * 全局反馈封装 — 项目内所有 toast/confirm 都通过这里
 * Why: 统一出口便于后续替换实现或加埋点;避免散落的 alert/confirm
 */

export const toast = {
  success(message: string, duration = 2500) {
    ElMessage({ type: 'success', message, duration, showClose: true, grouping: true })
  },
  error(message: string, duration = 3500) {
    ElMessage({ type: 'error', message, duration, showClose: true, grouping: true })
  },
  warning(message: string, duration = 3000) {
    ElMessage({ type: 'warning', message, duration, showClose: true, grouping: true })
  },
  info(message: string, duration = 2500) {
    ElMessage({ type: 'info', message, duration, showClose: true, grouping: true })
  },
}

export const notify = {
  success(title: string, message?: string) {
    ElNotification({ type: 'success', title, message, duration: 3500, position: 'top-right' })
  },
  error(title: string, message?: string) {
    ElNotification({ type: 'error', title, message, duration: 4500, position: 'top-right' })
  },
  warning(title: string, message?: string) {
    ElNotification({ type: 'warning', title, message, duration: 3500, position: 'top-right' })
  },
  info(title: string, message?: string) {
    ElNotification({ type: 'info', title, message, duration: 3500, position: 'top-right' })
  },
}

/**
 * 二次确认弹窗,默认危险样式
 * Why: 替换原生 confirm,统一外观和深色模式支持
 */
export async function confirm(
  message: string,
  title = '请确认',
  options: {
    type?: 'warning' | 'info' | 'success' | 'error'
    confirmText?: string
    cancelText?: string
    danger?: boolean
  } = {},
): Promise<boolean> {
  const { type = 'warning', confirmText = '确认', cancelText = '取消', danger = false } = options
  try {
    await (ElMessageBox.confirm(message, title, {
      confirmButtonText: confirmText,
      cancelButtonText: cancelText,
      type,
      confirmButtonClass: danger ? 'el-button--danger' : '',
      draggable: true,
      roundButton: false,
      closeOnClickModal: false,
    }) as Promise<MessageBoxData>)
    return true
  } catch {
    return false
  }
}
