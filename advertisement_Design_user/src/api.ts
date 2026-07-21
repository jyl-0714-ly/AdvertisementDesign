import type {
  ConversationReadStateVO,
  ConversationVO,
  CreateProjectFileRequest,
  FileAssetVO,
  LoginResponse,
  MarkReadRequest,
  MessageCursorPage,
  MessageVO,
  PageResult,
  PortfolioCaseVO,
  ProjectFileVO,
  ProjectStageVO,
  ProjectVO,
  Result,
  SendMessageRequest,
  StageActionVO,
  UpdateUserRequest,
  UserVO
} from './models'

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

function buildQuery(params?: Record<string, string | number | boolean | null | undefined>) {
  const query = new URLSearchParams()
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && `${value}` !== '') {
      query.set(key, String(value))
    }
  })
  const text = query.toString()
  return text ? `?${text}` : ''
}

async function handleResponse<T>(response: Response): Promise<T> {
  const contentType = response.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    const payload = (await response.json()) as Result<T> | T
    if (typeof payload === 'object' && payload !== null && 'code' in payload) {
      if (payload.code !== 0) {
        throw new Error(payload.message || '请求失败')
      }
      return (payload as Result<T>).data
    }
    return payload as T
  }
  if (!response.ok) {
    throw new Error(response.statusText || '请求失败')
  }
  return (await response.text()) as unknown as T
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  const token = localStorage.getItem('ad-user-token')
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers
  })
  if (!response.ok && response.headers.get('content-type')?.includes('application/json')) {
    const payload = await response.json().catch(() => null)
    if (payload && typeof payload === 'object' && 'message' in payload) {
      throw new Error((payload as { message?: string }).message || response.statusText)
    }
  }
  return handleResponse<T>(response)
}

async function requestBlob(path: string, init: RequestInit = {}) {
  const headers = new Headers(init.headers)
  const token = localStorage.getItem('ad-user-token')
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers
  })
  if (!response.ok) {
    throw new Error(response.statusText || '请求失败')
  }
  return response.blob()
}

export function fetchLogin(email: string, password: string) {
  return request<LoginResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password })
  })
}

export function fetchMe() {
  return request<UserVO>('/auth/me')
}

export function fetchLogout() {
  return request<boolean>('/auth/logout', { method: 'POST' })
}

export function updateMe(payload: UpdateUserRequest) {
  return request<UserVO>('/users/me', {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
}

export function listPortfolioCases(params: {
  industry?: string
  style?: string
  serviceType?: string
  keyword?: string
  page?: number
  size?: number
}) {
  return request<PageResult<PortfolioCaseVO>>(`/portfolio-cases${buildQuery(params)}`)
}

export function getPortfolioCase(id: number) {
  return request<PortfolioCaseVO>(`/portfolio-cases/${id}`)
}

export function listProjects(params: {
  status?: string
  currentStage?: string
  keyword?: string
  page?: number
  size?: number
}) {
  return request<PageResult<ProjectVO>>(`/projects${buildQuery(params)}`)
}

export function getProject(id: number) {
  return request<ProjectVO>(`/projects/${id}`)
}

export function listProjectStages(projectId: number) {
  return request<ProjectStageVO[]>(`/projects/${projectId}/stages`)
}

export function listConversations() {
  return request<ConversationVO[]>('/conversations')
}

export function listMessages(conversationId: number, params: { beforeMessageId?: number; size?: number } = {}) {
  return request<MessageCursorPage>(`/conversations/${conversationId}/messages${buildQuery(params)}`)
}

export function sendMessage(conversationId: number, payload: SendMessageRequest) {
  return request<MessageVO>(`/conversations/${conversationId}/messages`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function uploadFile(file: File) {
  const body = new FormData()
  body.append('file', file)
  return request<FileAssetVO>('/files', { method: 'POST', body })
}

export function markConversationRead(conversationId: number, payload: MarkReadRequest) {
  return request<ConversationReadStateVO>(`/conversations/${conversationId}/read`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function listProjectFiles(projectId: number, params: { stageCode?: string; fileRole?: string } = {}) {
  return request<ProjectFileVO[]>(`/projects/${projectId}/files${buildQuery(params)}`)
}

export function archiveProjectFile(projectId: number, payload: CreateProjectFileRequest) {
  return request<ProjectFileVO>(`/projects/${projectId}/files`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function deleteProjectFile(id: number) {
  return request<boolean>(`/project-files/${id}`, { method: 'DELETE' })
}

export function downloadFile(fileId: number) {
  return requestBlob(`/files/${fileId}/download`, { method: 'GET' })
}

export function listStageActions(projectId: number) {
  return request<StageActionVO[]>(`/projects/${projectId}/stage-actions`)
}

export function createStageAction(projectId: number, stageCode: string, requestNote: string) {
  return request<StageActionVO>(`/projects/${projectId}/stages/${stageCode}/actions`, {
    method: 'POST',
    body: JSON.stringify({ requestNote })
  })
}

export function confirmStageAction(actionId: number, responseNote = '') {
  return request<StageActionVO>(`/stage-actions/${actionId}/confirm`, {
    method: 'POST',
    body: JSON.stringify({ responseNote })
  })
}

export function rejectStageAction(actionId: number, responseNote = '') {
  return request<StageActionVO>(`/stage-actions/${actionId}/reject`, {
    method: 'POST',
    body: JSON.stringify({ responseNote })
  })
}
