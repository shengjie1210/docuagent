import axios from 'axios'
import router from '@/router'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  // 管理后台请求自动带上解锁 token（共享密码验证通过后获得）
  if (config.url && config.url.startsWith('/admin/')) {
    const adminToken = localStorage.getItem('adminToken')
    if (adminToken) {
      config.headers['X-Admin-Token'] = adminToken
    }
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    } else if (res.code === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('isAdmin')
      localStorage.removeItem('adminToken')
      ElMessage.warning(res.message || '登录已过期，请重新登录')
      router.push('/login')
      return Promise.reject(new Error(res.message || '登录已过期'))
    } else if (res.code === 403) {
      // 管理后台解锁失效/未解锁：清掉解锁标记，让下次进入重新弹密码框
      const url = response.config?.url || ''
      if (url.startsWith('/admin/') && !url.includes('/unlock')) {
        localStorage.removeItem('adminToken')
      }
      ElMessage.warning(res.message || '没有权限')
      return Promise.reject(new Error(res.message || '没有权限'))
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  (error) => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export const authApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data)
}

export const documentApi = {
  upload: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/document/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  uploadBatch: (files) => {
    const form = new FormData()
    Array.from(files).forEach((f) => form.append('files', f))
    return request.post('/document/uploadBatch', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    })
  },
  list: (params) => request.get('/document/list', { params }),
  detail: (id) => request.get('/document/' + id),
  content: (id) => request.get('/document/' + id + '/content'),
  delete: (id) => request.delete('/document/' + id),
  reindex: (id) => request.post('/document/' + id + '/reindex')
}

export const conversationApi = {
  list: () => request.get('/conversation/list'),
  create: (data) => request.post('/conversation/create', data || {}),
  delete: (id) => request.delete('/conversation/' + id),
  messages: (id) => request.get('/conversation/' + id + '/messages')
}

/**
 * 管理员后台 API
 */
export const adminApi = {
  // 用共享管理员密码解锁，返回解锁 token
  unlock: (password) => request.post('/admin/unlock', { password }),
  // 供应商管理
  listProviders: () => request.get('/admin/providers'),
  saveProvider: (data) => request.post('/admin/providers', data),
  updateProvider: (id, data) => request.put('/admin/providers/' + id, data),
  deleteProvider: (id) => request.delete('/admin/providers/' + id),
  enableProvider: (id) => request.post('/admin/providers/' + id + '/enable'),
  testProvider: (id) => request.post('/admin/providers/' + id + '/test'),
  testDraft: (data) => request.post('/admin/providers/test', data),
  providerBalance: (id) => request.get('/admin/providers/' + id + '/balance'),
  // RAG 配置
  getRagConfig: () => request.get('/admin/rag-config'),
  updateRagConfig: (data) => request.put('/admin/rag-config', data),
  // 系统监控
  systemInfo: () => request.get('/admin/system/info'),
  systemLogs: (lines) => request.get('/admin/system/logs', { params: { lines } }),
  systemErrorLogs: (lines) => request.get('/admin/system/logs/error', { params: { lines } })
}

/**
 * SSE 流式聊天
 */
export const fetchChatStream = async (conversationId, message, onMessage, onDone, onError) => {
  const token = localStorage.getItem('token')
  try {
    const response = await fetch('/api/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
      },
      body: JSON.stringify({ conversationId, message })
    })

    if (!response.ok) {
      let errMsg = 'HTTP ' + response.status
      try {
        const text = await response.text()
        const json = JSON.parse(text)
        if (json?.message) errMsg = json.message
      } catch (e) {}
      throw new Error(errMsg)
    }

    if (!response.body) throw new Error('Response body is empty')

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let eventName = ''
    let dataStr = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split(/\r?\n/)
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed) {
          if (eventName || dataStr) {
            if (eventName === 'message') {
              onMessage && onMessage({ content: dataStr })
            } else if (eventName === 'done') {
              try {
                const json = JSON.parse(dataStr)
                onMessage && onMessage({ sources: json.sources || [] })
              } catch (e) {
                console.warn('Parse done data failed:', e, dataStr)
              }
            } else if (eventName === 'error') {
              onError && onError(new Error(dataStr))
            }
            eventName = ''
            dataStr = ''
          }
        } else if (trimmed.startsWith('event:')) {
          eventName = trimmed.slice(6).trim()
        } else if (trimmed.startsWith('data:')) {
          dataStr = trimmed.slice(5).trim()
        }
      }
    }

    if (eventName || dataStr) {
      if (eventName === 'message') onMessage && onMessage({ content: dataStr })
      else if (eventName === 'done') {
        try {
          const json = JSON.parse(dataStr)
          onMessage && onMessage({ sources: json.sources || [] })
        } catch (e) {}
      }
    }

    onDone && onDone()
  } catch (err) {
    console.error('fetchChatStream error:', err)
    onError && onError(err)
    onDone && onDone()
  }
}

export default request