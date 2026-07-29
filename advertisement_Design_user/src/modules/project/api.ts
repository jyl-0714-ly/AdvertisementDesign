import { apiRequest } from '@/modules/http'
import type {
  CurrentProjectStageWorkspace,
  FirstRequirementRequest,
  FirstRequirementResponse,
  ProjectConversation,
  ProjectDetail,
  ProjectMessage,
  ProjectMessageDraftAsset,
  ProjectMessagePage,
  ProjectStage,
  ProjectStageHistory,
  ProjectSummary,
  ProjectWorkspaceProjection,
  SendProjectMessageRequest
} from './types'

const MESSAGE_PAGE_SIZE = 20

export function createFromFirstRequirement(
  payload: FirstRequirementRequest,
  idempotencyKey: string,
  signal?: AbortSignal
) {
  return apiRequest<FirstRequirementResponse>('/projects/from-first-requirement', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify(payload),
    signal
  })
}

export function listProjectSummaries(signal?: AbortSignal) {
  return apiRequest<ProjectSummary[]>('/projects', { signal })
}

export function getProjectDetail(projectId: number, signal?: AbortSignal) {
  return apiRequest<ProjectDetail>(`/projects/${projectId}`, { signal })
}

export function getProjectConversation(projectId: number, signal?: AbortSignal) {
  return apiRequest<ProjectConversation>(`/projects/${projectId}/conversation`, { signal })
}

export async function getProjectMessagePage(
  projectId: number,
  cursor?: number | null,
  signal?: AbortSignal
): Promise<ProjectMessagePage> {
  const query = new URLSearchParams({ size: String(MESSAGE_PAGE_SIZE) })
  if (cursor != null) query.set('beforeMessageId', String(cursor))
  return apiRequest<ProjectMessagePage>(
    `/projects/${projectId}/conversation/messages?${query.toString()}`,
    { signal }
  )
}

export function uploadProjectMessageDraft(projectId: number, file: File, signal?: AbortSignal) {
  const form = new FormData()
  form.append('file', file)
  return apiRequest<ProjectMessageDraftAsset>(`/projects/${projectId}/conversation/message-drafts`, {
    method: 'POST',
    body: form,
    signal
  })
}

export function sendProjectMessage(
  projectId: number,
  payload: SendProjectMessageRequest,
  idempotencyKey: string,
  signal?: AbortSignal
) {
  return apiRequest<ProjectMessage>(`/projects/${projectId}/conversation/messages`, {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify(payload),
    signal
  })
}

export function listProjectStages(projectId: number, signal?: AbortSignal) {
  return apiRequest<ProjectStage[]>(`/projects/${projectId}/stages`, { signal })
}

export function listProjectStageHistory(projectId: number, stageInstanceId: number, signal?: AbortSignal) {
  return apiRequest<ProjectStageHistory[]>(`/projects/${projectId}/stages/${stageInstanceId}/events`, { signal })
}

export function getCurrentProjectStage(projectId: number, signal?: AbortSignal) {
  return apiRequest<CurrentProjectStageWorkspace>(`/projects/${projectId}/stages/current`, { signal })
}

export async function getProjectWorkspace(projectId: number, signal?: AbortSignal): Promise<ProjectWorkspaceProjection> {
  const [project, conversation, stages, current] = await Promise.all([
    getProjectDetail(projectId, signal),
    getProjectConversation(projectId, signal),
    listProjectStages(projectId, signal),
    getCurrentProjectStage(projectId, signal)
  ])
  return {
    project,
    conversation,
    stages,
    materials: [],
    allowedActions: current.allowedActions.map(toAllowedAction)
  }
}

function toAllowedAction(code: string) {
  const labels: Record<string, { label: string; description: string }> = {
    CONFIRM_REQUIREMENT: { label: '确认需求版本', description: '确认当前需求版本后，项目将按已确认内容继续。' },
    SIGN_CONTRACT: { label: '查看签署事项', description: '进入当前合同签署事项。' },
    CONFIRM_REPORT: { label: '确认调研报告', description: '确认当前已发布的调研报告版本。' },
    CONFIRM_DESIGN: { label: '确认设计版本', description: '确认当前已审核并发布的设计版本。' },
    RECEIVE_DELIVERY: { label: '确认接收交付', description: '确认接收当前交付版本。' }
  }
  return { code, ...(labels[code] || { label: '查看当前事项', description: '查看当前阶段可办理的事项。' }), enabled: true }
}
