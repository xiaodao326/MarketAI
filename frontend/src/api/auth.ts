import { post, get, put } from './request'
import type {
  LoginRequest,
  RegisterRequest,
  RegisterResponse,
  LoginResponse,
  UserInfo,
  UpdateProfileRequest,
} from '@/types/auth'

const BASE = '/auth'

export const authApi = {
  register(data: RegisterRequest) {
    return post<RegisterResponse>(`${BASE}/register`, data)
  },

  login(data: LoginRequest) {
    return post<LoginResponse>(`${BASE}/login`, data)
  },

  logout() {
    return post<void>(`${BASE}/logout`)
  },

  getMe() {
    return get<UserInfo>(`${BASE}/me`)
  },

  updateProfile(data: UpdateProfileRequest) {
    return put<UserInfo>(`${BASE}/profile`, data)
  },
}
