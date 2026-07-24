<template>
  <section class="chat-workspace">
    <aside class="brief-panel">
      <button class="back-button" type="button" @click="router.push('/reception')">← 返回客户池</button>
      <div v-if="reception" class="client-profile">
        <div class="profile-head"><div class="avatar">{{ reception.customerName.slice(0, 1) }}</div><div><span>当前接待</span><h1>{{ reception.customerName }}</h1></div></div>
        <div class="score-line"><span>需求匹配度</span><strong>{{ reception.matchScore }}%</strong></div>
        <div class="summary-block"><span>需求类型</span><strong>{{ reception.projectType }}</strong></div>
        <div class="summary-grid"><div><span>行业</span><strong>{{ reception.industry }}</strong></div><div><span>预算</span><strong>{{ reception.budgetRange }}</strong></div><div><span>项目周期</span><strong>{{ reception.projectCycle }}</strong></div></div>
        <div class="description"><span>需求摘要</span><p>{{ reception.requirementDescription }}</p></div>
        <div class="match-reason"><span>匹配依据</span><p>{{ reception.matchReason }}</p></div>
      </div>
      <div v-else-if="loading" class="side-state">正在加载需求…</div>
    </aside>

    <main class="conversation-panel">
      <header class="conversation-head">
        <div><span class="online-dot"></span><div><strong>{{ reception?.customerName || '客户会话' }}</strong><small>人工接待 · 会话已建立</small></div></div>
        <span class="secure-label">需求沟通</span>
      </header>
      <div ref="messageList" class="message-list">
        <div v-if="error" class="message-error">{{ error }} <button type="button" @click="initialize">重试</button></div>
        <div v-if="loading" class="message-state">正在载入沟通记录…</div>
        <div v-else-if="!messages.length" class="message-state"><strong>开始首次沟通</strong><span>向客户确认需求细节、交付范围与时间安排。</span></div>
        <article v-for="message in messages" :key="message.id" class="message-row" :class="{ mine: message.senderRole === 'DESIGNER' }">
          <div class="message-avatar">{{ message.senderName.slice(0, 1) }}</div>
          <div class="message-body"><div class="message-meta"><strong>{{ message.senderRole === 'DESIGNER' ? '我' : message.senderName }}</strong><time>{{ formatTime(message.createdAt) }}</time></div><p>{{ message.content }}</p></div>
        </article>
      </div>
      <form class="composer" @submit.prevent="send">
        <textarea v-model="draft" maxlength="2000" rows="3" placeholder="输入消息，确认客户需求细节…" @keydown.meta.enter.prevent="send" @keydown.ctrl.enter.prevent="send"></textarea>
        <div class="composer-footer"><span>⌘ / Ctrl + Enter 发送</span><button type="submit" :disabled="sending || !draft.trim()">{{ sending ? '发送中…' : '发送消息' }}</button></div>
      </form>
    </main>
  </section>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getConsultantReception, listConsultantHumanMessages, sendConsultantHumanMessage } from '@/api'
import type { ConsultantHumanMessageVO, ConsultantReceptionVO } from '@/models'

const route = useRoute()
const router = useRouter()
const reception = ref<ConsultantReceptionVO | null>(null)
const messages = ref<ConsultantHumanMessageVO[]>([])
const draft = ref('')
const loading = ref(true)
const sending = ref(false)
const error = ref('')
const messageList = ref<HTMLElement | null>(null)

function formatTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit' }).format(date)
}
async function scrollLatest() { await nextTick(); if (messageList.value) messageList.value.scrollTop = messageList.value.scrollHeight }
async function initialize() {
  const intakeId = Number(route.params.intakeId)
  if (!Number.isFinite(intakeId)) { error.value = '无效的客户接待记录'; loading.value = false; return }
  loading.value = true; error.value = ''
  try {
    reception.value = await getConsultantReception(intakeId)
    messages.value = await listConsultantHumanMessages(reception.value.humanChatId)
    await scrollLatest()
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '客户会话加载失败' }
  finally { loading.value = false }
}
async function send() {
  const content = draft.value.trim()
  if (!content || sending.value || !reception.value) return
  sending.value = true; error.value = ''
  try {
    const message = await sendConsultantHumanMessage(reception.value.humanChatId, content)
    draft.value = ''; messages.value.push(message); await scrollLatest()
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '消息发送失败，请稍后重试' }
  finally { sending.value = false }
}
onMounted(initialize)
</script>

<style scoped>
.chat-workspace { height: 100%; display: grid; grid-template-columns: 340px minmax(0, 1fr); background: #fff; }
.brief-panel { min-width: 0; padding: 24px 24px 32px; overflow-y: auto; border-right: 1px solid var(--s-border); background: #f8f9fa; }
.back-button { padding: 0; border: 0; background: transparent; color: var(--s-muted); font: inherit; font-size: 12px; cursor: pointer; }.back-button:hover { color: var(--s-ink); }
.client-profile { margin-top: 32px; }.profile-head { display: flex; align-items: center; gap: 13px; }.avatar { width: 48px; height: 48px; display: grid; place-items: center; border-radius: 14px; background: #202631; color: #fff; font-weight: 650; }.profile-head span,.summary-block span,.summary-grid span,.description span,.match-reason span { color: var(--s-muted); font-size: 11px; }.profile-head h1 { margin: 4px 0 0; font-size: 20px; letter-spacing: -.02em; }
.score-line { margin: 28px 0; padding: 13px 0; display: flex; justify-content: space-between; align-items: center; border-top: 1px solid var(--s-border); border-bottom: 1px solid var(--s-border); color: var(--s-muted); font-size: 12px; }.score-line strong { color: var(--s-accent); font-size: 18px; }
.summary-block { display: grid; gap: 5px; }.summary-block strong { font-size: 18px; }.summary-grid { margin-top: 20px; display: grid; grid-template-columns: 1fr 1fr; gap: 18px 12px; }.summary-grid div:last-child { grid-column: 1 / -1; }.summary-grid div { display: grid; gap: 5px; }.summary-grid strong { font-size: 13px; }
.description,.match-reason { margin-top: 26px; }.description p,.match-reason p { margin: 8px 0 0; color: #3e4856; font-size: 13px; line-height: 1.75; }.match-reason { padding: 14px; border-radius: 10px; background: #fff; }.match-reason p { font-size: 12px; }.side-state { margin-top: 40px; color: var(--s-muted); font-size: 13px; }
.conversation-panel { min-width: 0; min-height: 0; display: grid; grid-template-rows: 66px minmax(0, 1fr) auto; }.conversation-head { padding: 0 24px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--s-border); }.conversation-head > div { display: flex; align-items: center; gap: 10px; }.conversation-head div div { display: grid; gap: 2px; }.conversation-head strong { font-size: 14px; }.conversation-head small { color: var(--s-muted); font-size: 11px; }.online-dot { width: 8px; height: 8px; border-radius: 50%; background: #20a06b; box-shadow: 0 0 0 4px rgba(32,160,107,.1); }.secure-label { padding: 6px 9px; border-radius: 7px; background: #f6f7f8; color: var(--s-muted); font-size: 11px; }
.message-list { min-height: 0; padding: 28px max(30px, calc((100% - 760px)/2)); overflow-y: auto; background: #fff; }.message-row { margin-bottom: 25px; display: flex; gap: 11px; align-items: flex-start; }.message-row.mine { flex-direction: row-reverse; }.message-avatar { width: 30px; height: 30px; flex: none; display: grid; place-items: center; border-radius: 9px; background: #edf0f3; color: #424d5c; font-size: 11px; font-weight: 650; }.mine .message-avatar { background: #202631; color: white; }.message-body { max-width: min(72%, 610px); }.message-meta { margin-bottom: 6px; display: flex; align-items: center; gap: 8px; }.mine .message-meta { justify-content: flex-end; }.message-meta strong { font-size: 11px; }.message-meta time { color: #9aa2ae; font-size: 10px; }.message-body p { margin: 0; padding: 11px 14px; border-radius: 4px 12px 12px 12px; background: #f3f4f6; color: #252d38; font-size: 14px; line-height: 1.7; white-space: pre-wrap; }.mine .message-body p { border-radius: 12px 4px 12px 12px; background: #242a35; color: #fff; }.message-state { height: 100%; min-height: 220px; display: grid; place-content: center; justify-items: center; gap: 7px; color: var(--s-muted); font-size: 12px; }.message-state strong { color: var(--s-ink); font-size: 15px; }.message-error { margin-bottom: 18px; padding: 10px 12px; border-radius: 8px; background: #fff3f1; color: #9f3a3a; font-size: 12px; }.message-error button { border: 0; background: transparent; color: inherit; text-decoration: underline; cursor: pointer; }
.composer { margin: 0 max(24px, calc((100% - 780px)/2)) 22px; border: 1px solid rgba(15,23,42,.12); border-radius: 12px; background: #fff; box-shadow: 0 10px 26px rgba(15,23,42,.07); overflow: hidden; }.composer textarea { width: 100%; min-height: 78px; padding: 14px 16px 8px; resize: none; border: 0; outline: none; box-sizing: border-box; color: var(--s-ink); font: inherit; font-size: 14px; line-height: 1.55; }.composer textarea::placeholder { color: #a0a7b1; }.composer-footer { padding: 8px 10px 10px 15px; display: flex; justify-content: space-between; align-items: center; }.composer-footer span { color: #a0a7b1; font-size: 10px; }.composer-footer button { padding: 8px 14px; border: 0; border-radius: 8px; background: var(--s-accent); color: white; font: inherit; font-size: 12px; font-weight: 600; cursor: pointer; }.composer-footer button:disabled { opacity: .45; cursor: default; }
@media (max-width: 760px) { .chat-workspace { grid-template-columns: 1fr; }.brief-panel { display: none; }.message-list { padding-inline: 18px; }.composer { margin-inline: 14px; } }
</style>
