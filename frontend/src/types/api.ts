// 统一的 API 响应格式
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 分页响应
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}
