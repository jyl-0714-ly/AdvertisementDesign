export type UserRole = 'CUSTOMER' | 'DESIGNER'
export type ProjectStatus = 'IN_PROGRESS' | 'COMPLETED' | 'PAUSED' | 'CANCELLED'
export type PortfolioStatus = 'DRAFT' | 'PUBLISHED' | 'OFFLINE'
export type ProjectStageStatus = 'TODO' | 'PENDING_CONFIRM' | 'REACHED' | 'REJECTED'
export type MessageSenderRole = 'CUSTOMER' | 'DESIGNER' | 'SYSTEM'
export type MessageType = 'TEXT' | 'IMAGE' | 'FILE' | 'EMOJI' | 'SYSTEM'
export type FileStatus = 'ACTIVE' | 'DELETED'
export type FileRole = 'MATERIAL' | 'REPORT' | 'DRAFT' | 'FINAL' | 'CONTRACT' | 'DELIVERABLE' | 'OTHER'
export type StageActionStatus = 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'CANCELLED'

export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
  pages: number
}

export interface UserVO {
  id: number
  email: string
  nickname: string
  role: UserRole
  avatar?: string | null
  phone?: string | null
}

export interface LoginResponse {
  token: string
  user: UserVO
}

export interface UpdateUserRequest {
  nickname?: string | null
  avatar?: string | null
  phone?: string | null
}

export interface PortfolioCaseRequest {
  title: string
  industry: string
  style: string
  serviceType: string
  coverUrl: string
  imageUrls?: string[]
  description: string
  sortOrder?: number | null
  status?: PortfolioStatus | null
}

export interface PortfolioCaseVO extends PortfolioCaseRequest {
  id: number
  createdAt: string
  updatedAt: string
}

export interface ProjectVO {
  id: number
  name: string
  description?: string | null
  customerId: number
  customerName?: string | null
  designerId: number
  designerName?: string | null
  currentStage: string
  currentStageName: string
  status: ProjectStatus
  progress: number
  createdAt: string
  updatedAt: string
}

export interface ProjectStageVO {
  id: number
  projectId: number
  stageCode: string
  stageName: string
  sortOrder: number
  status: ProjectStageStatus
  reachedAt?: string | null
  updatedAt?: string | null
}

export interface ConversationVO {
  id: number
  projectId: number
  projectName?: string | null
  customerId: number
  customerName?: string | null
  designerId: number
  designerName?: string | null
  lastMessage?: string | null
  lastMessageAt?: string | null
  unreadCount: number
}

export interface MessageVO {
  id: number
  conversationId: number
  senderId?: number | null
  senderRole: MessageSenderRole
  senderName: string
  messageType: MessageType
  content?: string | null
  files: FileAssetVO[]
  replyToMessageId?: number | null
  isDeleted?: boolean | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface MessageCursorPage {
  records: MessageVO[]
  hasMore: boolean
}

export interface MarkReadRequest {
  lastReadMessageId: number
}

export interface ConversationReadStateVO {
  conversationId: number
  userId: number
  lastReadMessageId?: number | null
  lastReadAt?: string | null
  unreadCount: number
}

export interface FileAssetVO {
  id: number
  originalName: string
  storageName?: string | null
  storageProvider?: string | null
  bucketName?: string | null
  objectKey?: string | null
  url?: string | null
  mimeType?: string | null
  fileSize?: number | null
  fileHash?: string | null
  status?: FileStatus | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface ProjectFileVO {
  id: number
  projectId: number
  projectStageId?: number | null
  stageCode?: string | null
  fileId: number
  uploaderId: number
  fileRole: FileRole
  description?: string | null
  file?: FileAssetVO | null
  createdAt?: string | null
}

export interface CreateProjectFileRequest {
  fileId: number
  projectStageId?: number | null
  stageCode?: string | null
  fileRole?: FileRole | null
  description?: string | null
}

export interface SendMessageRequest {
  messageType: MessageType
  content?: string | null
  fileIds?: number[] | null
  clientMessageId?: string | null
}
