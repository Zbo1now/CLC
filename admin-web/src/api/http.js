import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const SESSION_KEY = 'adminSessionId'

export function getAdminSessionId() {
  return localStorage.getItem(SESSION_KEY) || ''
}

export function setAdminSessionId(sessionId) {
  if (!sessionId) {
    localStorage.removeItem(SESSION_KEY)
    return
  }
  localStorage.setItem(SESSION_KEY, sessionId)
}

export function clearAdminSessionId() {
  localStorage.removeItem(SESSION_KEY)
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000
})

let isHandling401 = false

http.interceptors.request.use((config) => {
  const sessionId = getAdminSessionId()
  if (sessionId) {
    config.headers = config.headers || {}
    config.headers['X-Admin-Session-Id'] = sessionId
  }
  return config
})

http.interceptors.response.use(
  (resp) => resp.data,
  async (error) => {
    const hasResponse = !!error?.response
    const status = error?.response?.status

    if (!hasResponse) {
      ElMessage.error('无法连接到后端服务，请确认后端已启动（8081）并检查网络/代理配置')
      return Promise.reject(error)
    }

    if (status === 401) {
      clearAdminSessionId()

      if (window.location.pathname !== '/login' && !isHandling401) {
        isHandling401 = true
        ElMessage.warning('登录已过期，请重新登录')
        const redirect = encodeURIComponent(window.location.pathname + window.location.search)
        setTimeout(() => {
          window.location.href = `/login?redirect=${redirect}`
          isHandling401 = false
        }, 500)
      }

      return Promise.reject(error)
    }

    if (status >= 500) {
      const msg = error?.response?.data?.message || '服务器异常，请稍后重试'
      ElMessage.error(msg)
      return Promise.reject(error)
    }

    return Promise.reject(error)
  }
)

export default http
