import type {
  ConsultantHumanMessageVO,
  ConsultantReceptionVO,
  ConversationReadStateVO,
  ConversationVO,
  CreateProjectFileRequest,
  CreateProjectFromConsultationRequest,
  FileAssetVO,
  LoginResponse,
  MarkReadRequest,
  MessageCursorPage,
  MessageVO,
  OperationLogVO,
  PageResult,
  PortfolioCaseRequest,
  PortfolioCaseVO,
  ProjectFileVO,
  ProjectPreparationVO,
  ProjectStageVO,
  ProjectVO,
  Result,
  SendMessageRequest,
  StageActionRequest,
  StageActionResponseRequest,
  StageActionVO,
  UpdateProjectRequest,
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
  const token = localStorage.getItem('ad-designer-token')
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const response = await fetch(`${API_BASE}${path}`, { ...init, headers })
  return handleResponse<T>(response)
}

async function requestBlob(path: string, init: RequestInit = {}) {
  const headers = new Headers(init.headers)
  const token = localStorage.getItem('ad-designer-token')
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  const response = await fetch(`${API_BASE}${path}`, { ...init, headers })
  if (!response.ok) {
    throw new Error(response.statusText || '请求失败')
  }
  return response.blob()
}

export function fetchLogin(email: string, password: string) {
  return request<LoginResponse>('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) })
}
export function fetchMe() { return request<UserVO>('/auth/me') }
export function fetchLogout() { return request<boolean>('/auth/logout', { method: 'POST' }) }
export function updateMe(payload: UpdateUserRequest) { return request<UserVO>('/users/me', { method: 'PUT', body: JSON.stringify(payload) }) }

export function listPortfolioCases(params: { category?: string; industry?: string; style?: string; keyword?: string; featured?: boolean; page?: number; size?: number }) {
  return request<PageResult<PortfolioCaseVO>>(`/portfolio-cases${buildQuery(params)}`)
}
export function getPortfolioCase(id: number) { return request<PortfolioCaseVO>(`/portfolio-cases/${id}`) }
export function createPortfolioCase(payload: PortfolioCaseRequest) { return request<PortfolioCaseVO>('/portfolio-cases', { method: 'POST', body: JSON.stringify(payload) }) }
export function updatePortfolioCase(id: number, payload: PortfolioCaseRequest) { return request<PortfolioCaseVO>(`/portfolio-cases/${id}`, { method: 'PUT', body: JSON.stringify(payload) }) }
export function deletePortfolioCase(id: number) { return request<boolean>(`/portfolio-cases/${id}`, { method: 'DELETE' }) }

export function listProjects(params: { status?: string; currentStage?: string; keyword?: string; page?: number; size?: number }) {
  return request<PageResult<ProjectVO>>(`/projects${buildQuery(params)}`)
}
export function getProject(id: number) { return request<ProjectVO>(`/projects/${id}`) }
export function createProjectFromConsultation(payload: CreateProjectFromConsultationRequest) { return request<ProjectVO>('/projects/from-consultation', { method: 'POST', body: JSON.stringify(payload) }) }
export function updateProject(id: number, payload: UpdateProjectRequest) { return request<ProjectVO>(`/projects/${id}`, { method: 'PUT', body: JSON.stringify(payload) }) }
export function deleteProject(id: number) { return request<boolean>(`/projects/${id}`, { method: 'DELETE' }) }
export function listProjectStages(projectId: number) { return request<ProjectStageVO[]>(`/projects/${projectId}/stages`) }
export function listProjectActions(projectId: number, params: { stageCode?: string; status?: string } = {}) {
  return request<StageActionVO[]>(`/projects/${projectId}/stage-actions${buildQuery(params)}`)
}
export function createStageAction(projectId: number, stageCode: string, payload: StageActionRequest) {
  return request<StageActionVO>(`/projects/${projectId}/stages/${stageCode}/actions`, { method: 'POST', body: JSON.stringify(payload) })
}
export function confirmStageAction(actionId: number, payload: StageActionResponseRequest = {}) {
  return request<StageActionVO>(`/stage-actions/${actionId}/confirm`, { method: 'POST', body: JSON.stringify(payload) })
}
export function rejectStageAction(actionId: number, payload: StageActionResponseRequest = {}) {
  return request<StageActionVO>(`/stage-actions/${actionId}/reject`, { method: 'POST', body: JSON.stringify(payload) })
}

export function listConsultantReceptions() {
  return request<ConsultantReceptionVO[]>('/consultant-intakes/designer-receptions')
}
export function getConsultantReception(intakeId: number) {
  return request<ConsultantReceptionVO>(`/consultant-intakes/designer-receptions/${intakeId}`)
}
export function acceptConsultantReception(intakeId: number) {
  return request<ConsultantReceptionVO>(`/consultant-intakes/designer-receptions/${intakeId}/accept`, { method: 'POST' })
}
export function getProjectPreparation(intakeId: number) {
  return request<ProjectPreparationVO>(`/consultant-intakes/designer-receptions/${intakeId}/project-preparation`)
}
export function confirmConsultationContract(intakeId: number) {
  return request<ProjectPreparationVO>(`/consultant-intakes/designer-receptions/${intakeId}/project-preparation/contract-confirmation`, { method: 'POST' })
}
export function confirmConsultationInitialPayment(intakeId: number) {
  return request<ProjectPreparationVO>(`/consultant-intakes/designer-receptions/${intakeId}/project-preparation/initial-payment-confirmation`, { method: 'POST' })
}
export function listConsultantHumanMessages(humanChatId: string) {
  return request<ConsultantHumanMessageVO[]>(`/consultant-intakes/human-chats/${encodeURIComponent(humanChatId)}/messages`)
}
export function sendConsultantHumanMessage(humanChatId: string, content: string) {
  return request<ConsultantHumanMessageVO>(`/consultant-intakes/human-chats/${encodeURIComponent(humanChatId)}/messages`, {
    method: 'POST',
    body: JSON.stringify({ content })
  })
}

export function listConversations() { return request<ConversationVO[]>('/conversations') }
export function listMessages(conversationId: number, params: { beforeMessageId?: number; size?: number } = {}) {
  return request<MessageCursorPage>(`/conversations/${conversationId}/messages${buildQuery(params)}`)
}
export function sendMessage(conversationId: number, payload: SendMessageRequest) {
  return request<MessageVO>(`/conversations/${conversationId}/messages`, { method: 'POST', body: JSON.stringify(payload) })
}
export function markConversationRead(conversationId: number, payload: MarkReadRequest) {
  return request<ConversationReadStateVO>(`/conversations/${conversationId}/read`, { method: 'POST', body: JSON.stringify(payload) })
}

export function uploadConversationFile(conversationId: number, file: File) {
  const form = new FormData()
  form.append('file', file)
  const image = file.type.startsWith('image/')
  return request<FileAssetVO>(`/conversations/${conversationId}/file-assets?image=${image}`, {
    method: 'POST',
    body: form
  })
}
export function uploadProjectFile(projectId: number, fileRole: string, file: File) {
  const form = new FormData()
  form.append('file', file)
  return request<FileAssetVO>(`/projects/${projectId}/file-assets?fileRole=${encodeURIComponent(fileRole)}`, {
    method: 'POST',
    body: form
  })
}
export function downloadFile(fileId: number) { return requestBlob(`/files/${fileId}/download`) }
export function getFile(fileId: number) { return request<FileAssetVO>(`/files/${fileId}`) }
export function listProjectFiles(projectId: number, params: { stageCode?: string; fileRole?: string } = {}) {
  return request<ProjectFileVO[]>(`/projects/${projectId}/files${buildQuery(params)}`)
}
export function archiveProjectFile(projectId: number, payload: CreateProjectFileRequest) {
  return request<ProjectFileVO>(`/projects/${projectId}/files`, { method: 'POST', body: JSON.stringify(payload) })
}
export function deleteProjectFile(id: number) { return request<boolean>(`/project-files/${id}`, { method: 'DELETE' }) }

export function listOperationLogs(projectId: number, params: { bizType?: string; action?: string; page?: number; size?: number } = {}) {
  return request<PageResult<OperationLogVO>>(`/projects/${projectId}/operation-logs${buildQuery(params)}`)
}
