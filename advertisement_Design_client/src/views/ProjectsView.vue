<template>
  <div class="panel-grid">
    <PageSection title="项目列表" subtitle="浏览和筛选项目">
      <div class="form-grid" style="margin-bottom: 16px">
        <el-input v-model="filters.keyword" placeholder="关键词" clearable />
        <el-input v-model="filters.currentStage" placeholder="当前阶段" clearable />
        <el-input v-model="filters.status" placeholder="状态" clearable />
        <el-button type="primary" @click="load">查询</el-button>
      </div>
      <div class="card-list">
        <button
          v-for="project in projects"
          :key="project.id"
          class="card-item"
          :class="{ active: selectedId === project.id }"
          @click="select(project.id)"
        >
          <div class="card-meta">
            <strong>{{ project.name }}</strong>
            <span class="badge" :class="statusClass(project.status)">{{ project.status }}</span>
          </div>
          <div class="muted" style="margin-top: 8px">{{ project.currentStageName }}</div>
          <div class="muted">{{ project.description || '暂无说明' }}</div>
        </button>
      </div>
    </PageSection>

    <PageSection title="项目摘要" subtitle="基础信息和最近会话">
      <div v-if="selectedProject" class="stack">
        <div class="card-item">
          <div class="card-meta">
            <strong>{{ selectedProject.name }}</strong>
            <span class="badge primary">{{ selectedProject.progress }}%</span>
          </div>
          <div class="muted" style="margin-top: 8px">{{ selectedProject.description || '暂无说明' }}</div>
        </div>
        <div class="card-item">
          <div class="section-head" style="margin-bottom: 8px">
            <h4>最近消息</h4>
          </div>
          <div class="muted">{{ selectedConversation?.lastMessage || '暂无消息' }}</div>
        </div>
      </div>
      <div v-else class="empty">请选择一个项目查看。</div>
    </PageSection>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageSection from '@/components/PageSection.vue'
import { listConversations, listProjects } from '@/api'
import type { ConversationVO, ProjectVO } from '@/models'

const router = useRouter()
const route = useRoute()
const projects = ref<ProjectVO[]>([])
const conversations = ref<ConversationVO[]>([])
const selectedId = ref<number | null>(null)
const filters = reactive({
  keyword: '',
  currentStage: '',
  status: ''
})

const selectedProject = computed(() => projects.value.find((item) => item.id === selectedId.value) || null)
const selectedConversation = computed(() => conversations.value.find((item) => item.projectId === selectedId.value) || null)

function statusClass(status: string) {
  if (status === 'IN_PROGRESS') return 'warning'
  if (status === 'COMPLETED') return 'success'
  if (status === 'CANCELLED') return 'danger'
  return ''
}

async function load() {
  const [projectPage, conversationList] = await Promise.all([
    listProjects({
      keyword: filters.keyword || undefined,
      currentStage: filters.currentStage || undefined,
      status: filters.status || undefined,
      page: 1,
      size: 50
    }),
    listConversations()
  ])
  projects.value = projectPage.records
  conversations.value = conversationList
  if (!selectedId.value) {
    selectedId.value = route.query.id ? Number(route.query.id) : projects.value[0]?.id || null
  }
}

function select(id: number) {
  selectedId.value = id
  router.push({ path: '/projects', query: { id } })
}

onMounted(load)
</script>
