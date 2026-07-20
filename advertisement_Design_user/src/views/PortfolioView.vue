<template>
  <div class="panel-grid">
    <PageSection title="案例库" subtitle="公开作品案例浏览">
      <div class="form-grid" style="margin-bottom: 16px">
        <el-input v-model="filters.keyword" placeholder="关键词" clearable />
        <el-input v-model="filters.industry" placeholder="行业" clearable />
        <el-input v-model="filters.style" placeholder="风格" clearable />
        <el-input v-model="filters.serviceType" placeholder="服务类型" clearable />
      </div>
      <div class="table-actions" style="margin-bottom: 16px">
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <div class="grid-2">
        <button
          v-for="item in cases"
          :key="item.id"
          class="card-item"
          :class="{ active: detail?.id === item.id }"
          @click="detail = item"
        >
          <div class="card-meta">
            <strong>{{ item.title }}</strong>
            <span class="badge">{{ item.serviceType }}</span>
          </div>
          <div class="muted" style="margin-top: 8px">{{ item.industry }} / {{ item.style }}</div>
          <div class="muted">{{ item.description }}</div>
        </button>
      </div>
    </PageSection>

    <PageSection title="案例详情" subtitle="支持图片与文字替换">
      <div v-if="detail" class="stack">
        <div class="card-item">
          <div class="card-meta">
            <strong>{{ detail.title }}</strong>
            <span class="badge primary">{{ detail.status }}</span>
          </div>
          <div class="muted" style="margin-top: 8px">{{ detail.description }}</div>
        </div>
        <div class="card-item">
          <div class="section-head" style="margin-bottom: 8px">
            <h4>图片链接</h4>
          </div>
          <div class="stack">
            <div v-for="url in detail.imageUrls || []" :key="url" class="muted">{{ url }}</div>
          </div>
        </div>
      </div>
      <div v-else class="empty">请选择一个案例。</div>
    </PageSection>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import PageSection from '@/components/PageSection.vue'
import { listPortfolioCases } from '@/api'
import type { PortfolioCaseVO } from '@/models'

const cases = ref<PortfolioCaseVO[]>([])
const detail = ref<PortfolioCaseVO | null>(null)
const filters = reactive({
  keyword: '',
  industry: '',
  style: '',
  serviceType: ''
})

async function load() {
  const page = await listPortfolioCases({
    keyword: filters.keyword || undefined,
    industry: filters.industry || undefined,
    style: filters.style || undefined,
    serviceType: filters.serviceType || undefined,
    page: 1,
    size: 20
  })
  cases.value = page.records
  detail.value = detail.value || cases.value[0] || null
}

function reset() {
  filters.keyword = ''
  filters.industry = ''
  filters.style = ''
  filters.serviceType = ''
  load()
}

onMounted(load)
</script>
