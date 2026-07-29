<template><section class="stage-actions"><header><small>当前阶段</small><strong>可进行的操作</strong></header><div v-if="actions.length"><button v-for="action in actions" :key="action.code" type="button" :disabled="action.enabled === false" @click="emit('action',action)"><strong>{{ action.label }}</strong><small v-if="action.description">{{ action.description }}</small></button></div><p v-else>当前没有需要你处理的操作。项目服务有新进展时会在这里显示。</p></section></template>
<script setup lang="ts">
import type { ProjectAllowedAction } from '@/modules/project/types'

defineProps<{ actions: ProjectAllowedAction[] }>()
const emit = defineEmits<{
  (event: 'action', action: ProjectAllowedAction): void
}>()
</script>
<style scoped>.stage-actions{padding:18px 0;border-bottom:1px solid #e5e7e3}.stage-actions header{margin-bottom:10px;display:grid;gap:2px}.stage-actions header small{color:#9b632f;font-size:9px;font-weight:750;letter-spacing:.08em}.stage-actions header strong{font-size:13px}.stage-actions>div{display:grid;gap:6px}.stage-actions button{width:100%;padding:9px 10px;border:1px solid #ddd7cf;border-radius:7px;background:#fffaf4;display:grid;gap:2px;color:#5e4631;text-align:left;cursor:pointer}.stage-actions button:disabled{opacity:.45;cursor:not-allowed}.stage-actions button strong{font-size:11px}.stage-actions button small,.stage-actions p{color:#7c817d;font-size:9px;line-height:1.6}.stage-actions p{margin:0}</style>