<template>
  <div class="login-wrap">
    <div class="login-card">
      <div class="login-copy">
        <BrandMark :title="appName" :subtitle="appSubTitle" initials="AD" />
        <div class="stack">
          <h1 style="margin: 0; font-size: 28px">客户工作台</h1>
          <p class="muted" style="margin: 0; line-height: 1.7">
            用于查看项目进度、浏览案例、与设计师保持消息沟通。
          </p>
        </div>
        <div class="surface pad">
          <div class="section-head" style="margin-bottom: 8px">
            <h4>演示账号</h4>
          </div>
          <div class="stack">
            <div class="badge primary">客户：customer@example.com / 123456</div>
            <div class="badge">设计师：designer@example.com / 123456</div>
          </div>
        </div>
      </div>
      <div class="login-form">
        <div class="section-head">
          <h2>登录</h2>
        </div>
        <el-form :model="form" label-position="top" @submit.prevent="submit">
          <el-form-item label="邮箱">
            <el-input v-model="form.email" autocomplete="username" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" show-password autocomplete="current-password" />
          </el-form-item>
          <div class="table-actions">
            <el-button type="primary" :loading="loading" @click="submit">进入系统</el-button>
            <el-button @click="fillCustomer">客户账号</el-button>
            <el-button @click="fillDesigner">设计师账号</el-button>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import BrandMark from '@/components/BrandMark.vue'
import { appName, appSubTitle } from '@/config'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({
  email: 'customer@example.com',
  password: '123456'
})

function fillCustomer() {
  form.email = 'customer@example.com'
  form.password = '123456'
}

function fillDesigner() {
  form.email = 'designer@example.com'
  form.password = '123456'
}

async function submit() {
  try {
    loading.value = true
    await auth.login(form.email, form.password)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>
