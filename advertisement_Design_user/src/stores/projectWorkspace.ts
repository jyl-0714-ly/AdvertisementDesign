import { computed, reactive, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  getProjectMessagePage,
  getProjectWorkspace,
  listProjectStageHistory,
  sendProjectMessage,
  uploadProjectMessageDraft
} from '@/modules/project/api'
import type {
  ProjectDraftAttachment,
  ProjectMessage,
  ProjectWorkspaceProjection
} from '@/modules/project/types'

export interface ProjectWorkspaceState {
  projection: ProjectWorkspaceProjection | null
  messages: ProjectMessage[]
  nextBeforeMessageId: number | null
  hasMore: boolean
  loading: boolean
  loadingOlder: boolean
  sending: boolean
  error: string | null
  messageError: string | null
  composerText: string
  attachments: ProjectDraftAttachment[]
  clientMessageId: string
  idempotencyKey: string
}

function newIntent() {
  return { clientMessageId: crypto.randomUUID(), idempotencyKey: crypto.randomUUID() }
}

function createState(): ProjectWorkspaceState {
  const intent = newIntent()
  return {
    projection: null,
    messages: [],
    nextBeforeMessageId: null,
    hasMore: true,
    loading: false,
    loadingOlder: false,
    sending: false,
    error: null,
    messageError: null,
    composerText: '',
    attachments: [],
    ...intent
  }
}

function messageTime(message: ProjectMessage) {
  const parsed = Date.parse(message.sentAt)
  return Number.isNaN(parsed) ? message.id : parsed
}

export function mergeMessagesChronologically(current: ProjectMessage[], incoming: ProjectMessage[]) {
  const deduped = new Map<number, ProjectMessage>()
  current.forEach(item => deduped.set(item.id, item))
  incoming.forEach(item => deduped.set(item.id, item))
  return [...deduped.values()].sort((left, right) => messageTime(left) - messageTime(right) || left.id - right.id)
}

export const useProjectWorkspaceStore = defineStore('project-workspace', () => {
  const entries = reactive<Record<number, ProjectWorkspaceState>>({})
  const projectControllers = new Map<number, AbortController>()
  const pageControllers = new Map<number, AbortController>()
  const sendControllers = new Map<number, AbortController>()
  const uploadControllers = new Map<string, AbortController>()
  const activeProjectId = ref<number | null>(null)

  const active = computed(() => activeProjectId.value == null ? null : entries[activeProjectId.value] || null)

  function ensure(projectId: number) {
    return entries[projectId] ||= createState()
  }

  function expected(projectId: number, conversationId?: number) {
    if (activeProjectId.value !== projectId) return false
    if (conversationId == null) return true
    return entries[projectId]?.projection?.conversation.id === conversationId
  }

  async function openProject(projectId: number) {
    activeProjectId.value = projectId
    projectControllers.forEach((controller, id) => { if (id !== projectId) controller.abort() })
    pageControllers.forEach((controller, id) => { if (id !== projectId) controller.abort() })
    sendControllers.forEach((controller, id) => { if (id !== projectId) controller.abort() })
    uploadControllers.forEach((controller, key) => { if (!key.startsWith(`${projectId}:`)) controller.abort() })
    const state = ensure(projectId)
    projectControllers.get(projectId)?.abort()
    pageControllers.get(projectId)?.abort()
    const controller = new AbortController()
    projectControllers.set(projectId, controller)
    state.loading = true
    state.error = null
    try {
      const projection = await getProjectWorkspace(projectId, controller.signal)
      if (controller.signal.aborted || !expected(projectId) || projection.project.id !== projectId || projection.conversation.projectId !== projectId) return
      state.projection = {
        ...projection,
        stages: [...projection.stages].sort((left, right) => left.sortOrder - right.sortOrder),
        materials: projection.materials || [],
        allowedActions: projection.allowedActions || []
      }
      if (!state.messages.length) await loadMessages(projectId, true)
    } catch (error) {
      if ((error as DOMException)?.name !== 'AbortError' && expected(projectId)) {
        state.error = error instanceof Error ? error.message : '项目工作台暂时无法打开，请稍后重试。'
      }
    } finally {
      if (projectControllers.get(projectId) === controller) state.loading = false
    }
  }

  async function loadMessages(projectId: number, initial = false) {
    const state = ensure(projectId)
    const conversationId = state.projection?.conversation.id
    if (!conversationId || state.loadingOlder || (!initial && !state.hasMore)) return
    pageControllers.get(projectId)?.abort()
    const controller = new AbortController()
    pageControllers.set(projectId, controller)
    state.loadingOlder = true
    state.messageError = null
    try {
      const page = await getProjectMessagePage(projectId, initial ? null : state.nextBeforeMessageId, controller.signal)
      if (controller.signal.aborted || !expected(projectId, conversationId)) return
      if (page.items.some(item => item.conversationId !== conversationId)) return
      state.messages = mergeMessagesChronologically(initial ? [] : state.messages, page.items)
      state.nextBeforeMessageId = page.nextBeforeMessageId
      state.hasMore = page.hasMore && page.nextBeforeMessageId != null
    } catch (error) {
      if ((error as DOMException)?.name !== 'AbortError' && expected(projectId, conversationId)) {
        state.messageError = error instanceof Error ? error.message : '消息暂时没有加载成功，请重试。'
      }
    } finally {
      if (pageControllers.get(projectId) === controller) state.loadingOlder = false
    }
  }

  async function loadStageHistory(projectId: number, stageId: number) {
    const state = ensure(projectId)
    const stage = state.projection?.stages.find(item => item.id === stageId)
    if (!stage || stage.histories) return
    const conversationId = state.projection?.conversation.id
    const controller = projectControllers.get(projectId)
    try {
      const histories = await listProjectStageHistory(projectId, stageId, controller?.signal)
      if (!expected(projectId, conversationId)) return
      const target = state.projection?.stages.find(item => item.id === stageId)
      if (target) target.histories = [...histories].sort((a, b) => Date.parse(b.occurredAt) - Date.parse(a.occurredAt))
    } catch (error) {
      if ((error as DOMException)?.name !== 'AbortError' && expected(projectId, conversationId)) {
        state.error = error instanceof Error ? error.message : '阶段记录暂时无法读取。'
      }
    }
  }

  function addFiles(projectId: number, files: File[]) {
    const state = ensure(projectId)
    files.slice(0, Math.max(0, 10 - state.attachments.length)).forEach(file => {
      const draft: ProjectDraftAttachment = {
        localId: crypto.randomUUID(), file, status: 'UPLOADING', assetId: null,
        originalName: file.name, fileSize: file.size, error: null
      }
      state.attachments.push(draft)
      void uploadAttachment(projectId, draft.localId)
    })
  }

  async function uploadAttachment(projectId: number, localId: string) {
    const state = ensure(projectId)
    const draft = state.attachments.find(item => item.localId === localId)
    const conversationId = state.projection?.conversation.id
    if (!draft || !conversationId) return
    const uploadKey = `${projectId}:${localId}`
    uploadControllers.get(uploadKey)?.abort()
    const controller = new AbortController()
    uploadControllers.set(uploadKey, controller)
    draft.status = 'UPLOADING'
    draft.error = null
    try {
      const asset = await uploadProjectMessageDraft(projectId, draft.file, controller.signal)
      if (controller.signal.aborted || !expected(projectId, conversationId) || !state.attachments.some(item => item.localId === localId)) return
      draft.assetId = asset.id
      draft.originalName = asset.name || draft.file.name
      draft.fileSize = asset.size ?? draft.file.size
      draft.status = 'UPLOADED'
    } catch (error) {
      if (controller.signal.aborted || !expected(projectId, conversationId)) return
      draft.status = 'FAILED'
      draft.error = error instanceof Error ? error.message : '上传失败，请重试。'
    } finally {
      if (uploadControllers.get(uploadKey) === controller) uploadControllers.delete(uploadKey)
    }
  }

  function removeAttachment(projectId: number, localId: string) {
    uploadControllers.get(`${projectId}:${localId}`)?.abort()
    uploadControllers.delete(`${projectId}:${localId}`)
    const state = ensure(projectId)
    state.attachments = state.attachments.filter(item => item.localId !== localId)
  }

  async function send(projectId: number) {
    const state = ensure(projectId)
    const conversationId = state.projection?.conversation.id
    const content = state.composerText.trim()
    const uploadedIds = state.attachments.flatMap(item => item.status === 'UPLOADED' && item.assetId != null ? [item.assetId] : [])
    if (!conversationId || state.sending || (!content && !uploadedIds.length)) return false
    if (state.attachments.some(item => item.status !== 'UPLOADED')) {
      state.messageError = '请先重试或移除未上传成功的附件。'
      return false
    }
    sendControllers.get(projectId)?.abort()
    const controller = new AbortController()
    sendControllers.set(projectId, controller)
    state.sending = true
    state.messageError = null
    try {
      const message = await sendProjectMessage(projectId, {
        content,
        clientMessageId: state.clientMessageId,
        fileAssetIds: uploadedIds
      }, state.idempotencyKey, controller.signal)
      if (controller.signal.aborted || !expected(projectId, conversationId) || message.conversationId !== conversationId) return false
      state.messages = mergeMessagesChronologically(state.messages, [message])
      state.composerText = ''
      state.attachments = []
      Object.assign(state, newIntent())
      return true
    } catch (error) {
      if ((error as DOMException)?.name !== 'AbortError' && expected(projectId, conversationId)) {
        state.messageError = error instanceof Error ? error.message : '消息没有发送成功，请重试。'
      }
      return false
    } finally {
      if (sendControllers.get(projectId) === controller) state.sending = false
    }
  }

  function close() {
    activeProjectId.value = null
    projectControllers.forEach(controller => controller.abort())
    pageControllers.forEach(controller => controller.abort())
    sendControllers.forEach(controller => controller.abort())
    uploadControllers.forEach(controller => controller.abort())
  }

  return {
    entries, activeProjectId, active, ensure, openProject, loadMessages, loadStageHistory,
    addFiles, uploadAttachment, removeAttachment, send, close
  }
})
