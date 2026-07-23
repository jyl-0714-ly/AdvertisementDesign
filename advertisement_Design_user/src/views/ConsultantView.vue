<template>
  <div class="consultant-page">
    <aside class="consultant-sessions" aria-label="顾问会话列表">
      <header class="session-heading">
        <div>
          <small>AD CLIENT SERVICE</small>
          <h1>在线咨询</h1>
        </div>
        <span class="session-count">1</span>
      </header>

      <div class="session-search" aria-hidden="true">
        <el-icon><Search /></el-icon>
        <span>搜索会话</span>
      </div>

      <div class="session-section-label">当前会话</div>
      <button type="button" class="session-item active">
        <span class="company-avatar">AD<i></i></span>
        <span class="session-copy">
          <span><strong>AD有限公司 · 项目顾问</strong><time>刚刚</time></span>
          <small>{{ sessionPreview }}</small>
        </span>
      </button>

      <div class="service-note">
        <span>服务时间</span>
        <strong>工作日 09:00–18:00</strong>
        <p>非服务时间留言，我们将在上线后尽快回复。</p>
      </div>
    </aside>

    <main class="consultant-chat">
      <header class="chat-heading">
        <span class="company-avatar large">AD<i></i></span>
        <div>
          <h2>AD有限公司 · 项目顾问</h2>
          <p><span></span>在线 · 为您提供设计需求咨询</p>
        </div>
        <div class="chat-company-mark">AD CREATIVE</div>
      </header>

      <section ref="messageArea" class="message-area" aria-live="polite">
        <div class="conversation-date">今天</div>

        <article v-for="message in messages" :key="message.id" class="message-row" :class="{ mine: message.sender === 'customer' }">
          <span v-if="message.sender === 'consultant'" class="company-avatar message-avatar">AD</span>
          <div class="message-content">
            <div v-if="message.sender === 'consultant'" class="sender-name">AD有限公司 · 项目顾问</div>
            <div class="message-bubble">
              <template v-if="message.kind === 'welcome'">
                <p>您好，欢迎来到 AD 设计。</p>
                <p>我们专注于：</p>
                <ul>
                  <li>品牌视觉设计</li>
                  <li>宣传物料设计</li>
                  <li>活动物料设计</li>
                  <li>商业空间视觉设计</li>
                </ul>
                <p>为了更好地帮助您，请告诉我们您的设计需求。</p>
              </template>
              <template v-else>{{ message.text }}</template>
            </div>
            <time>{{ message.time }}</time>
          </div>
          <span v-if="message.sender === 'customer'" class="customer-avatar">{{ customerInitial }}</span>
        </article>
      </section>

      <footer class="message-composer">
        <div class="quick-consult">
          <span>快捷咨询</span>
          <button v-for="item in quickOptions" :key="item" type="button" @click="sendQuickMessage(item)">{{ item }}</button>
        </div>
        <div class="composer-box">
          <textarea v-model="draft" rows="3" placeholder="请描述您的设计需求，例如项目类型、使用场景和期望时间…" @keydown.enter.exact.prevent="sendMessage"></textarea>
          <div class="composer-actions">
            <span>Enter 发送 · Shift + Enter 换行</span>
            <button type="button" :disabled="!draft.trim()" @click="sendMessage">
              发送消息
              <el-icon><Position /></el-icon>
            </button>
          </div>
        </div>
      </footer>
    </main>

    <aside class="consultant-profile" aria-label="顾问信息">
      <div class="profile-cover">
        <span>AD CREATIVE STUDIO</span>
        <strong>让设计成为<br />品牌增长的语言</strong>
        <small>BRAND · PRINT · SPACE · CAMPAIGN</small>
      </div>

      <section class="profile-card company-card">
        <span class="profile-logo">AD</span>
        <div>
          <small>服务机构</small>
          <h3>AD有限公司</h3>
          <p>品牌与商业视觉设计团队</p>
        </div>
      </section>

      <section class="profile-card">
        <header>项目顾问</header>
        <div class="consultant-person">
          <span>顾</span>
          <div><strong>在线项目顾问</strong><small><i></i>当前在线</small></div>
        </div>
        <p>从需求梳理、服务范围到合作流程，为您的设计项目提供前期咨询。</p>
      </section>

      <section class="profile-card service-scope">
        <header>服务范围</header>
        <div><span>品牌视觉</span><span>宣传设计</span><span>包装设计</span><span>活动设计</span><span>空间视觉</span></div>
      </section>

      <section class="privacy-note">
        <el-icon><Lock /></el-icon>
        <p><strong>企业会话保护</strong><span>您的需求内容仅用于项目咨询与服务沟通。</span></p>
      </section>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { Lock, Position, Search } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

type ConsultantMessage = {
  id: number
  sender: 'consultant' | 'customer'
  text: string
  time: string
  kind?: 'welcome'
}

const auth = useAuthStore()
const draft = ref('')
const messageArea = ref<HTMLElement | null>(null)
let messageSequence = 1
const quickOptions = ['品牌升级', '宣传设计', '包装设计', '活动设计', '其他需求']
const messages = ref<ConsultantMessage[]>([
  { id: 1, sender: 'consultant', text: '', time: currentTime(), kind: 'welcome' }
])

const customerInitial = computed(() => auth.user?.nickname?.slice(0, 1) || '我')
const sessionPreview = computed(() => messages.value.at(-1)?.text || '欢迎来到 AD 设计，请告诉我们您的需求。')

function currentTime() {
  return new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date())
}

async function appendCustomerMessage(text: string) {
  messages.value.push({ id: ++messageSequence, sender: 'customer', text, time: currentTime() })
  await nextTick()
  messageArea.value?.scrollTo({ top: messageArea.value.scrollHeight, behavior: 'smooth' })
}

function sendQuickMessage(type: string) {
  void appendCustomerMessage(`我想咨询${type}相关服务。`)
}

function sendMessage() {
  const text = draft.value.trim()
  if (!text) return
  draft.value = ''
  void appendCustomerMessage(text)
}
</script>

<style scoped>
.consultant-page {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-columns: 280px minmax(520px, 1fr) 320px;
  overflow: hidden;
  background: #f6f7f9;
  color: #111827;
  font-family: 'Inter', 'PingFang SC', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.consultant-sessions,
.consultant-profile {
  min-width: 0;
  overflow-y: auto;
  background: #f8f9fb;
}
.consultant-sessions { border-right: 1px solid rgba(15, 23, 42, .07); }
.consultant-profile { padding-bottom: 20px; border-left: 1px solid rgba(15, 23, 42, .07); }

.session-heading {
  min-height: 86px;
  padding: 20px 20px 15px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}
.session-heading small { color: #D97745; font-size: 10px; font-weight: 700; letter-spacing: .11em; }
.session-heading h1 { margin: 5px 0 0; font-size: 22px; font-weight: 600; letter-spacing: -.025em; }
.session-count { min-width: 24px; height: 24px; padding: 0 7px; border-radius: 12px; display: grid; place-items: center; background: #eceff3; color: #64748b; font-size: 12px; }

.session-search {
  height: 38px;
  margin: 0 14px 19px;
  padding: 0 12px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  background: #eef1f4;
  color: #94a3b8;
  font-size: 13px;
}
.session-section-label { padding: 0 20px 8px; color: #94a3b8; font-size: 11px; font-weight: 600; }
.session-item {
  width: calc(100% - 20px);
  min-height: 82px;
  margin: 0 10px;
  padding: 13px 12px;
  border: 1px solid transparent;
  border-radius: 12px;
  display: flex;
  align-items: flex-start;
  gap: 11px;
  text-align: left;
  background: transparent;
  cursor: pointer;
}
.session-item.active { border-color: rgba(217, 119, 69, .17); background: #fffaf7; box-shadow: 0 5px 18px rgba(15, 23, 42, .045); }
.company-avatar {
  width: 42px;
  height: 42px;
  flex: none;
  border-radius: 12px;
  position: relative;
  display: grid;
  place-items: center;
  background: #111827;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: .04em;
}
.company-avatar i { width: 10px; height: 10px; border: 2px solid #fff; border-radius: 50%; position: absolute; right: -2px; bottom: -2px; background: #22a06b; }
.session-copy { min-width: 0; flex: 1; display: grid; gap: 8px; }
.session-copy > span { display: flex; align-items: center; justify-content: space-between; gap: 6px; }
.session-copy strong { overflow: hidden; font-size: 14px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.session-copy time { flex: none; color: #94a3b8; font-size: 10px; }
.session-copy small { overflow: hidden; color: #64748b; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.service-note { margin: 28px 20px 0; padding-top: 18px; border-top: 1px solid rgba(15, 23, 42, .06); display: grid; gap: 5px; }
.service-note span { color: #94a3b8; font-size: 11px; }
.service-note strong { font-size: 13px; font-weight: 600; }
.service-note p { margin: 0; color: #94a3b8; font-size: 11px; line-height: 1.6; }

.consultant-chat { min-width: 0; min-height: 0; display: grid; grid-template-rows: 78px minmax(0, 1fr) auto; background: #fff; }
.chat-heading { padding: 0 26px; border-bottom: 1px solid rgba(15, 23, 42, .065); display: flex; align-items: center; gap: 13px; }
.company-avatar.large { width: 44px; height: 44px; }
.chat-heading > div:nth-child(2) { min-width: 0; flex: 1; }
.chat-heading h2 { margin: 0 0 5px; font-size: 17px; font-weight: 600; letter-spacing: -.015em; }
.chat-heading p { margin: 0; display: flex; align-items: center; gap: 6px; color: #64748b; font-size: 12px; }
.chat-heading p span { width: 7px; height: 7px; border-radius: 50%; background: #22a06b; }
.chat-company-mark { color: #c4cad3; font-size: 10px; font-weight: 700; letter-spacing: .13em; }

.message-area { min-height: 0; padding: 28px 34px 34px; overflow-y: auto; background: #fafbfc; }
.conversation-date { margin-bottom: 25px; color: #94a3b8; font-size: 11px; text-align: center; }
.message-row { max-width: 720px; margin-bottom: 24px; display: flex; align-items: flex-start; gap: 11px; }
.message-row.mine { margin-left: auto; justify-content: flex-end; }
.message-avatar { width: 36px; height: 36px; border-radius: 10px; font-size: 10px; }
.customer-avatar { width: 36px; height: 36px; flex: none; border-radius: 10px; display: grid; place-items: center; background: #e9edf2; color: #475569; font-size: 12px; font-weight: 600; }
.message-content { max-width: min(620px, calc(100% - 50px)); }
.sender-name { margin: 0 0 6px 2px; color: #64748b; font-size: 12px; }
.message-bubble { padding: 14px 17px; border: 1px solid rgba(15, 23, 42, .07); border-radius: 5px 12px 12px 12px; background: #fff; color: #263244; font-size: 14px; line-height: 1.7; box-shadow: 0 5px 18px rgba(15, 23, 42, .055); }
.mine .message-bubble { border-color: rgba(217, 119, 69, .15); border-radius: 12px 5px 12px 12px; background: #fff7f2; color: #3f2c22; box-shadow: none; }
.message-bubble p { margin: 0 0 8px; }
.message-bubble p:last-child { margin-bottom: 0; }
.message-bubble ul { margin: 5px 0 11px; padding: 10px 14px 10px 30px; border-radius: 10px; background: #f7f8fa; }
.message-bubble li { padding: 2px 0; }
.message-content time { display: block; margin-top: 5px; color: #a2acb9; font-size: 10px; }
.mine .message-content time { text-align: right; }

.message-composer { padding: 11px 26px 18px; border-top: 1px solid rgba(15, 23, 42, .065); background: #fff; }
.quick-consult { margin-bottom: 10px; display: flex; align-items: center; gap: 8px; overflow-x: auto; }
.quick-consult > span { margin-right: 2px; color: #94a3b8; font-size: 11px; white-space: nowrap; }
.quick-consult button { height: 30px; padding: 0 12px; border: 1px solid rgba(15, 23, 42, .09); border-radius: 15px; background: #fff; color: #475569; cursor: pointer; font: inherit; font-size: 12px; white-space: nowrap; transition: border-color .16s, color .16s, background .16s; }
.quick-consult button:hover { border-color: rgba(217, 119, 69, .32); background: #fff8f4; color: #b85f32; }
.composer-box { padding: 10px 12px 9px; border: 1px solid rgba(15, 23, 42, .1); border-radius: 12px; background: #f9fafb; transition: border-color .16s, box-shadow .16s; }
.composer-box:focus-within { border-color: rgba(217, 119, 69, .48); box-shadow: 0 0 0 3px rgba(217, 119, 69, .07); }
.composer-box textarea { width: 100%; min-height: 54px; padding: 0; border: 0; outline: 0; resize: none; background: transparent; color: #1f2937; font: inherit; font-size: 14px; line-height: 1.6; }
.composer-box textarea::placeholder { color: #a2acb9; }
.composer-actions { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.composer-actions > span { color: #a2acb9; font-size: 10px; }
.composer-actions button { height: 34px; padding: 0 14px; border: 0; border-radius: 9px; display: inline-flex; align-items: center; gap: 7px; background: #D97745; color: #fff; cursor: pointer; font: inherit; font-size: 13px; font-weight: 600; box-shadow: 0 4px 12px rgba(217, 119, 69, .18); }
.composer-actions button:disabled { background: #d8dce2; box-shadow: none; cursor: not-allowed; }

.profile-cover { height: 148px; margin: 14px; padding: 19px; border-radius: 12px; position: relative; overflow: hidden; display: flex; flex-direction: column; justify-content: space-between; background: linear-gradient(145deg, #111827, #293240); color: #fff; box-shadow: 0 10px 25px rgba(15, 23, 42, .13); }
.profile-cover::after { content: ''; width: 90px; height: 90px; border: 1px solid rgba(255, 255, 255, .13); position: absolute; right: -28px; top: -28px; transform: rotate(18deg); }
.profile-cover span, .profile-cover small { color: rgba(255, 255, 255, .55); font-size: 9px; font-weight: 600; letter-spacing: .12em; }
.profile-cover strong { font-size: 19px; font-weight: 600; line-height: 1.45; letter-spacing: -.015em; }
.profile-card { margin: 0 14px 12px; padding: 16px; border: 1px solid rgba(15, 23, 42, .07); border-radius: 12px; background: #fff; box-shadow: 0 4px 14px rgba(15, 23, 42, .025); }
.profile-card > header { margin-bottom: 13px; color: #64748b; font-size: 12px; font-weight: 600; }
.company-card { display: flex; align-items: center; gap: 11px; }
.profile-logo { width: 40px; height: 40px; flex: none; border-radius: 11px; display: grid; place-items: center; background: #111827; color: #fff; font-size: 11px; font-weight: 700; }
.company-card > div { min-width: 0; }
.company-card small { color: #94a3b8; font-size: 10px; }
.company-card h3 { margin: 2px 0; font-size: 14px; font-weight: 600; }
.company-card p, .profile-card > p { margin: 0; color: #7b8797; font-size: 11px; line-height: 1.6; }
.consultant-person { margin-bottom: 11px; display: flex; align-items: center; gap: 10px; }
.consultant-person > span { width: 38px; height: 38px; border-radius: 50%; display: grid; place-items: center; background: #edf0f3; color: #475569; font-size: 12px; font-weight: 600; }
.consultant-person > div { display: grid; gap: 3px; }
.consultant-person strong { font-size: 13px; font-weight: 600; }
.consultant-person small { display: flex; align-items: center; gap: 5px; color: #22a06b; font-size: 10px; }
.consultant-person i { width: 6px; height: 6px; border-radius: 50%; background: #22a06b; }
.service-scope > div { display: flex; flex-wrap: wrap; gap: 7px; }
.service-scope span { padding: 5px 9px; border-radius: 7px; background: #f2f4f6; color: #64748b; font-size: 11px; }
.privacy-note { margin: 18px 20px; display: flex; align-items: flex-start; gap: 9px; color: #94a3b8; }
.privacy-note .el-icon { margin-top: 2px; }
.privacy-note p { margin: 0; display: grid; gap: 3px; }
.privacy-note strong { color: #64748b; font-size: 11px; font-weight: 600; }
.privacy-note span { font-size: 10px; line-height: 1.5; }

button:focus-visible, textarea:focus-visible { outline: 2px solid rgba(217, 119, 69, .6); outline-offset: 2px; }

@media (max-width: 1180px) {
  .consultant-page { grid-template-columns: 250px minmax(500px, 1fr); }
  .consultant-profile { display: none; }
}
@media (max-width: 760px) {
  .consultant-page { grid-template-columns: 1fr; }
  .consultant-sessions { display: none; }
  .message-area { padding: 22px 18px; }
  .message-composer { padding: 10px 14px 14px; }
  .chat-company-mark, .composer-actions > span { display: none; }
}
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { scroll-behavior: auto !important; transition-duration: .01ms !important; }
}
</style>
