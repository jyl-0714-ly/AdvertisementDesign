<template>
  <div class="case-page">
    <header class="case-header">
      <button type="button" class="case-back" @click="router.push('/')"><el-icon><ArrowLeft /></el-icon> 返回作品集</button>
      <button type="button" class="consult-button" @click="router.push('/login')">免费咨询</button>
    </header>
    <main v-loading="loading" class="case-main">
      <template v-if="detail">
        <section class="case-hero" :style="coverStyle(detail)">
          <span>{{ detail.industry }} / {{ detail.style }}</span>
          <h1>{{ detail.title }}</h1>
          <p>{{ detail.serviceType }}</p>
        </section>
        <section class="case-content">
          <aside class="case-meta">
            <div><span>行业</span><strong>{{ detail.industry }}</strong></div>
            <div><span>风格</span><strong>{{ detail.style }}</strong></div>
            <div><span>服务</span><strong>{{ detail.serviceType }}</strong></div>
          </aside>
          <article>
            <span class="public-eyebrow">DESIGN NOTE</span>
            <h2>设计说明</h2>
            <p>{{ detail.description }}</p>
            <div class="case-gallery">
              <button v-for="(image, index) in gallery" :key="image" type="button" :class="{ active: index === activeIndex }" @click="activeIndex = index">
                <span>方案 {{ String(index + 1).padStart(2, '0') }}</span><b>{{ galleryLabels[index] }}</b>
              </button>
            </div>
            <div class="case-large-image" :class="`image-${activeIndex}`">
              <span>VISUAL SYSTEM</span><strong>{{ galleryLabels[activeIndex] }}</strong>
            </div>
          </article>
        </section>
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

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref<PortfolioCaseVO | null>(null)
const activeIndex = ref(0)
const galleryLabels = ['品牌主视觉', '应用延展', '细节规范']
const gallery = computed(() => detail.value?.imageUrls?.length ? detail.value.imageUrls : galleryLabels)

function coverStyle(item: PortfolioCaseVO) {
  const colors = [['#102a43', '#1d72a3'], ['#1d3b2a', '#86a840'], ['#633d29', '#e4a766'], ['#e84e34', '#ffc940'], ['#25213f', '#947ed9'], ['#1d5260', '#5ec2be']]
  const palette = colors[item.id % colors.length]
  return { '--case-a': palette[0], '--case-b': palette[1] }
}

onMounted(async () => {
  loading.value = true
  try { detail.value = await getPortfolioCase(Number(route.params.id)) }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '案例加载失败') }
  finally { loading.value = false }
})
</script>

<style>
.case-page { min-height: 100vh; background: #fbfcfe; color: #172033; }.case-header { height: 72px; max-width: 1280px; padding: 0 28px; margin: auto; display: flex; align-items: center; justify-content: space-between; }.case-back { border: 0; padding: 0; background: transparent; display: inline-flex; align-items: center; gap: 7px; color: #40516a; cursor: pointer; font-size: 14px; }.case-main { max-width: 1224px; margin: auto; padding: 8px 28px 70px; }.case-hero { min-height: 370px; padding: 48px; display: flex; flex-direction: column; justify-content: flex-end; color: #fff; background: linear-gradient(132deg, var(--case-a), var(--case-b)); position: relative; overflow: hidden; }.case-hero::after { content: ''; width: 360px; height: 360px; position: absolute; right: -90px; top: -120px; border: 38px solid rgba(255,255,255,.18); border-radius: 50%; }.case-hero span, .case-hero p, .case-hero h1 { position: relative; z-index: 1; }.case-hero span { font-size: 13px; font-weight: 700; }.case-hero h1 { max-width: 650px; margin: 16px 0 12px; font-size: 46px; line-height: 1.15; }.case-hero p { margin: 0; font-size: 17px; }.case-content { padding: 54px 0; display: grid; grid-template-columns: 220px minmax(0, 1fr); gap: 72px; }.case-meta { border-top: 1px solid #dfe7f1; }.case-meta div { padding: 17px 0; border-bottom: 1px solid #dfe7f1; display: grid; gap: 6px; }.case-meta span { color: #718096; font-size: 12px; }.case-meta strong { font-size: 14px; }.case-content h2 { margin: 11px 0 16px; font-size: 32px; }.case-content article > p { max-width: 700px; margin: 0; color: #64748b; line-height: 2; }.case-gallery { margin-top: 38px; display: flex; border-bottom: 1px solid #dfe7f1; }.case-gallery button { flex: 1; padding: 13px 0 15px; border: 0; border-bottom: 2px solid transparent; background: transparent; color: #718096; cursor: pointer; text-align: left; display: grid; gap: 5px; }.case-gallery button.active { color: #1367d1; border-color: #1367d1; }.case-gallery span { font-size: 11px; }.case-gallery b { font-size: 14px; }.case-large-image { height: 370px; margin-top: 22px; padding: 28px; display: flex; flex-direction: column; justify-content: space-between; color: white; background: linear-gradient(135deg, #25324e, #6089b5); }.case-large-image.image-1 { background: linear-gradient(135deg, #774f33, #e7a865); }.case-large-image.image-2 { background: linear-gradient(135deg, #215052, #82c4ae); }.case-large-image span { font-size: 12px; font-weight: 700; letter-spacing: 1.1px; }.case-large-image strong { font-size: 36px; }.case-not-found { padding: 100px 0; text-align: center; color: #718096; }
@media (max-width: 700px) { .case-main, .case-header { padding-left: 18px; padding-right: 18px; }.case-hero { min-height: 300px; padding: 27px; }.case-hero h1 { font-size: 34px; }.case-content { grid-template-columns: 1fr; gap: 34px; padding: 34px 0; }.case-meta { display: grid; grid-template-columns: repeat(3, 1fr); gap: 11px; }.case-meta div { padding: 10px 0; }.case-large-image { height: 260px; } }
</style>
