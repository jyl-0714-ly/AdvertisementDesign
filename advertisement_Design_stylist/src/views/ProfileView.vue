<template>
  <div class="panel-grid">
    <PageSection title="个人资料" subtitle="修改昵称、头像和手机号">
      <div class="grid-2">
        <div class="card-item">
          <div class="section-head" style="margin-bottom: 8px">
            <h4>当前信息</h4>
          </div>
          <div class="stack">
            <div class="muted">邮箱：{{ auth.user?.email || '—' }}</div>
            <div class="muted">角色：{{ auth.user?.role || '—' }}</div>
            <div class="muted">昵称：{{ auth.user?.nickname || '—' }}</div>
          </div>
        </div>
        <div class="surface pad">
          <el-form :model="form" label-position="top">
            <el-form-item label="昵称">
              <el-input v-model="form.nickname" />
            </el-form-item>
            <el-form-item label="头像">
              <el-input v-model="form.avatar" placeholder="头像 URL" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="form.phone" placeholder="手机号" />
            </el-form-item>
            <div class="table-actions">
              <el-button type="primary" :loading="saving" @click="save">保存</el-button>
              <el-button @click="reset">重置</el-button>
            </div>
          </el-form>
        </div>
      </div>
    </PageSection>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watchEffect } from 'vue'
import { ElMessage } from 'element-plus'
import PageSection from '@/components/PageSection.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const saving = ref(false)
const form = reactive({
  nickname: '',
  avatar: '',
  phone: ''
})

watchEffect(() => {
  form.nickname = auth.user?.nickname || ''
  form.avatar = auth.user?.avatar || ''
  form.phone = auth.user?.phone || ''
})

function reset() {
  form.nickname = auth.user?.nickname || ''
  form.avatar = auth.user?.avatar || ''
  form.phone = auth.user?.phone || ''
}

async function save() {
  try {
    saving.value = true
    await auth.updateProfile({
      nickname: form.nickname || null,
      avatar: form.avatar || null,
      phone: form.phone || null
    })
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}
</script>
