<template>
  <div class="panel-grid">
    <PageSection :title="project?.name || '项目详情'" subtitle="阶段动作、消息、文件和日志">
      <div v-if="project" class="grid-2">
        <div class="card-item">
          <div class="card-meta">
            <strong>{{ project.name }}</strong>
            <span class="badge primary">{{ project.status }}</span>
          </div>
          <div class="muted" style="margin-top: 8px">{{ project.description || '暂无说明' }}</div>
          <div class="muted" style="margin-top: 8px">当前阶段：{{ project.currentStageName }}</div>
          <div class="muted">进度：{{ project.progress }}%</div>
        </div>
        <div class="card-item">
          <div class="section-head" style="margin-bottom: 8px">
            <h4>项目成员</h4>
          </div>
          <div class="muted">客户：{{ project.customerName || '—' }}</div>
          <div class="muted">设计师：{{ project.designerName || '—' }}</div>
        </div>
      </div>
    </PageSection>

    <PageSection title="阶段进度" subtitle="项目阶段总览">
      <div class="timeline">
        <div v-for="stage in stages" :key="stage.id" class="timeline-item">
          <div class="timeline-row">
            <div>
              <strong>{{ stage.stageName }}</strong>
              <div class="muted">{{ stage.stageCode }}</div>
            </div>
            <span class="badge" :class="stageClass(stage.status)">{{ stage.status }}</span>
          </div>
          <div class="muted">更新时间：{{ stage.updatedAt || stage.reachedAt || '—' }}</div>
        </div>
      </div>
    </PageSection>

    <div class="grid-2">
      <PageSection title="阶段动作" subtitle="发起确认和处理结果">
        <div class="surface pad" style="margin-bottom: 16px">
          <div class="form-grid">
            <el-input v-model="actionForm.stageCode" placeholder="阶段编码" />
            <el-input v-model="actionForm.requestNote" placeholder="请求说明" />
          </div>
          <div class="table-actions" style="margin-top: 12px">
            <el-button type="primary" :loading="creatingAction" @click="createActionSubmit">发起确认</el-button>
            <el-button @click="reload">刷新</el-button>
          </div>
        </div>
        <div class="card-list">
          <div v-for="action in actions" :key="action.id" class="card-item">
            <div class="card-meta">
              <strong>{{ action.stageCode }}</strong>
              <span class="badge" :class="actionClass(action.status)">{{ action.status }}</span>
            </div>
            <div class="muted" style="margin-top: 8px">{{ action.requestNote || '暂无说明' }}</div>
            <div class="table-actions" style="margin-top: 8px">
              <el-button size="small" @click="confirm(action.id)">确认</el-button>
              <el-button size="small" type="danger" plain @click="reject(action.id)">驳回</el-button>
            </div>
          </div>
        </div>
      </PageSection>

      <PageSection title="项目文件" subtitle="上传、归档和下载">
        <div class="surface pad" style="margin-bottom: 16px">
          <el-upload :auto-upload="false" :show-file-list="false" @change="selectFile">
            <el-button>选择文件</el-button>
          </el-upload>
          <div class="muted" style="margin-top: 8px">{{ selectedFile?.name || '未选择文件' }}</div>
          <div class="table-actions" style="margin-top: 12px">
            <el-button type="primary" :loading="uploading" @click="uploadSelectedFile">上传文件</el-button>
          </div>
        </div>
        <div class="surface pad" style="margin-bottom: 16px">
          <div class="form-grid">
            <el-input v-model.number="archiveForm.fileId" placeholder="文件 ID" />
            <el-input v-model.number="archiveForm.projectStageId" placeholder="阶段 ID" />
            <el-input v-model="archiveForm.stageCode" placeholder="阶段编码" />
            <el-select v-model="archiveForm.fileRole" placeholder="文件角色">
              <el-option label="DELIVERABLE" value="DELIVERABLE" />
              <el-option label="REPORT" value="REPORT" />
              <el-option label="DRAFT" value="DRAFT" />
              <el-option label="MATERIAL" value="MATERIAL" />
              <el-option label="FINAL" value="FINAL" />
              <el-option label="CONTRACT" value="CONTRACT" />
              <el-option label="OTHER" value="OTHER" />
            </el-select>
            <el-input v-model="archiveForm.description" placeholder="文件说明" />
          </div>
          <div class="table-actions" style="margin-top: 12px">
            <el-button type="primary" :loading="archiving" @click="archiveSelectedFile">归档</el-button>
          </div>
        </div>
        <div class="card-list">
          <div v-for="file in files" :key="file.id" class="card-item">
            <div class="card-meta">
              <strong>{{ file.file?.originalName || file.description || '文件' }}</strong>
              <span class="badge">{{ file.fileRole }}</span>
            </div>
            <div class="muted" style="margin-top: 8px">{{ file.stageCode || '—' }}</div>
            <div class="table-actions" style="margin-top: 8px">
              <el-button size="small" @click="download(file.file?.id || file.fileId, file.file?.originalName || 'file.bin')">下载</el-button>
              <el-button size="small" type="danger" plain @click="removeFile(file.id)">删除</el-button>
            </div>
          </div>
        </div>
      </PageSection>
    </div>

    <div class="grid-2">
      <PageSection title="项目消息" subtitle="与客户沟通">
        <div class="messages">
          <div v-for="message in messages" :key="message.id" class="message" :class="{ system: message.senderRole === 'SYSTEM' }">
            <div class="message-head">
              <strong>{{ message.senderName }}</strong>
              <span class="muted">{{ message.createdAt || '—' }}</span>
            </div>
            <div>{{ message.content || message.messageType }}</div>
          </div>
        </div>
        <div class="surface pad" style="margin-top: 16px">
          <el-input v-model="messageText" type="textarea" :rows="3" placeholder="输入消息内容" />
          <div class="table-actions" style="margin-top: 12px">
            <el-button type="primary" :loading="sending" @click="send">发送消息</el-button>
          </div>
        </div>
      </PageSection>

      <PageSection title="操作日志" subtitle="项目过程记录">
        <div class="card-list">
          <div v-for="log in logs" :key="log.id" class="card-item">
            <div class="card-meta">
              <strong>{{ log.action }}</strong>
              <span class="badge">{{ log.bizType }}</span>
            </div>
            <div class="muted" style="margin-top: 8px">{{ log.description }}</div>
            <div class="muted">{{ log.createdAt || '—' }}</div>
          </div>
        </div>
      </PageSection>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageSection from '@/components/PageSection.vue'
import {
  archiveProjectFile,
  confirmStageAction,
  createStageAction,
  deleteProjectFile,
  downloadFile,
  getProject,
  listConversations,
  listMessages,
  listOperationLogs,
  listProjectActions,
  listProjectFiles,
  listProjectStages,
  rejectStageAction,
  sendMessage,
  uploadFile
} from '@/api'
import type { ConversationVO, FileRole, MessageVO, OperationLogVO, ProjectFileVO, ProjectStageVO, ProjectVO, StageActionVO } from '@/models'

const route = useRoute()
const project = ref<ProjectVO | null>(null)
const stages = ref<ProjectStageVO[]>([])
const actions = ref<StageActionVO[]>([])
const files = ref<ProjectFileVO[]>([])
const logs = ref<OperationLogVO[]>([])
const messages = ref<MessageVO[]>([])
const conversation = ref<ConversationVO | null>(null)
const messageText = ref('')
const creatingAction = ref(false)
const sending = ref(false)
const uploading = ref(false)
const archiving = ref(false)
const selectedFile = ref<File | null>(null)

const actionForm = reactive({
  stageCode: '',
  requestNote: ''
})
const archiveForm = reactive({
  fileId: 0,
  projectStageId: 0,
  stageCode: '',
  fileRole: 'DELIVERABLE' as FileRole,
  description: ''
})

const projectId = computed(() => Number(route.params.id))

function stageClass(status: string) {
  if (status === 'REACHED') return 'success'
  if (status === 'PENDING_CONFIRM') return 'warning'
  if (status === 'REJECTED') return 'danger'
  return ''
}

function actionClass(status: string) {
  if (status === 'CONFIRMED') return 'success'
  if (status === 'PENDING') return 'warning'
  if (status === 'REJECTED') return 'danger'
  return ''
}

async function reload() {
  if (!projectId.value) return
  project.value = await getProject(projectId.value)
  stages.value = await listProjectStages(projectId.value)
  actions.value = await listProjectActions(projectId.value)
  files.value = await listProjectFiles(projectId.value)
  const conversationList = await listConversations()
  conversation.value = conversationList.find((item) => item.projectId === projectId.value) || null
  if (conversation.value) {
    const page = await listMessages(conversation.value.id, { size: 50 })
    messages.value = page.records
  } else {
    messages.value = []
  }
  logs.value = (await listOperationLogs(projectId.value, { page: 1, size: 20 })).records
  actionForm.stageCode = project.value?.currentStage || actionForm.stageCode
  archiveForm.stageCode = project.value?.currentStage || archiveForm.stageCode
}

async function createActionSubmit() {
  if (!project.value || !actionForm.stageCode) return
  try {
    creatingAction.value = true
    await createStageAction(project.value.id, actionForm.stageCode, { requestNote: actionForm.requestNote })
    ElMessage.success('已发起确认')
    actionForm.requestNote = ''
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发起失败')
  } finally {
    creatingAction.value = false
  }
}

async function confirm(id: number) {
  try {
    const note = await ElMessageBox.prompt('确认说明', '确认阶段', { inputValue: '确认进入下一阶段。' })
    await confirmStageAction(id, { responseNote: note.value })
    await reload()
  } catch {}
}

async function reject(id: number) {
  try {
    const note = await ElMessageBox.prompt('驳回说明', '驳回阶段', { inputValue: '需要补充内容。' })
    await rejectStageAction(id, { responseNote: note.value })
    await reload()
  } catch {}
}

function selectFile(uploadFileEvent: { raw?: File }) {
  selectedFile.value = uploadFileEvent.raw || null
  if (selectedFile.value) {
    archiveForm.fileId = 0
  }
}

async function uploadSelectedFile() {
  if (!selectedFile.value) return
  try {
    uploading.value = true
    const file = await uploadFile(selectedFile.value)
    archiveForm.fileId = file.id
    ElMessage.success('上传成功')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '上传失败')
  } finally {
    uploading.value = false
  }
}

async function archiveSelectedFile() {
  if (!project.value || !archiveForm.fileId) return
  try {
    archiving.value = true
    await archiveProjectFile(project.value.id, {
      fileId: archiveForm.fileId,
      projectStageId: archiveForm.projectStageId || undefined,
      stageCode: archiveForm.stageCode || undefined,
      fileRole: archiveForm.fileRole,
      description: archiveForm.description
    })
    ElMessage.success('归档成功')
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '归档失败')
  } finally {
    archiving.value = false
  }
}

async function removeFile(id: number) {
  try {
    await deleteProjectFile(id)
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

async function download(fileId: number, name: string) {
  const blob = await downloadFile(fileId)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = name
  link.click()
  URL.revokeObjectURL(url)
}

async function send() {
  if (!conversation.value || !messageText.value.trim()) return
  try {
    sending.value = true
    const message = await sendMessage(conversation.value.id, {
      messageType: 'TEXT',
      content: messageText.value.trim(),
      fileIds: [],
      clientMessageId: `designer-${Date.now()}`
    })
    messages.value = [...messages.value, message]
    messageText.value = ''
  } finally {
    sending.value = false
  }
}

watch(projectId, reload, { immediate: true })
</script>
