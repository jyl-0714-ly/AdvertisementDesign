<template>
  <div class="studio-home">
    <header class="studio-header">
      <button class="studio-brand" type="button" aria-label="返回首页" @click="goHome">
        <span>AD</span><strong>广告设计工作室</strong>
      </button>
      <nav aria-label="主导航">
        <button type="button" @click="goHome">首页</button>
        <button type="button" @click="scrollTo('services')">服务内容</button>
        <button type="button" @click="scrollTo('portfolio')">作品集</button>
        <button type="button" @click="scrollTo('process')">合作流程</button>
        <button type="button" @click="scrollTo('about')">关于我们</button>
      </nav>
      <button class="contact-button" type="button" @click="contactVisible = true">联系我们</button>
    </header>

    <main>
      <section class="studio-hero">
        <div class="hero-index"><span>INDEPENDENT CREATIVE STUDIO</span><small>品牌策略 · 视觉设计 · 数字体验</small></div>
        <div class="hero-copy">
          <h1>让品牌被看见，<br />让价值被记住。</h1>
          <p>我们与品牌共同定义问题，再以清晰、有辨识度的视觉语言回应商业目标。</p>
          <div class="hero-actions">
            <button class="primary" type="button" @click="scrollTo('portfolio')">浏览精选案例</button>
            <button type="button" @click="startConsultation">发起项目咨询</button>
          </div>
        </div>
        <div class="hero-note"><b>策略先于形式</b><p>从品牌定位到实际触点，每一项设计决策都有明确依据。</p></div>
      </section>

      <section id="services" class="studio-section services-section">
        <header class="section-header"><span>01 / SERVICES</span><div><h2>五项核心服务</h2><p>覆盖品牌从建立、传播到持续生长的关键设计场景。</p></div></header>
        <div class="service-list">
          <article v-for="(service, index) in serviceDefinitions" :key="service.slug">
            <span>{{ String(index + 1).padStart(2, '0') }}</span>
            <div><small>{{ service.en }}</small><h3>{{ service.title }}</h3><p>{{ service.introduction }}</p></div>
            <button type="button" :aria-label="`了解${service.title}`" @click="router.push(`/services/${service.slug}`)">查看服务 →</button>
          </article>
        </div>
      </section>

      <section id="portfolio" class="studio-section portfolio-section">
        <header class="section-header"><span>02 / SELECTED WORK</span><div><h2>精选案例</h2><p>以真实项目封面呈现近期公开作品。</p></div><button class="text-link" type="button" @click="viewAllCases">{{ auth.isLoggedIn ? '查看全部案例' : '登录后查看更多案例' }} →</button></header>
        <div v-loading="loading" class="featured-grid">
          <article v-for="(item, index) in visibleCases" :key="item.id" :class="{ lead: index === 0 }">
            <button class="case-cover" type="button" @click="openCase(item.id)">
              <img v-if="item.coverUrl && !failedCovers.has(item.id)" :src="item.coverUrl" :alt="`${item.title}案例封面`" loading="lazy" @error="markCoverFailed(item.id)" />
              <span v-else class="cover-fallback" aria-hidden="true">{{ portfolioCategoryLabel(item.category) }}</span>
            </button>
            <div class="case-copy"><span>{{ portfolioCategoryLabel(item.category) }} / {{ item.industry }}</span><h3>{{ item.title }}</h3><p>{{ item.serviceType }} · {{ item.style }}</p><button type="button" @click="openCase(item.id)">查看项目 →</button></div>
          </article>
        </div>
        <div v-if="!loading && !visibleCases.length" class="portfolio-empty">精选案例正在整理中，敬请期待。</div>
      </section>

      <section id="process" class="studio-section process-section">
        <header class="section-header"><span>03 / PROCESS</span><div><h2>把复杂项目拆成清晰阶段</h2><p>重要决策有共识，关键成果可追溯。</p></div></header>
        <ol><li v-for="(step, index) in processSteps" :key="step.title"><span>{{ String(index + 1).padStart(2, '0') }}</span><div><h3>{{ step.title }}</h3><p>{{ step.description }}</p></div></li></ol>
      </section>

      <section id="about" class="studio-cta"><span>START A PROJECT</span><h2>有一个需要认真讨论的项目？</h2><p>告诉我们业务背景、目标和时间计划，我们会协助梳理下一步。</p><button type="button" @click="startConsultation">{{ auth.isLoggedIn ? '开始咨询' : '登录后开始咨询' }} →</button></section>
    </main>

    <el-dialog v-model="contactVisible" title="联系我们" width="440px" align-center>
      <p class="contact-intro">欢迎与我们聊聊您的项目，我们会尽快回复。</p>
      <div class="contact-list"><div v-for="item in contactItems" :key="item.label"><span>{{ item.label }}</span><strong>{{ item.value }}</strong></div></div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listPortfolioCases } from '@/api'
import type { PortfolioCaseVO } from '@/models'
import { serviceDefinitions } from '@/services'
import { useAuthStore } from '@/stores/auth'
import { portfolioCategoryLabel } from '@/utils/portfolioLabels'
import { rememberPortfolioPosition } from '@/utils/portfolioReturn'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const featuredCases = ref<PortfolioCaseVO[]>([])
const failedCovers = ref(new Set<number>())
const contactVisible = ref(false)
const visibleCases = computed(() => featuredCases.value.slice(0, 5))
const processSteps = [
  { title: '需求沟通', description: '理解业务背景、目标与项目边界。' },
  { title: '策略提案', description: '形成方向建议、计划与报价。' },
  { title: '设计执行', description: '推进核心方案并保持阶段沟通。' },
  { title: '修改确认', description: '依据共识优化并完成定稿。' },
  { title: '交付落地', description: '交付文件、规范与后续支持。' }
]
const contactItems = [
  { label: '公司电话', value: '400-888-2026' },
  { label: '公司邮箱', value: 'service@advertisement-design.com' },
  { label: '公司地址', value: '北京市朝阳区创意设计产业园 A 座 801' }
]

function goHome() { void router.push('/').then(() => window.scrollTo({ top: 0, behavior: 'smooth' })) }
function scrollTo(id: string) {
  const element = document.getElementById(id)
  if (element) element.scrollIntoView({ behavior: 'smooth', block: 'start' })
  else void router.push({ path: '/', hash: `#${id}` })
}
function startConsultation() { void router.push(auth.isLoggedIn ? '/workbench' : { path: '/login', query: { redirect: '/workbench' } }) }
function viewAllCases() {
  if (!auth.isLoggedIn) {
    ElMessage.info('登录后查看更多案例')
    void router.push({ path: '/login', query: { redirect: '/portfolio' } })
    return
  }
  void router.push('/portfolio')
}
function openCase(id: number) { rememberPortfolioPosition(route.fullPath); void router.push(`/cases/${id}`) }
function markCoverFailed(id: number) { failedCovers.value = new Set(failedCovers.value).add(id) }
async function loadFeatured() {
  loading.value = true
  try { featuredCases.value = (await listPortfolioCases({ featured: true, page: 1, size: 5 })).records.slice(0, 5) }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '案例加载失败') }
  finally { loading.value = false }
}
onMounted(loadFeatured)
</script>

<style scoped>
.studio-home { --ink:#171614; --paper:#f5f1e9; --accent:#a34f28; min-height:100vh; color:var(--ink); background:#fff; font-family:"Noto Sans SC","PingFang SC",sans-serif; }
.studio-header { height:72px; padding:0 clamp(20px,5vw,72px); position:sticky; top:0; z-index:30; display:grid; grid-template-columns:1fr auto 1fr; align-items:center; border-bottom:1px solid #ded9d1; background:rgba(255,255,255,.96); backdrop-filter:blur(12px); }
.studio-brand,.studio-header nav button,.contact-button,.hero-actions button,.text-link { border:0; background:none; cursor:pointer; font:inherit; }.studio-brand { padding:0; display:flex; align-items:center; gap:11px; justify-self:start; }.studio-brand span { width:35px;height:35px;display:grid;place-items:center;background:var(--ink);color:#fff;font-size:11px;font-weight:800; }.studio-brand strong{font-size:14px}.studio-header nav{display:flex;gap:34px}.studio-header nav button{height:72px;color:#4e4943;font-size:13px}.studio-header nav button:hover{color:var(--accent)}.contact-button{justify-self:end;padding:11px 18px!important;background:var(--ink)!important;color:#fff;font-size:13px!important}
.studio-hero { min-height:650px; padding:clamp(50px,8vw,110px) clamp(24px,7vw,100px) 48px; display:grid; grid-template-columns:190px minmax(0,1fr) 260px; gap:clamp(25px,5vw,80px); align-items:start; background:var(--paper); border-bottom:1px solid #ded9d1; }.hero-index{display:grid;gap:13px;color:var(--accent);font-size:10px;font-weight:700;letter-spacing:.13em}.hero-index small{color:#756d64;line-height:1.7;letter-spacing:.03em}.hero-copy h1{max-width:820px;margin:0;font-family:Georgia,"Songti SC",serif;font-size:clamp(56px,7.5vw,112px);font-weight:400;line-height:1.03;letter-spacing:-.055em}.hero-copy p{max-width:560px;margin:38px 0 0;color:#615a52;font-size:16px;line-height:1.9}.hero-actions{margin-top:35px;display:flex;gap:12px}.hero-actions button{padding:13px 20px;border:1px solid #b7afa5;color:var(--ink)}.hero-actions .primary{border-color:var(--accent);background:var(--accent);color:#fff}.hero-note{margin-top:260px;padding-top:17px;border-top:1px solid #bdb5aa}.hero-note b{font-size:13px}.hero-note p{margin:9px 0 0;color:#736b62;font-size:12px;line-height:1.7}
.studio-section{padding:90px clamp(24px,7vw,100px);scroll-margin-top:72px}.section-header{margin-bottom:50px;display:grid;grid-template-columns:190px minmax(0,1fr) auto;gap:clamp(25px,5vw,80px);align-items:end}.section-header>span{align-self:start;color:var(--accent);font-size:10px;font-weight:800;letter-spacing:.12em}.section-header h2{margin:0;font-family:Georgia,"Songti SC",serif;font-size:clamp(34px,4vw,54px);font-weight:400}.section-header p{margin:12px 0 0;color:#766e65;font-size:13px}.text-link{color:var(--accent);font-size:13px}
.services-section{background:#fff}.service-list{border-top:1px solid #d9d4cd}.service-list article{padding:30px 0;display:grid;grid-template-columns:190px minmax(0,1fr) auto;gap:clamp(25px,5vw,80px);align-items:center;border-bottom:1px solid #d9d4cd}.service-list>article>span{color:#9e958b;font-size:11px}.service-list small{color:var(--accent);font-size:9px;letter-spacing:.12em}.service-list h3{margin:5px 0 8px;font-size:25px;font-weight:500}.service-list p{max-width:620px;margin:0;color:#766e65;font-size:13px;line-height:1.7}.service-list button,.case-copy button{border:0;background:none;color:var(--accent);cursor:pointer;font:inherit;font-size:12px}
.portfolio-section{background:#1d1c1a;color:#fff}.portfolio-section .section-header p{color:#aaa39a}.featured-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:55px 24px}.featured-grid article.lead{grid-column:1/-1}.case-cover{width:100%;aspect-ratio:1.5;padding:0;border:0;overflow:hidden;background:#34312d;cursor:pointer}.lead .case-cover{aspect-ratio:2.25}.case-cover img{width:100%;height:100%;display:block;object-fit:cover;transition:transform .5s ease}.case-cover:hover img{transform:scale(1.015)}.cover-fallback{width:100%;height:100%;display:grid;place-items:center;background:linear-gradient(135deg,#453b32,#8d634a);color:rgba(255,255,255,.75);font-family:Georgia,serif;font-size:clamp(24px,4vw,58px)}.case-copy{padding-top:16px;position:relative}.case-copy>span{color:#b46c47;font-size:10px;letter-spacing:.08em}.case-copy h3{margin:7px 0;font-size:22px;font-weight:500}.case-copy p{margin:0;color:#aaa39a;font-size:12px}.case-copy button{position:absolute;right:0;bottom:0}.portfolio-empty{padding:70px;border:1px solid #413e3a;text-align:center;color:#aaa39a}
.process-section ol{margin:0;padding:0;border-top:1px solid #d9d4cd;list-style:none}.process-section li{padding:23px 0;display:grid;grid-template-columns:190px 1fr;border-bottom:1px solid #d9d4cd}.process-section li>span{color:var(--accent);font-size:11px}.process-section li div{display:grid;grid-template-columns:240px 1fr;gap:30px}.process-section h3,.process-section p{margin:0}.process-section h3{font-size:18px}.process-section p{color:#766e65;font-size:13px;line-height:1.7}.studio-cta{padding:90px clamp(24px,7vw,100px);text-align:center;background:var(--paper)}.studio-cta>span{color:var(--accent);font-size:10px;font-weight:800;letter-spacing:.13em}.studio-cta h2{margin:14px 0;font-family:Georgia,"Songti SC",serif;font-size:clamp(36px,5vw,64px);font-weight:400}.studio-cta p{color:#756d64}.studio-cta button{margin-top:22px;padding:14px 24px;border:0;background:var(--accent);color:#fff;cursor:pointer}.contact-intro{color:#766e65}.contact-list>div{padding:14px 0;display:grid;gap:4px;border-top:1px solid #eee}.contact-list span{color:#8b8279;font-size:11px}.contact-list strong{font-size:14px}
@media(max-width:900px){.studio-header{grid-template-columns:1fr auto}.studio-header nav{display:none}.studio-hero{grid-template-columns:1fr;min-height:auto}.hero-note{margin-top:20px}.section-header,.service-list article{grid-template-columns:100px minmax(0,1fr)}.section-header .text-link,.service-list article>button{grid-column:2}.process-section li{grid-template-columns:100px 1fr}}
@media(max-width:600px){.studio-hero{padding:55px 20px}.hero-copy h1{font-size:50px}.hero-actions{align-items:stretch;flex-direction:column}.studio-section{padding:65px 20px}.section-header,.service-list article{grid-template-columns:1fr;gap:16px}.section-header .text-link,.service-list article>button{grid-column:auto;justify-self:start}.featured-grid{grid-template-columns:1fr}.featured-grid article.lead{grid-column:auto}.lead .case-cover,.case-cover{aspect-ratio:1.3}.case-copy button{position:static;margin-top:13px}.process-section li{grid-template-columns:48px 1fr}.process-section li div{grid-template-columns:1fr;gap:8px}}
@media(prefers-reduced-motion:reduce){*,*::before,*::after{scroll-behavior:auto!important;transition-duration:.01ms!important;animation-duration:.01ms!important}.case-cover:hover img{transform:none}}
</style>
