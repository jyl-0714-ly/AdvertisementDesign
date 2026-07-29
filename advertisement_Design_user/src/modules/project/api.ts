import { apiRequest } from '@/modules/http'
import type { FirstRequirementRequest, FirstRequirementResponse, ProjectDetail, ProjectSummary } from './types'

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
