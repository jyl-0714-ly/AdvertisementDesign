<template>
  <div class="case-page">
    <header class="case-header">
      <button type="button" class="case-back" @click="backToPortfolio"><el-icon><ArrowLeft /></el-icon> 返回作品集</button>
      <button type="button" class="consult-button" @click="startConsultation">{{ auth.isLoggedIn ? '咨询类似项目' : '登录后咨询' }}</button>
    </header>
    <main v-loading="loading" class="case-main">
      <template v-if="detail">
        <section class="case-hero">
          <img v-if="detail.coverUrl && !coverFailed" :src="detail.coverUrl" :alt="`${detail.title}案例封面`" @error="coverFailed = true" />
          <div v-else class="image-fallback"><span>{{ detail.serviceType }}</span></div>
          <div class="hero-copy"><span>{{ detail.industry }} / {{ detail.style }}</span><h1>{{ detail.title }}</h1><p>{{ detail.serviceType }}</p></div>
        </section>

        <section class="case-overview">
          <aside class="case-meta"><div><span>行业</span><strong>{{ detail.industry }}</strong></div><div><span>风格</span><strong>{{ detail.style }}</strong></div><div><span>服务内容</span><strong>{{ detail.serviceType }}</strong></div></aside>
          <article><span class="eyebrow">01 / PROJECT OVERVIEW</span><h2>项目概述</h2><p>{{ compatibilityDescription }}</p></article>
        </section>

        <section class="case-section"><header><span>02 / DESIGN APPROACH</span><h2>设计说明</h2></header><p>{{ compatibilityDescription }}</p></section>

        <section class="case-section gallery-section">
          <header><span>03 / PROJECT IMAGES</span><h2>项目画面</h2></header>
          <div v-if="gallery.length" class="case-gallery">
            <figure v-for="(image, index) in gallery" :key="`${image}-${index}`">
              <img v-if="!failedGallery.has(index)" :src="image" :alt="`${detail.title}项目图片 ${index + 1}`" loading="lazy" @error="markGalleryFailed(index)" />
              <div v-else class="image-fallback"><span>图片暂时无法显示</span></div>
              <figcaption>{{ detail.title }} / {{ String(index + 1).padStart(2, '0') }}</figcaption>
            </figure>
          </div>
          <p v-else class="gallery-empty">该案例暂未发布更多项目图片。</p>
        </section>

        <section class="case-closing"><span>DISCUSS A PROJECT</span><h2>需要类似的设计支持？</h2><p>在需求沟通工作台说明您的业务背景、项目范围和时间计划。</p><button type="button" @click="startConsultation">{{ auth.isLoggedIn ? '开始咨询' : '登录后开始咨询' }} →</button></section>
      </template>
      <div v-else-if="!loading" class="case-not-found">案例不存在或已下线。</div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getPortfolioCase } from '@/api'
import type { PortfolioCaseVO } from '@/models'
import { useAuthStore } from '@/stores/auth'
import { getPortfolioReturnLocation } from '@/utils/portfolioReturn'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const detail = ref<PortfolioCaseVO | null>(null)
const coverFailed = ref(false)
const failedGallery = ref(new Set<number>())
const gallery = computed(() => detail.value?.imageUrls?.filter(url => url.trim()) || [])
const compatibilityDescription = computed(() => detail.value?.description?.trim() || '该案例的项目说明正在整理中。')

function markGalleryFailed(index: number) { failedGallery.value = new Set(failedGallery.value).add(index) }
function startConsultation() { void router.push(auth.isLoggedIn ? '/workspace/new' : { path: '/login', query: { redirect: '/workspace/new' } }) }
function backToPortfolio() {
  const returnLocation = getPortfolioReturnLocation()
  if (!returnLocation) { void router.push({ path: '/', hash: '#portfolio' }); return }
  if (window.history.state?.back === returnLocation.path) { router.back(); return }
  void router.push(returnLocation.path)
}

onMounted(async () => {
  loading.value = true
  try { detail.value = await getPortfolioCase(Number(route.params.id)) }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '案例加载失败') }
  finally { loading.value = false }
})
</script>

<style scoped>
.case-page{--ink:#1b1917;--accent:#a5532d;min-height:100vh;background:#f8f6f2;color:var(--ink)}.case-header{height:72px;padding:0 clamp(20px,5vw,72px);display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #ded9d1;background:#fff}.case-back,.consult-button,.case-closing button{border:0;background:none;cursor:pointer;font:inherit}.case-back{display:inline-flex;align-items:center;gap:7px;color:#5f5851}.consult-button{padding:11px 18px;background:var(--ink);color:#fff}.case-main{max-width:1440px;margin:auto;padding:0 clamp(20px,5vw,72px) 80px}.case-hero{min-height:min(68vw,720px);position:relative;overflow:hidden;background:#302d29}.case-hero>img{width:100%;height:100%;position:absolute;inset:0;object-fit:cover}.case-hero::after{content:'';position:absolute;inset:0;background:linear-gradient(180deg,transparent 40%,rgba(0,0,0,.68))}.hero-copy{padding:clamp(30px,6vw,76px);position:absolute;left:0;right:0;bottom:0;z-index:1;color:#fff}.hero-copy>span,.eyebrow,.case-section header span,.case-closing>span{color:#d58a60;font-size:10px;font-weight:800;letter-spacing:.13em}.hero-copy h1{max-width:850px;margin:13px 0;font-family:Georgia,"Songti SC",serif;font-size:clamp(40px,6vw,78px);font-weight:400;line-height:1.08}.hero-copy p{margin:0;color:#e0d9d3}.image-fallback{width:100%;height:100%;min-height:320px;display:grid;place-items:center;background:linear-gradient(135deg,#292622,#9a684d);color:rgba(255,255,255,.78);font-family:Georgia,serif;font-size:clamp(20px,4vw,50px)}.case-overview{padding:85px 0;display:grid;grid-template-columns:260px minmax(0,1fr);gap:clamp(50px,8vw,130px)}.case-meta{border-top:1px solid #d7d1ca}.case-meta div{padding:16px 0;display:grid;gap:5px;border-bottom:1px solid #d7d1ca}.case-meta span{color:#8a8178;font-size:11px}.case-meta strong{font-size:14px}.case-overview article{max-width:760px}.case-overview h2,.case-section h2,.case-closing h2{margin:12px 0 20px;font-family:Georgia,"Songti SC",serif;font-size:clamp(32px,4vw,52px);font-weight:400}.case-overview p,.case-section>p{margin:0;color:#655e57;font-size:15px;line-height:2;white-space:pre-wrap}.case-section{padding:75px 0;border-top:1px solid #d7d1ca}.case-section header{margin-bottom:30px}.gallery-section{padding-bottom:20px}.case-gallery{display:grid;gap:48px}.case-gallery figure{margin:0}.case-gallery img{width:100%;max-height:900px;display:block;object-fit:cover;background:#e7e1da}.case-gallery figcaption{margin-top:10px;color:#8a8178;font-size:10px;letter-spacing:.08em}.gallery-empty{padding:70px;border:1px dashed #cfc7be;text-align:center;color:#8a8178!important}.case-closing{margin-top:70px;padding:80px 25px;text-align:center;background:#211f1c;color:#fff}.case-closing p{color:#aaa39a}.case-closing button{margin-top:20px;padding:13px 22px;background:var(--accent);color:#fff}.case-not-found{padding:130px 0;text-align:center;color:#817870}
@media(max-width:700px){.case-main{padding-left:0;padding-right:0}.case-hero{min-height:520px}.case-overview,.case-section{margin-left:20px;margin-right:20px}.case-overview{grid-template-columns:1fr;padding:55px 0;gap:40px}.case-meta{display:grid;grid-template-columns:repeat(3,1fr);gap:12px}.case-closing{margin-left:20px;margin-right:20px}.case-gallery{gap:28px}}
@media(prefers-reduced-motion:reduce){*,*::before,*::after{scroll-behavior:auto!important;transition-duration:.01ms!important;animation-duration:.01ms!important}}
</style>
