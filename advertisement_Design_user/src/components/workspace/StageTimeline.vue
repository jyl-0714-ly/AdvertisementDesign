<template>
  <section class="stage-timeline"><header><small>七阶段服务</small><strong>项目进度</strong></header><ol><li v-for="stage in stages" :key="stage.id" :class="stageClass(stage)"><button type="button" @click="select(stage)"><span>{{ stage.sortOrder }}</span><div><strong>{{ stage.stageName }}</strong><small>{{ stageStatus(stage.status) }}</small></div></button><div v-if="expanded === stage.id" class="stage-history"><p v-if="!stage.histories">正在读取阶段记录…</p><p v-else-if="!stage.histories.length">此阶段暂无历史记录。</p><article v-for="event in stage.histories || []" :key="event.id"><time>{{ dateText(event.occurredAt) }}</time><span>{{ historyText(event) }}</span></article></div></li></ol></section>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import type { ProjectStage, ProjectStageHistory } from '@/modules/project/types'
defineProps<{ stages: ProjectStage[] }>()
const emit = defineEmits<{ history: [stageId: number] }>()
const expanded = ref<number | null>(null)
function select(stage: ProjectStage) { expanded.value = expanded.value === stage.id ? null : stage.id; if (expanded.value) emit('history', stage.id) }
function stageClass(stage: ProjectStage) { return { current: stage.status === 'IN_PROGRESS' || stage.status === 'ACTIVE', completed: stage.status === 'COMPLETED' } }
function stageStatus(status: string) { return ({ PENDING: '待开始', ACTIVE: '进行中', IN_PROGRESS: '进行中', COMPLETED: '已完成', BLOCKED: '等待处理', REOPENED: '已重新开启' } as Record<string,string>)[status] || '等待更新' }
function dateText(value: string) { return value ? value.replace('T',' ').slice(5,16) : '' }
function historyText(event: ProjectStageHistory) { if (event.reason) return event.reason; if (event.toStatus === 'COMPLETED') return '阶段已完成'; if (event.toStatus === 'ACTIVE' || event.toStatus === 'IN_PROGRESS') return '阶段已开始'; return '阶段记录已更新' }
</script>
<style scoped>
.stage-timeline{padding-bottom:20px;border-bottom:1px solid #e5e7e3}.stage-timeline header{margin-bottom:15px;display:grid;gap:2px}.stage-timeline header small{color:#9b632f;font-size:9px;font-weight:750;letter-spacing:.08em}.stage-timeline header strong{font-size:14px}.stage-timeline ol{margin:0;padding:0;list-style:none}.stage-timeline li{position:relative}.stage-timeline li:not(:last-child):before{content:"";position:absolute;left:11px;top:28px;bottom:-5px;width:1px;background:#dfe2dd}.stage-timeline li.completed:not(:last-child):before{background:#789a85}.stage-timeline li>button{width:100%;padding:6px 0;border:0;background:transparent;display:flex;align-items:center;gap:10px;text-align:left;cursor:pointer}.stage-timeline li>button>span{width:23px;height:23px;z-index:1;border:1px solid #d8dcd7;border-radius:50%;display:grid;place-items:center;background:#fafbf9;color:#8a908b;font-size:9px}.stage-timeline li.current>button>span{border-color:#a36a37;background:#fff6eb;color:#8b5728}.stage-timeline li.completed>button>span{border-color:#789a85;background:#789a85;color:#fff}.stage-timeline li>button div{display:grid;gap:1px}.stage-timeline li>button strong{font-size:11px}.stage-timeline li>button small{color:#8c928d;font-size:9px}.stage-history{margin:3px 0 6px 33px;padding:7px 9px;border-left:2px solid #d7c2ab;background:#f7f7f4}.stage-history p,.stage-history article{margin:0;color:#747b76;font-size:9px;line-height:1.6}.stage-history article{display:grid;grid-template-columns:70px 1fr;gap:5px}.stage-history time{color:#a0a49f}
</style>
