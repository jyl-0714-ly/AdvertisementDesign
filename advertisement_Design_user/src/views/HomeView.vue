<template>
  <div class="public-site">
    <header class="public-header">
      <button class="public-brand" type="button" @click="router.push('/')">
        <span class="public-brand-mark">AD</span>
        <span>
          <strong>广告设计工作室</strong>
          <small>品牌与视觉设计</small>
        </span>
      </button>
      <nav class="public-nav" aria-label="主导航">
        <a href="#portfolio">作品集</a>
        <a href="#process">合作流程</a>
        <button class="text-action" type="button" @click="goLogin">登录工作台</button>
        <button class="consult-button" type="button" @click="contactVisible = true">联系我们</button>
      </nav>
    </header>

    <main>
      <section class="public-intro">
        <div class="public-intro-copy">
          <span class="public-eyebrow">CREATIVE PARTNERSHIP</span>
          <h1>让品牌表达，<br />有清晰的方向。</h1>
          <p>从需求梳理到最终交付，以可追溯的协作流程完成每一次创意沟通。</p>
        </div>
        <div class="public-art" aria-hidden="true">
          <div class="art-card art-card-main"><span>BRAND</span><b>01</b></div>
          <div class="art-card art-card-side"><span>IDEA</span><b>+</b></div>
          <div class="art-line"></div>
        </div>
      </section>

      <section id="portfolio" class="portfolio-section">
        <div class="section-intro">
          <div>
            <span class="public-eyebrow">SELECTED WORK</span>
            <h2>作品集</h2>
          </div>
          <p>按行业、风格与服务类型，找到更接近你项目的视觉方向。</p>
        </div>

        <div class="portfolio-filter-bar">
          <el-input v-model="filters.keyword" placeholder="搜索项目、行业或设计风格" :prefix-icon="Search" clearable @keyup.enter="load" />
          <el-select v-model="filters.industry" placeholder="行业" clearable>
            <el-option v-for="option in industries" :key="option" :label="option" :value="option" />
          </el-select>
          <el-select v-model="filters.style" placeholder="风格" clearable>
            <el-option v-for="option in styles" :key="option" :label="option" :value="option" />
          </el-select>
          <el-select v-model="filters.serviceType" placeholder="服务类型" clearable>
            <el-option v-for="option in serviceTypes" :key="option" :label="option" :value="option" />
          </el-select>
          <button class="filter-reset" type="button" @click="reset">重置</button>
        </div>

        <div v-loading="loading" class="portfolio-grid">
          <article v-for="item in cases" :key="item.id" class="portfolio-card" @click="router.push(`/cases/${item.id}`)">
            <div class="portfolio-cover" :style="coverStyle(item)">
              <span>{{ item.industry }}</span>
              <b>{{ item.serviceType }}</b>
            </div>
            <div class="portfolio-card-copy">
              <span>{{ item.style }}</span>
              <h3>{{ item.title }}</h3>
              <p>{{ item.description }}</p>
              <button type="button">查看案例 <el-icon><ArrowRight /></el-icon></button>
            </div>
          </article>
        </div>
        <div v-if="!loading && !cases.length" class="portfolio-empty">没有找到匹配的案例，试试调整筛选条件。</div>
      </section>

      <section id="process" class="public-process">
        <div class="section-intro compact">
          <div>
            <span class="public-eyebrow">HOW WE WORK</span>
            <h2>每一步，都清晰可见</h2>
          </div>
        </div>
        <div class="process-list">
          <div><strong>01</strong><span>需求沟通</span></div>
          <div><strong>02</strong><span>方向确认</span></div>
          <div><strong>03</strong><span>设计深化</span></div>
          <div><strong>04</strong><span>成果交付</span></div>
        </div>
      </section>
    </main>

    <el-dialog v-model="contactVisible" title="联系我们" width="480px" align-center class="contact-dialog">
      <p class="contact-dialog-intro">工作日 09:30 - 18:30，我们会尽快回复你的咨询。</p>
      <div class="contact-list">
        <div v-for="item in contactItems" :key="item.label" class="contact-item">
          <el-icon><component :is="item.icon" /></el-icon>
          <div><span>{{ item.label }}</span><strong>{{ item.value }}</strong></div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, LocationInformation, Message, Phone, Search, Service } from '@element-plus/icons-vue'
import { listPortfolioCases } from '@/api'
import type { PortfolioCaseVO } from '@/models'

const router = useRouter()
const loading = ref(false)
const cases = ref<PortfolioCaseVO[]>([])
const contactVisible = ref(false)
const filters = reactive({ keyword: '', industry: '', style: '', serviceType: '' })
const contactItems = [
  { label: '公司前台电话', value: '010-8888 6666', icon: Phone },
  { label: '公司客服电话', value: '400-888-2026', icon: Service },
  { label: '公司邮箱', value: 'service@advertisement-design.com', icon: Message },
  { label: '公司地址', value: '北京市朝阳区创意设计产业园 A 座 801', icon: LocationInformation }
]

const industries = computed(() => [...new Set(cases.value.map((item) => item.industry))])
const styles = computed(() => [...new Set(cases.value.map((item) => item.style))])
const serviceTypes = computed(() => [...new Set(cases.value.map((item) => item.serviceType))])

const coverPalettes = [
  ['#102a43', '#1d72a3'], ['#1d3b2a', '#86a840'], ['#633d29', '#e4a766'],
  ['#e84e34', '#ffc940'], ['#25213f', '#947ed9'], ['#1d5260', '#5ec2be']
]

function coverStyle(item: PortfolioCaseVO) {
  const colors = coverPalettes[item.id % coverPalettes.length]
  return { '--cover-a': colors[0], '--cover-b': colors[1] }
}

async function load() {
  loading.value = true
  try {
    const page = await listPortfolioCases({ ...filters, page: 1, size: 30 })
    cases.value = page.records
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '案例加载失败')
  } finally {
    loading.value = false
  }
}

function reset() {
  Object.assign(filters, { keyword: '', industry: '', style: '', serviceType: '' })
}

function goLogin() {
  router.push('/login')
}

watch(() => [filters.industry, filters.style, filters.serviceType], load)
onMounted(load)
</script>

<style>
.public-site { min-height: 100vh; background: #fbfcfe; color: #172033; }
.public-header { height: 78px; padding: 0 max(32px, calc((100vw - 1280px) / 2)); display: flex; align-items: center; justify-content: space-between; background: rgba(251, 252, 254, .94); border-bottom: 1px solid #e8edf3; position: sticky; top: 0; z-index: 20; backdrop-filter: blur(14px); }
.public-brand { padding: 0; border: 0; background: transparent; display: flex; align-items: center; gap: 11px; text-align: left; cursor: pointer; color: inherit; }
.public-brand-mark { width: 37px; height: 37px; display: grid; place-items: center; border-radius: 10px; color: white; background: #1367d1; font-size: 14px; font-weight: 800; }
.public-brand strong, .public-brand small { display: block; }
.public-brand strong { font-size: 15px; }.public-brand small { margin-top: 2px; color: #718096; font-size: 11px; }
.public-nav { display: flex; align-items: center; gap: 26px; font-size: 14px; }.public-nav a { color: #536174; }.public-nav a:hover { color: #1367d1; }
.text-action, .filter-reset { border: 0; padding: 0; background: transparent; color: #536174; cursor: pointer; font-size: 14px; }
.consult-button { border: 0; border-radius: 7px; min-height: 38px; padding: 0 17px; background: #1367d1; color: white; cursor: pointer; font-size: 14px; font-weight: 700; box-shadow: 0 8px 18px rgba(19, 103, 209, .2); }
.consult-button.large { min-height: 46px; padding: 0 20px; }
.public-intro { max-width: 1280px; min-height: 510px; margin: auto; padding: 84px 28px 68px; display: grid; grid-template-columns: 1.05fr .95fr; gap: 64px; align-items: center; }
.public-intro-copy { max-width: 580px; }.public-eyebrow { display: inline-block; color: #1367d1; font-size: 11px; font-weight: 800; letter-spacing: 1.2px; }
.public-intro h1 { margin: 18px 0; font-size: 52px; line-height: 1.18; letter-spacing: 0; }.public-intro p { max-width: 440px; margin: 0; color: #637188; font-size: 16px; line-height: 1.9; }
.public-art { height: 360px; position: relative; overflow: hidden; border-radius: 8px; background: linear-gradient(135deg, #dbe7fb, #f2f6fe 65%); }.public-art::before { content: ''; width: 250px; height: 250px; border: 32px solid #ffd76e; border-radius: 50%; position: absolute; right: -65px; top: -85px; }.art-card { position: absolute; display: flex; flex-direction: column; justify-content: space-between; padding: 23px; color: white; }.art-card span { font-size: 11px; font-weight: 700; letter-spacing: 1.2px; }.art-card b { font-size: 52px; line-height: 1; }.art-card-main { width: 250px; height: 278px; left: 58px; bottom: 0; background: #1367d1; box-shadow: 20px 20px 0 #1d2b47; }.art-card-side { width: 165px; height: 158px; right: 48px; bottom: 41px; background: #f2b20e; color: #25334d; }.art-line { position: absolute; left: 30px; right: 30px; top: 43px; border-top: 2px solid #25334d; opacity: .25; }
.portfolio-section, .public-process { max-width: 1280px; margin: auto; padding: 74px 28px; }.portfolio-section { border-top: 1px solid #e8edf3; }.section-intro { display: flex; align-items: flex-end; justify-content: space-between; gap: 30px; margin-bottom: 31px; }.section-intro h2 { margin: 9px 0 0; font-size: 34px; }.section-intro p { max-width: 350px; margin: 0; color: #718096; font-size: 14px; line-height: 1.7; }
.portfolio-filter-bar { display: grid; grid-template-columns: 1.5fr repeat(3, 1fr) auto; gap: 10px; padding: 12px; margin-bottom: 27px; border: 1px solid #e7edf5; background: #fff; border-radius: 8px; }.portfolio-filter-bar .el-input__wrapper, .portfolio-filter-bar .el-select__wrapper { box-shadow: none; background: #f6f8fb; min-height: 39px; }.filter-reset { padding: 0 11px; color: #47617e; }
.portfolio-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; }.portfolio-card { min-width: 0; border: 1px solid #e7edf5; background: #fff; cursor: pointer; transition: transform .2s ease, box-shadow .2s ease; }.portfolio-card:hover { transform: translateY(-4px); box-shadow: 0 18px 38px rgba(30, 57, 89, .13); }.portfolio-cover { aspect-ratio: 1.28; padding: 18px; display: flex; flex-direction: column; justify-content: space-between; color: #fff; background: linear-gradient(135deg, var(--cover-a), var(--cover-b)); overflow: hidden; position: relative; }.portfolio-cover::after { content: ''; width: 110px; height: 110px; position: absolute; right: -29px; bottom: -41px; border: 17px solid rgba(255,255,255,.3); border-radius: 50%; }.portfolio-cover span { font-size: 12px; font-weight: 700; z-index: 1; }.portfolio-cover b { font-size: 27px; line-height: 1.1; z-index: 1; }.portfolio-card-copy { padding: 19px 19px 17px; }.portfolio-card-copy > span { color: #6983a1; font-size: 12px; }.portfolio-card h3 { margin: 8px 0; font-size: 18px; }.portfolio-card p { min-height: 44px; margin: 0; color: #728096; font-size: 13px; line-height: 1.7; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }.portfolio-card button { margin-top: 15px; padding: 0; border: 0; background: transparent; color: #1367d1; display: inline-flex; align-items: center; gap: 5px; cursor: pointer; font-size: 13px; font-weight: 700; }.portfolio-empty { padding: 58px; text-align: center; color: #718096; border: 1px dashed #cbd8e6; }
.public-process { padding-top: 34px; padding-bottom: 80px; }.section-intro.compact { margin-bottom: 23px; }.process-list { display: grid; grid-template-columns: repeat(4, 1fr); border-top: 1px solid #dce5ef; border-bottom: 1px solid #dce5ef; }.process-list div { padding: 22px 18px; border-right: 1px solid #dce5ef; display: grid; gap: 12px; }.process-list div:last-child { border: 0; }.process-list strong { color: #1367d1; font-size: 13px; }.process-list span { font-size: 16px; font-weight: 700; }
.contact-dialog .el-dialog { border-radius: 8px; }.contact-dialog .el-dialog__header { margin-right: 0; padding: 23px 25px 15px; border-bottom: 1px solid #edf1f5; }.contact-dialog .el-dialog__title { color: #263448; font-size: 18px; font-weight: 750; }.contact-dialog .el-dialog__body { padding: 17px 25px 27px; }.contact-dialog-intro { margin: 0 0 16px; color: #7d8b9c; font-size: 13px; }.contact-list { display: grid; }.contact-item { padding: 14px 0; border-top: 1px solid #edf1f5; display: flex; align-items: center; gap: 11px; }.contact-item > .el-icon { width: 34px; height: 34px; border-radius: 7px; display: grid; place-items: center; background: #eaf3ff; color: #1367d1; font-size: 17px; }.contact-item div { display: grid; gap: 3px; }.contact-item span { color: #8190a2; font-size: 11px; }.contact-item strong { color: #35465c; font-size: 13px; font-weight: 650; }
@media (max-width: 820px) { .public-header { padding: 0 18px; }.public-nav { gap: 12px; }.public-nav a { display: none; }.public-intro { grid-template-columns: 1fr; padding-top: 52px; gap: 35px; }.public-intro h1 { font-size: 38px; }.public-art { height: 270px; }.art-card-main { left: 32px; width: 200px; height: 220px; }.art-card-side { right: 25px; bottom: 24px; }.portfolio-grid { grid-template-columns: 1fr; }.portfolio-filter-bar { grid-template-columns: 1fr 1fr; }.portfolio-filter-bar > :first-child { grid-column: 1 / -1; }.section-intro { align-items: flex-start; flex-direction: column; }.process-list { grid-template-columns: 1fr 1fr; }.process-list div:nth-child(2) { border-right: 0; }.process-list div:nth-child(-n+2) { border-bottom: 1px solid #dce5ef; } }
</style>
