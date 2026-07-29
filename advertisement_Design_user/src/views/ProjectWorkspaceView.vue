<template>
  <div class="project-workspace-view">
    <ProjectListPane :project-id="projectId" />
    <section v-if="state?.loading && !state.projection" class="workspace-state"><strong>正在打开项目…</strong><p>沟通记录和阶段信息正在载入。</p></section>
    <section v-else-if="state?.error && !state.projection" class="workspace-state workspace-state--error"><strong>项目暂时无法打开</strong><p>{{ state.error }}</p><button type="button" @click="workspace.openProject(projectId)">重新加载</button></section>
    <template v-else-if="state?.projection">
      <ProjectConversationPane :projection="state.projection" :state="state" @load-older="workspace.loadMessages(projectId)" @files="workspace.addFiles(projectId,$event)" @retry="workspace.uploadAttachment(projectId,$event)" @remove="workspace.removeAttachment(projectId,$event)" @send="workspace.send(projectId)"/>
      <ProjectInspectorPane :projection="state.projection" @history="workspace.loadStageHistory(projectId,$event)" @action="handleAction"/>
    </template>
  </div>
</template>
<script setup lang="ts">
import { computed, onBeforeUnmount, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import ProjectListPane from '@/components/workspace/ProjectListPane.vue'
import ProjectConversationPane from '@/components/workspace/ProjectConversationPane.vue'
import ProjectInspectorPane from '@/components/workspace/ProjectInspectorPane.vue'
import type { ProjectAllowedAction } from '@/modules/project/types'
import { useProjectWorkspaceStore } from '@/stores/projectWorkspace'
const route=useRoute();const workspace=useProjectWorkspaceStore();const projectId=computed(()=>Number(route.params.projectId));const state=computed(()=>workspace.entries[projectId.value] || null)
watch(projectId,id=>{if(Number.isSafeInteger(id)&&id>0)void workspace.openProject(id)},{immediate:true})
function handleAction(action:ProjectAllowedAction){ElMessage.info(`${action.label}将在对应业务页面中完成。`)}
onBeforeUnmount(()=>workspace.close())
</script>
<style scoped>.project-workspace-view{height:100%;min-height:0;display:grid;grid-template-columns:248px minmax(480px,1fr) 292px;background:#f3f4f2;color:#1f2522}.workspace-state{grid-column:2/-1;display:grid;place-content:center;justify-items:center;background:#fff;color:#747b76;text-align:center}.workspace-state strong{color:#303632;font-size:16px}.workspace-state p{max-width:420px;font-size:12px}.workspace-state button{padding:8px 12px;border:0;border-radius:7px;background:#282e2a;color:#fff;cursor:pointer}.workspace-state--error p{color:#94433e}@media(max-width:980px){.project-workspace-view{grid-template-columns:210px 1fr}.project-workspace-view>:last-child{display:none}}@media(max-width:720px){.project-workspace-view{grid-template-columns:1fr}.project-workspace-view>:first-child{display:none}.workspace-state{grid-column:1}}</style>
