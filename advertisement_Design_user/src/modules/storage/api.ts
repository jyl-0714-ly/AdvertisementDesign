import { apiRequest } from '@/modules/http'
import type { FileAssetDraft } from './types'

export function uploadFirstRequirementDraft(file: File, signal?: AbortSignal) {
  const form = new FormData()
  form.append('file', file)
  return apiRequest<FileAssetDraft>('/projects/first-requirement-drafts', {
    method: 'POST',
    body: form,
    signal
  })
}
