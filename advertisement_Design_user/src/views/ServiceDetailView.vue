<template>
  <div class="service-detail-page">
    <header class="service-detail-header">
      <button class="service-detail-brand" type="button" @click="router.push('/')"><span>AD</span><strong>广告设计工作室</strong></button>
      <button class="service-detail-back" type="button" @click="router.push({ path: '/', hash: '#services' })">返回服务内容</button>
    </header>

    <main>
      <section class="service-detail-hero">
        <div><span class="service-sequence">SERVICE / {{ String(serviceIndex + 1).padStart(2, '0') }}</span><h1>{{ service.title }}</h1><p>{{ service.introduction }}</p><button type="button" @click="startConsultation">咨询这项服务 →</button></div>
        <div class="service-monogram" aria-hidden="true"><span>{{ service.mark }}</span><b>{{ service.en }}</b></div>
      </section>

      <nav class="service-section-nav" aria-label="页面目录">
        <a href="#introduction">服务介绍</a><a href="#content">服务内容</a><a href="#customers">适合客户类型</a><a href="#cases">对应案例</a><a href="#workflow">合作流程</a><a href="#consultation">咨询入口</a>
      </nav>

      <section id="introduction" class="service-block split-block">
        <div class="block-label"><span>01</span><strong>服务介绍</strong></div>
        <div><h2>{{ service.title }}，从策略共识开始</h2><p>{{ service.introduction }}</p><p>{{ service.detail }}</p></div>
      </section>

      <section id="content" class="service-block">
        <div class="block-label"><span>02</span><strong>服务内容</strong></div>
        <div class="deliverable-grid"><article v-for="(item, index) in service.contents" :key="item.title"><span>{{ String(index + 1).padStart(2, '0') }}</span><h3>{{ item.title }}</h3><p>{{ item.description }}</p></article></div>
      </section>

      <section id="customers" class="service-block split-block muted-block">
        <div class="block-label"><span>03</span><strong>适合客户类型</strong></div>
        <div><h2>适合正在经历这些阶段的品牌</h2><ul class="customer-types"><li v-for="item in service.customers" :key="item">{{ item }}</li></ul></div>
      </section>

      <section id="cases" class="service-block">
        <div class="block-heading"><div class="block-label"><span>04</span><strong>对应案例</strong></div><button type="button" @click="viewAllCases">查看全部案例 →</button></div>
        <div v-loading="loading" class="service-case-grid"><article v-for="item in relatedCases" :key="item.id" @click="openCase(item.id)"><div class="service-case-cover"><img v-if="item.coverUrl && !failedCovers.has(item.id)" :src="item.coverUrl" :alt="`${item.title}案例封面`" loading="lazy" @error="markCoverFailed(item.id)" /><span v-else class="service-case-fallback">{{ item.serviceType }}</span></div><h3>{{ item.title }}</h3><p>{{ item.style }} · {{ item.industry }}</p></article></div>
        <p v-if="!loading && !relatedCases.length" class="service-empty">相关案例正在整理中，您也可以先与顾问沟通需求。</p>
      </section>

      <section id="workflow" class="service-block muted-block">
        <div class="block-label"><span>05</span><strong>合作流程</strong></div>
        <div class="workflow-grid"><article v-for="(item, index) in workflow" :key="item.title"><b>{{ String(index + 1).padStart(2, '0') }}</b><h3>{{ item.title }}</h3><p>{{ item.description }}</p></article></div>
      </section>

      <section id="consultation" class="service-consultation">
        <div><span>LET'S TALK</span><h2>让我们聊聊您的{{ service.title }}需求</h2><p>提交需求后，项目顾问将协助梳理目标、范围与下一步计划。</p></div><button type="button" @click="startConsultation">开始咨询 →</button>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listPortfolioCases } from '@/api'
import type { PortfolioCaseVO } from '@/models'
import { caseMatchesService, defaultService, serviceBySlug, serviceDefinitions } from '@/services'
import { useAuthStore } from '@/stores/auth'
import { rememberPortfolioPosition } from '@/utils/portfolioReturn'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const cases = ref<PortfolioCaseVO[]>([])
const failedCovers = ref(new Set<number>())
const service = computed(() => serviceBySlug[String(route.params.slug)] || defaultService)
const serviceIndex = computed(() => Math.max(serviceDefinitions.findIndex(item => item.slug === service.value.slug), 0))
const relatedCases = computed(() => cases.value.filter(item => caseMatchesService(item.serviceType, service.value)).slice(0, 3))
const workflow = [{ title: '需求沟通', description: '明确业务背景、目标与项目边界。' }, { title: '策略提案', description: '形成方向建议、计划与报价。' }, { title: '设计执行', description: '推进核心方案并持续沟通。' }, { title: '修改确认', description: '根据共识优化并完成定稿。' }, { title: '交付落地', description: '交付文件、规范与后续支持。' }]
function startConsultation() { void router.push(auth.isLoggedIn ? '/consultant' : { path: '/login', query: { redirect: '/consultant' } }) }
function viewAllCases() { if (!auth.isLoggedIn) { ElMessage.info('登录后查看更多案例'); void router.push({ path: '/login', query: { redirect: '/portfolio' } }); return }; void router.push('/portfolio') }
function openCase(id: number) { rememberPortfolioPosition(route.fullPath); void router.push(`/cases/${id}`) }
function markCoverFailed(id: number) { failedCovers.value = new Set(failedCovers.value).add(id) }
onMounted(async () => { loading.value = true; try { cases.value = (await listPortfolioCases({ page: 1, size: 30 })).records } catch (error) { ElMessage.error(error instanceof Error ? error.message : '案例加载失败') } finally { loading.value = false } })
</script>

<style scoped>
.service-detail-page { min-height: 100vh; color: #201d1a; background: #fff; }.service-detail-header { height: 68px; padding: 0 clamp(22px,6vw,84px); display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #ebe5df; }.service-detail-brand,.service-detail-back { border: 0; background: none; cursor: pointer; }.service-detail-brand { padding: 0; display: flex; align-items: center; gap: 10px; }.service-detail-brand span { width: 34px; height: 34px; display: grid; place-items: center; border-radius: 5px; color: #fff; background: #171717; font-size: 12px; font-weight: 800; }.service-detail-brand strong { font-size: 14px; }.service-detail-back { color: #8f532e; font-size: 13px; }
.service-detail-hero { min-height: 410px; padding: 70px clamp(24px,8vw,120px); display: grid; grid-template-columns: 1fr .65fr; align-items: center; gap: 60px; color: #fff; background: #171817; }.service-sequence { color: #c88354; font-size: 11px; font-weight: 800; letter-spacing: .12em; }.service-detail-hero h1 { margin: 18px 0; font-size: clamp(42px,5vw,68px); letter-spacing: -.04em; }.service-detail-hero p { max-width: 620px; margin: 0; color: #c8c3be; font-size: 16px; line-height: 1.9; }.service-detail-hero button { margin-top: 30px; padding: 13px 22px; border: 0; border-radius: 4px; color: #fff; background: #af6334; cursor: pointer; font-weight: 700; }.service-monogram { aspect-ratio: 1; max-width: 300px; justify-self: end; display: grid; place-items: center; align-content: center; border: 1px solid #58514b; transform: rotate(5deg); background: linear-gradient(145deg,#282724,#171817); }.service-monogram span { color: #b86a39; font: 100px Georgia,serif; }.service-monogram b { color: #8d857e; font-size: 10px; letter-spacing: .25em; }
.service-section-nav { min-height: 58px; padding: 0 clamp(20px,8vw,120px); position: sticky; top: 0; z-index: 10; display: flex; align-items: center; justify-content: center; gap: 35px; border-bottom: 1px solid #ece6e0; background: rgba(255,255,255,.96); }.service-section-nav a { color: #645b54; font-size: 12px; }.service-section-nav a:hover { color: #a75b2a; }
.service-block { padding: 78px clamp(24px,8vw,120px); scroll-margin-top: 58px; }.split-block { display: grid; grid-template-columns: 240px 1fr; gap: 70px; }.muted-block { background: #f7f4f0; }.block-label { display: flex; align-items: baseline; gap: 10px; }.block-label span { color: #b76c3d; font-size: 11px; font-weight: 800; }.block-label strong { font-size: 15px; }.split-block h2 { margin: 0 0 20px; font-size: 34px; }.split-block > div:last-child > p { max-width: 780px; color: #686059; line-height: 1.9; }.deliverable-grid { margin-top: 28px; display: grid; grid-template-columns: repeat(4,1fr); gap: 18px; }.deliverable-grid article { min-height: 190px; padding: 25px; border: 1px solid #e8e0d8; }.deliverable-grid span { color: #b76c3d; font-size: 11px; }.deliverable-grid h3 { margin: 24px 0 10px; }.deliverable-grid p { margin: 0; color: #7b7169; font-size: 13px; line-height: 1.7; }.customer-types { margin: 24px 0 0; padding: 0; display: grid; grid-template-columns: 1fr 1fr; gap: 12px; list-style: none; }.customer-types li { padding: 16px 18px; border-left: 3px solid #b76c3d; background: #fff; color: #5b534d; font-size: 13px; }
.block-heading { display: flex; align-items: center; justify-content: space-between; }.block-heading button { border: 0; background: none; color: #a75b2a; cursor: pointer; }.service-case-grid { margin-top: 28px; display: grid; grid-template-columns: repeat(3,1fr); gap: 20px; }.service-case-grid article { cursor: pointer; }.service-case-cover { aspect-ratio: 1.45; overflow: hidden; background: #ede7df; }.service-case-cover img { width: 100%; height: 100%; display: block; object-fit: cover; }.service-case-fallback { width: 100%; height: 100%; display: grid; place-items: center; color: #fff; background: linear-gradient(135deg,#24211e,#9d6541); font-size: 20px; font-weight: 700; }.service-case-grid h3 { margin: 14px 0 5px; }.service-case-grid p { margin: 0; color: #857a72; font-size: 12px; }.service-empty { padding: 35px 0; color: #857a72; }.workflow-grid { margin-top: 30px; display: grid; grid-template-columns: repeat(5,1fr); gap: 16px; }.workflow-grid article { padding: 20px 18px; border-top: 2px solid #b76c3d; background: #fff; }.workflow-grid b { color: #b76c3d; font-size: 11px; }.workflow-grid h3 { margin: 20px 0 8px; }.workflow-grid p { color: #7c726a; font-size: 12px; line-height: 1.6; }
.service-consultation { padding: 55px clamp(24px,8vw,120px); display: flex; align-items: center; justify-content: space-between; color: #fff; background: #1c1c1b; }.service-consultation span { color: #bc7041; font-size: 10px; font-weight: 800; letter-spacing: .12em; }.service-consultation h2 { margin: 8px 0; font-size: 29px; }.service-consultation p { margin: 0; color: #aaa39d; font-size: 13px; }.service-consultation button { min-width: 150px; height: 45px; border: 0; border-radius: 4px; color: #fff; background: #b66635; cursor: pointer; font-weight: 700; }
@media(max-width:800px){.service-detail-hero,.split-block{grid-template-columns:1fr}.service-monogram{display:none}.service-section-nav{overflow-x:auto;justify-content:flex-start}.service-section-nav a{min-width:max-content}.deliverable-grid{grid-template-columns:1fr 1fr}.workflow-grid{grid-template-columns:1fr 1fr}.service-case-grid{grid-template-columns:1fr}.service-consultation{align-items:flex-start;gap:24px;flex-direction:column}}@media(max-width:480px){.deliverable-grid,.customer-types,.workflow-grid{grid-template-columns:1fr}.service-block{padding:52px 20px}.split-block{gap:25px}}@media(prefers-reduced-motion:reduce){*,*::before,*::after{scroll-behavior:auto!important;transition-duration:.01ms!important;animation-duration:.01ms!important}}
</style>
