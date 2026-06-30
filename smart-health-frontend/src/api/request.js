import axios from 'axios'
import { showToast } from 'vant'
import { getToken, clearAuth } from '@/utils/storage'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000
})

// 请求拦截器：自动附加 JWT token
request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：解包 Result<T>，统一错误处理
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    // 业务错误
    showToast(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      if (status === 401 || (data && data.code === 401)) {
        clearAuth()
        showToast('登录已过期，请重新登录')
        router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
      } else {
        showToast(data?.message || '网络请求失败')
      }
    } else {
      showToast('网络连接失败，请检查网络')
    }
    return Promise.reject(error)
  }
)

export default request
