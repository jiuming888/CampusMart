import axios, { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig, AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { getTokenKey, getUserKey } from '@/utils/tabId'

// 统一响应数据格式
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 覆盖 AxiosInstance 的方法返回类型：去掉 AxiosPromise 包装，直接返回 Promise<T>
export type UnpackedAxiosInstance = Omit<AxiosInstance, 'get' | 'post' | 'put' | 'delete' | 'patch' | 'head' | 'options'> & {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  head<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  options<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
}

const request: UnpackedAxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000
}) as UnpackedAxiosInstance

// 请求拦截器
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 使用 sessionStorage - 关闭浏览器后登录状态自动清除
    const token = sessionStorage.getItem(getTokenKey())
    if (token) {
      config.headers['Token'] = token
    }
    // 开发环境打印请求
    if (import.meta.env.DEV) {
      console.log('[API Request]', config.method?.toUpperCase(), config.url, config.params || config.data)
    }
    return config
  },
  (error: any) => {
    return Promise.reject(error)
  }
)

// 响应拦截器 — 成功时返回 res.data（已解包），失败时抛出 Error
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data as ApiResponse
    // 开发环境打印响应
    if (import.meta.env.DEV) {
      console.log('[API Response]', response.config.url, {
        status: response.status,
        res
      })
    }
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    const data = res.data as any
    if (import.meta.env.DEV) {
      console.log('[API Response Data]', data)
    }
    return data
  },
  (error: any) => {
    if (import.meta.env.DEV) {
      console.log('[API Error]', error)
      if (error.response) {
        console.log('[API Error Response]', error.response.data)
      }
    }
    if (error.response?.status === 401) {
      sessionStorage.removeItem(getTokenKey())
      sessionStorage.removeItem(getUserKey())
      window.location.href = '/login'
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
