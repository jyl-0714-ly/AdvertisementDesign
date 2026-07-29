<template>
  <div ref="scroller" class="message-list">
    <div class="message-list__top">
      <button v-if="hasMore" type="button" :disabled="loadingOlder" @click="emit('load-older')">{{ loadingOlder ? '正在读取更早消息…' : '查看更早消息' }}</button>
      <span v-else-if="messages.length">已显示全部沟通记录</span>
    </div>
    <p v-if="error" class="message-error">{{ error }} <button type="button" @click="emit('load-older')">重试</button></p>
    <section v-if="!messages.length && !loadingOlder" class="message-empty"><strong>项目沟通从这里继续</strong><p>发送消息或资料后，沟通记录会按时间保存在当前项目中。</p></section>
    <article v-for="message in messages" :key="message.id" class="message-row" :class="{ customer: !isTeam(message) }">
      <div class="identity">{{ isTeam(message) ? '项目服务团队' : (message.displayIdentity || '我') }}</div>
      <div v-if="message.correctionMessageId != null" class="correction-note">这是对较早消息的更正</div>
      <div class="bubble">
        <p v-if="message.content">{{ message.content }}</p>
        <div v-if="message.attachments?.length" class="message-files"><FileAttachment v-for="file in message.attachments" :key="file.id" :attachment="file" /></div>
      </div>
      <div class="message-meta"><time>{{ formatTime(message.sentAt) }}</time><span v-if="isCorrected(message.id)">已有更正</span><button v-else-if="!isTeam(message) && message.correctionMessageId == null" type="button" @click="emit('correct', message.id)">更正此消息</button></div>
    </article>
  </div>
</template>
<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import type { ProjectMessage } from '@/modules/project/types'
import FileAttachment from './FileAttachment.vue'
const props = defineProps<{ messages: ProjectMessage[]; hasMore: boolean; loadingOlder: boolean; error: string | null }>()
const emit = defineEmits<{ 'load-older': []; correct: [messageId: number] }>()
const scroller = ref<HTMLElement | null>(null)
function isTeam(message: ProjectMessage) { return message.displayIdentity === '项目服务团队' }
function isCorrected(messageId: number) { return props.messages.some(item => item.correctionMessageId === messageId) }
function formatTime(value: string) { return value ? value.replace('T', ' ').slice(5, 16) : '' }
watch(() => props.messages.at(-1)?.id, async (_, previous) => { if (previous == null) { await nextTick(); scroller.value?.scrollTo({ top: scroller.value.scrollHeight }) } })
</script>
<style scoped>
.message-list{min-height:0;padding:20px clamp(22px,4vw,56px);overflow:auto;scrollbar-gutter:stable}.message-list__top{min-height:30px;display:grid;place-items:center;color:#999f99;font-size:10px}.message-list__top button,.message-error button{border:0;background:transparent;color:#8c5e33;font-size:11px;cursor:pointer}.message-empty{margin:15vh auto 0;max-width:360px;text-align:center;color:#737a75}.message-empty strong{color:#3a403c;font-size:14px}.message-empty p{font-size:12px;line-height:1.7}.message-error{margin:8px auto 18px;padding:9px 12px;border-radius:7px;background:#fdf2f2;color:#943d38;font-size:11px;text-align:center}.message-row{max-width:74%;margin:0 auto 22px 0;display:grid;gap:5px}.message-row.customer{margin-right:0;margin-left:auto;justify-items:end}.identity{color:#626963;font-size:10px;font-weight:700}.bubble{padding:12px 14px;border-radius:4px 14px 14px;background:#eef0ed;color:#282e2a}.customer .bubble{border:1px solid #ead9c7;border-radius:14px 4px 14px 14px;background:#fff7ed}.bubble p{margin:0;white-space:pre-wrap;font-size:13px;line-height:1.7;overflow-wrap:anywhere}.message-files{margin-top:9px;display:grid;gap:6px}.correction-note{color:#8c5e33;font-size:9px}.message-meta{display:flex;align-items:center;gap:8px;color:#9a9f9b;font-size:9px}.message-meta button{padding:0;border:0;background:transparent;color:#8c5e33;font:inherit;cursor:pointer}.message-meta span{color:#6f766f}
</style>
