<template>
  <div class="customer-auth-page">
    <main class="customer-auth-shell">
      <section class="customer-auth-intro">
        <BrandMark :title="appName" :subtitle="appSubTitle" initials="AD" />

        <div class="intro-copy">
          <span class="intro-tag">客户入口</span>
          <h1>让创意交付有据可循</h1>
          <p>全流程透明化协作，随时掌握项目动态。</p>
        </div>

        <ul class="intro-list">
          <li>
            <span class="list-dot"></span>
            <div>
              <strong>实时进度</strong>
              <p>关键节点一目了然，拒绝盲目等待。</p>
            </div>
          </li>
          <li>
            <span class="list-dot"></span>
            <div>
              <strong>资料归档</strong>
              <p>历史稿件与确认记录永久保存。</p>
            </div>
          </li>
          <li>
            <span class="list-dot"></span>
            <div>
              <strong>高效沟通</strong>
              <p>针对具体稿件在线反馈，减少误解。</p>
            </div>
          </li>
        </ul>
      </section>

      <section class="customer-auth-form-panel">
        <div class="form-heading">
          <span class="welcome-badge">Welcome back</span>
          <h2>欢迎回来</h2>
          <p>选择适合你的方式登录客户工作台。</p>
        </div>

        <div class="mode-switch" role="tablist" aria-label="登录方式">
          <button type="button" :class="{ active: loginMode === 'password' }" @click="loginMode = 'password'">
            <Lock class="mode-icon" />
            <span>邮箱密码</span>
          </button>
          <button type="button" :class="{ active: loginMode === 'emailCode' }" @click="loginMode = 'emailCode'">
            <Message class="mode-icon" />
            <span>邮箱验证码</span>
          </button>
        </div>

        <el-form :model="form" class="auth-form" label-position="top" @submit.prevent="submit">
          <el-form-item label="邮箱">
            <el-input
              v-model="form.account"
              autocomplete="email"
              placeholder="请输入邮箱"
              :prefix-icon="User"
              size="large"
            />
            <button type="button" class="demo-link" @click="fillCustomer">使用演示账号登录</button>
          </el-form-item>

          <el-form-item v-if="loginMode === 'password'" label="密码">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              autocomplete="current-password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              size="large"
            />
          </el-form-item>

          <el-form-item v-else label="邮箱验证码">
            <div class="verify-row">
              <el-input
                v-model="form.emailCode"
                autocomplete="one-time-code"
                placeholder="请输入邮箱验证码"
                :prefix-icon="Message"
                size="large"
              />
              <button type="button" class="verify-btn" :disabled="codeCooldown > 0" @click="sendEmailCode">
                {{ codeCooldown > 0 ? `${codeCooldown}s` : '获取验证码' }}
              </button>
            </div>
          </el-form-item>

          <div class="auth-tools">
            <button type="button" class="plain-link" @click="forgotPassword">忘记密码？</button>
          </div>

          <el-button class="submit-btn" type="primary" :loading="loading" native-type="submit" size="large">
            登录
          </el-button>

          <div class="register-line">
            <span>还没有账号？</span>
            <button type="button" class="plain-link strong" @click="register">请点击注册！</button>
          </div>
        </el-form>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Message, User } from '@element-plus/icons-vue'
import BrandMark from '@/components/BrandMark.vue'
import { appName, appSubTitle } from '@/config'
import { useAuthStore } from '@/stores/auth'

type LoginMode = 'password' | 'emailCode'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const loginMode = ref<LoginMode>('password')
const codeCooldown = ref(0)
let codeTimer: ReturnType<typeof window.setInterval> | undefined

const form = reactive({
  account: 'customer@example.com',
  password: '123456',
  emailCode: ''
})

function fillCustomer() {
  form.account = 'customer@example.com'
  form.password = '123456'
  form.emailCode = '123456'
}

function sendEmailCode() {
  if (!form.account) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  form.emailCode = '123456'
  codeCooldown.value = 60
  ElMessage.success('验证码已发送至邮箱，演示验证码为 123456')
  if (codeTimer) window.clearInterval(codeTimer)
  codeTimer = window.setInterval(() => {
    codeCooldown.value -= 1
    if (codeCooldown.value <= 0 && codeTimer) {
      window.clearInterval(codeTimer)
      codeTimer = undefined
    }
  }, 1000)
}

function forgotPassword() {
  ElMessage.info('忘记密码功能将在后续版本接入')
}

function register() {
  ElMessage.info('注册功能将在后续版本接入')
}

async function submit() {
  if (!form.account) {
    ElMessage.warning('请输入邮箱')
    return
  }
  if (loginMode.value === 'password' && !form.password) {
    ElMessage.warning('请输入密码')
    return
  }
  if (loginMode.value === 'emailCode' && !form.emailCode) {
    ElMessage.warning('请输入邮箱验证码')
    return
  }
  if (loginMode.value === 'emailCode' && form.emailCode !== '123456') {
    ElMessage.error('验证码错误')
    return
  }

  try {
    loading.value = true
    await auth.login(form.account, loginMode.value === 'password' ? form.password : '123456')
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败')
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
  if (codeTimer) window.clearInterval(codeTimer)
})
</script>

<style>
.customer-auth-page {
  min-height: 100vh;
  padding: 28px;
  display: grid;
  place-items: center;
  overflow: hidden;
  background:
    radial-gradient(circle at 8% 12%, rgba(56, 189, 248, 0.28), transparent 24%),
    radial-gradient(circle at 82% 18%, rgba(20, 184, 166, 0.2), transparent 22%),
    radial-gradient(circle at 78% 82%, rgba(251, 191, 36, 0.16), transparent 24%),
    linear-gradient(135deg, #f7fdff 0%, #eaf8ff 52%, #f8feff 100%);
}

.customer-auth-shell {
  width: min(1040px, calc(100vw - 42px));
  min-height: 620px;
  display: grid;
  grid-template-columns: 0.98fr 1.02fr;
  overflow: hidden;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(186, 230, 253, 0.95);
  box-shadow:
    0 30px 80px rgba(14, 116, 144, 0.16),
    0 1px 0 rgba(255, 255, 255, 0.8) inset;
}

.customer-auth-intro {
  padding: 52px 48px;
  display: grid;
  align-content: center;
  gap: 30px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(223, 246, 255, 0.8)),
    radial-gradient(circle at 18% 18%, rgba(14, 165, 233, 0.16), transparent 28%);
  border-right: 1px solid #dff3ff;
}

.customer-auth-intro .brand-mark {
  border-radius: 14px;
  background: linear-gradient(135deg, #22c7f7, #6ee7b7);
  box-shadow: 0 16px 34px rgba(14, 165, 233, 0.24);
}

.customer-auth-intro .brand-copy strong {
  color: #0f172a;
}

.customer-auth-intro .brand-copy span {
  color: #64748b;
}

.intro-copy {
  display: grid;
  gap: 14px;
}

.intro-tag {
  width: fit-content;
  padding: 7px 13px;
  border-radius: 999px;
  background: #e0f7ff;
  color: #0284c7;
  border: 1px solid #bae6fd;
  font-size: 12px;
  font-weight: 700;
}

.intro-copy h1 {
  margin: 0;
  color: #0f172a;
  font-size: 38px;
  line-height: 1.15;
  letter-spacing: 0;
}

.intro-copy p {
  margin: 0;
  color: #475569;
  font-size: 15px;
  line-height: 1.8;
}

.intro-list {
  margin: 0;
  padding: 0;
  display: grid;
  gap: 18px;
  list-style: none;
}

.intro-list li {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.list-dot {
  width: 10px;
  height: 10px;
  margin-top: 8px;
  border-radius: 999px;
  background: linear-gradient(135deg, #0ea5e9, #6ee7b7);
  box-shadow: 0 0 0 6px rgba(14, 165, 233, 0.1);
}

.intro-list strong {
  display: block;
  margin-bottom: 5px;
  color: #0f172a;
  font-size: 15px;
}

.intro-list p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.75;
}

.customer-auth-form-panel {
  padding: 52px 52px;
  display: grid;
  align-content: center;
  gap: 22px;
  background: #ffffff;
}

.form-heading {
  display: grid;
  gap: 8px;
}

.welcome-badge {
  color: #0ea5e9;
  font-size: 12px;
  font-weight: 700;
}

.form-heading h2 {
  margin: 0;
  color: #0f172a;
  font-size: 32px;
  font-weight: 750;
  letter-spacing: 0;
}

.form-heading p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
}

.mode-switch {
  padding: 6px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  border-radius: 18px;
  background: #eef9ff;
  border: 1px solid #d7f0ff;
}

.mode-switch button {
  min-width: 0;
  height: 46px;
  border: 0;
  border-radius: 14px;
  background: transparent;
  color: #64748b;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition:
    background 0.2s ease,
    color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.mode-switch button.active {
  color: #0369a1;
  background: linear-gradient(135deg, #ffffff, #f6fdff);
  box-shadow:
    0 12px 24px rgba(14, 116, 144, 0.12),
    0 0 0 1px rgba(186, 230, 253, 0.9) inset;
}

.mode-switch button:hover {
  color: #0284c7;
}

.mode-icon {
  width: 17px;
  height: 17px;
}

.auth-form {
  display: grid;
  gap: 4px;
}

.auth-form .el-form-item {
  margin-bottom: 15px;
}

.auth-form .el-form-item__label {
  color: #334155;
  font-size: 13px;
  font-weight: 650;
  line-height: 1.5;
  padding-bottom: 7px;
}

.auth-form .el-input__wrapper {
  min-height: 52px;
  border-radius: 15px;
  background: #fbfdff;
  box-shadow:
    inset 0 0 0 1px #cfe8fb,
    0 8px 20px rgba(14, 116, 144, 0.04);
  transition:
    background 0.2s ease,
    box-shadow 0.2s ease;
}

.auth-form .el-input__wrapper.is-focus,
.auth-form .el-input__wrapper:hover {
  background: #ffffff;
  box-shadow:
    inset 0 0 0 1px #38bdf8,
    0 0 0 4px rgba(14, 165, 233, 0.12);
}

.auth-form .el-input__inner {
  color: #0f172a;
  font-size: 14px;
}

.auth-form .el-input__inner::placeholder {
  color: #94a3b8;
}

.auth-form .el-input__prefix-inner,
.auth-form .el-input__suffix-inner {
  color: #94a3b8;
}

.demo-link,
.plain-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: #0284c7;
  font-size: 12px;
  cursor: pointer;
}

.demo-link {
  margin-top: 8px;
  font-weight: 650;
}

.demo-link:hover,
.plain-link:hover {
  color: #0369a1;
}

.verify-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 118px;
  gap: 10px;
}

.verify-btn {
  height: 52px;
  border: 0;
  border-radius: 15px;
  background: #e0f7ff;
  color: #0284c7;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.verify-btn:disabled {
  color: #94a3b8;
  cursor: not-allowed;
}

.auth-tools {
  min-height: 18px;
  margin-top: -5px;
  display: flex;
  justify-content: flex-end;
}

.submit-btn {
  width: 100%;
  height: 52px;
  margin-top: 4px;
  border: none;
  border-radius: 15px;
  background: linear-gradient(135deg, #0ea5e9, #22c7f7 52%, #6ee7b7);
  color: #ffffff;
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 2px;
  box-shadow: 0 18px 34px rgba(14, 165, 233, 0.24);
}

.submit-btn:hover {
  filter: brightness(1.03);
}

.register-line {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
}

.plain-link.strong {
  font-weight: 750;
}

@media (max-width: 900px) {
  .customer-auth-shell {
    width: min(100%, 540px);
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .customer-auth-intro {
    border-right: 0;
    border-bottom: 1px solid #dff3ff;
  }
}

@media (max-width: 560px) {
  .customer-auth-page {
    padding: 14px;
  }

  .customer-auth-intro,
  .customer-auth-form-panel {
    padding: 28px 22px;
  }

  .intro-copy h1 {
    font-size: 29px;
  }

  .verify-row {
    grid-template-columns: 1fr;
  }

  .verify-btn {
    width: 100%;
  }
}
</style>
