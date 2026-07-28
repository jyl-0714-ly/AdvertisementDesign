<template>
  <div class="workbench" v-loading="loading">
    <section class="desktop-access-note">
      <span>CREATIVE WORKSPACE</span>
      <strong>请使用桌面端访问工作台</strong>
      <p>项目沟通、设计稿确认与阶段推进需要更宽的工作区域。</p>
    </section>
    <aside class="conversation-panel">
      <div class="conversation-title"><div><span>客户协作</span><strong>客户会话</strong></div><el-icon><ChatDotRound /></el-icon></div>
      <button v-for="item in conversations" :key="item.id" type="button" class="conversation-item" :class="{ active: item.id === activeConversationId }" @click="selectConversation(item.id)">
        <span class="conversation-avatar">{{ (item.projectName || '项目').slice(0, 1) }}</span>
        <span class="conversation-copy">
          <strong>{{ item.projectName }}</strong>
          <small class="conversation-stage"><b>当前</b>{{ conversationStage(item) }}</small>
          <small class="conversation-latest"><b>最新</b>{{ item.lastMessage || '暂无消息' }}</small>
        </span>
        <span v-if="item.unreadCount" class="unread-dot">{{ item.unreadCount }}</span>
      </button>
      <div v-if="!conversations.length && !loading" class="conversation-empty">暂无客户会话</div>
    </aside>

    <section v-if="activeProject" class="chat-panel">
      <div class="stage-flow-panel">
        <div class="stage-flow-heading">
          <span>项目协作流程</span>
          <div><small>每个关键节点均需双方确认</small><button type="button" class="status-drawer-trigger" @click="inspectorOpen = true"><el-icon><DataAnalysis /></el-icon>项目状态</button></div>
        </div>
        <div class="stage-flow" aria-label="项目业务流程">
          <button v-for="stage in stages" :key="stage.stageCode" type="button" :class="[stageState(stage), { selected: selectedStageCode === stage.stageCode }]" @click="selectedStageCode = stage.stageCode">
            <i><span>{{ stage.sortOrder }}</span></i><strong>{{ stage.stageName }}</strong><small>{{ stageText(stage) }}</small>
          </button>
        </div>
      </div>

      <div ref="messageContainer" class="message-list">
        <template v-for="message in messages" :key="message.id">
          <div v-if="message.senderRole === 'SYSTEM'" class="system-event-card">
            <span><el-icon><Bell /></el-icon></span>
            <div><strong>{{ systemEventTitle(message.content) }}</strong><p>{{ message.content }}</p></div>
            <time>{{ formatFullTime(message.createdAt) }}</time>
          </div>
          <div v-else class="chat-message" :class="{ mine: message.senderId === auth.user?.id }">
            <span class="message-avatar">{{ message.senderName.slice(0, 1) }}</span>
            <div><div class="message-meta">{{ message.senderName }} <time>{{ formatTime(message.createdAt) }}</time></div>
              <div class="message-bubble">
                <p v-if="message.content">{{ message.content }}</p>
                <a v-for="file in message.files" :key="file.id" class="message-file" :class="{ 'image-preview': file.mimeType?.startsWith('image/') }" :href="file.url || '#'" target="_blank" @click.prevent="downloadMessageFile(file.id, file.originalName)">
                  <img v-if="file.mimeType?.startsWith('image/') && file.url" :src="file.url" :alt="file.originalName" />
                  <span><el-icon><Paperclip /></el-icon>{{ file.originalName }}</span>
                </a>
              </div>
            </div>
          </div>
        </template>
      </div>

      <footer class="chat-composer">
        <div class="composer-tools">
          <button type="button" title="添加表情" @click="showEmoji = !showEmoji"><el-icon><Sunny /></el-icon></button>
          <button type="button" title="发送图片或文件" @click="fileInput?.click()"><el-icon><Paperclip /></el-icon></button>
          <button type="button" title="添加设计稿图片" @click="fileInput?.click()"><el-icon><Picture /></el-icon></button>
          <i class="composer-tool-divider"></i>
          <button type="button" class="composer-stage-tool" title="打开阶段操作" @click="openStageActions"><el-icon><Operation /></el-icon><span>阶段操作</span></button>
          <input ref="fileInput" type="file" hidden accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.zip" @change="selectFile" />
          <span v-if="pendingFile" class="pending-file">{{ pendingFile.name }} <button type="button" @click="pendingFile = null">移除</button></span>
        </div>
        <div v-if="showEmoji" class="emoji-strip"><button v-for="emoji in emojis" :key="emoji" type="button" @click="messageText += emoji">{{ emoji }}</button></div>
        <el-input v-model="messageText" type="textarea" :rows="3" resize="none" placeholder="输入消息，按 Ctrl + Enter 发送" @keydown.ctrl.enter.prevent="send" />
        <div class="composer-footer"><span>支持文字、图片、文件和表情</span><el-button type="primary" :loading="sending" @click="send">发送</el-button></div>
      </footer>
    </section>

    <button v-if="activeProject" type="button" class="inspector-backdrop" :class="{ visible: inspectorOpen }" aria-label="关闭项目状态" @click="inspectorOpen = false"></button>
    <aside v-if="activeProject" class="project-inspector" :class="{ open: inspectorOpen }">
      <header class="inspector-header"><div><span>PROJECT STATUS</span><h2>项目控制中心</h2></div><b>{{ activeProject.progress }}%</b><button type="button" class="inspector-close" aria-label="关闭项目状态" @click="inspectorOpen = false"><el-icon><Close /></el-icon></button></header>
      <div class="inspector-progress"><span :style="{ width: `${activeProject.progress}%` }"></span></div>
      <section class="project-facts">
        <div class="project-cover" aria-label="项目视觉封面">
          <span>AD CREATIVE STUDIO · CLIENT PROJECT</span>
          <strong>{{ activeProject.name.slice(0, 1) }}</strong>
          <small>NO.{{ String(activeProject.id).padStart(3, '0') }} / PROFESSIONAL DESIGN SERVICE</small>
        </div>
        <div class="studio-identity"><span class="studio-mark">AD</span><div><small>项目服务机构</small><strong>AD Creative Studio</strong><p>品牌策略与视觉设计团队</p></div><em>设计服务中</em></div>
        <div class="project-tags"><span>品牌设计</span><span>客户协作</span><span class="active-tag">{{ activeProject.currentStageName }}</span></div>
        <div><span>项目名称</span><strong>{{ activeProject.name }}</strong></div>
        <div><span>客户信息</span><strong>{{ activeProject.customerName || '—' }}</strong></div>
        <div class="project-owner"><b>{{ (activeProject.designerName || '待').slice(0, 1) }}</b><span><small>项目负责人</small><strong>{{ activeProject.designerName || '待分配' }}</strong></span><em>设计顾问</em></div>
        <div><span>项目周期</span><strong>{{ projectPeriod(activeProject) }}</strong></div>
        <div><span>合同金额</span><strong>以合同记录为准</strong></div>
        <div class="current-stage-card"><span>当前阶段</span><strong>{{ activeProject.currentStageName }}</strong></div>
        <button type="button" class="inspector-project-link" @click="router.push(`/projects/${activeProject.id}`)">查看完整项目记录 <el-icon><ArrowRight /></el-icon></button>
      </section>

      <section v-if="selectedStage" ref="stageActionPanel" class="inspector-stage-action" :class="stageState(selectedStage)">
        <header><span>当前节点操作</span><small>{{ stageText(selectedStage) }}</small></header>
        <strong>{{ selectedStage.stageName }}</strong>
        <p>{{ currentStageNotice }}</p>
        <el-button type="primary" :disabled="businessActionDisabled" @click="handleStage(selectedStage)">{{ businessActionLabel }}</el-button>
      </section>

      <section class="compact-timeline" aria-label="项目进度记录">
        <header><span>进度记录</span><small>{{ reachedStageCount }}/{{ stages.length }} 已达成</small></header>
        <button v-for="stage in stages" :key="stage.stageCode" type="button" :class="[stageState(stage), { selected: selectedStageCode === stage.stageCode }]" @click="selectedStageCode = stage.stageCode">
          <i></i><span><strong>{{ stage.stageName }}</strong><small>{{ stageText(stage) }}</small></span>
        </button>
      </section>

      <section class="project-team">
        <header>协作信息</header>
        <div><span>客户联系人</span><strong>{{ activeProject.customerName || '—' }}</strong></div>
        <div><span>项目编号</span><strong>#{{ String(activeProject.id).padStart(4, '0') }}</strong></div>
        <div><span>确认机制</span><strong>双方节点确认</strong></div>
      </section>
    </aside>
    <section v-else-if="!loading" class="chat-empty">选择一个项目会话开始沟通。</section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Bell, ChatDotRound, Close, DataAnalysis, Operation, Paperclip, Picture, Sunny } from '@element-plus/icons-vue'
import { confirmStageAction, createStageAction, downloadFile, getProject, listConversations, listMessages, listProjectStages, listProjectActions, markConversationRead, rejectStageAction, sendMessage, uploadConversationFile } from '@/api'
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
const selectedStageCode = ref('')
const messages = ref<MessageVO[]>([])
const messageText = ref('')
const pendingFile = ref<File | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const messageContainer = ref<HTMLElement | null>(null)
const stageActionPanel = ref<HTMLElement | null>(null)
const showEmoji = ref(false)
const inspectorOpen = ref(false)
const emojis = ['😀', '👍', '🎨', '✨', '📌', '✅']

const activeConversation = computed(() => conversations.value.find((item) => item.id === activeConversationId.value) || null)
const selectedStage = computed(() => stages.value.find((item) => item.stageCode === selectedStageCode.value) || stages.value[0] || null)
const selectedStageAction = computed(() => selectedStage.value ? latestAction(selectedStage.value.stageCode) : undefined)
const reachedStageCount = computed(() => stages.value.filter((item) => item.status === 'REACHED').length)
const currentStageNotice = computed(() => {
  const stage = selectedStage.value
  if (!stage) return '项目阶段加载中。'
  if (stage.status === 'REACHED') return '该节点已经由双方确认，相关沟通与文件记录已归档。'
  if (stage.status === 'REJECTED') return '该节点曾被驳回，可补充说明后重新发起确认。'
  if (stage.status === 'PENDING_CONFIRM') {
    return selectedStageAction.value?.confirmUserId === auth.user?.id
      ? '对方已提交节点成果，正在等待你确认或提出修改意见。'
      : '确认请求已发出，正在等待对方处理。'
  }
  return '完成该节点工作后发起确认，对方确认后项目进入下一阶段。'
})
const businessActionDisabled = computed(() => {
  const stage = selectedStage.value
  if (!stage || stage.status === 'REACHED') return true
  return stage.status === 'PENDING_CONFIRM' && selectedStageAction.value?.confirmUserId !== auth.user?.id
})
const businessActionLabel = computed(() => {
  const stage = selectedStage.value
  if (!stage) return '节点操作'
  if (stage.status === 'REACHED') return '节点已达成'
  if (stage.status === 'PENDING_CONFIRM') return selectedStageAction.value?.confirmUserId === auth.user?.id ? '处理确认' : '等待对方确认'
  if (stage.status === 'REJECTED') return '重新发起确认'
  return stage.stageCode === 'REQUIREMENT_GUIDE' ? '发送需求模板' : '发起节点确认'
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

function conversationStage(conversation: ConversationVO) {
  return conversation.id === activeConversationId.value && activeProject.value
    ? activeProject.value.currentStageName
    : '点击查看项目进度'
}

function stageTemplate(stage: ProjectStageVO) {
  if (stage.stageCode === 'REQUIREMENT_GUIDE') return '已发送需求模板，请补充品牌背景、目标用户、传播目标、参考风格与交付时间。'
  return `发起「${stage.stageName}」确认，请对方核对后确认或提出修改意见。`
}

async function openStageActions() {
  inspectorOpen.value = true
  await nextTick()
  stageActionPanel.value?.scrollIntoView({ behavior: 'smooth', block: 'center' })
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
  inspectorOpen.value = false
  activeConversationId.value = id
  const conversation = conversations.value.find((item) => item.id === id)
  if (!conversation) return
  loading.value = true
  try {
    const [project, projectStages, stageActions] = await Promise.all([getProject(conversation.projectId), listProjectStages(conversation.projectId), listProjectActions(conversation.projectId)])
    activeProject.value = project
    stages.value = projectStages
    actions.value = stageActions
    const focusStage = projectStages.find((item) => item.status === 'PENDING_CONFIRM') || projectStages.find((item) => item.stageCode === project.currentStage) || projectStages[0]
    selectedStageCode.value = focusStage?.stageCode || ''
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
        await confirmStageAction(action.id, { responseNote: '设计师已确认。' })
      } catch (decision) {
        if (decision === 'cancel') await rejectStageAction(action.id, { responseNote: '暂不确认，请补充或修改后重新提交。' })
        else return
      }
    } else {
      const note = stageTemplate(stage)
      await ElMessageBox.confirm(`将向对方发起「${stage.stageName}」确认。`, '发起阶段确认', { confirmButtonText: '发起确认', cancelButtonText: '取消', type: 'info' })
      if (stage.stageCode === 'REQUIREMENT_GUIDE' && activeConversation.value) {
        await sendMessage(activeConversation.value.id, { messageType: 'TEXT', content: note, fileIds: [] })
      }
      await createStageAction(activeProject.value.id, stage.stageCode, { requestNote: note })
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
    if (pendingFile.value) uploaded = await uploadConversationFile(activeConversation.value.id, pendingFile.value)
    const message = await sendMessage(activeConversation.value.id, {
      messageType: uploaded ? (uploaded.mimeType?.startsWith('image/') ? 'IMAGE' : 'FILE') : 'TEXT',
      content: messageText.value.trim() || null,
      fileIds: uploaded ? [uploaded.id] : [],
      clientMessageId: `designer-${Date.now()}`
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
function formatFullTime(value?: string | null) { return value ? value.replace('T', ' ').slice(0, 16) : '时间待记录' }
function systemEventTitle(content?: string | null) {
  const stage = content?.match(/阶段「([^」]+)」/)?.[1]
  if (!stage) return '项目协作动态'
  if (content?.includes('已达成')) return `阶段【${stage}】已完成`
  if (content?.includes('已驳回')) return `阶段【${stage}】需修改`
  return `阶段【${stage}】状态更新`
}
function projectPeriod(project: ProjectVO) { return `${project.createdAt.slice(0, 10)} 至今` }

onMounted(async () => {
  loading.value = true
  try { conversations.value = await listConversations(); if (conversations.value[0]) await selectConversation(conversations.value[0].id) }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '会话列表加载失败') }
  finally { loading.value = false }
})
</script>

<style>
/* =====================================================
   STYLIST 端客户沟通 — 对齐 USER 端工作台
   色彩系统：
     统一墨黑 #0f172a | 协作琥珀 #D97745 | 达成绿 #1a8a5c
   ===================================================== */

/* ---- 布局骨架 ---- */
.workbench {
  width: 100%;
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-columns: 264px minmax(0, 1fr) 308px;
  border: 0;
  border-radius: 0;
  box-shadow: none;
  background: #f7f8fa;
  position: relative;
  overflow: hidden;
}

.desktop-access-note { display: none; }

/* ---- 左侧会话列表 ---- */
.conversation-panel {
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  border-right: 1px solid #e4e7ec;
  background: #ffffff;
}

.conversation-title {
  min-height: 72px;
  padding: 18px 18px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 2;
  background: #ffffff;
  border-bottom: 1px solid #f0f2f5;
}
.conversation-title span {
  display: block;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}
.conversation-title strong {
  display: block;
  margin-top: 3px;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}
.conversation-title .el-icon {
  color: #94a3b8;
  font-size: 18px;
}

.conversation-item {
  width: calc(100% - 16px);
  min-height: 110px;
  margin: 0 8px 6px;
  padding: 14px 12px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  display: flex;
  align-items: flex-start;
  gap: 11px;
  text-align: left;
  cursor: pointer;
  position: relative;
  transition: background 160ms, border-color 160ms, box-shadow 160ms;
}
.conversation-item:hover {
  border-color: #e4e7ec;
  background: #f7f8fa;
}
.conversation-item.active {
  border-color: #e4e7ec;
  background: #f7f8fa;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
}

.conversation-avatar {
  width: 40px;
  height: 40px;
  margin-top: 1px;
  flex: none;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: #f1f5f9;
  color: #475569;
  font-size: 15px;
  font-weight: 700;
}
.conversation-item.active .conversation-avatar {
  background: #0f172a;
  color: #ffffff;
}

.conversation-copy {
  min-width: 0;
  display: grid;
  gap: 5px;
}
.conversation-copy > strong {
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.3;
  padding-right: 22px;
}
.conversation-copy small {
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 400;
}
.conversation-copy small b {
  flex: none;
  color: #D97745;
  font-size: 10px;
  font-weight: 600;
}
.conversation-stage { margin-top: 1px; }
.conversation-latest {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread-dot {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  display: grid;
  place-items: center;
  position: absolute;
  right: 12px;
  top: 14px;
  background: #D97745;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
}

.conversation-empty {
  padding: 56px 16px;
  color: #94a3b8;
  text-align: center;
  font-size: 13px;
}

/* ---- 中间聊天区 ---- */
.chat-panel {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #ffffff;
  border-right: 1px solid #e4e7ec;
}

/* 阶段流程条 */
.stage-flow-panel {
  height: 80px;
  min-height: 80px;
  padding: 10px 22px 9px;
  flex: none;
  border-bottom: 1px solid #e4e7ec;
  background: #ffffff;
  overflow: hidden;
}

.stage-flow-heading {
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.stage-flow-heading span {
  color: #475569;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}
.stage-flow-heading small { color: #94a3b8; font-size: 11px; }
.stage-flow-heading > div { display: flex; align-items: center; gap: 12px; }

.status-drawer-trigger {
  display: none;
  min-height: 32px;
  padding: 0 10px;
  border: 1px solid #e4e7ec;
  border-radius: 7px;
  align-items: center;
  gap: 5px;
  background: #f7f8fa;
  color: #475569;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
}

/* 阶段流程节点 */
.stage-flow {
  display: grid;
  grid-template-columns: repeat(7, minmax(78px, 1fr));
  min-width: 640px;
}
.stage-flow > button {
  min-width: 78px;
  padding: 0 4px;
  border: 0;
  background: transparent;
  display: grid;
  justify-items: center;
  grid-template-rows: 22px minmax(20px, auto) 14px;
  color: #94a3b8;
  cursor: pointer;
  position: relative;
}
.stage-flow > button::before {
  content: '';
  height: 1px;
  position: absolute;
  left: 0;
  right: 0;
  top: 10px;
  background: #e4e7ec;
}
.stage-flow > button:first-child::before { left: 50%; }
.stage-flow > button:last-child::before { right: 50%; }

.stage-flow > button i {
  width: 20px;
  height: 20px;
  border: 1.5px solid #e4e7ec;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: #ffffff;
  color: #94a3b8;
  font-style: normal;
  z-index: 1;
}
.stage-flow > button i span { font-size: 9px; font-weight: 700; }
.stage-flow > button strong {
  max-width: 86px;
  align-self: center;
  color: #64748b;
  font-size: 11px;
  font-weight: 500;
  line-height: 1.25;
  text-align: center;
}
.stage-flow > button small { font-size: 10px; color: #94a3b8; }
.stage-flow > button:hover strong,
.stage-flow > button.selected strong { color: #0f172a; font-weight: 600; }
.stage-flow > button.selected i { box-shadow: 0 0 0 3px #e9f0f7; }

.stage-flow > button.reached i {
  border-color: #1a8a5c;
  background: #1a8a5c;
  color: #ffffff;
}
.stage-flow > button.reached small { color: #1a8a5c; }
.stage-flow > button.pending i {
  border-color: #D97745;
  background: #FFF7F2;
  color: #D97745;
}
.stage-flow > button.pending small { color: #D97745; }
.stage-flow > button.rejected i {
  border-color: #9f3a3a;
  background: #fdf2f2;
  color: #9f3a3a;
}
.stage-flow > button.rejected small { color: #9f3a3a; }

/* 消息列表 */
.message-list {
  min-height: 0;
  padding: 24px 28px;
  flex: 1 1 auto;
  overflow-y: auto;
  overscroll-behavior: contain;
  display: grid;
  align-content: start;
  gap: 20px;
  background: #fafbfc;
  scrollbar-gutter: stable;
}

.system-event-card {
  width: min(620px, 88%);
  min-height: 28px;
  padding: 3px 0;
  justify-self: center;
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  border-top: 1px solid #e4e7ec;
  border-bottom: 1px solid #e4e7ec;
}
.system-event-card > span {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: transparent;
  color: #94a3b8;
  font-size: 12px;
}
.system-event-card > div { min-width: 0; }
.system-event-card strong {
  display: block;
  overflow: hidden;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.system-event-card p { display: none; }
.system-event-card time { color: #b0b8c6; font-size: 12px; font-weight: 400; }

.chat-message {
  max-width: min(72%, 600px);
  display: flex;
  align-items: flex-start;
  gap: 10px;
}
.chat-message.mine {
  justify-self: end;
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  flex: none;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: #f1f5f9;
  color: #475569;
  font-size: 14px;
  font-weight: 700;
}
.chat-message:not(.mine) .message-avatar {
  background: #0f172a;
  color: #ffffff;
}

.message-meta {
  margin-bottom: 6px;
  color: #64748b;
  font-size: 12px;
  font-weight: 500;
}
.message-meta time {
  margin-left: 6px;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 400;
}
.mine .message-meta { text-align: right; }

.message-bubble {
  padding: 11px 14px;
  border-radius: 4px 14px 14px;
  background: #1e293b;
  color: #f8fafc;
  font-size: 14px;
  line-height: 1.65;
  font-weight: 400;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
}
.mine .message-bubble {
  border: 1px solid #e4e7ec;
  border-radius: 14px 4px 14px 14px;
  background: #ffffff;
  color: #1e293b;
  box-shadow: none;
}
.message-bubble p { margin: 0; white-space: pre-wrap; }

.message-file {
  margin-top: 7px;
  padding: 9px 11px;
  border: 1px solid rgba(255,255,255,0.14);
  border-radius: 9px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: inherit;
  text-decoration: none;
}
.mine .message-file {
  border-color: #e4e7ec;
  background: #f7f8fa;
}
.message-file.image-preview { width: min(360px, 100%); display: block; }
.message-file.image-preview img {
  width: 100%;
  max-height: 260px;
  border-radius: 8px;
  display: block;
  object-fit: cover;
}

/* 输入框 */
.chat-composer {
  height: 138px;
  min-height: 138px;
  padding: 8px 22px 10px;
  flex: none;
  display: flex;
  flex-direction: column;
  border-top: 1px solid #e4e7ec;
  background: #ffffff;
  position: relative;
}

.composer-tools {
  min-height: 28px;
  flex: none;
  display: flex;
  align-items: center;
  gap: 2px;
}
.composer-tools > button {
  width: 30px;
  height: 28px;
  border: 0;
  border-radius: 7px;
  display: grid;
  place-items: center;
  color: #64748b;
  background: transparent;
  cursor: pointer;
  transition: background 140ms, color 140ms;
}
.composer-tools > button:hover { background: #f1f5f9; color: #0f172a; }
.composer-tools .el-icon { font-size: 18px; }
.composer-tool-divider {
  width: 1px;
  height: 16px;
  margin: 0 4px;
  background: #e4e7ec;
}
.composer-stage-tool {
  width: auto !important;
  padding: 0 9px !important;
  gap: 5px;
  display: inline-flex !important;
}
.composer-stage-tool span { color: inherit; font-size: 12px; font-weight: 500; }

.pending-file {
  margin-left: 6px;
  padding: 3px 8px;
  border-radius: 6px;
  background: #FFF7F2;
  color: #D97745;
  font-size: 11px;
  font-weight: 500;
}
.pending-file button {
  margin-left: 5px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #9f3a3a;
  cursor: pointer;
  font-size: 11px;
}

.emoji-strip {
  padding: 7px 10px;
  border: 1px solid #e4e7ec;
  border-radius: 10px;
  display: flex;
  gap: 4px;
  position: absolute;
  left: 20px;
  bottom: 134px;
  z-index: 3;
  background: #ffffff;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.09);
}
.emoji-strip button {
  padding: 3px;
  border: 0;
  background: transparent;
  cursor: pointer;
  font-size: 17px;
}

.chat-composer .el-textarea {
  height: 66px;
  flex: none;
}
.chat-composer .el-textarea__inner {
  height: 66px !important;
  min-height: 66px !important;
  padding: 9px 12px;
  border: 1px solid #e4e7ec !important;
  border-radius: 10px !important;
  background: #f7f8fa !important;
  color: #1e293b !important;
  font-size: 14px;
  line-height: 1.5;
  box-shadow: none !important;
  overflow-y: auto;
  resize: none;
}
.chat-composer .el-textarea__inner:focus {
  border-color: #b0bac8 !important;
}
.chat-composer .el-textarea__inner::placeholder { color: #94a3b8 !important; }

.composer-footer {
  min-height: 30px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.composer-footer > span { color: #94a3b8; font-size: 12px; }
.composer-footer .el-button {
  min-width: 72px;
  height: 34px;
  border-radius: 8px !important;
  background: #0f172a !important;
  border-color: #0f172a !important;
  color: #ffffff !important;
  font-size: 14px !important;
  font-weight: 500 !important;
}
.composer-footer .el-button:hover {
  background: #1e293b !important;
  border-color: #1e293b !important;
}

/* ---- 右侧项目检视面板 ---- */
.project-inspector {
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-bottom: 20px;
  background: #f7f8fa;
  border-left: 1px solid #e4e7ec;
}

.inspector-header {
  min-height: 80px;
  padding: 18px 18px 12px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  position: relative;
}
.inspector-header span {
  color: #94a3b8;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}
.inspector-header h2 {
  margin: 3px 0 0;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}
.inspector-header > b { color: #0f172a; font-size: 26px; font-weight: 700; }

.inspector-progress {
  height: 4px;
  margin: 0 18px 14px;
  overflow: hidden;
  border-radius: 2px;
  background: #e4e7ec;
}
.inspector-progress span {
  height: 100%;
  display: block;
  background: #D97745;
  border-radius: 2px;
  transition: width 400ms ease;
}

.inspector-close { display: none; }
.inspector-backdrop { display: none; }

.project-facts,
.inspector-stage-action,
.compact-timeline,
.project-team {
  margin: 0 12px 12px;
  padding: 14px;
  border: 1px solid #e4e7ec;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
}

.project-cover {
  height: 108px;
  padding: 14px !important;
  overflow: hidden;
  display: block !important;
  border-radius: 10px;
  background: #0f172a;
  color: #ffffff;
  position: relative;
}
.project-cover::before {
  content: '';
  width: 100px;
  height: 68px;
  border: 1px solid rgba(255,255,255,0.18);
  position: absolute;
  right: -14px;
  top: 14px;
  transform: rotate(-8deg);
}
.project-cover::after {
  content: '';
  width: 46px;
  height: 4px;
  position: absolute;
  right: 14px;
  bottom: 12px;
  background: #D97745;
  border-radius: 2px;
}
.project-cover span {
  color: #94a3b8;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}
.project-cover strong {
  display: block;
  margin-top: 8px;
  color: #ffffff;
  font-size: 40px;
  font-weight: 700;
  line-height: 1;
}
.project-cover small {
  position: absolute;
  left: 14px;
  bottom: 12px;
  color: #64748b;
  font-size: 10px;
  font-weight: 500;
}

.project-tags {
  min-height: 0 !important;
  padding: 8px 0 4px !important;
  display: flex !important;
  flex-wrap: wrap;
  align-items: center;
  gap: 5px !important;
}
.project-tags span {
  padding: 3px 8px;
  border: 1px solid #e4e7ec;
  border-radius: 999px;
  background: #f7f8fa;
  color: #64748b;
  font-size: 11px;
  font-weight: 500;
}

.project-facts > div {
  padding: 6px 0;
  min-height: 34px;
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  align-items: center;
  gap: 8px;
}
.project-facts > div span { color: #94a3b8; font-size: 12px; }
.project-facts > div strong {
  overflow: hidden;
  color: #334155;
  font-size: 12px;
  font-weight: 500;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-owner {
  min-height: 56px !important;
  margin: 6px 0 !important;
  padding: 10px !important;
  display: flex !important;
  align-items: center;
  gap: 10px !important;
  border-radius: 9px;
  background: #f7f8fa !important;
}
.project-owner > b {
  width: 34px;
  height: 34px;
  flex: none;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: #0f172a;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
}
.project-owner > span { min-width: 0; flex: 1; display: grid; gap: 2px; }
.project-owner small { color: #94a3b8; font-size: 11px; }
.project-owner strong {
  overflow: hidden;
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.project-owner em {
  flex: none;
  color: #D97745;
  font-size: 11px;
  font-style: normal;
  font-weight: 600;
}

.current-stage-card {
  min-height: 50px !important;
  margin-top: 6px;
  padding: 9px 11px !important;
  border-left: 3px solid #D97745 !important;
  border-radius: 0 8px 8px 0 !important;
  display: grid !important;
  align-content: center;
  background: #FFF7F2 !important;
}
.current-stage-card span { color: #B95F31; font-size: 11px; }
.current-stage-card strong {
  color: #8F4325;
  font-size: 14px;
  font-weight: 600;
  text-align: left;
}

.inspector-project-link {
  width: 100%;
  min-height: 36px;
  margin-top: 10px;
  padding: 0 11px;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f7f8fa;
  color: #334155;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: border-color 160ms, color 160ms;
}
.inspector-project-link:hover { border-color: #D97745; color: #D97745; }

/* 阶段操作卡 */
.inspector-stage-action header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.inspector-stage-action header span {
  color: #475569;
  font-size: 12px;
  font-weight: 600;
}
.inspector-stage-action header small {
  color: #D97745;
  font-size: 12px;
  font-weight: 500;
}
.inspector-stage-action > strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 15px;
  font-weight: 600;
}
.inspector-stage-action p {
  margin: 5px 0 12px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.55;
}
.inspector-stage-action .el-button {
  width: 100%;
  height: 36px;
  margin: 0;
  border-radius: 8px !important;
  font-size: 13px !important;
  font-weight: 500 !important;
}
.inspector-stage-action .el-button--primary {
  background: #0f172a !important;
  border-color: #0f172a !important;
}
.inspector-stage-action.reached header small { color: #1a8a5c; }
.inspector-stage-action.pending header small { color: #D97745; }
.inspector-stage-action.rejected header small { color: #9f3a3a; }

/* 进度时间线 */
.compact-timeline { display: block; }
.compact-timeline > header {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.compact-timeline > header span {
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
}
.compact-timeline > header small { color: #94a3b8; font-size: 12px; }

.compact-timeline > button {
  width: 100%;
  min-height: 46px;
  padding: 5px 6px 5px 0;
  border: 0;
  background: transparent;
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  align-items: center;
  text-align: left;
  cursor: pointer;
  position: relative;
}
.compact-timeline > button::before {
  content: '';
  width: 1px;
  position: absolute;
  left: 6px;
  top: 26px;
  bottom: -16px;
  background: #e4e7ec;
}
.compact-timeline > button:last-child::before { display: none; }
.compact-timeline > button > i {
  width: 13px;
  height: 13px;
  border: 2.5px solid #f7f8fa;
  border-radius: 50%;
  background: #cbd5e1;
  box-shadow: 0 0 0 1px #cbd5e1;
  z-index: 1;
}
.compact-timeline > button > span {
  min-width: 0;
  padding: 7px 8px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  transition: background 140ms;
}
.compact-timeline > button:hover > span,
.compact-timeline > button.selected > span { background: #f1f5f9; }
.compact-timeline > button strong {
  overflow: hidden;
  color: #334155;
  font-size: 13px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.compact-timeline > button small {
  flex: none;
  color: #94a3b8;
  font-size: 12px;
}
.compact-timeline > button.reached > i { background: #1a8a5c; box-shadow: 0 0 0 1px #1a8a5c; }
.compact-timeline > button.pending > i { background: #D97745; box-shadow: 0 0 0 1px #D97745; }
.compact-timeline > button.rejected > i { background: #9f3a3a; box-shadow: 0 0 0 1px #9f3a3a; }
.compact-timeline > button.reached small { color: #1a8a5c; }
.compact-timeline > button.pending small { color: #D97745; }
.compact-timeline > button.rejected small { color: #9f3a3a; }

/* 协作信息 */
.project-team { margin-top: 0; }
.project-team header {
  margin-bottom: 8px;
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
}
.project-team > div {
  min-height: 36px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.project-team span { color: #94a3b8; font-size: 12px; }
.project-team strong {
  overflow: hidden;
  color: #334155;
  font-size: 12px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 商业 SaaS 精修层：保持功能布局，统一视觉语法 ===== */
.workbench {
  font-family: 'Inter', 'PingFang SC', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background: #f7f8fa;
}

.conversation-panel,
.chat-panel { border-color: rgba(15, 23, 42, 0.07); }

.conversation-title {
  min-height: 78px;
  padding: 20px 20px 16px;
  border-bottom: 0;
}
.conversation-title strong,
.inspector-header h2 { font-size: 20px; font-weight: 600; letter-spacing: -0.02em; }

.conversation-item {
  min-height: 92px;
  margin: 4px 10px 8px;
  padding: 15px 14px;
  border-color: transparent;
  border-radius: 12px;
}
.conversation-item:hover {
  border-color: rgba(15, 23, 42, 0.07);
  background: #f8f9fb;
}
.conversation-item.active {
  border-color: rgba(217, 119, 69, 0.18);
  background: #fffaf7;
  box-shadow: 0 5px 18px rgba(15, 23, 42, 0.055);
}
.conversation-item.active::before {
  content: '';
  width: 3px;
  border-radius: 3px;
  position: absolute;
  left: -1px;
  top: 18px;
  bottom: 18px;
  background: #D97745;
}
.conversation-avatar { border-radius: 12px; }
.conversation-copy { gap: 7px; }
.conversation-copy > strong { font-size: 15px; font-weight: 600; letter-spacing: -0.01em; }
.conversation-latest { display: none !important; }
.conversation-copy small { font-size: 12px; }
.conversation-copy small b { color: #D97745; }
.unread-dot { background: #D97745; }

.stage-flow-panel {
  height: 74px;
  min-height: 74px;
  padding: 11px 28px 8px;
  border-color: rgba(15, 23, 42, 0.06);
}
.stage-flow-heading { margin-bottom: 5px; }
.stage-flow-heading span { font-size: 12px; font-weight: 600; letter-spacing: 0; text-transform: none; }
.stage-flow > button { grid-template-rows: 18px minmax(18px, auto) 12px; }
.stage-flow > button::before { top: 8px; background: #edf0f3; }
.stage-flow > button i { width: 17px; height: 17px; }
.stage-flow > button i span { font-size: 8px; }
.stage-flow > button strong { font-size: 11px; }
.stage-flow > button small { font-size: 10px; }
.stage-flow > button.pending i { border-color: #D97745; background: #fff7f2; color: #D97745; }
.stage-flow > button.pending small { color: #D97745; }
.stage-flow > button.selected i { box-shadow: 0 0 0 3px rgba(217, 119, 69, 0.1); }

.message-list { padding: 30px 34px; gap: 24px; background: #fafbfc; }
.system-event-card { border: 0; color: #94a3b8; }
.chat-message { max-width: min(74%, 640px); }
.message-avatar { border-radius: 12px; }
.message-meta { font-size: 12px; }
.message-bubble {
  padding: 14px 16px;
  border-radius: 5px 12px 12px 12px;
  font-size: 14px;
  line-height: 1.65;
  box-shadow: 0 5px 16px rgba(15, 23, 42, 0.07);
}
.mine .message-bubble {
  border-color: rgba(15, 23, 42, 0.08);
  border-radius: 12px 5px 12px 12px;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.035);
}
.message-file { border-radius: 12px; }

.chat-composer { height: 148px; min-height: 148px; padding: 10px 26px 12px; border-color: rgba(15, 23, 42, 0.07); }
.chat-composer .el-textarea__inner { border-color: rgba(15, 23, 42, 0.09) !important; border-radius: 12px !important; background: #f8f9fb !important; }
.chat-composer .el-textarea__inner:focus { border-color: rgba(217, 119, 69, 0.55) !important; box-shadow: 0 0 0 3px rgba(217, 119, 69, 0.08) !important; }
.composer-footer .el-button,
.inspector-stage-action .el-button--primary {
  border-color: #D97745 !important;
  background: #D97745 !important;
  box-shadow: 0 4px 12px rgba(217, 119, 69, 0.18);
}
.composer-footer .el-button:hover,
.inspector-stage-action .el-button--primary:hover { border-color: #c86537 !important; background: #c86537 !important; }

.project-inspector { border-left: 0; background: #f6f7f9; }
.inspector-header { min-height: 88px; padding: 20px 20px 14px; }
.inspector-header > b { font-size: 24px; font-weight: 600; letter-spacing: -0.03em; }
.inspector-progress { margin: 0 20px 18px; background: #e8ebef; }
.inspector-progress span { background: #D97745; }
.project-facts,
.inspector-stage-action,
.compact-timeline,
.project-team {
  margin: 0 14px 14px;
  padding: 16px;
  border-color: rgba(15, 23, 42, 0.075);
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.035);
}
.project-cover {
  height: 126px;
  padding: 17px !important;
  border-radius: 12px;
  background: linear-gradient(145deg, #111827 0%, #1f2937 100%);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.14);
}
.project-cover::after { right: 17px; bottom: 15px; background: rgba(255, 255, 255, 0.72); }
.project-cover strong { margin-top: 12px; font-size: 42px; font-weight: 600; }
.project-cover small { left: 17px; bottom: 15px; font-size: 9px; letter-spacing: 0.04em; }

.studio-identity {
  min-height: 70px !important;
  margin: 10px 0 4px;
  padding: 11px 0 !important;
  display: flex !important;
  align-items: center;
  gap: 10px !important;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}
.studio-mark {
  width: 38px;
  height: 38px;
  flex: none;
  border-radius: 11px;
  display: grid;
  place-items: center;
  background: #111827;
  color: #fff !important;
  font-size: 11px !important;
  font-weight: 700;
  letter-spacing: .04em;
}
.studio-identity > div { min-width: 0; flex: 1; display: grid; gap: 1px; }
.studio-identity small { color: #94a3b8; font-size: 10px; }
.studio-identity strong { color: #111827; font-size: 13px; font-weight: 600; text-align: left; }
.studio-identity p { margin: 0; color: #94a3b8; font-size: 10px; }
.studio-identity em { flex: none; color: #D97745; font-size: 10px; font-style: normal; font-weight: 600; }

.project-tags { padding: 8px 0 8px !important; }
.project-tags span { border-color: rgba(15, 23, 42, 0.07); border-radius: 999px; background: #f8f9fb; }
.project-tags .active-tag { border-color: rgba(217, 119, 69, 0.2); background: #fff5ef; color: #D97745; }
.project-owner { border-radius: 12px; background: #f8f9fb !important; }
.project-owner > b { background: #111827; }
.project-owner em { color: #64748b; }
.current-stage-card { border-left-color: #D97745 !important; border-radius: 0 12px 12px 0 !important; background: #fff7f2 !important; }
.inspector-project-link { border-color: rgba(15, 23, 42, 0.08); border-radius: 12px; background: #f8f9fb; }
.inspector-project-link:hover { border-color: #D97745; color: #D97745; }

.compact-timeline > button { min-height: 40px; }
.compact-timeline > button > i { width: 11px; height: 11px; }
.compact-timeline > button > span { padding: 6px 8px; border-radius: 8px; }
.compact-timeline > button.pending > i { background: #D97745; box-shadow: 0 0 0 1px #D97745; }
.compact-timeline > button.pending small { color: #D97745; }

@media (prefers-reduced-motion: reduce) {
  .workbench *, .workbench *::before, .workbench *::after { scroll-behavior: auto !important; transition-duration: 0.01ms !important; }
}

/* 空状态 */
.chat-empty {
  display: grid;
  place-items: center;
  color: #94a3b8;
  background: #fafbfc;
  grid-column: span 2;
}

/* ---- 响应式 ---- */
@media (max-width: 1439px) {
  .workbench { grid-template-columns: 220px minmax(0, 1fr) 268px; }
  .message-list { padding-left: 22px; padding-right: 22px; }
  .project-facts, .inspector-stage-action, .compact-timeline, .project-team {
    margin-left: 9px;
    margin-right: 9px;
    padding-left: 12px;
    padding-right: 12px;
  }
}

@media (max-width: 1199px) {
  .workbench { grid-template-columns: 220px minmax(0, 1fr); }
  .status-drawer-trigger { display: inline-flex; }
  .project-inspector {
    width: 308px;
    max-width: calc(100vw - 40px);
    position: fixed;
    top: 58px;
    right: 0;
    bottom: 0;
    z-index: 45;
    border-left: 1px solid #e4e7ec;
    box-shadow: -20px 0 50px rgba(15, 23, 42, 0.12);
    transform: translateX(105%);
    transition: transform 220ms ease;
  }
  .project-inspector.open { transform: translateX(0); }
  .inspector-close {
    width: 32px;
    height: 32px;
    padding: 0;
    border: 1px solid #e4e7ec;
    border-radius: 8px;
    display: grid;
    place-items: center;
    position: absolute;
    right: 14px;
    top: 16px;
    background: #ffffff;
    color: #475569;
    cursor: pointer;
  }
  .inspector-header > b { margin-right: 40px; }
  .inspector-backdrop {
    padding: 0;
    border: 0;
    display: block;
    position: fixed;
    inset: 58px 0 0;
    z-index: 44;
    background: rgba(15, 23, 42, 0.2);
    opacity: 0;
    pointer-events: none;
    transition: opacity 220ms ease;
  }
  .inspector-backdrop.visible { opacity: 1; pointer-events: auto; }
}

@media (max-width: 899px) {
  .workbench {
    display: grid;
    grid-template-columns: 1fr;
    background: #ffffff;
  }
  .workbench > :not(.desktop-access-note) { display: none !important; }
  .desktop-access-note {
    padding: 40px 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    text-align: center;
  }
  .desktop-access-note span {
    color: #D97745;
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.06em;
    text-transform: uppercase;
  }
  .desktop-access-note strong {
    margin-top: 12px;
    color: #0f172a;
    font-size: 26px;
    font-weight: 700;
  }
  .desktop-access-note p {
    max-width: 420px;
    margin: 10px 0 0;
    color: #64748b;
    font-size: 15px;
    line-height: 1.6;
  }
}
</style>
