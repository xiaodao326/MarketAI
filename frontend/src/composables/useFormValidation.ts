import { reactive } from 'vue'

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type ValidationRule = (value: any) => string | null

export const validators = {
  required: (msg = '此项为必填') => (v: unknown) => {
    if (v === null || v === undefined || v === '') return msg
    if (typeof v === 'string' && v.trim() === '') return msg
    return null
  },

  email: (msg = '邮箱格式不正确') => (v: string) => {
    if (!v) return null
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v) ? null : msg
  },

  minLength: (min: number, msg?: string) => (v: string) => {
    if (!v) return null
    return v.length >= min ? null : (msg || `最少${min}个字符`)
  },

  maxLength: (max: number, msg?: string) => (v: string) => {
    if (!v) return null
    return v.length <= max ? null : (msg || `最多${max}个字符`)
  },

  match: (getTarget: () => unknown, msg = '两次输入不一致') => (v: unknown) => {
    return v === getTarget() ? null : msg
  },

  pattern: (regex: RegExp, msg: string) => (v: string) => {
    if (!v) return null
    return regex.test(v) ? null : msg
  },
}

export function useFormValidation() {
  const errors = reactive<Record<string, string>>({})

  function validate(rules: Record<string, ValidationRule[]>, values: Record<string, unknown>): boolean {
    Object.keys(errors).forEach((k) => delete errors[k])

    let isValid = true
    for (const [field, fieldRules] of Object.entries(rules)) {
      for (const rule of fieldRules) {
        const error = rule(values[field])
        if (error) {
          errors[field] = error
          isValid = false
          break
        }
      }
    }
    return isValid
  }

  function setError(field: string, message: string) {
    errors[field] = message
  }

  function clearErrors() {
    Object.keys(errors).forEach((k) => delete errors[k])
  }

  return { errors, validate, setError, clearErrors }
}
