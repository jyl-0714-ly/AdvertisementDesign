export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  const token = localStorage.getItem('ad-user-token')
  if (token && !headers.has('Authorization')) headers.set('Authorization', `Bearer ${token}`)
  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const response = await fetch(`${API_BASE}${path}`, { ...init, headers })
  const isJson = response.headers.get('content-type')?.includes('application/json')
  const payload = isJson ? await response.json().catch(() => null) : null
  if (!response.ok) {
    throw new Error(payload?.message || response.statusText || '请求失败')
  }
  if (payload && typeof payload === 'object' && 'code' in payload) {
    const result = payload as ApiResult<T>
    if (result.code !== 0) throw new Error(result.message || '请求失败')
    return result.data
  }
  return payload as T
}
