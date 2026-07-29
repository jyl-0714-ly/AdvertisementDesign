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
  confirmedRequirementVersionId?: number | null
  startedAt: string | null
  pausedAt?: string | null
  completedAt?: string | null
  terminatedAt?: string | null
  createdAt?: string | null
  updatedAt: string | null
  version: number
}

export interface ProjectConversation {
  id: number
  projectId: number
  status: string
  lastMessageId: number | null
  lastMessagePreview: string | null
  lastMessageAt: string | null
  version: number
}

export interface MessageAttachment {
  id: number
  fileAssetId: number
  displayOrder: number
  createdAt: string | null
  name: string
  mimeType: string | null
  size: number | null
  downloadPath: string | null
}

export interface ProjectMessage {
  id: number
  conversationId: number
  messageType: string
  content: string | null
  displayIdentity: string
  replyToMessageId: number | null
  correctionMessageId: number | null
  attachments: MessageAttachment[]
  sentAt: string
}

export interface ProjectMessagePage {
  items: ProjectMessage[]
  nextBeforeMessageId: number | null
  hasMore: boolean
}

export interface SendProjectMessageRequest {
  content: string
  clientMessageId: string
  fileAssetIds: number[]
  correctionMessageId?: number | null
}

export interface ManualProjectRenameRequest {
  name: string
  version: number
}

export interface RestoreAutomaticNamingRequest {
  version: number
}

export interface ProjectStageHistory {
  id: number
  eventType: string
  fromStatus: string | null
  toStatus: string | null
  relatedObjectType: string | null
  relatedObjectId: number | null
  relatedObjectVersion: number | null
  reason: string | null
  occurredAt: string
}

export interface ProjectStage {
  id: number
  projectId: number
  stageCode: string
  stageName: string
  sortOrder: number
  status: string
  activationCount: number
  activatedAt: string | null
  completedAt: string | null
  version: number
  histories?: ProjectStageHistory[]
}

export interface ProjectMaterial {
  id: number
  name: string
  category: string
  version: number | null
  status: string | null
  updatedAt: string | null
  downloadUrl?: string | null
}

export interface ProjectAllowedAction {
  code: string
  label: string
  description?: string | null
  targetId?: number | null
  targetVersion?: number | null
  enabled?: boolean
}

export interface ProjectWorkspaceProjection {
  project: ProjectDetail
  conversation: ProjectConversation
  stages: ProjectStage[]
  materials: ProjectMaterial[]
  allowedActions: ProjectAllowedAction[]
}

export type ProjectDraftUploadStatus = 'UPLOADING' | 'UPLOADED' | 'FAILED'

export interface ProjectDraftAttachment {
  localId: string
  file: File
  status: ProjectDraftUploadStatus
  assetId: number | null
  originalName: string
  fileSize: number
  error: string | null
}

export interface ProjectMessageDraftAsset {
  id: number
  name: string
  mimeType: string | null
  size: number
  downloadPath: string | null
}

export interface CurrentProjectStageWorkspace {
  stage: ProjectStage
  allowedActions: string[]
  versions: unknown[]
  materials: unknown[]
}
