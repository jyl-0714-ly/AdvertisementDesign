export type DraftUploadStatus = 'UPLOADING' | 'UPLOADED' | 'FAILED'

export interface FileAssetDraft {
  id: number
  originalName: string
  mimeType: string | null
  fileSize: number
  status: string
}

export interface RequirementDraftAttachment {
  localId: string
  file: File
  status: DraftUploadStatus
  assetId: number | null
  error: string | null
}
