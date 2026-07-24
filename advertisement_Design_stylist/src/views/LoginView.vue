<template>
  <div class="designer-login-page">
    <div class="login-card">
      <section class="hero-panel">
        <div class="brand-area">
          <div class="logo-icon">AD</div>
          <h1 class="brand-name">{{ appShortName }}</h1>
          <p class="brand-sub">{{ appSubTitle }}</p>
        </div>

        <div class="feature-stack">
          <div class="feature-item">
            <span class="feature-index">01</span>
            <div>
              <strong>项目流转</strong>
              <p>从接单到交付，按阶段推进设计任务。</p>
            </div>
          </div>
          <div class="feature-item">
            <span class="feature-index">02</span>
            <div>
              <strong>文件归档</strong>
              <p>统一记录设计稿、确认稿与交付物。</p>
            </div>
          </div>
          <div class="feature-item">
            <span class="feature-index">03</span>
            <div>
              <strong>案例维护</strong>
              <p>沉淀项目成果，方便后续展示与复用。</p>
            </div>
          </div>
        </div>
      </section>

      <form class="login-form" @submit.prevent="submit">
        <div class="form-head">
          <h2>设计师登录</h2>
          <p>使用工号或邮箱进入工作台。</p>
        </div>

        <div class="field">
          <el-input
            v-model="form.email"
            size="large"
            :prefix-icon="User"
            placeholder="请输入设计师工号/用户名"
            autocomplete="username"
          />
        </div>

        <div class="field">
          <el-input
            v-model="form.password"
            size="large"
            type="password"
            show-password
            :prefix-icon="Lock"
            placeholder="请输入密码"
            autocomplete="current-password"
          />
        </div>

        <el-button class="login-btn" type="primary" native-type="submit" size="large" :loading="loading" :icon="ArrowRight">
          登 录
        </el-button>

        <div class="footer-links"><button type="button" class="footer-link" @click="fillDesigner">使用演示设计师账号</button></div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Lock, User } from '@element-plus/icons-vue'
import { appShortName, appSubTitle } from '@/config'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ email: 'designer@example.com', password: '123456' })

function fillDesigner() {
  form.email = 'designer@example.com'
  form.password = '123456'
}

async function submit() {
  try {
    loading.value = true
    await auth.login(form.email, form.password)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/reception'
    await router.replace(redirect)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.designer-login-page {
  min-height: 100vh;
  padding: 24px;
  display: grid;
  place-items: center;
  overflow: hidden;
  background:
    radial-gradient(circle at 15% 50%, rgba(76, 29, 149, 0.4), transparent 25%),
    radial-gradient(circle at 85% 30%, rgba(14, 165, 233, 0.4), transparent 25%),
    linear-gradient(135deg, #0f172a 0%, #1e1b4b 100%);
  background-size: 150% 150%;
  animation: fluidMove 15s ease infinite alternate;
}

.login-card {
  width: min(920px, calc(100vw - 40px));
  min-height: 560px;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  border-left: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  position: relative;
  overflow: hidden;
}

.login-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.06), transparent 36%),
    linear-gradient(225deg, rgba(56, 189, 248, 0.08), transparent 42%);
  pointer-events: none;
}

.hero-panel,
.login-form {
  position: relative;
  z-index: 1;
}

.hero-panel {
  padding: 52px 46px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 32px;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  text-align: left;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), rgba(255, 255, 255, 0)),
    linear-gradient(135deg, rgba(15, 23, 42, 0.35), rgba(15, 23, 42, 0.08));
}

.brand-area {
  margin-bottom: 0;
}

.logo-icon {
  width: 48px;
  height: 48px;
  margin: 0 auto 16px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #38bdf8, #818cf8);
  box-shadow: 0 10px 15px -3px rgba(56, 189, 248, 0.3);
}

.brand-name {
  margin: 0 0 8px;
  color: #fff;
  font-size: 24px;
  font-weight: 300;
  letter-spacing: 4px;
}

.brand-sub {
  margin: 0;
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
  letter-spacing: 1px;
}

.feature-stack {
  display: grid;
  gap: 16px;
}

.feature-item {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.feature-index {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  color: #b9d8ff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1px;
  background: linear-gradient(135deg, rgba(56, 189, 248, 0.18), rgba(129, 140, 248, 0.18));
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.feature-item strong {
  display: block;
  margin-bottom: 6px;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
}

.feature-item p {
  margin: 0;
  color: rgba(255, 255, 255, 0.64);
  line-height: 1.7;
  font-size: 13px;
}

.login-form {
  padding: 52px 44px;
  display: grid;
  align-content: center;
  gap: 16px;
  text-align: left;
  background: rgba(16, 20, 45, 0.16);
}

.form-head {
  margin-bottom: 8px;
}

.form-head h2 {
  margin: 0 0 8px;
  color: #fff;
  font-size: 28px;
  font-weight: 500;
  letter-spacing: 1px;
}

.form-head p {
  margin: 0;
  color: rgba(255, 255, 255, 0.52);
  line-height: 1.7;
  font-size: 13px;
}

.field {
  position: relative;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 54px;
  border-radius: 12px;
  padding: 1px 16px;
  background: rgba(0, 0, 0, 0.2);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.1);
  transition:
    background 0.3s ease,
    box-shadow 0.3s ease,
    border-color 0.3s ease;
}

.login-form :deep(.el-input__wrapper:hover),
.login-form :deep(.el-input__wrapper.is-focus) {
  background: rgba(0, 0, 0, 0.4);
  box-shadow:
    inset 0 0 0 1px rgba(56, 189, 248, 0.5),
    0 0 0 4px rgba(56, 189, 248, 0.1);
}

.login-form :deep(.el-input__inner) {
  color: #fff;
  font-size: 14px;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.3);
}

.login-form :deep(.el-input__prefix-inner) {
  color: rgba(255, 255, 255, 0.4);
}

.login-form :deep(.el-input__suffix-inner) {
  color: rgba(255, 255, 255, 0.4);
}

.login-btn {
  width: 100%;
  height: 52px;
  margin-top: 6px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(90deg, #0ea5e9, #6366f1);
  color: #fff;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 2px;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px -5px rgba(99, 102, 241, 0.4);
}

.login-btn:active {
  transform: translateY(0);
}

.footer-links {
  margin-top: 4px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
}

.footer-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: color 0.3s ease;
}

.footer-link:hover {
  color: #38bdf8;
}

@keyframes fluidMove {
  0% {
    background-position: 0% 50%;
  }
  100% {
    background-position: 100% 50%;
  }
}

@media (max-width: 480px) {
  .designer-login-page {
    padding: 16px;
  }

  .login-card {
    width: min(100%, 420px);
    min-height: auto;
    grid-template-columns: 1fr;
    border-radius: 20px;
  }

  .hero-panel {
    padding: 28px 22px 22px;
    border-right: 0;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  }

  .login-form {
    padding: 28px 22px 30px;
  }

  .brand-name {
    font-size: 22px;
    letter-spacing: 3px;
  }
}
</style>
