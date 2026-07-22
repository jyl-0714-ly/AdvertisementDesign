<template>
  <div class="panel-grid">
    <PageSection title="案例管理" subtitle="创建、编辑和下线作品案例">
      <div class="table-actions" style="margin-bottom: 16px">
        <el-button type="primary" @click="openCreate">新建案例</el-button>
        <el-button @click="load">刷新</el-button>
      </div>
      <div class="grid-2">
        <button
          v-for="item in cases"
          :key="item.id"
          class="card-item"
          :class="{ active: detail?.id === item.id }"
          @click="select(item)"
        >
          <div class="card-meta">
            <strong>{{ item.title }}</strong>
            <span class="badge">{{ portfolioStatusLabel(item.status) }}</span>
          </div>
          <div class="muted" style="margin-top: 8px">{{ item.industry }} / {{ item.style }}</div>
          <div class="muted">{{ item.serviceType }}</div>
        </button>
      </div>
    </PageSection>

    <PageSection title="案例详情" subtitle="支持直接编辑">
      <div v-if="detail" class="stack">
        <div class="card-item">
          <div class="card-meta">
            <strong>{{ detail.title }}</strong>
            <span class="badge primary">{{ portfolioStatusLabel(detail.status) }}</span>
          </div>
          <div class="muted" style="margin-top: 8px">{{ detail.description }}</div>
        </div>
        <div class="table-actions">
          <el-button type="primary" @click="openEdit(detail)">编辑</el-button>
          <el-button type="danger" plain @click="remove(detail.id)">删除</el-button>
        </div>
      </div>
      <div v-else class="empty">请选择一个案例。</div>
    </PageSection>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px">
      <el-form :model="form" label-position="top" class="drawer-form">
        <el-form-item label="标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="行业">
          <el-input v-model="form.industry" />
        </el-form-item>
        <el-form-item label="风格">
          <el-input v-model="form.style" />
        </el-form-item>
        <el-form-item label="服务类型">
          <el-input v-model="form.serviceType" />
        </el-form-item>
        <el-form-item label="封面 URL">
          <el-input v-model="form.coverUrl" />
        </el-form-item>
        <el-form-item label="图片 URL（逗号分隔）">
          <el-input v-model="form.imageUrls" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="排序">
            <el-input v-model.number="form.sortOrder" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status">
              <el-option label="已发布" value="PUBLISHED" />
              <el-option label="草稿" value="DRAFT" />
              <el-option label="已下线" value="OFFLINE" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="table-actions">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageSection from '@/components/PageSection.vue'
import { createPortfolioCase, deletePortfolioCase, listPortfolioCases, updatePortfolioCase } from '@/api'
import type { PortfolioCaseVO } from '@/models'
import { portfolioStatusLabel } from '@/utils/displayLabels'

const cases = ref<PortfolioCaseVO[]>([])
const detail = ref<PortfolioCaseVO | null>(null)
const dialogVisible = ref(false)
const saving = ref(false)
const editId = ref<number | null>(null)
const form = reactive({
  title: '',
  industry: '',
  style: '',
  serviceType: '',
  coverUrl: '',
  imageUrls: '',
  description: '',
  sortOrder: 0,
  status: 'PUBLISHED'
})

const dialogTitle = computed(() => (editId.value ? '编辑案例' : '新建案例'))

async function load() {
  const page = await listPortfolioCases({ page: 1, size: 50 })
  cases.value = page.records
  detail.value = detail.value || cases.value[0] || null
}

function select(item: PortfolioCaseVO) {
  detail.value = item
}

function openCreate() {
  editId.value = null
  Object.assign(form, {
    title: '',
    industry: '',
    style: '',
    serviceType: '',
    coverUrl: '',
    imageUrls: '',
    description: '',
    sortOrder: 0,
    status: 'PUBLISHED'
  })
  dialogVisible.value = true
}

function openEdit(item: PortfolioCaseVO) {
  editId.value = item.id
  Object.assign(form, {
    title: item.title,
    industry: item.industry,
    style: item.style,
    serviceType: item.serviceType,
    coverUrl: item.coverUrl,
    imageUrls: (item.imageUrls || []).join(', '),
    description: item.description,
    sortOrder: item.sortOrder || 0,
    status: item.status
  })
  dialogVisible.value = true
}

async function save() {
  try {
    saving.value = true
    const payload = {
      title: form.title,
      industry: form.industry,
      style: form.style,
      serviceType: form.serviceType,
      coverUrl: form.coverUrl,
      imageUrls: form.imageUrls.split(',').map((item) => item.trim()).filter(Boolean),
      description: form.description,
      sortOrder: Number(form.sortOrder) || 0,
      status: form.status as 'DRAFT' | 'PUBLISHED' | 'OFFLINE'
    }
    if (editId.value) {
      await updatePortfolioCase(editId.value, payload)
    } else {
      await createPortfolioCase(payload)
    }
    dialogVisible.value = false
    ElMessage.success('保存成功')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该案例？', '删除案例', { type: 'warning' })
    await deletePortfolioCase(id)
    await load()
  } catch {}
}

onMounted(load)
</script>
