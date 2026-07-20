<template>
  <div class="panel-grid">
    <PageSection :title="project?.name || '项目详情'" subtitle="阶段进度、消息沟通、文件归档">
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
      <div v-else class="empty">加载中或项目不存在。</div>
    </PageSection>

    <div class="grid-2">
      <PageSection title="阶段进度" subtitle="项目流程节点">
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

      <PageSection title="项目文件" subtitle="归档后的成果文件">
        <div class="stack">
          <div v-for="file in files" :key="file.id" class="card-item">
            <div class="card-meta">
              <strong>{{ file.file?.originalName || file.description || '文件' }}</strong>
              <span class="badge">{{ file.fileRole }}</span>
            </div>
            <div class="muted" style="margin-top: 8px">{{ file.stageCode || '—' }}</div>
          </div>
          <div v-if="!files.length" class="empty">暂无归档文件。</div>
        </div>
      </PageSection>
    </div>

    <div class="grid-2">
      <PageSection title="项目消息" subtitle="与设计师沟通">
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
          <div class="field">
            <el-input v-model="messageText" type="textarea" :rows="3" placeholder="输入消息内容" />
          </div>
          <div class="table-actions" style="margin-top: 12px">
            <el-button type="primary" :loading="sending" @click="send">发送消息</el-button>
            <el-button @click="reload">刷新</el-button>
          </div>
        </div>
      </PageSection>

      <PageSection title="项目入口" subtitle="跳转与操作">
        <div class="stack">
          <el-button @click="router.push('/dashboard')">返回首页</el-button>
          <el-button @click="router.push('/projects')">项目列表</el-button>
        </div>
      </PageSection>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageSection from '@/components/PageSection.vue'
import { getProject, listConversations, listMessages, listProjectFiles, listProjectStages, sendMessage } from '@/api'
import type { ConversationVO, MessageVO, ProjectFileVO, ProjectStageVO, ProjectVO } from '@/models'

const route = useRoute()
const router = useRouter()
const project = ref<ProjectVO | null>(null)
const stages = ref<ProjectStageVO[]>([])
const files = ref<ProjectFileVO[]>([])
const conversation = ref<ConversationVO | null>(null)
const messages = ref<MessageVO[]>([])
const messageText = ref('')
const sending = ref(false)

const projectId = computed(() => Number(route.params.id))

function stageClass(status: string) {
  if (status === 'REACHED') return 'success'
  if (status === 'PENDING_CONFIRM') return 'warning'
  if (status === 'REJECTED') return 'danger'
  return ''
}

async function reload() {
  if (!projectId.value) return
  project.value = await getProject(projectId.value)
  stages.value = await listProjectStages(projectId.value)
  files.value = await listProjectFiles(projectId.value)
  const list = await listConversations()
  conversation.value = list.find((item) => item.projectId === projectId.value) || null
  if (conversation.value) {
    const page = await listMessages(conversation.value.id, { size: 50 })
    messages.value = page.records
  } else {
    messages.value = []
  }
}

async function send() {
  if (!conversation.value || !messageText.value.trim()) return
  try {
    sending.value = true
    const message = await sendMessage(conversation.value.id, {
      messageType: 'TEXT',
      content: messageText.value.trim(),
      fileIds: [],
      clientMessageId: `client-${Date.now()}`
    })
    messages.value = [...messages.value, message]
    messageText.value = ''
  } finally {
    sending.value = false
  }
}

watch(projectId, reload, { immediate: true })
</script>
