<template>
  <div class="project-hub" v-loading="loading">
    <header class="project-hub-head">
      <div><span class="workspace-eyebrow">PROJECTS</span><h1>我的项目</h1><p>按当前阶段查看所有协作中的设计项目。</p></div>
      <button type="button" class="go-workbench" @click="router.push('/workbench')"><el-icon><ChatDotRound /></el-icon>进入需求沟通</button>
    </header>

    <section v-for="group in groups" :key="group.name" class="project-group">
      <div class="project-group-title"><h2>{{ group.name }}</h2><span>{{ group.items.length }} 个项目</span></div>
      <div class="project-grid">
        <article v-for="project in group.items" :key="project.id" class="project-summary" @click="router.push(`/projects/${project.id}`)">
          <header><span class="project-status" :class="project.status.toLowerCase()">{{ statusName(project.status) }}</span><time>{{ formatDate(project.updatedAt) }}</time></header>
          <h3>{{ project.name }}</h3>
          <p>{{ project.description || '暂无项目说明' }}</p>
          <div class="project-stage-row"><span>当前阶段</span><strong>{{ project.currentStageName }}</strong></div>
          <div class="project-progress"><span :style="{ width: `${project.progress}%` }"></span></div>
          <footer><b>{{ project.progress }}%</b><span>{{ project.designerName || '设计师' }}</span><el-icon><ArrowRight /></el-icon></footer>
        </article>
      </div>
    </section>
    <div v-if="!loading && !projects.length" class="project-empty">当前没有项目，欢迎通过免费咨询发起新的合作。</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, ChatDotRound } from '@element-plus/icons-vue'
import { listProjects } from '@/api'
import type { ProjectVO } from '@/models'
import { projectStatusLabel } from '@/utils/displayLabels'

const router = useRouter()
const loading = ref(false)
const projects = ref<ProjectVO[]>([])
const groups = computed(() => {
  const values = new Map<string, ProjectVO[]>()
  projects.value.forEach((project) => values.set(project.currentStageName || '待启动', [...(values.get(project.currentStageName || '待启动') || []), project]))
  return [...values.entries()].map(([name, items]) => ({ name, items }))
})

const statusName = projectStatusLabel
const formatDate = (value?: string) => value ? value.replace('T', ' ').slice(0, 10) : ''

onMounted(async () => {
  loading.value = true
  try { projects.value = (await listProjects({ page: 1, size: 50 })).records }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '项目加载失败') }
  finally { loading.value = false }
})
</script>

<style>
.project-hub { min-height: calc(100vh - 111px); }.project-hub-head { padding: 13px 0 28px; display: flex; align-items: flex-end; justify-content: space-between; gap: 25px; }.workspace-eyebrow { color: #1367d1; font-size: 11px; font-weight: 800; letter-spacing: 1px; }.project-hub-head h1 { margin: 7px 0; font-size: 30px; }.project-hub-head p { margin: 0; color: #748398; font-size: 14px; }.go-workbench { min-height: 38px; padding: 0 13px; border: 1px solid #bbd5f1; background: #fff; display: inline-flex; align-items: center; gap: 5px; color: #1367d1; cursor: pointer; font-size: 13px; font-weight: 700; }.project-group { margin-top: 29px; }.project-group-title { margin-bottom: 12px; display: flex; align-items: center; gap: 9px; }.project-group-title h2 { margin: 0; font-size: 16px; }.project-group-title span { color: #8594a7; font-size: 12px; }.project-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }.project-summary { min-height: 215px; padding: 17px; border: 1px solid #dde6f0; background: #fff; cursor: pointer; transition: transform .18s, box-shadow .18s; }.project-summary:hover { transform: translateY(-3px); box-shadow: 0 15px 30px rgba(35, 56, 86, .1); }.project-summary > header, .project-summary footer { display: flex; align-items: center; justify-content: space-between; }.project-status { padding: 3px 7px; background: #e7f4ff; color: #1675c5; font-size: 11px; font-weight: 700; }.project-status.completed { background: #e7f6ed; color: #23754d; }.project-status.paused { background: #fff2d4; color: #9e6a00; }.project-summary time { color: #99a6b4; font-size: 11px; }.project-summary h3 { margin: 21px 0 7px; font-size: 17px; }.project-summary p { min-height: 37px; margin: 0; color: #748398; font-size: 12px; line-height: 1.6; }.project-stage-row { margin-top: 18px; display: flex; justify-content: space-between; gap: 10px; color: #718096; font-size: 12px; }.project-stage-row strong { color: #43536a; font-weight: 700; }.project-progress { height: 5px; margin-top: 9px; overflow: hidden; background: #e9eff5; }.project-progress span { height: 100%; display: block; background: #1a75d4; }.project-summary footer { margin-top: 9px; color: #8795a7; font-size: 11px; }.project-summary footer b { color: #1367d1; font-size: 13px; }.project-summary footer .el-icon { color: #516880; font-size: 15px; }.project-empty { padding: 70px; border: 1px dashed #ccd9e8; color: #7c8a9b; text-align: center; }
@media (max-width: 960px) { .project-grid { grid-template-columns: 1fr 1fr; } }
@media (max-width: 600px) { .project-hub-head { align-items: flex-start; flex-direction: column; }.project-grid { grid-template-columns: 1fr; } }
</style>
