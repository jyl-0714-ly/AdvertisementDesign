<template>
  <div class="consultant-page">
    <aside class="consultant-sessions" aria-label="服务会话列表">
      <header class="session-heading">
        <div>
          <small>AD CLIENT SERVICE</small>
          <h1>在线咨询</h1>
        </div>
        <span class="session-count" :aria-label="`${sessions.length} 个会话`">{{ sessions.length }}</span>
      </header>

      <div class="session-search" aria-hidden="true">
        <el-icon><Search /></el-icon>
        <span>搜索会话</span>
      </div>

      <div class="session-section-label">当前会话</div>
      <button
        v-for="session in sessions"
        :key="session.id"
        type="button"
        class="session-item"
        :class="{ active: activeSession === session.id }"
        :aria-current="activeSession === session.id ? 'true' : undefined"
        @click="switchSession(session.id)"
      >
        <span v-if="session.id === 'consultant'" class="company-avatar">AD<i></i></span>
        <span v-else class="designer-avatar compact">
          <img v-if="matchedDesigner?.avatar" :src="matchedDesigner.avatar" alt="" />
          <span v-else>{{ designerInitial }}</span><i :class="{ offline: !matchedDesigner?.online }"></i>
        </span>
        <span class="session-copy">
          <span><strong>{{ session.title }}</strong><time>{{ session.time }}</time></span>
          <small>{{ session.preview }}</small>
          <em v-if="session.label">{{ session.label }}</em>
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
        <span v-if="activeSession === 'consultant'" class="company-avatar large">AD<i></i></span>
        <span v-else class="designer-avatar large">
          <img v-if="matchedDesigner?.avatar" :src="matchedDesigner.avatar" alt="" />
          <span v-else>{{ designerInitial }}</span><i :class="{ offline: !matchedDesigner?.online }"></i>
        </span>
        <div>
          <h2>{{ activeTitle }}</h2>
          <p><span :class="{ offline: activeSession === 'human' && !matchedDesigner?.online }"></span>{{ activeStatus }}</p>
        </div>
        <div class="chat-company-mark">{{ activeSession === 'human' ? 'HUMAN SERVICE' : 'AD CREATIVE' }}</div>
      </header>

      <section ref="messageArea" class="message-area" aria-live="polite" aria-label="会话消息">
        <div class="conversation-date">今天</div>

        <article v-for="message in activeMessages" :key="message.id" class="message-row" :class="{ mine: message.sender === 'customer' }">
          <span v-if="message.sender !== 'customer' && activeSession === 'consultant'" class="company-avatar message-avatar">AD</span>
          <span v-else-if="message.sender !== 'customer'" class="designer-avatar message-avatar">
            <img v-if="matchedDesigner?.avatar" :src="matchedDesigner.avatar" alt="" />
            <span v-else>{{ designerInitial }}</span>
          </span>
          <div class="message-content" :class="{ wide: message.kind === 'intake' || message.kind === 'summary' }">
            <div v-if="message.sender !== 'customer'" class="sender-name">{{ senderName }}</div>

            <div v-if="message.kind === 'intake'" class="intake-card">
              <header>
                <span>项目需求登记</span>
                <small>完整填写后，我们将整理为正式需求摘要</small>
              </header>
              <form class="intake-form" @submit.prevent="prepareSummary">
                <label>
                  <span>项目类型 <b>*</b></span>
                  <select v-model="intake.projectType" required aria-required="true">
                    <option value="" disabled>请选择设计服务</option>
                    <option v-for="option in projectTypes" :key="option" :value="option">{{ option }}</option>
                  </select>
                </label>
                <label>
                  <span>所属行业 <b>*</b></span>
                  <input v-model.trim="intake.industry" required maxlength="50" placeholder="例如：餐饮、科技、零售" />
                </label>
                <label class="full-field">
                  <span>需求说明 <b>*</b></span>
                  <textarea v-model.trim="intake.requirementDescription" required maxlength="1000" rows="4" placeholder="请说明使用场景、设计内容、目标受众及期望风格"></textarea>
                  <small>{{ intake.requirementDescription.length }}/1000</small>
                </label>
                <label>
                  <span>预算范围 <b>*</b></span>
                  <select v-model="intake.budgetRange" required aria-required="true">
                    <option value="" disabled>请选择预算区间</option>
                    <option v-for="option in budgetRanges" :key="option" :value="option">{{ option }}</option>
                  </select>
                </label>
                <label>
                  <span>项目周期 <b>*</b></span>
                  <select v-model="intake.projectCycle" required aria-required="true">
                    <option value="" disabled>请选择期望周期</option>
                    <option v-for="option in projectCycles" :key="option" :value="option">{{ option }}</option>
                  </select>
                </label>
                <div class="form-note"><el-icon><Lock /></el-icon>需求仅用于服务评估及设计师匹配</div>
                <button type="submit" class="primary-action">生成需求摘要</button>
              </form>
            </div>

            <div v-else-if="message.kind === 'summary'" class="summary-card">
              <header>
                <div><small>REQUIREMENT BRIEF</small><h3>项目需求摘要</h3></div>
                <span>{{ intakeResult ? '已交接' : '待确认' }}</span>
              </header>
              <dl>
                <div><dt>项目类型</dt><dd>{{ intake.projectType }}</dd></div>
                <div><dt>所属行业</dt><dd>{{ intake.industry }}</dd></div>
                <div class="summary-wide"><dt>核心需求</dt><dd>{{ intake.requirementDescription }}</dd></div>
                <div><dt>预算范围</dt><dd>{{ intake.budgetRange }}</dd></div>
                <div><dt>项目周期</dt><dd>{{ intake.projectCycle }}</dd></div>
              </dl>
              <div v-if="matchError" class="match-error" role="alert">
                <strong>暂未完成匹配</strong><span>{{ matchError }}</span>
              </div>
              <footer>
                <template v-if="!intakeResult">
                  <button type="button" class="secondary-action" :disabled="isMatching" @click="editIntake">修改需求</button>
                  <button type="button" class="primary-action" :disabled="isMatching" @click="submitIntake">
                    <span v-if="isMatching" class="button-spinner" aria-hidden="true"></span>
                    {{ isMatching ? '正在匹配适合您的设计师...' : '提交设计师' }}
                  </button>
                </template>
                <span v-else class="handoff-state">需求已交接给 {{ matchedDesigner?.nickname }}，请在人工服务会话继续沟通。</span>
              </footer>
            </div>

            <div v-else class="message-bubble">
              <template v-if="message.kind === 'welcome'">
                <p>您好，欢迎来到 AD 设计。</p>
                <p>我们将先为您梳理项目范围，再安排适合的设计师接洽。请填写下方需求信息，也可以随时在输入框补充说明。</p>
              </template>
              <template v-else>{{ message.text }}</template>
            </div>
            <time v-if="message.kind !== 'intake'">{{ message.time }}</time>
          </div>
          <span v-if="message.sender === 'customer'" class="customer-avatar">{{ customerInitial }}</span>
        </article>
      </section>

      <footer class="message-composer">
        <div v-if="activeSession === 'consultant'" class="quick-consult">
          <span>快捷咨询</span>
          <button v-for="item in quickOptions" :key="item" type="button" @click="sendQuickMessage(item)">{{ item }}</button>
        </div>
        <div v-if="humanChatError" class="composer-error" role="alert">{{ humanChatError }}</div>
        <div class="composer-box">
          <textarea
            v-model="draft"
            rows="3"
            :placeholder="activeSession === 'human' ? `给${matchedDesigner?.nickname || '设计师'}留言…` : '补充您的设计需求或向项目顾问提问…'"
            aria-label="消息内容"
            @keydown.enter.exact.prevent="sendMessage"
          ></textarea>
          <div class="composer-actions">
            <span>Enter 发送 · Shift + Enter 换行</span>
            <button type="button" :disabled="!draft.trim() || isSendingHumanMessage" @click="sendMessage">
              {{ isSendingHumanMessage ? '发送中…' : '发送消息' }}
              <el-icon><Position /></el-icon>
            </button>
          </div>
        </div>
      </footer>
    </main>

    <aside class="consultant-profile" aria-label="服务信息">
      <template v-if="activeSession === 'consultant'">
        <div class="profile-cover">
          <span>AD CREATIVE STUDIO</span>
          <strong>让设计成为<br />品牌增长的语言</strong>
          <small>BRAND · PRINT · SPACE · CAMPAIGN</small>
        </div>
        <section class="profile-card company-card">
          <span class="profile-logo">AD</span>
          <div><small>服务机构</small><h3>AD有限公司</h3><p>品牌与商业视觉设计团队</p></div>
        </section>
        <section class="profile-card">
          <header>项目顾问</header>
          <div class="consultant-person"><span>顾</span><div><strong>在线项目顾问</strong><small><i></i>当前在线</small></div></div>
          <p>从需求梳理、服务范围到合作流程，为您的设计项目提供前期咨询。</p>
        </section>
        <section class="profile-card service-scope">
          <header>服务范围</header>
          <div><span>品牌视觉</span><span>宣传设计</span><span>包装设计</span><span>活动设计</span><span>空间视觉</span></div>
        </section>
      </template>
      <template v-else>
        <div class="designer-profile-cover">
          <span class="designer-avatar profile-avatar">
            <img v-if="matchedDesigner?.avatar" :src="matchedDesigner.avatar" :alt="`${matchedDesigner.nickname}的头像`" />
            <span v-else>{{ designerInitial }}</span>
          </span>
          <small>项目设计师</small>
          <h3>{{ matchedDesigner?.nickname }}</h3>
          <p><i :class="{ offline: !matchedDesigner?.online }"></i>{{ matchedDesigner?.online ? '当前在线' : '暂时离线，可留言' }}</p>
        </div>
        <section class="profile-card service-scope">
          <header>专业方向</header>
          <div><span v-for="item in matchedDesigner?.specialties || []" :key="item">{{ item }}</span></div>
        </section>
        <section class="profile-card handoff-card">
          <header>服务已衔接</header>
          <p>设计师已收到您的完整需求摘要，您可以在当前会话继续沟通细节。</p>
          <small v-if="intakeResult">需求编号：{{ intakeResult.intakeId }}</small>
        </section>
      </template>
      <section class="privacy-note">
        <el-icon><Lock /></el-icon>
        <p><strong>企业会话保护</strong><span>您的需求内容仅用于项目咨询与服务沟通。</span></p>
      </section>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref } from 'vue'
import { Lock, Position, Search } from '@element-plus/icons-vue'
import { createConsultantIntake, listConsultantHumanMessages, sendConsultantHumanMessage } from '@/api'
import type { ConsultantHumanMessageVO, ConsultantIntakeRequest, ConsultantIntakeResponse, MatchedDesignerVO } from '@/models'
import { useAuthStore } from '@/stores/auth'

type SessionId = 'consultant' | 'human'
type ConsultantMessage = {
  id: number
  sender: 'consultant' | 'designer' | 'customer'
  text: string
  time: string
  kind?: 'welcome' | 'intake' | 'summary'
}

const auth = useAuthStore()
const draft = ref('')
const messageArea = ref<HTMLElement | null>(null)
const activeSession = ref<SessionId>('consultant')
const isMatching = ref(false)
const isSendingHumanMessage = ref(false)
const matchError = ref('')
const humanChatError = ref('')
const matchedDesigner = ref<MatchedDesignerVO | null>(null)
const intakeResult = ref<ConsultantIntakeResponse | null>(null)
let messageSequence = 2

const projectTypes = ['品牌视觉设计', '宣传物料设计', '包装设计', '活动视觉设计', '商业空间视觉设计', '其他设计服务']
const budgetRanges = ['5,000 元以内', '5,000–10,000 元', '10,000–30,000 元', '30,000–50,000 元', '50,000 元以上', '待沟通评估']
const projectCycles = ['1 周以内', '1–2 周', '2–4 周', '1–2 个月', '2 个月以上', '待沟通确认']
const quickOptions = ['品牌升级', '宣传设计', '包装设计', '活动设计', '其他需求']
const intake = reactive<ConsultantIntakeRequest>({ projectType: '', industry: '', requirementDescription: '', budgetRange: '', projectCycle: '' })
const consultantMessages = ref<ConsultantMessage[]>([
  { id: 1, sender: 'consultant', text: '', time: currentTime(), kind: 'welcome' },
  { id: 2, sender: 'consultant', text: '', time: currentTime(), kind: 'intake' }
])
const humanMessages = ref<ConsultantMessage[]>([])

const customerInitial = computed(() => auth.user?.nickname?.slice(0, 1) || '我')
const designerInitial = computed(() => matchedDesigner.value?.nickname?.slice(0, 1) || '设')
const activeMessages = computed(() => activeSession.value === 'human' ? humanMessages.value : consultantMessages.value)
const senderName = computed(() => activeSession.value === 'human' ? matchedDesigner.value?.nickname || '项目设计师' : 'AD有限公司 · 项目顾问')
const activeTitle = computed(() => activeSession.value === 'human' ? `${matchedDesigner.value?.nickname || '项目设计师'} · 人工服务` : 'AD有限公司 · 项目顾问')
const activeStatus = computed(() => activeSession.value === 'human'
  ? `${matchedDesigner.value?.online ? '在线' : '离线'} · 已接收您的需求摘要`
  : '在线 · 为您提供设计需求咨询')
const sessions = computed(() => {
  const consultantLast = consultantMessages.value.at(-1)
  const items = [{ id: 'consultant' as SessionId, title: 'AD有限公司 · 项目顾问', preview: previewText(consultantLast, '请填写项目需求信息。'), time: consultantLast?.time || '刚刚', label: '' }]
  if (matchedDesigner.value) {
    const humanLast = humanMessages.value.at(-1)
    items.push({ id: 'human', title: matchedDesigner.value.nickname, preview: previewText(humanLast, '设计师已接收您的需求摘要。'), time: humanLast?.time || '刚刚', label: '人工服务' })
  }
  return items
})

function currentTime() {
  return new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date())
}

function previewText(message: ConsultantMessage | undefined, fallback: string) {
  if (!message) return fallback
  if (message.kind === 'summary') return '项目需求摘要已生成'
  if (message.kind === 'intake') return '请填写项目需求信息'
  if (message.kind === 'welcome') return '欢迎来到 AD 设计，请告诉我们您的需求。'
  return message.text || fallback
}

async function scrollToLatest() {
  await nextTick()
  messageArea.value?.scrollTo({ top: messageArea.value.scrollHeight, behavior: 'smooth' })
}

function switchSession(session: SessionId) {
  activeSession.value = session
  draft.value = ''
  humanChatError.value = ''
  if (session === 'human') void loadHumanMessages()
  void scrollToLatest()
}

function toHumanMessage(message: ConsultantHumanMessageVO): ConsultantMessage {
  return {
    id: message.id,
    sender: message.senderRole === 'CUSTOMER' ? 'customer' : 'designer',
    text: message.content,
    time: new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date(message.createdAt))
  }
}

async function loadHumanMessages() {
  const chatId = intakeResult.value?.humanChatId
  if (!chatId) return
  try {
    const messages = await listConsultantHumanMessages(chatId)
    if (messages.length) humanMessages.value = messages.map(toHumanMessage)
  } catch (error) {
    humanChatError.value = error instanceof Error ? error.message : '人工服务消息加载失败，请稍后重试。'
  }
}

async function appendCustomerMessage(text: string) {
  activeMessages.value.push({ id: ++messageSequence, sender: 'customer', text, time: currentTime() })
  await scrollToLatest()
}

function sendQuickMessage(type: string) {
  void appendCustomerMessage(`我想咨询${type}相关服务。`)
}

async function sendMessage() {
  const text = draft.value.trim()
  if (!text || isSendingHumanMessage.value) return
  if (activeSession.value !== 'human') {
    draft.value = ''
    await appendCustomerMessage(text)
    return
  }
  const chatId = intakeResult.value?.humanChatId
  if (!chatId) {
    humanChatError.value = '人工服务会话尚未建立，请先提交需求并完成设计师匹配。'
    return
  }
  isSendingHumanMessage.value = true
  humanChatError.value = ''
  try {
    const message = await sendConsultantHumanMessage(chatId, text)
    draft.value = ''
    humanMessages.value.push(toHumanMessage(message))
    await scrollToLatest()
  } catch (error) {
    humanChatError.value = error instanceof Error ? error.message : '消息发送失败，请稍后重试。'
  } finally {
    isSendingHumanMessage.value = false
  }
}

async function prepareSummary() {
  consultantMessages.value = consultantMessages.value.filter(message => message.kind !== 'intake' && message.kind !== 'summary')
  consultantMessages.value.push({
    id: ++messageSequence,
    sender: 'customer',
    text: `已提交${intake.projectType}项目需求，请协助确认。`,
    time: currentTime()
  })
  consultantMessages.value.push({ id: ++messageSequence, sender: 'consultant', text: '', time: currentTime(), kind: 'summary' })
  await scrollToLatest()
}

async function editIntake() {
  matchError.value = ''
  consultantMessages.value = consultantMessages.value.filter(message => message.kind !== 'summary')
  if (!consultantMessages.value.some(message => message.kind === 'intake')) {
    consultantMessages.value.push({ id: ++messageSequence, sender: 'consultant', text: '', time: currentTime(), kind: 'intake' })
  }
  await scrollToLatest()
}

async function submitIntake() {
  if (isMatching.value || intakeResult.value) return
  isMatching.value = true
  matchError.value = ''
  try {
    const result = await createConsultantIntake({ ...intake })
    intakeResult.value = result
    matchedDesigner.value = result.matchedDesigner
    const greetings = (result.greetingMessages || []).filter(text => text.trim()).slice(0, 2)
    humanMessages.value = greetings.map(text => ({
      id: ++messageSequence,
      sender: 'designer',
      text,
      time: currentTime()
    }))
    activeSession.value = 'human'
    await scrollToLatest()
  } catch (error) {
    matchError.value = error instanceof Error ? `${error.message}，请检查网络后重新提交。` : '服务暂时不可用，请稍后重新提交。'
    await scrollToLatest()
  } finally {
    isMatching.value = false
  }
}
</script>

<style scoped>
.consultant-page { height: 100%; min-height: 0; display: grid; grid-template-columns: 280px minmax(520px, 1fr) 320px; overflow: hidden; background: #f6f7f9; color: #111827; font-family: 'Inter', 'PingFang SC', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; }
.consultant-sessions, .consultant-profile { min-width: 0; overflow-y: auto; background: #f8f9fb; }
.consultant-sessions { border-right: 1px solid rgba(15, 23, 42, .07); }
.consultant-profile { padding-bottom: 20px; border-left: 1px solid rgba(15, 23, 42, .07); }
.session-heading { min-height: 86px; padding: 20px 20px 15px; display: flex; align-items: flex-end; justify-content: space-between; }
.session-heading small { color: #D97745; font-size: 10px; font-weight: 700; letter-spacing: .11em; }
.session-heading h1 { margin: 5px 0 0; font-size: 22px; font-weight: 600; letter-spacing: -.025em; }
.session-count { min-width: 24px; height: 24px; padding: 0 7px; border-radius: 12px; display: grid; place-items: center; background: #eceff3; color: #64748b; font-size: 12px; }
.session-search { height: 38px; margin: 0 14px 19px; padding: 0 12px; border-radius: 10px; display: flex; align-items: center; gap: 8px; background: #eef1f4; color: #94a3b8; font-size: 13px; }
.session-section-label { padding: 0 20px 8px; color: #94a3b8; font-size: 11px; font-weight: 600; }
.session-item { width: calc(100% - 20px); min-height: 82px; margin: 0 10px 6px; padding: 13px 12px; border: 1px solid transparent; border-radius: 12px; display: flex; align-items: flex-start; gap: 11px; text-align: left; background: transparent; cursor: pointer; }
.session-item.active { border-color: rgba(217, 119, 69, .17); background: #fffaf7; box-shadow: 0 5px 18px rgba(15, 23, 42, .045); }
.company-avatar, .designer-avatar { width: 42px; height: 42px; flex: none; border-radius: 12px; position: relative; display: grid; place-items: center; overflow: visible; background: #111827; color: #fff; font-size: 12px; font-weight: 700; letter-spacing: .04em; }
.designer-avatar { border-radius: 50%; background: #344052; }
.designer-avatar img { width: 100%; height: 100%; border-radius: inherit; object-fit: cover; }
.company-avatar i, .designer-avatar i { width: 10px; height: 10px; border: 2px solid #fff; border-radius: 50%; position: absolute; right: -2px; bottom: -2px; background: #22a06b; }
.designer-avatar i.offline, .chat-heading p span.offline, .designer-profile-cover p i.offline { background: #94a3b8; }
.designer-avatar.compact { overflow: visible; }
.session-copy { min-width: 0; flex: 1; display: grid; gap: 6px; }
.session-copy > span { display: flex; align-items: center; justify-content: space-between; gap: 6px; }
.session-copy strong { overflow: hidden; font-size: 14px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.session-copy time { flex: none; color: #94a3b8; font-size: 10px; }
.session-copy small { overflow: hidden; color: #64748b; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.session-copy em { width: fit-content; padding: 2px 6px; border-radius: 4px; background: #e9edf2; color: #475569; font-size: 9px; font-style: normal; font-weight: 600; }
.service-note { margin: 28px 20px 0; padding-top: 18px; border-top: 1px solid rgba(15, 23, 42, .06); display: grid; gap: 5px; }
.service-note span { color: #94a3b8; font-size: 11px; }
.service-note strong { font-size: 13px; font-weight: 600; }
.service-note p { margin: 0; color: #94a3b8; font-size: 11px; line-height: 1.6; }
.consultant-chat { min-width: 0; min-height: 0; display: grid; grid-template-rows: 78px minmax(0, 1fr) auto; background: #fff; }
.chat-heading { padding: 0 26px; border-bottom: 1px solid rgba(15, 23, 42, .065); display: flex; align-items: center; gap: 13px; }
.company-avatar.large, .designer-avatar.large { width: 44px; height: 44px; }
.chat-heading > div:nth-child(2) { min-width: 0; flex: 1; }
.chat-heading h2 { margin: 0 0 5px; font-size: 17px; font-weight: 600; letter-spacing: -.015em; }
.chat-heading p { margin: 0; display: flex; align-items: center; gap: 6px; color: #64748b; font-size: 12px; }
.chat-heading p span { width: 7px; height: 7px; border-radius: 50%; background: #22a06b; }
.chat-company-mark { color: #c4cad3; font-size: 10px; font-weight: 700; letter-spacing: .13em; }
.message-area { min-height: 0; padding: 28px 34px 34px; overflow-y: auto; background: #fafbfc; }
.conversation-date { margin-bottom: 25px; color: #94a3b8; font-size: 11px; text-align: center; }
.message-row { max-width: 760px; margin-bottom: 24px; display: flex; align-items: flex-start; gap: 11px; }
.message-row.mine { margin-left: auto; justify-content: flex-end; }
.message-avatar { width: 36px; height: 36px; border-radius: 10px; font-size: 10px; overflow: hidden; }
.customer-avatar { width: 36px; height: 36px; flex: none; border-radius: 10px; display: grid; place-items: center; background: #e9edf2; color: #475569; font-size: 12px; font-weight: 600; }
.message-content { max-width: min(620px, calc(100% - 50px)); }
.message-content.wide { width: min(650px, calc(100vw - 390px)); max-width: calc(100% - 47px); }
.sender-name { margin: 0 0 6px 2px; color: #64748b; font-size: 12px; }
.message-bubble { padding: 14px 17px; border: 1px solid rgba(15, 23, 42, .07); border-radius: 5px 12px 12px 12px; background: #fff; color: #263244; font-size: 14px; line-height: 1.7; box-shadow: 0 5px 18px rgba(15, 23, 42, .055); white-space: pre-wrap; }
.mine .message-bubble { border-color: rgba(217, 119, 69, .15); border-radius: 12px 5px 12px 12px; background: #fff7f2; color: #3f2c22; box-shadow: none; }
.message-bubble p { margin: 0 0 8px; }
.message-bubble p:last-child { margin-bottom: 0; }
.message-content time { display: block; margin-top: 5px; color: #a2acb9; font-size: 10px; }
.mine .message-content time { text-align: right; }
.intake-card, .summary-card { overflow: hidden; border: 1px solid rgba(15, 23, 42, .09); border-radius: 5px 14px 14px 14px; background: #fff; box-shadow: 0 12px 32px rgba(15, 23, 42, .07); }
.intake-card > header { padding: 17px 20px; border-bottom: 1px solid #edf0f3; display: grid; gap: 3px; background: #f8f9fb; }
.intake-card > header span { font-size: 15px; font-weight: 650; }
.intake-card > header small { color: #7b8797; font-size: 11px; }
.intake-form { padding: 18px 20px 20px; display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
.intake-form label { min-width: 0; position: relative; display: grid; gap: 7px; }
.intake-form label > span { color: #475569; font-size: 12px; font-weight: 600; }
.intake-form b { color: #b4533b; }
.intake-form input, .intake-form select, .intake-form textarea { width: 100%; box-sizing: border-box; border: 1px solid #dce1e7; border-radius: 8px; outline: none; background: #fff; color: #1f2937; font: inherit; font-size: 13px; transition: border-color .16s, box-shadow .16s; }
.intake-form input, .intake-form select { height: 40px; padding: 0 11px; }
.intake-form textarea { padding: 10px 11px 24px; resize: vertical; line-height: 1.55; }
.intake-form input:focus, .intake-form select:focus, .intake-form textarea:focus { border-color: rgba(217, 119, 69, .62); box-shadow: 0 0 0 3px rgba(217, 119, 69, .08); }
.intake-form .full-field { grid-column: 1 / -1; }
.intake-form label > small { position: absolute; right: 9px; bottom: 7px; color: #a2acb9; font-size: 9px; }
.form-note { display: flex; align-items: center; gap: 6px; color: #7b8797; font-size: 10px; }
.intake-form > .primary-action { justify-self: end; }
.primary-action, .secondary-action { min-height: 38px; padding: 0 15px; border-radius: 8px; display: inline-flex; align-items: center; justify-content: center; gap: 8px; cursor: pointer; font: inherit; font-size: 13px; font-weight: 600; }
.primary-action { border: 0; background: #D97745; color: #fff; box-shadow: 0 4px 12px rgba(217, 119, 69, .17); }
.secondary-action { border: 1px solid #dce1e7; background: #fff; color: #475569; }
.primary-action:disabled, .secondary-action:disabled { opacity: .62; cursor: not-allowed; }
.summary-card > header { padding: 18px 20px; display: flex; align-items: center; justify-content: space-between; background: #202a38; color: #fff; }
.summary-card > header small { color: #9fa8b5; font-size: 9px; font-weight: 700; letter-spacing: .12em; }
.summary-card > header h3 { margin: 4px 0 0; font-size: 17px; font-weight: 600; }
.summary-card > header > span { padding: 4px 8px; border: 1px solid rgba(255,255,255,.18); border-radius: 5px; color: #d8dde4; font-size: 10px; }
.summary-card dl { margin: 0; padding: 18px 20px; display: grid; grid-template-columns: 1fr 1fr; gap: 16px 24px; }
.summary-card dl div { min-width: 0; }
.summary-card .summary-wide { grid-column: 1 / -1; padding: 13px 0; border-top: 1px solid #edf0f3; border-bottom: 1px solid #edf0f3; }
.summary-card dt { margin-bottom: 5px; color: #94a3b8; font-size: 10px; font-weight: 600; }
.summary-card dd { margin: 0; color: #253144; font-size: 13px; line-height: 1.65; white-space: pre-wrap; }
.summary-card footer { padding: 14px 20px; border-top: 1px solid #edf0f3; display: flex; justify-content: flex-end; gap: 9px; background: #fafbfc; }
.handoff-state { color: #526071; font-size: 12px; line-height: 1.6; }
.match-error { margin: 0 20px 14px; padding: 10px 12px; border: 1px solid #efd5cf; border-radius: 8px; display: grid; gap: 3px; background: #fff8f6; color: #934b3d; font-size: 11px; line-height: 1.5; }
.match-error strong { font-size: 12px; }
.button-spinner { width: 13px; height: 13px; border: 2px solid rgba(255,255,255,.45); border-top-color: #fff; border-radius: 50%; animation: spin .75s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.message-composer { padding: 11px 26px 18px; border-top: 1px solid rgba(15, 23, 42, .065); background: #fff; }
.composer-error { margin-bottom: 9px; padding: 8px 11px; border: 1px solid #efd5cf; border-radius: 8px; background: #fff8f6; color: #934b3d; font-size: 11px; }
.quick-consult { margin-bottom: 10px; display: flex; align-items: center; gap: 8px; overflow-x: auto; }
.quick-consult > span { margin-right: 2px; color: #94a3b8; font-size: 11px; white-space: nowrap; }
.quick-consult button { height: 30px; padding: 0 12px; border: 1px solid rgba(15, 23, 42, .09); border-radius: 15px; background: #fff; color: #475569; cursor: pointer; font: inherit; font-size: 12px; white-space: nowrap; }
.composer-box { padding: 10px 12px 9px; border: 1px solid rgba(15, 23, 42, .1); border-radius: 12px; background: #f9fafb; transition: border-color .16s, box-shadow .16s; }
.composer-box:focus-within { border-color: rgba(217, 119, 69, .48); box-shadow: 0 0 0 3px rgba(217, 119, 69, .07); }
.composer-box textarea { width: 100%; min-height: 54px; padding: 0; border: 0; outline: 0; resize: none; background: transparent; color: #1f2937; font: inherit; font-size: 14px; line-height: 1.6; }
.composer-box textarea::placeholder { color: #a2acb9; }
.composer-actions { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.composer-actions > span { color: #a2acb9; font-size: 10px; }
.composer-actions button { height: 34px; padding: 0 14px; border: 0; border-radius: 9px; display: inline-flex; align-items: center; gap: 7px; background: #D97745; color: #fff; cursor: pointer; font: inherit; font-size: 13px; font-weight: 600; box-shadow: 0 4px 12px rgba(217, 119, 69, .18); }
.composer-actions button:disabled { background: #d8dce2; box-shadow: none; cursor: not-allowed; }
.profile-cover { height: 148px; margin: 14px; padding: 19px; box-sizing: border-box; border-radius: 12px; position: relative; overflow: hidden; display: flex; flex-direction: column; justify-content: space-between; background: linear-gradient(145deg, #111827, #293240); color: #fff; box-shadow: 0 10px 25px rgba(15, 23, 42, .13); }
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
.consultant-person small { display: flex; align-items: center; gap: 5px; color: #16865a; font-size: 10px; }
.consultant-person i { width: 6px; height: 6px; border-radius: 50%; background: #22a06b; }
.service-scope > div { display: flex; flex-wrap: wrap; gap: 7px; }
.service-scope span { padding: 5px 9px; border-radius: 7px; background: #f2f4f6; color: #64748b; font-size: 11px; }
.designer-profile-cover { margin: 14px 14px 12px; padding: 26px 18px 22px; border-radius: 12px; display: flex; flex-direction: column; align-items: center; background: #202a38; color: #fff; text-align: center; }
.profile-avatar { width: 68px; height: 68px; margin-bottom: 13px; border: 3px solid rgba(255,255,255,.14); font-size: 20px; }
.designer-profile-cover small { color: #9fa8b5; font-size: 10px; }
.designer-profile-cover h3 { margin: 5px 0 8px; font-size: 19px; font-weight: 600; }
.designer-profile-cover p { margin: 0; display: flex; align-items: center; gap: 6px; color: #cbd2db; font-size: 11px; }
.designer-profile-cover p i { width: 7px; height: 7px; border-radius: 50%; background: #22a06b; }
.handoff-card small { display: block; margin-top: 10px; color: #94a3b8; font-size: 9px; }
.privacy-note { margin: 18px 20px; display: flex; align-items: flex-start; gap: 9px; color: #94a3b8; }
.privacy-note .el-icon { margin-top: 2px; }
.privacy-note p { margin: 0; display: grid; gap: 3px; }
.privacy-note strong { color: #64748b; font-size: 11px; font-weight: 600; }
.privacy-note span { font-size: 10px; line-height: 1.5; }
button:focus-visible, textarea:focus-visible, input:focus-visible, select:focus-visible { outline: 2px solid rgba(217, 119, 69, .65); outline-offset: 2px; }
@media (max-width: 1180px) {
  .consultant-page { grid-template-columns: 250px minmax(500px, 1fr); }
  .consultant-profile { display: none; }
  .message-content.wide { width: min(650px, calc(100vw - 330px)); }
}
@media (max-width: 760px) {
  .consultant-page { grid-template-columns: 1fr; }
  .consultant-sessions { display: none; }
  .message-area { padding: 22px 18px; }
  .message-composer { padding: 10px 14px 14px; }
  .chat-company-mark, .composer-actions > span { display: none; }
  .message-content.wide { width: calc(100vw - 83px); max-width: calc(100% - 47px); }
  .intake-form, .summary-card dl { grid-template-columns: 1fr; }
  .intake-form .full-field, .summary-card .summary-wide { grid-column: auto; }
  .form-note { grid-column: 1; }
  .intake-form > .primary-action { width: 100%; }
  .summary-card footer { flex-direction: column-reverse; }
  .summary-card footer button { width: 100%; }
}
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { scroll-behavior: auto !important; transition-duration: .01ms !important; animation-duration: .01ms !important; }
}
</style>
