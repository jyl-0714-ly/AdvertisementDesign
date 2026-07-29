import { apiRequest } from '@/modules/http'
import type {
  CurrentProjectStageWorkspace,
  FirstRequirementRequest,
  FirstRequirementResponse,
  ManualProjectRenameRequest,
  ProjectConversation,
  ProjectDetail,
  ProjectMessage,
  ProjectMessageDraftAsset,
  ProjectMessagePage,
  ProjectStage,
  ProjectStageHistory,
  ProjectSummary,
  ProjectWorkspaceProjection,
  RestoreAutomaticNamingRequest,
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

export function renameProjectManually(projectId: number, payload: ManualProjectRenameRequest) {
  return apiRequest<ProjectDetail>(`/projects/${projectId}/name/manual`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function restoreAutomaticProjectNaming(projectId: number, payload: RestoreAutomaticNamingRequest) {
  return apiRequest<ProjectDetail>(`/projects/${projectId}/name/restore-auto`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
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
    START_PROCESSING: { label: '开始处理', description: '项目服务团队开始处理当前阶段事项。' },
    WAIT_FOR_CUSTOMER: { label: '等待客户反馈', description: '当前阶段等待您补充或确认信息。' },
    REQUEST_REVIEW: { label: '进入审核', description: '将当前阶段事项提交审核。' },
    COMPLETE: { label: '完成当前阶段', description: '按当前已确认的业务事实完成本阶段。' },
    SUSPEND: { label: '暂停当前阶段', description: '暂停当前阶段，恢复后可继续推进。' },
    RESUME: { label: '恢复当前阶段', description: '恢复已暂停的当前阶段。' },
    REOPEN: { label: '重新开启阶段', description: '保留历史记录并开始新的处理轮次。' }
  }
  return { code, ...(labels[code] || { label: '查看当前事项', description: '查看当前阶段可办理的事项。' }), enabled: true }
}
