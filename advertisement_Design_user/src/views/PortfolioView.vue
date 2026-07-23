<template>
  <div class="workspace-portfolio" v-loading="loading">
    <header class="workspace-portfolio-head">
      <div><span class="workspace-eyebrow">CASE LIBRARY</span><h1>全部案例库</h1><p>登录客户专享案例库，按合作场景、行业与风格快速定位参考方向。</p></div>
      <span class="portfolio-count">{{ filteredCases.length }} 个案例</span>
    </header>

    <div class="workspace-category-tabs" role="tablist" aria-label="案例分类">
      <button v-for="option in portfolioCategoryOptions" :key="option.value || 'all'" type="button" :class="{ active: filters.category === option.value }" @click="filters.category = option.value">{{ option.label }}</button>
    </div>
    <div class="workspace-filter-bar">
      <el-input v-model="filters.keyword" :prefix-icon="Search" clearable placeholder="搜索项目、行业或风格" />
      <el-select v-model="filters.industry" clearable placeholder="行业"><el-option v-for="item in industries" :key="item" :label="item" :value="item" /></el-select>
      <el-select v-model="filters.style" clearable placeholder="风格"><el-option v-for="item in styles" :key="item" :label="item" :value="item" /></el-select>
      <button type="button" @click="reset">重置</button>
    </div>

    <div class="workspace-case-grid">
      <article v-for="item in filteredCases" :key="item.id" class="workspace-case-card" @click="openCase(item.id)">
        <div class="workspace-case-cover" :style="coverStyle(item)"><span>{{ portfolioCategoryLabel(item.category) }} · {{ item.industry }}</span><b>{{ item.serviceType }}</b><i>{{ String(item.id).padStart(2, '0') }}</i></div>
        <div class="workspace-case-copy"><span>{{ item.style }}</span><h2>{{ item.title }}</h2><p>{{ item.description }}</p><button type="button">查看详情 <el-icon><ArrowRight /></el-icon></button></div>
      </article>
    </div>
    <div v-if="!loading && !filteredCases.length" class="workspace-portfolio-empty">没有找到匹配案例。</div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Search } from '@element-plus/icons-vue'
import { listPortfolioCases } from '@/api'
import type { PortfolioCaseVO } from '@/models'
import { portfolioCategoryLabel, portfolioCategoryOptions } from '@/utils/portfolioLabels'
import { rememberPortfolioPosition, restorePortfolioPosition } from '@/utils/portfolioReturn'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const cases = ref<PortfolioCaseVO[]>([])
const filters = reactive({ keyword: '', category: '' as PortfolioCaseVO['category'] | '', industry: '', style: '' })
const industries = computed(() => [...new Set(cases.value.map((item) => item.industry))])
const styles = computed(() => [...new Set(cases.value.map((item) => item.style))])
const filteredCases = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return cases.value.filter((item) => (!filters.category || item.category === filters.category) && (!filters.industry || item.industry === filters.industry) && (!filters.style || item.style === filters.style) && (!keyword || `${item.title}${item.description}${item.industry}${item.style}${item.serviceType}`.toLowerCase().includes(keyword)))
})
const palettes = [['#173c61', '#6ca8d1'], ['#1b4d3e', '#91bd5a'], ['#553a2d', '#d7975e'], ['#b93c38', '#f6b939'], ['#3d3267', '#9f8dde'], ['#14555e', '#73c7c1']]
function coverStyle(item: PortfolioCaseVO) { const pair = palettes[item.id % palettes.length]; return { '--case-cover-a': pair[0], '--case-cover-b': pair[1] } }
function reset() { Object.assign(filters, { keyword: '', category: '', industry: '', style: '' }) }
function openCase(id: number) { rememberPortfolioPosition(route.fullPath); router.push(`/cases/${id}`) }
async function loadCases() {
  loading.value = true
  try { cases.value = (await listPortfolioCases({ page: 1, size: 50 })).records }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '案例加载失败') }
  finally { loading.value = false }
  await nextTick()
  restorePortfolioPosition(route.fullPath)
}
onMounted(loadCases)
</script>

<style>
.workspace-portfolio { min-height: calc(100vh - 111px); }.workspace-portfolio-head { padding: 14px 0 28px; display: flex; align-items: flex-end; justify-content: space-between; gap: 25px; }.workspace-portfolio-head h1 { margin: 7px 0; font-size: 30px; }.workspace-portfolio-head p { margin: 0; color: #748398; font-size: 14px; }.portfolio-count { padding: 5px 9px; color: #5078a4; background: #e8f2ff; font-size: 12px; }.workspace-category-tabs { margin-bottom: 12px; display: flex; gap: 26px; overflow-x: auto; border-bottom: 1px solid #dce6f0; }.workspace-category-tabs button { min-width: max-content; padding: 0 0 11px; border: 0; border-bottom: 2px solid transparent; background: transparent; color: #748398; cursor: pointer; font-size: 13px; }.workspace-category-tabs button:hover, .workspace-category-tabs button.active { color: #172033; }.workspace-category-tabs button.active { border-bottom-color: #172033; font-weight: 700; }.workspace-filter-bar { padding: 11px; border: 1px solid #dce6f0; background: #fff; display: grid; grid-template-columns: 1.45fr 1fr 1fr auto; gap: 10px; }.workspace-filter-bar .el-input__wrapper, .workspace-filter-bar .el-select__wrapper { min-height: 38px; box-shadow: none; background: #f5f8fc; }.workspace-filter-bar > button { padding: 0 11px; border: 0; background: transparent; color: #5c7089; cursor: pointer; font-size: 13px; }.workspace-case-grid { margin-top: 17px; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 15px; }.workspace-case-card { border: 1px solid #dce6f0; background: #fff; cursor: pointer; transition: transform .18s ease, box-shadow .18s ease; }.workspace-case-card:hover { transform: translateY(-3px); box-shadow: 0 16px 30px rgba(26, 50, 78, .11); }.workspace-case-cover { aspect-ratio: 1.36; padding: 16px; display: flex; flex-direction: column; justify-content: space-between; position: relative; overflow: hidden; color: #fff; background: linear-gradient(135deg, var(--case-cover-a), var(--case-cover-b)); }.workspace-case-cover::after { content: ''; width: 123px; height: 123px; position: absolute; right: -35px; bottom: -34px; border: 19px solid rgba(255,255,255,.24); border-radius: 50%; }.workspace-case-cover span { font-size: 11px; font-weight: 700; z-index: 1; }.workspace-case-cover b { max-width: 150px; font-size: 22px; line-height: 1.2; z-index: 1; }.workspace-case-cover i { position: absolute; right: 17px; top: 15px; color: rgba(255,255,255,.67); font-size: 12px; font-style: normal; font-weight: 800; }.workspace-case-copy { padding: 16px; }.workspace-case-copy > span { color: #6683a2; font-size: 11px; }.workspace-case-copy h2 { margin: 7px 0; font-size: 16px; }.workspace-case-copy p { min-height: 39px; margin: 0; overflow: hidden; color: #77869a; font-size: 12px; line-height: 1.65; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }.workspace-case-copy button { margin-top: 13px; padding: 0; border: 0; background: transparent; display: inline-flex; align-items: center; gap: 3px; color: #1367d1; cursor: pointer; font-size: 12px; font-weight: 700; }.workspace-portfolio-empty { margin-top: 17px; padding: 70px; border: 1px dashed #ccd8e6; color: #8795a7; text-align: center; }
@media (max-width: 920px) { .workspace-case-grid { grid-template-columns: 1fr 1fr; }.workspace-filter-bar { grid-template-columns: 1fr 1fr; }.workspace-filter-bar > :first-child { grid-column: 1 / -1; } } @media (max-width: 580px) { .workspace-portfolio-head { align-items: flex-start; flex-direction: column; }.workspace-case-grid { grid-template-columns: 1fr; } }
</style>
