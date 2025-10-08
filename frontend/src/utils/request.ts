import { message } from 'antd'
import type { ApiResponse } from '../types/api'

/**
 * 统一处理 API 响应
 * @param response axios 响应对象
 * @param showError 是否显示错误消息
 * @returns 响应数据
 */
export function handleApiResponse<T>(
  response: { data: ApiResponse<T> },
  showError: boolean = true
): T | null {
  const { code, message: msg, data } = response.data

  if (code === 200) {
    return data
  } else {
    if (showError) {
      message.error(msg || '请求失败')
    }
    return null
  }
}

/**
 * 统一处理 API 错误
 * @param error axios 错误对象
 * @param showError 是否显示错误消息
 */
export function handleApiError(error: any, showError: boolean = true) {
  const errorMsg = error.response?.data?.message || '请求失败'
  if (showError) {
    message.error(errorMsg)
  }
  console.error('API Error:', error)
}
