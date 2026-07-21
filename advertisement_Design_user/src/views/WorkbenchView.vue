<template>
  <div class="workbench" v-loading="loading">
    <aside class="conversation-panel">
      <div class="conversation-title"><div><span>协作沟通</span><strong>项目会话</strong></div><el-icon><ChatDotRound /></el-icon></div>
      <button v-for="item in conversations" :key="item.id" type="button" class="conversation-item" :class="{ active: item.id === activeConversationId }" @click="selectConversation(item.id)">
        <span class="conversation-avatar">{{ (item.projectName || '项目').slice(0, 1) }}</span>
        <span class="conversation-copy"><strong>{{ item.projectName }}</strong><small>{{ item.lastMessage || '暂无消息' }}</small></span>
        <span v-if="item.unreadCount" class="unread-dot">{{ item.unreadCount }}</span>
      </button>
      <div v-if="!conversations.length && !loading" class="conversation-empty">暂无可用会话</div>
    </aside>

    <section v-if="activeProject" class="chat-panel">
      <header class="chat-header">
        <div><h1>{{ activeProject.name }}</h1><p>{{ activeProject.currentStageName }} <span>{{ activeProject.progress }}%</span></p></div>
        <button type="button" class="project-link" @click="router.push(`/projects/${activeProject.id}`)">查看项目 <el-icon><ArrowRight /></el-icon></button>
      </header>

      <div class="stage-toolbar" aria-label="项目业务阶段">
        <button v-for="stage in stages" :key="stage.stageCode" type="button" class="stage-button" :class="stageState(stage)" @click="handleStage(stage)">
          <span>{{ stage.sortOrder }}</span><strong>{{ stage.stageName }}</strong><small>{{ stageText(stage) }}</small>
        </button>
      </div>

      <div class="stage-notice">
        <el-icon><InfoFilled /></el-icon>
        <span>{{ currentStageNotice }}</span>
      </div>

      <div ref="messageContainer" class="message-list">
        <template v-for="message in messages" :key="message.id">
          <div v-if="message.senderRole === 'SYSTEM'" class="system-message">{{ message.content }}</div>
          <div v-else class="chat-message" :class="{ mine: message.senderId === auth.user?.id }">
            <span class="message-avatar">{{ message.senderName.slice(0, 1) }}</span>
            <div><div class="message-meta">{{ message.senderName }} <time>{{ formatTime(message.createdAt) }}</time></div>
              <div class="message-bubble">
                <p v-if="message.content">{{ message.content }}</p>
                <a v-for="file in message.files" :key="file.id" class="message-file" :href="file.url || '#'" target="_blank" @click.prevent="downloadMessageFile(file.id, file.originalName)"><el-icon><Paperclip /></el-icon>{{ file.originalName }}</a>
              </div>
            </div>
          </div>
        </template>
      </div>

      <footer class="chat-composer">
        <div class="composer-tools">
          <button type="button" title="添加表情" @click="showEmoji = !showEmoji"><el-icon><Sunny /></el-icon></button>
          <button type="button" title="发送图片或文件" @click="fileInput?.click()"><el-icon><Paperclip /></el-icon></button>
          <input ref="fileInput" type="file" hidden accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.zip" @change="selectFile" />
          <span v-if="pendingFile" class="pending-file">{{ pendingFile.name }} <button type="button" @click="pendingFile = null">移除</button></span>
        </div>
        <div v-if="showEmoji" class="emoji-strip"><button v-for="emoji in emojis" :key="emoji" type="button" @click="messageText += emoji">{{ emoji }}</button></div>
        <el-input v-model="messageText" type="textarea" :autosize="{ minRows: 3, maxRows: 5 }" placeholder="输入消息，按 Ctrl + Enter 发送" @keydown.ctrl.enter.prevent="send" />
        <div class="composer-footer"><span>支持文字、图片、文件和表情</span><el-button type="primary" :loading="sending" @click="send">发送</el-button></div>
      </footer>
    </section>
    <section v-else-if="!loading" class="chat-empty">选择一个项目会话开始沟通。</section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, ChatDotRound, InfoFilled, Paperclip, Sunny } from '@element-plus/icons-vue'
import { confirmStageAction, createStageAction, downloadFile, getProject, listConversations, listMessages, listProjectStages, listStageActions, markConversationRead, rejectStageAction, sendMessage, uploadFile } from '@/api'
import type { ConversationVO, FileAssetVO, MessageVO, ProjectStageVO, ProjectVO, StageActionVO } from '@/models'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const sending = ref(false)
const conversations = ref<ConversationVO[]>([])
const activeConversationId = ref<number | null>(null)
const activeProject = ref<ProjectVO | null>(null)
const stages = ref<ProjectStageVO[]>([])
const actions = ref<StageActionVO[]>([])
const messages = ref<MessageVO[]>([])
const messageText = ref('')
const pendingFile = ref<File | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const messageContainer = ref<HTMLElement | null>(null)
const showEmoji = ref(false)
const emojis = ['😀', '👍', '🎨', '✨', '📌', '✅']

const activeConversation = computed(() => conversations.value.find((item) => item.id === activeConversationId.value) || null)
const currentStageNotice = computed(() => {
  const pending = stages.value.find((item) => item.status === 'PENDING_CONFIRM')
  return pending ? `阶段「${pending.stageName}」正在等待对方确认，确认后项目将进入下一步。` : '通过顶部阶段按钮发起确认，所有关键节点均会留下系统记录。'
})

function latestAction(stageCode: string) {
  return actions.value.filter((item) => item.stageCode === stageCode).sort((a, b) => b.id - a.id)[0]
}

function stageText(stage: ProjectStageVO) {
  if (stage.status === 'REACHED') return '已达成'
  if (stage.status === 'PENDING_CONFIRM') return '等待确认'
  if (stage.status === 'REJECTED') return '已驳回'
  return '待发起'
}

function stageState(stage: ProjectStageVO) {
  return { reached: stage.status === 'REACHED', pending: stage.status === 'PENDING_CONFIRM', rejected: stage.status === 'REJECTED' }
}

function stageTemplate(stage: ProjectStageVO) {
  if (stage.stageCode === 'REQUIREMENT_GUIDE') return '已发送需求模板，请补充品牌背景、目标用户、传播目标、参考风格与交付时间。'
  return `发起「${stage.stageName}」确认，请对方核对后确认或提出修改意见。`
}

async function reloadMessages() {
  if (!activeConversation.value) return
  const page = await listMessages(activeConversation.value.id, { size: 100 })
  messages.value = page.records
  const latest = messages.value.at(-1)
  if (latest) await markConversationRead(activeConversation.value.id, { lastReadMessageId: latest.id })
  await nextTick()
  messageContainer.value?.scrollTo({ top: messageContainer.value.scrollHeight })
}

async function selectConversation(id: number) {
  activeConversationId.value = id
  const conversation = conversations.value.find((item) => item.id === id)
  if (!conversation) return
  loading.value = true
  try {
    const [project, projectStages, stageActions] = await Promise.all([getProject(conversation.projectId), listProjectStages(conversation.projectId), listStageActions(conversation.projectId)])
    activeProject.value = project
    stages.value = projectStages
    actions.value = stageActions
    await reloadMessages()
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '会话加载失败') }
  finally { loading.value = false }
}

async function handleStage(stage: ProjectStageVO) {
  if (!activeProject.value) return
  const action = latestAction(stage.stageCode)
  if (stage.status === 'REACHED') return
  try {
    if (stage.status === 'PENDING_CONFIRM' && action) {
      if (action.confirmUserId !== auth.user?.id) { ElMessage.info('已发起确认，正在等待对方确认。'); return }
      try {
        await ElMessageBox.confirm(`确认达成「${stage.stageName}」吗？`, '阶段确认', { confirmButtonText: '确认达成', cancelButtonText: '驳回', distinguishCancelAndClose: true, type: 'info' })
        await confirmStageAction(action.id, '已确认。')
      } catch (decision) {
        if (decision === 'cancel') await rejectStageAction(action.id, '暂不确认，请补充或修改后重新提交。')
        else return
      }
    } else {
      const note = stageTemplate(stage)
      await ElMessageBox.confirm(`将向对方发起「${stage.stageName}」确认。`, '发起阶段确认', { confirmButtonText: '发起确认', cancelButtonText: '取消', type: 'info' })
      if (stage.stageCode === 'REQUIREMENT_GUIDE' && activeConversation.value) {
        await sendMessage(activeConversation.value.id, { messageType: 'TEXT', content: note, fileIds: [] })
      }
      await createStageAction(activeProject.value.id, stage.stageCode, note)
      ElMessage.success('已发起，正在等待对方确认。')
    }
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    if (error === 'cancel') return
    ElMessage.error(error instanceof Error ? error.message : '操作未完成')
  }
  await selectConversation(activeConversationId.value || 0)
}

async function selectFile(event: Event) {
  pendingFile.value = (event.target as HTMLInputElement).files?.[0] || null
  if (fileInput.value) fileInput.value.value = ''
}

async function send() {
  if (!activeConversation.value || (!messageText.value.trim() && !pendingFile.value)) return
  sending.value = true
  try {
    let uploaded: FileAssetVO | null = null
    if (pendingFile.value) uploaded = await uploadFile(pendingFile.value)
    const message = await sendMessage(activeConversation.value.id, {
      messageType: uploaded ? (uploaded.mimeType?.startsWith('image/') ? 'IMAGE' : 'FILE') : 'TEXT',
      content: messageText.value.trim() || null,
      fileIds: uploaded ? [uploaded.id] : [],
      clientMessageId: `customer-${Date.now()}`
    })
    messages.value.push(message)
    messageText.value = ''
    pendingFile.value = null
    await nextTick()
    messageContainer.value?.scrollTo({ top: messageContainer.value.scrollHeight, behavior: 'smooth' })
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '消息发送失败') }
  finally { sending.value = false }
}

async function downloadMessageFile(id: number, name: string) {
  try {
    const blob = await downloadFile(id)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url; link.download = name; link.click(); URL.revokeObjectURL(url)
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '下载失败') }
}

function formatTime(value?: string | null) { return value ? value.replace('T', ' ').slice(5, 16) : '' }

onMounted(async () => {
  loading.value = true
  try { conversations.value = await listConversations(); if (conversations.value[0]) await selectConversation(conversations.value[0].id) }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '会话列表加载失败') }
  finally { loading.value = false }
})
</script>

<style>
.workbench { min-height: calc(100vh - 138px); display: grid; grid-template-columns: 285px minmax(0, 1fr); overflow: hidden; border: 1px solid #dfe7f1; background: #fff; }.conversation-panel { border-right: 1px solid #e5ebf2; background: #fafbfd; }.conversation-title { padding: 22px 18px 16px; display: flex; align-items: center; justify-content: space-between; }.conversation-title span, .conversation-title strong { display: block; }.conversation-title span { color: #718096; font-size: 12px; }.conversation-title strong { margin-top: 3px; font-size: 18px; }.conversation-title .el-icon { color: #1367d1; font-size: 20px; }.conversation-item { width: 100%; min-height: 78px; padding: 13px 15px; border: 0; border-top: 1px solid #edf1f6; background: transparent; display: flex; align-items: center; gap: 10px; text-align: left; cursor: pointer; position: relative; }.conversation-item.active { background: #edf6ff; }.conversation-avatar, .message-avatar { width: 36px; height: 36px; flex: none; border-radius: 8px; display: grid; place-items: center; background: #dcecff; color: #1367d1; font-size: 14px; font-weight: 800; }.conversation-copy { min-width: 0; display: grid; gap: 5px; }.conversation-copy strong { color: #263448; font-size: 13px; }.conversation-copy small { overflow: hidden; color: #7c899b; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }.unread-dot { min-width: 18px; height: 18px; padding: 0 5px; border-radius: 9px; display: grid; place-items: center; position: absolute; right: 14px; top: 15px; background: #1367d1; color: #fff; font-size: 10px; }.conversation-empty { padding: 50px 10px; color: #94a3b8; text-align: center; font-size: 13px; }.chat-panel { min-width: 0; display: grid; grid-template-rows: auto auto auto minmax(300px, 1fr) auto; }.chat-header { padding: 19px 25px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #e8edf3; }.chat-header h1 { margin: 0; font-size: 18px; }.chat-header p { margin: 6px 0 0; color: #7b899c; font-size: 12px; }.chat-header p span { color: #1367d1; margin-left: 5px; }.project-link { padding: 0; border: 0; background: transparent; display: inline-flex; align-items: center; gap: 4px; color: #1367d1; cursor: pointer; font-size: 13px; }.stage-toolbar { padding: 13px 18px; display: grid; grid-template-columns: repeat(7, minmax(96px, 1fr)); gap: 7px; overflow-x: auto; border-bottom: 1px solid #e8edf3; }.stage-button { min-height: 62px; padding: 8px; border: 1px solid #dce6f2; background: #fff; display: grid; grid-template-columns: 20px 1fr; column-gap: 5px; text-align: left; cursor: pointer; }.stage-button > span { width: 18px; height: 18px; grid-row: span 2; border-radius: 50%; display: grid; place-items: center; background: #edf3fa; color: #69809c; font-size: 10px; font-weight: 700; }.stage-button strong { color: #36465d; font-size: 12px; line-height: 1.25; }.stage-button small { color: #8a99ab; font-size: 10px; }.stage-button.reached { background: #f4fbf7; border-color: #cbe8d6; }.stage-button.reached > span { background: #d9f2e2; color: #167247; }.stage-button.pending { background: #fff9ed; border-color: #f3d999; }.stage-button.pending > span { background: #ffe4a4; color: #9a6800; }.stage-button.rejected { border-color: #f2c3c3; background: #fff7f7; }.stage-notice { padding: 9px 25px; display: flex; align-items: center; gap: 7px; background: #f5f9fe; color: #60738c; font-size: 12px; }.stage-notice .el-icon { color: #1367d1; }.message-list { min-height: 0; padding: 23px 25px; overflow: auto; background: #fff; display: grid; align-content: start; gap: 15px; }.system-message { justify-self: center; padding: 5px 10px; border-radius: 4px; background: #f1f4f7; color: #7c8998; font-size: 11px; }.chat-message { max-width: min(75%, 550px); display: flex; align-items: flex-start; gap: 8px; }.chat-message.mine { justify-self: end; flex-direction: row-reverse; }.message-avatar { width: 30px; height: 30px; border-radius: 7px; font-size: 12px; }.message-meta { margin-bottom: 5px; color: #68798f; font-size: 11px; }.message-meta time { margin-left: 6px; color: #a1adbb; }.mine .message-meta { text-align: right; }.message-bubble { padding: 10px 12px; border-radius: 3px 12px 12px; background: #f2f5f8; color: #334155; font-size: 13px; line-height: 1.65; }.mine .message-bubble { border-radius: 12px 3px 12px 12px; background: #dceeff; color: #1c4f82; }.message-bubble p { margin: 0; white-space: pre-wrap; }.message-file { margin-top: 5px; padding: 7px 8px; display: flex; align-items: center; gap: 5px; background: rgba(255,255,255,.55); color: inherit; font-size: 12px; }.chat-composer { padding: 10px 18px 13px; border-top: 1px solid #e8edf3; }.composer-tools { min-height: 26px; display: flex; align-items: center; gap: 3px; }.composer-tools > button { width: 26px; height: 26px; border: 0; display: grid; place-items: center; color: #73849a; background: transparent; cursor: pointer; }.composer-tools .el-icon { font-size: 17px; }.pending-file { margin-left: 6px; padding: 3px 6px; background: #edf4fc; color: #4d7096; font-size: 11px; }.pending-file button { margin-left: 4px; padding: 0; border: 0; background: transparent; color: #c15f5f; cursor: pointer; font-size: 11px; }.emoji-strip { padding: 6px 0; display: flex; gap: 4px; }.emoji-strip button { padding: 2px; border: 0; background: transparent; cursor: pointer; font-size: 17px; }.chat-composer .el-textarea__inner { padding: 8px 0; box-shadow: none; resize: none; font-size: 13px; }.composer-footer { display: flex; align-items: center; justify-content: space-between; }.composer-footer span { color: #9aa7b6; font-size: 11px; }.composer-footer .el-button { min-width: 64px; }.chat-empty { display: grid; place-items: center; color: #8a99aa; border: 1px solid #dfe7f1; background: #fff; }
@media (max-width: 900px) { .workbench { grid-template-columns: 1fr; }.conversation-panel { max-height: 185px; overflow: auto; border-right: 0; border-bottom: 1px solid #e5ebf2; }.conversation-title { padding: 12px 15px; }.conversation-item { min-height: 60px; }.chat-panel { min-height: calc(100vh - 250px); }.stage-toolbar { grid-template-columns: repeat(7, 115px); }.chat-message { max-width: 88%; }.message-list { padding: 17px; }.chat-header, .stage-notice { padding-left: 17px; padding-right: 17px; } }
</style>
