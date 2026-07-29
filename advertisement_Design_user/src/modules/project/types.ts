export type FirstRequirementStatus = 'INVALID_REQUIREMENT' | 'PROJECT_CREATED' | 'IDEMPOTENT_REPLAY'

export interface FirstRequirementRequest {
  organizationId: number
  content: string
  clientMessageId: string
  fileAssetIds: number[]
}

export interface FirstRequirementResponse {
  status: FirstRequirementStatus
  projectId: number | null
  conversationId: number | null
  projectName: string | null
  currentStage: string | null
  guidance: string | null
}

export interface ProjectSummary {
  id: number
  name: string
  status: string
  startedAt: string | null
  updatedAt: string | null
}

export interface ProjectDetail {
  id: number
  organizationId: number
  name: string
  nameSource: 'AUTO' | 'MANUAL'
  description: string | null
  status: string
  startedAt: string | null
  updatedAt: string | null
  version: number
}
