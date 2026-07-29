<template>
  <aside class="project-inspector-pane">
    <section class="project-name">
      <small>项目名称</small>
      <div v-if="editing" class="name-editor">
        <input v-model="draftName" maxlength="100" :disabled="saving" @keydown.enter.prevent="saveName" @keydown.esc.prevent="cancelEdit">
        <div><button type="button" :disabled="saving || !draftName.trim()" @click="saveName">{{ saving ? '保存中…' : '保存' }}</button><button type="button" :disabled="saving" @click="cancelEdit">取消</button></div>
      </div>
      <div v-else class="name-summary"><strong>{{ projection.project.name }}</strong><button type="button" @click="startEdit">修改</button></div>
      <p>{{ projection.project.nameSource === 'MANUAL' ? '当前使用你设置的名称。' : '需求信息稳定后，项目名称可以自动整理。' }}</p>
      <button v-if="projection.project.nameSource === 'MANUAL'" class="restore-button" type="button" :disabled="saving" @click="restoreAuto">恢复自动命名</button>
    </section>
    <StageTimeline :stages="projection.stages" @history="emit('history',$event)"/>
    <StageActionPanel :actions="projection.allowedActions" @action="emit('action',$event)"/>
    <section class="materials"><header><small>项目资料</small><strong>资料与版本</strong></header><p v-if="!projection.materials.length">项目资料发布后会归档在这里。</p><a v-for="item in projection.materials" :key="item.id" :href="item.downloadUrl || undefined" :target="item.downloadUrl ? '_blank' : undefined"><span>{{ item.category }}</span><strong>{{ item.name }}</strong><small>{{ item.version == null ? '等待版本' : `版本 ${item.version}` }}</small></a></section>
    <section class="project-facts"><small>项目信息</small><dl><div><dt>项目状态</dt><dd>{{ statusText }}</dd></div><div><dt>最近更新</dt><dd>{{ dateText(projection.project.updatedAt) }}</dd></div></dl></section>
  </aside>
</template>
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { ProjectAllowedAction, ProjectWorkspaceProjection } from '@/modules/project/types'
import StageTimeline from './StageTimeline.vue'
import StageActionPanel from './StageActionPanel.vue'
const props = defineProps<{ projection: ProjectWorkspaceProjection; savingName?: boolean }>()
const emit = defineEmits<{ history: [id: number]; action: [action: ProjectAllowedAction]; rename: [name: string]; 'restore-auto': [] }>()
const editing = ref(false)
const draftName = ref(props.projection.project.name)
const saving = computed(() => props.savingName === true)
const statusText = computed(() => ({ACTIVE:'进行中',PAUSED:'已暂停',COMPLETED:'已完成',TERMINATED:'已终止'} as Record<string,string>)[props.projection.project.status] || '处理中')
watch(() => props.projection.project.name, value => { if (!editing.value) draftName.value = value })
function dateText(value: string | null) { return value ? value.replace('T',' ').slice(0,16) : '等待更新' }
function startEdit() { draftName.value = props.projection.project.name; editing.value = true }
function cancelEdit() { draftName.value = props.projection.project.name; editing.value = false }
function saveName() { const name = draftName.value.trim(); if (!name) return; emit('rename', name); editing.value = false }
function restoreAuto() { emit('restore-auto') }
</script>
<style scoped>
.project-inspector-pane{min-height:0;padding:23px 20px;border-left:1px solid #dde0dc;background:#fafbf9;overflow:auto}.project-name{padding:0 0 18px;border-bottom:1px solid #e5e7e3}.project-name>small,.materials header small,.project-facts>small{color:#9b632f;font-size:9px;font-weight:750;letter-spacing:.08em}.name-summary{margin-top:7px;display:flex;align-items:start;gap:8px}.name-summary strong{min-width:0;flex:1;font-size:13px;line-height:1.45}.name-summary button,.name-editor button,.restore-button{border:0;background:transparent;color:#8c5e33;font-size:9px;cursor:pointer}.project-name p{margin:6px 0 0;color:#7c817d;font-size:9px;line-height:1.55}.restore-button{margin-top:7px;padding:0;font-weight:700}.name-editor{margin-top:7px;display:grid;gap:7px}.name-editor input{width:100%;padding:7px 8px;border:1px solid #cfd3ce;border-radius:6px;background:#fff;color:#282e2a;font:11px inherit;box-sizing:border-box}.name-editor div{display:flex;gap:8px}.name-editor button:first-child{padding:5px 8px;border-radius:5px;background:#282e2a;color:#fff}.materials{padding:18px 0;border-bottom:1px solid #e5e7e3}.materials header{margin-bottom:10px;display:grid;gap:2px}.materials header strong{font-size:13px}.materials>p{margin:0;color:#7c817d;font-size:9px;line-height:1.6}.materials>a{padding:8px 0;border-top:1px solid #e7e9e5;display:grid;grid-template-columns:1fr auto;gap:2px;color:inherit;text-decoration:none}.materials>a span{grid-column:1/-1;color:#8a908b;font-size:8px}.materials>a strong{font-size:10px}.materials>a small{color:#8b5b31;font-size:9px}.project-facts{padding-top:18px}.project-facts dl{margin:8px 0 0}.project-facts dl div{padding:8px 0;border-top:1px solid #e7e9e5;display:flex;justify-content:space-between}.project-facts dt{color:#858b86;font-size:9px}.project-facts dd{margin:0;font-size:10px;font-weight:650}
</style>
