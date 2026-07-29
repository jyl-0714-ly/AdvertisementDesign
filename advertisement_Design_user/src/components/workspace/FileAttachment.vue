<template><article class="file-attachment"><span aria-hidden="true">{{ extension }}</span><div><strong>{{ attachment.name || `项目文件 ${attachment.fileAssetId}` }}</strong><small>{{ sizeText }}</small></div><a v-if="attachment.downloadPath" :href="attachment.downloadPath" target="_blank" rel="noopener">查看</a></article></template>
<script setup lang="ts">
import { computed } from 'vue'
import type { MessageAttachment } from '@/modules/project/types'
const props = defineProps<{ attachment: MessageAttachment }>()
const extension = computed(() => { const name = props.attachment.name || ''; return name.includes('.') ? name.split('.').pop()!.slice(0, 4).toUpperCase() : '文件' })
const sizeText = computed(() => props.attachment.size == null ? '项目附件' : props.attachment.size < 1024 * 1024 ? `${Math.ceil(props.attachment.size / 1024)} KB` : `${(props.attachment.size / 1024 / 1024).toFixed(1)} MB`)
</script>
<style scoped>.file-attachment{min-width:210px;padding:9px;border:1px solid rgba(120,125,120,.2);border-radius:8px;display:flex;align-items:center;gap:9px;background:rgba(255,255,255,.64)}.file-attachment>span{width:36px;height:36px;border-radius:6px;display:grid;place-items:center;background:#ecece8;color:#6b716c;font-size:9px;font-weight:750}.file-attachment div{min-width:0;display:grid;gap:2px;flex:1}.file-attachment strong{overflow:hidden;font-size:11px;text-overflow:ellipsis;white-space:nowrap}.file-attachment small{color:#777e78;font-size:9px}.file-attachment a{color:#88592e;font-size:10px;text-decoration:none}</style>