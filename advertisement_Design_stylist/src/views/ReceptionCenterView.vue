<template>
  <section class="reception-page">
    <header class="page-intro">
      <div>
        <span class="eyebrow">CLIENT RECEPTION</span>
        <h1>客户接待中心</h1>
        <p>查看为你匹配的新需求，在进入项目流程前完成首次沟通。</p>
      </div>
      <div class="queue-stat"><strong>{{ pendingCount }}</strong><span>待接待</span></div>
    </header>

    <div class="filter-row" aria-label="接待状态筛选">
      <button :class="{ active: filter === 'pending' }" type="button" @click="filter = 'pending'">待接待</button>
      <button :class="{ active: filter === 'accepted' }" type="button" @click="filter = 'accepted'">接待中</button>
      <button :class="{ active: filter === 'all' }" type="button" @click="filter = 'all'">全部</button>
    </div>

    <div v-if="loading" class="state-panel">正在同步客户需求…</div>
    <div v-else-if="error" class="state-panel state-panel--error">
      <span>{{ error }}</span><button type="button" @click="load">重新加载</button>
    </div>
    <div v-else-if="!visibleReceptions.length" class="state-panel">
      <strong>当前没有{{ filter === 'pending' ? '待接待' : filter === 'accepted' ? '接待中' : '' }}客户</strong>
      <span>新的匹配需求会显示在这里。</span>
    </div>

    <div v-else class="customer-grid">
      <article v-for="item in visibleReceptions" :key="item.intakeId" class="customer-card">
        <div class="card-head">
          <div class="avatar">{{ item.customerName.slice(0, 1) }}</div>
          <div class="identity"><strong>{{ item.customerName }}</strong><span>{{ formatTime(item.createdAt) }} 匹配</span></div>
          <div class="score"><strong>{{ item.matchScore }}%</strong><span>匹配度</span></div>
        </div>
        <div class="brief-title"><span>需求</span><h2>{{ item.projectType }}</h2></div>
        <dl class="meta-grid">
          <div><dt>行业</dt><dd>{{ item.industry }}</dd></div>
          <div><dt>预算</dt><dd>{{ item.budgetRange }}</dd></div>
          <div><dt>项目周期</dt><dd>{{ item.projectCycle }}</dd></div>
        </dl>
        <div class="match-note"><span class="match-dot"></span><div><small>匹配原因</small><p>{{ item.matchReason }}</p></div></div>
        <button class="primary-button" type="button" :disabled="acceptingId === item.intakeId" @click="openReception(item)">
          {{ acceptingId === item.intakeId ? '正在进入…' : item.status === 'MATCHED' ? '接待客户' : '继续沟通' }}
          <span aria-hidden="true">→</span>
        </button>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { acceptConsultantReception, listConsultantReceptions } from '@/api'
import type { ConsultantReceptionVO } from '@/models'

const router = useRouter()
const receptions = ref<ConsultantReceptionVO[]>([])
const loading = ref(true)
const error = ref('')
const filter = ref<'pending' | 'accepted' | 'all'>('pending')
const acceptingId = ref<number | null>(null)
const pendingCount = computed(() => receptions.value.filter(item => item.status === 'MATCHED').length)
const visibleReceptions = computed(() => receptions.value.filter(item =>
  filter.value === 'all' || (filter.value === 'pending' ? item.status === 'MATCHED' : item.status === 'ACCEPTED')
))

function formatTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '刚刚' : new Intl.DateTimeFormat('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(date)
}

async function load() {
  loading.value = true
  error.value = ''
  try { receptions.value = await listConsultantReceptions() }
  catch (cause) { error.value = cause instanceof Error ? cause.message : '客户需求加载失败' }
  finally { loading.value = false }
}

async function openReception(item: ConsultantReceptionVO) {
  if (acceptingId.value !== null) return
  acceptingId.value = item.intakeId
  error.value = ''
  try {
    if (item.status === 'MATCHED') await acceptConsultantReception(item.intakeId)
    await router.push(`/reception/${item.intakeId}`)
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '暂时无法接待该客户' }
  finally { acceptingId.value = null }
}

onMounted(load)
</script>

<style scoped>
.reception-page { max-width: 1240px; margin: 0 auto; padding: 34px 10px 64px; }
.page-intro { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 38px; }
.eyebrow { color: var(--s-accent); font-size: 11px; font-weight: 700; letter-spacing: .16em; }
h1 { margin: 8px 0 8px; font-size: clamp(28px, 3vw, 38px); line-height: 1.16; letter-spacing: -.04em; font-weight: 650; }
.page-intro p { margin: 0; color: var(--s-muted); font-size: 14px; }
.queue-stat { min-width: 92px; padding-left: 22px; border-left: 1px solid var(--s-border); display: grid; }
.queue-stat strong { font-size: 28px; line-height: 1; font-weight: 600; }
.queue-stat span { margin-top: 7px; color: var(--s-muted); font-size: 12px; }
.filter-row { display: flex; gap: 6px; margin-bottom: 20px; }
.filter-row button { padding: 8px 13px; border: 0; border-radius: 8px; background: transparent; color: var(--s-muted); font: inherit; font-size: 13px; cursor: pointer; }
.filter-row button.active { background: #fff; color: var(--s-ink); box-shadow: 0 1px 0 var(--s-border), 0 4px 14px rgba(15,23,42,.05); }
.customer-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.customer-card { min-width: 0; padding: 22px; border: 1px solid var(--s-border); border-radius: 14px; background: rgba(255,255,255,.94); box-shadow: 0 10px 30px rgba(15,23,42,.035); transition: transform 180ms, box-shadow 180ms; }
.customer-card:hover { transform: translateY(-2px); box-shadow: 0 16px 38px rgba(15,23,42,.07); }
.card-head { display: flex; align-items: center; gap: 11px; }
.avatar { width: 38px; height: 38px; display: grid; place-items: center; border-radius: 11px; background: #1d2430; color: white; font-size: 14px; font-weight: 650; }
.identity { min-width: 0; display: grid; gap: 3px; }
.identity strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; }
.identity span { color: var(--s-muted); font-size: 11px; }
.score { margin-left: auto; display: grid; justify-items: end; }
.score strong { color: var(--s-accent); font-size: 18px; }
.score span { color: var(--s-muted); font-size: 10px; }
.brief-title { margin: 28px 0 18px; }
.brief-title span { color: var(--s-muted); font-size: 11px; }
.brief-title h2 { margin: 5px 0 0; font-size: 21px; letter-spacing: -.025em; font-weight: 600; }
.meta-grid { margin: 0; display: grid; grid-template-columns: 1fr 1fr; gap: 16px 12px; }
.meta-grid div:last-child { grid-column: 1 / -1; }
dt { margin-bottom: 5px; color: var(--s-muted); font-size: 11px; } dd { margin: 0; font-size: 13px; font-weight: 520; }
.match-note { margin: 22px 0; padding: 13px 14px; display: flex; gap: 10px; border-radius: 10px; background: #f8f9fa; }
.match-dot { width: 7px; height: 7px; margin-top: 4px; flex: none; border-radius: 50%; background: #20a06b; box-shadow: 0 0 0 4px rgba(32,160,107,.1); }
.match-note small { color: var(--s-muted); font-size: 10px; }.match-note p { margin: 4px 0 0; color: #3d4859; font-size: 12px; line-height: 1.55; }
.primary-button { width: 100%; min-height: 42px; padding: 0 15px; border: 0; border-radius: 9px; display: flex; justify-content: space-between; align-items: center; background: #202631; color: #fff; font: inherit; font-size: 13px; font-weight: 600; cursor: pointer; }
.primary-button:hover { background: var(--s-accent); }.primary-button:disabled { opacity: .55; cursor: wait; }
.state-panel { min-height: 240px; display: grid; place-content: center; justify-items: center; gap: 8px; border: 1px dashed rgba(15,23,42,.11); border-radius: 14px; color: var(--s-muted); font-size: 13px; background: rgba(255,255,255,.48); }
.state-panel strong { color: var(--s-ink); font-size: 16px; }.state-panel button { border: 0; background: transparent; color: var(--s-accent); cursor: pointer; }.state-panel--error { color: #9f3a3a; }
@media (max-width: 980px) { .customer-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 650px) { .reception-page { padding-top: 20px; }.page-intro { align-items: flex-start; }.queue-stat { display: none; }.customer-grid { grid-template-columns: 1fr; } }
@media (prefers-reduced-motion: reduce) { .customer-card { transition: none; } }
</style>
