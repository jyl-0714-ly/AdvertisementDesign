<template>
  <div class="panel-grid">
    <div class="grid-4">
      <StatCard label="项目总数" :value="stats.totalProjects" hint="当前账号可见项目" />
      <StatCard label="进行中" :value="stats.activeProjects" hint="协作中的项目" />
      <StatCard label="公开案例" :value="stats.caseCount" hint="案例库数量" />
      <StatCard label="未读消息" :value="stats.unreadMessages" hint="会话未读计数" />
    </div>

    <div class="grid-2">
      <PageSection title="项目列表" subtitle="点击进入项目详情">
        <div class="card-list">
          <button v-for="project in projects" :key="project.id" class="card-item" @click="openProject(project.id)">
            <div class="card-meta">
              <strong>{{ project.name }}</strong>
              <span class="badge" :class="statusClass(project.status)">{{ projectStatusLabel(project.status) }}</span>
            </div>
            <div class="muted" style="margin-top: 8px">{{ project.customerName || '—' }} / {{ project.designerName || '—' }}</div>
            <div class="muted">{{ project.currentStageName }} · {{ project.progress }}%</div>
          </button>
        </div>
      </PageSection>

      <PageSection title="最近会话" subtitle="项目沟通状态">
        <div class="card-list">
          <div v-for="conversation in conversations" :key="conversation.id" class="card-item">
            <div class="card-meta">
              <strong>{{ conversation.projectName }}</strong>
              <span class="badge primary">{{ conversation.unreadCount }} 未读</span>
            </div>
            <div class="muted" style="margin-top: 8px">{{ conversation.lastMessage || '暂无消息' }}</div>
            <div class="muted">{{ conversation.lastMessageAt || '—' }}</div>
          </div>
        </div>
      </PageSection>
    </div>

    <PageSection title="项目入口" subtitle="正式项目须从已接待的客户咨询中创建">
      <div class="table-actions">
        <el-button type="primary" @click="router.push('/reception')">前往客户接待</el-button>
        <el-button @click="load">刷新</el-button>
      </div>
    </PageSection>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageSection from '@/components/PageSection.vue'
import StatCard from '@/components/StatCard.vue'
import { listConversations, listPortfolioCases, listProjects } from '@/api'
import type { ConversationVO, PortfolioCaseVO, ProjectVO } from '@/models'
import { projectStatusLabel } from '@/utils/displayLabels'
import { ElMessage } from 'element-plus'

const router = useRouter()
const projects = ref<ProjectVO[]>([])
const conversations = ref<ConversationVO[]>([])
const cases = ref<PortfolioCaseVO[]>([])

const stats = computed(() => ({
  totalProjects: projects.value.length,
  activeProjects: projects.value.filter((item) => item.status === 'IN_PROGRESS').length,
  caseCount: cases.value.length,
  unreadMessages: conversations.value.reduce((sum, item) => sum + item.unreadCount, 0)
}))

function statusClass(status: string) {
  if (status === 'IN_PROGRESS') return 'warning'
  if (status === 'COMPLETED') return 'success'
  if (status === 'CANCELLED') return 'danger'
  return ''
}

async function load() {
  try {
    const [projectPage, conversationList, casePage] = await Promise.all([
      listProjects({ page: 1, size: 50 }),
      listConversations(),
      listPortfolioCases({ page: 1, size: 8 })
    ])
    projects.value = projectPage.records
    conversations.value = conversationList
    cases.value = casePage.records
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '首页数据加载失败')
  }
}

function openProject(id: number) {
  router.push(`/projects/${id}`)
}

onMounted(load)
</script>
