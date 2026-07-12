import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'
import { showToast } from 'vant'
import { getToken, clearAuth } from '@/utils/storage'
import router from '@/router'

interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// ============ ETag 缓存（用于问诊会话列表/详情） ============
const etagCache = new Map<string, { etag: string; data: any }>()
const MAX_ETAG_CACHE = 100

// ============ 401 防重入锁 ============
let isRedirecting401 = false

/** 生成缓存 key：method + url + 序列化后的 params */
function cacheKey(config: InternalAxiosRequestConfig): string {
  const params = config.params ? JSON.stringify(config.params) : ''
  return `${config.method?.toUpperCase()}:${config.url}:${params}`
}

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json; charset=UTF-8',
    'Accept-Charset': 'UTF-8'
  },
  // 将 304 视为成功响应，由拦截器统一处理
  validateStatus: (status) => (status >= 200 && status < 300) || status === 304
})

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }

    // FormData 请求：清除 Content-Type，让浏览器自动生成 multipart boundary
    if (config.data instanceof FormData) {
      config.headers.delete('Content-Type')
    }

    // 对会话相关接口附加 If-None-Match
    if (config.url?.startsWith('/api/v1/ai/sessions/')) {
      const key = cacheKey(config)
      const cached = etagCache.get(key)
      if (cached?.etag) {
        config.headers['If-None-Match'] = cached.etag
      }
    }

    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    if (response.status === 304) {
      // 304 Not Modified — 返回缓存的 data
      const key = cacheKey(response.config)
      const cached = etagCache.get(key)
      if (cached) {
        return cached.data
      }
      // 无缓存兜底：让调用方重新请求（理论上不会发生）
      return Promise.reject(new Error('ETag 缓存命中但本地无缓存数据'))
    }

    const res = response.data
    if (res.code === 200) {
      // 对会话相关接口缓存 ETag + data
      if (response.config.url?.startsWith('/api/v1/ai/sessions/')) {
        const etag = response.headers['etag'] as string | undefined
        if (etag) {
          const key = cacheKey(response.config)
          // LRU 淘汰：超过上限时清除最早的缓存
          if (etagCache.size >= MAX_ETAG_CACHE) {
            const firstKey = etagCache.keys().next().value
            if (firstKey) etagCache.delete(firstKey)
          }
          etagCache.set(key, { etag, data: res.data })
        }
      }
      return res.data
    }
    showToast(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      if (status === 401 || (data && data.code === 401)) {
        if (!isRedirecting401) {
          isRedirecting401 = true
          clearAuth()
          showToast('登录已过期，请重新登录')
          router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
          // 3秒后重置锁，允许用户重新登录后再次触发
          setTimeout(() => { isRedirecting401 = false }, 3000)
        }
      } else if (status !== 304) {
        showToast(data?.message || '网络请求失败')
      }
    } else {
      showToast('网络连接失败，请检查网络')
    }
    return Promise.reject(error)
  }
)

/**
 * 类型安全的 HTTP 方法包装
 * 由于响应拦截器已解包 ApiResponse.data，此处直接返回 Promise<T>
 */
const http = {
  get<T = any>(url: string, config?: any): Promise<T> {
    return request.get(url, config) as Promise<T>
  },
  post<T = any>(url: string, data?: any, config?: any): Promise<T> {
    return request.post(url, data, config) as Promise<T>
  },
  put<T = any>(url: string, data?: any, config?: any): Promise<T> {
    return request.put(url, data, config) as Promise<T>
  },
  del<T = any>(url: string, config?: any): Promise<T> {
    return request.delete(url, config) as Promise<T>
  }
}

export default http
