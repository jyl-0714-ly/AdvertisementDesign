<template>
  <div class="project-detail" v-loading="loading">
    <button class="detail-back" type="button" @click="router.push('/projects')"><el-icon><ArrowLeft /></el-icon> 返回我的项目</button>
    <template v-if="project">
      <header class="detail-header">
        <div><span class="workspace-eyebrow">PROJECT OVERVIEW</span><h1>{{ project.name }}</h1><p>{{ project.description || '暂无项目说明' }}</p></div>
        <button type="button" class="go-workbench" @click="router.push(`/workspace/${projectId}`)"><el-icon><ChatDotRound /></el-icon>进入沟通工作台</button>
      </header>
      <section class="detail-progress-panel">
        <div class="progress-label"><div><span>当前进度</span><strong>{{ project.currentStageName }}</strong></div><b>{{ project.progress }}%</b></div>
        <div class="detail-progress"><span :style="{ width: `${project.progress}%` }"></span></div>
        <div class="milestone-track">
          <button v-for="stage in stages" :key="stage.id" type="button" :class="stage.status.toLowerCase()" @click="activeStage = stage.stageCode"><i></i><strong>{{ stage.stageName }}</strong><small>{{ stageStatus(stage.status) }}</small></button>
        </div>
      </section>

      <div class="detail-layout">
        <section class="timeline-panel">
          <div class="detail-section-head"><div><h2>项目里程碑</h2><p>按阶段记录每一次确认与交付。</p></div><span>时间线</span></div>
          <div class="milestone-timeline">
            <article v-for="stage in stages" :key="stage.id" :class="{ selected: activeStage === stage.stageCode }" @click="activeStage = stage.stageCode">
              <i :class="stage.status.toLowerCase()"></i><div><header><strong>{{ stage.stageName }}</strong><span :class="stage.status.toLowerCase()">{{ stageStatus(stage.status) }}</span></header><p>{{ stageCopy(stage) }}</p><time>{{ stage.reachedAt || stage.updatedAt || '等待推进' }}</time></div>
            </article>
          </div>
        </section>
        <aside class="detail-side">
          <section class="members-panel"><h2>项目服务</h2><div><span>客户联系人</span><strong>{{ project.customerName || '—' }}</strong></div><div><span>服务身份</span><strong>项目服务团队</strong></div></section>
          <section class="archive-panel"><div class="detail-section-head"><div><h2>文件归档</h2><p>按阶段归档的项目产物。</p></div><span>{{ files.length }}</span></div>
            <div v-for="file in filteredFiles" :key="file.id" class="archive-item"><el-icon><Document /></el-icon><div><strong>{{ file.file?.originalName || file.description || '项目文件' }}</strong><small>{{ stageName(file.stageCode) }} · {{ fileRoleLabel(file.fileRole) }}</small></div><button type="button" title="下载文件" @click="download(file)"><el-icon><Download /></el-icon></button></div>
            <div v-if="!filteredFiles.length" class="archive-empty">该阶段暂未归档文件。</div>
          </section>
        </aside>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ChatDotRound, Document, Download } from '@element-plus/icons-vue'
import { downloadFile, getProject, listProjectFiles, listProjectStages } from '@/api'
import type { ProjectFileVO, ProjectStageVO, ProjectVO } from '@/models'
import { fileRoleLabel, stageStatusLabel } from '@/utils/displayLabels'

const route = useRoute(); const router = useRouter()
const loading = ref(false); const project = ref<ProjectVO | null>(null); const stages = ref<ProjectStageVO[]>([]); const files = ref<ProjectFileVO[]>([]); const activeStage = ref('')
const projectId = computed(() => Number(route.params.id))
const filteredFiles = computed(() => activeStage.value ? files.value.filter((item) => item.stageCode === activeStage.value) : files.value)
const stageStatus = stageStatusLabel
const stageCopy = (stage: ProjectStageVO) => stage.status === 'REACHED' ? '双方已确认该阶段，项目记录已归档。' : stage.status === 'PENDING_CONFIRM' ? '已发起确认，正在等待对方处理。' : stage.status === 'REJECTED' ? '该阶段被驳回，请在沟通工作台继续处理。' : '尚未发起，完成上一阶段后可推进。'
const stageName = (code?: string | null) => stages.value.find((item) => item.stageCode === code)?.stageName || '未分类'

async function reload() {
  if (!projectId.value) return
  loading.value = true
  try { const data = await Promise.all([getProject(projectId.value), listProjectStages(projectId.value), listProjectFiles(projectId.value)]); project.value = data[0]; stages.value = data[1]; files.value = data[2]; activeStage.value = project.value.currentStage || stages.value[0]?.stageCode || '' }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '项目加载失败') }
  finally { loading.value = false }
}

async function download(file: ProjectFileVO) {
  if (!file.fileId) return
  try { const blob = await downloadFile(file.fileId); const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = file.file?.originalName || '项目文件'; link.click(); URL.revokeObjectURL(url) }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '下载失败') }
}
watch(projectId, reload, { immediate: true })
</script>

<style>
.project-detail { min-height: calc(100vh - 111px); }.detail-back { padding: 0; border: 0; background: transparent; display: inline-flex; align-items: center; gap: 5px; color: #60738b; cursor: pointer; font-size: 13px; }.detail-header { padding: 19px 0 26px; display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; }.detail-header h1 { margin: 8px 0; font-size: 31px; }.detail-header p { max-width: 730px; margin: 0; color: #728197; font-size: 14px; line-height: 1.7; }.detail-progress-panel { padding: 23px 25px 0; border: 1px solid #dce6f0; background: #fff; }.progress-label { display: flex; justify-content: space-between; align-items: flex-end; }.progress-label span, .progress-label strong { display: block; }.progress-label span { color: #7d8b9d; font-size: 12px; }.progress-label strong { margin-top: 6px; font-size: 17px; }.progress-label > b { color: #1367d1; font-size: 28px; }.detail-progress { height: 7px; margin-top: 17px; overflow: hidden; background: #eaf0f6; }.detail-progress span { height: 100%; display: block; background: #1367d1; }.milestone-track { min-width: 750px; padding: 26px 0 20px; display: grid; grid-template-columns: repeat(7, 1fr); gap: 3px; }.milestone-track button { padding: 0 8px; border: 0; background: transparent; display: grid; gap: 7px; text-align: left; color: #8796a8; cursor: pointer; }.milestone-track i { width: 11px; height: 11px; border-radius: 50%; background: #d6e0ea; box-shadow: 16px 4px 0 -4px #d6e0ea; }.milestone-track button.reached i { background: #1e9b62; box-shadow: 16px 4px 0 -4px #cde9da; }.milestone-track button.pending_confirm i { background: #e8a419; }.milestone-track button.rejected i { background: #d86060; }.milestone-track strong { color: #405168; font-size: 12px; line-height: 1.35; }.milestone-track small { font-size: 10px; }.detail-layout { margin-top: 16px; display: grid; grid-template-columns: minmax(0, 1.15fr) minmax(310px, .85fr); gap: 16px; }.timeline-panel, .members-panel, .archive-panel { padding: 22px; border: 1px solid #dce6f0; background: #fff; }.detail-section-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }.detail-section-head h2, .members-panel h2 { margin: 0; font-size: 16px; }.detail-section-head p { margin: 5px 0 0; color: #8492a4; font-size: 12px; }.detail-section-head > span { color: #8190a1; font-size: 12px; }.milestone-timeline { margin-top: 24px; }.milestone-timeline article { padding: 0 0 22px; display: grid; grid-template-columns: 18px 1fr; gap: 11px; cursor: pointer; }.milestone-timeline article > i { width: 10px; height: 10px; margin-top: 5px; border-radius: 50%; background: #d5dfe9; box-shadow: 0 17px 0 -4px #d7e2ed; }.milestone-timeline article > i.reached { background: #1e9b62; }.milestone-timeline article > i.pending_confirm { background: #e8a419; }.milestone-timeline article > i.rejected { background: #d86060; }.milestone-timeline header { display: flex; align-items: center; justify-content: space-between; gap: 10px; }.milestone-timeline header strong { font-size: 14px; }.milestone-timeline header span { padding: 2px 6px; background: #edf2f7; color: #708095; font-size: 10px; }.milestone-timeline header span.reached { background: #e1f5e9; color: #29794f; }.milestone-timeline header span.pending_confirm { background: #fff0cc; color: #986300; }.milestone-timeline header span.rejected { background: #fde5e5; color: #b74444; }.milestone-timeline p { margin: 7px 0; color: #78879a; font-size: 12px; }.milestone-timeline time { color: #a1adbb; font-size: 11px; }.detail-side { display: grid; align-content: start; gap: 16px; }.members-panel { display: grid; gap: 13px; }.members-panel > div { padding-top: 12px; border-top: 1px solid #edf1f5; display: flex; justify-content: space-between; color: #758397; font-size: 12px; }.members-panel strong { color: #3d4d62; }.archive-panel { min-height: 260px; }.archive-item { padding: 13px 0; border-bottom: 1px solid #edf1f5; display: flex; align-items: center; gap: 8px; }.archive-item > .el-icon { color: #4181c5; font-size: 18px; }.archive-item > div { min-width: 0; flex: 1; display: grid; gap: 3px; }.archive-item strong { overflow: hidden; color: #44536a; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }.archive-item small { color: #98a5b4; font-size: 10px; }.archive-item button { border: 0; background: transparent; color: #607c9c; cursor: pointer; }.archive-empty { padding: 45px 0; color: #9aa6b5; text-align: center; font-size: 12px; }
@media (max-width: 870px) { .detail-layout { grid-template-columns: 1fr; }.detail-progress-panel { overflow: auto; }.detail-header { align-items: flex-start; flex-direction: column; } }
</style>
