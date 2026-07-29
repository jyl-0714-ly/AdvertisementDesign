import { apiRequest } from '@/modules/http'
import type { OrganizationContext } from './types'

export function listMyOrganizations(signal?: AbortSignal) {
  return apiRequest<OrganizationContext[]>('/users/me/organizations', { signal })
}
