<template>
  <section class="message-composer">
    <div v-if="attachments.length" class="draft-files"><article v-for="file in attachments" :key="file.localId"><div><strong>{{ file.originalName }}</strong><small :class="file.status.toLowerCase()">{{ statusText(file) }}</small></div><button v-if="file.status === 'FAILED'" type="button" @click="emit('retry', file.localId)">重试</button><button type="button" :disabled="sending" @click="emit('remove', file.localId)">移除</button></article></div>
    <textarea :value="modelValue" :disabled="sending" maxlength="5000" placeholder="输入项目消息，或添加需要归入当前项目的资料…" @input="emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)" @keydown.meta.enter.prevent="emit('send')" @keydown.ctrl.enter.prevent="emit('send')"></textarea>
    <p v-if="error" class="composer-error">{{ error }}</p>
    <footer><label><input type="file" multiple :disabled="sending" @change="selectFiles">添加附件</label><span>{{ modelValue.length }}/5000 · Ctrl/⌘ + Enter 发送</span><button type="button" :disabled="!canSend" @click="emit('send')">{{ sending ? '正在发送…' : '发送' }}</button></footer>
  </section>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import type { ProjectDraftAttachment } from '@/modules/project/types'
const props = defineProps<{ modelValue: string; attachments: ProjectDraftAttachment[]; sending: boolean; error: string | null }>()
const emit = defineEmits<{ 'update:modelValue': [value: string]; files: [files: File[]]; retry: [localId: string]; remove: [localId: string]; send: [] }>()
const canSend = computed(() => !props.sending && props.attachments.every(item => item.status !== 'UPLOADING') && (props.modelValue.trim().length > 0 || props.attachments.some(item => item.status === 'UPLOADED')))
function statusText(file: ProjectDraftAttachment) { return file.status === 'UPLOADING' ? '上传中' : file.status === 'UPLOADED' ? '已就绪' : file.error || '上传失败' }
function selectFiles(event: Event) { const input = event.target as HTMLInputElement; emit('files', Array.from(input.files || [])); input.value = '' }
</script>
<style scoped>
.message-composer{margin:0 clamp(20px,4vw,54px) 22px;border:1px solid #d9dcd8;border-radius:12px;background:#fff;overflow:hidden;box-shadow:0 13px 36px rgba(42,49,44,.08)}.message-composer:focus-within{border-color:#b98b61}.message-composer textarea{width:100%;min-height:82px;padding:14px 16px;border:0;outline:0;resize:none;box-sizing:border-box;color:#282e2a;font:13px/1.65 inherit}.message-composer footer{min-height:46px;padding:8px 10px;border-top:1px solid #eceeeb;display:flex;align-items:center;gap:12px;background:#fafbf9}.message-composer footer label,.message-composer footer button{padding:7px 10px;border-radius:7px;font-size:11px;font-weight:650;cursor:pointer}.message-composer footer label{border:1px solid #d8dcd7;background:#fff}.message-composer footer label input{display:none}.message-composer footer span{margin-left:auto;color:#929791;font-size:9px}.message-composer footer button{border:0;background:#282e2a;color:#fff}.message-composer footer button:disabled{opacity:.4;cursor:not-allowed}.draft-files{padding:10px 11px 0;display:grid;gap:5px}.draft-files article{padding:8px 9px;border-radius:7px;display:flex;align-items:center;gap:6px;background:#f4f5f3}.draft-files article div{min-width:0;display:grid;gap:2px;flex:1}.draft-files strong{overflow:hidden;font-size:11px;text-overflow:ellipsis;white-space:nowrap}.draft-files small{color:#69706b;font-size:9px}.draft-files small.failed{color:#a53e38}.draft-files small.uploaded{color:#327153}.draft-files button{border:0;background:transparent;color:#7d6044;font-size:10px;cursor:pointer}.composer-error{margin:0 12px 8px;color:#9b403a;font-size:10px}
</style>
