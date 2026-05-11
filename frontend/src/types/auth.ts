export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  nickname: string
}

export interface RegisterResponse {
  user_id: number
  token: string
}

export interface LoginResponse {
  userId: number
  email: string
  nickname: string
  avatarUrl: string | null
  token: string
}

export interface UserInfo {
  userId: number
  email: string
  nickname: string
  avatarUrl: string | null
  createdAt: string
}

export interface UpdateProfileRequest {
  nickname?: string
  avatarUrl?: string
}
