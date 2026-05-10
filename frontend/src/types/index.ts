// 统一 API 响应格式
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

// 分页请求参数
export interface PageQuery {
  page: number
  size: number
  sort?: string
}

// 分页响应
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
  pages: number
}