<template>
  <div class="workspace-shell" :class="{ 'workspace-shell--workbench': route.meta.layout === 'workspace' }">
    <header class="workspace-header">
      <button class="workspace-brand" type="button" @click="router.push('/')">
        <span>AD</span><strong>{{ appName }}</strong>
      </button>
      <nav class="workspace-nav">
        <button type="button" :class="{ active: route.path === '/portfolio' }" @click="router.push('/portfolio')">作品集</button>
        <button v-for="item in navItems" :key="item.path" type="button" :class="{ active: item.path === '/workspace/new' ? route.path.startsWith('/workspace/') : route.path === item.path || route.path.startsWith(`${item.path}/`) }" @click="router.push(item.path)">
          <el-icon><component :is="item.icon" /></el-icon>{{ item.label }}
        </button>
      </nav>
      <div class="workspace-user">
        <el-popover placement="bottom-end" trigger="hover" :width="260" :show-after="220" popper-class="account-popover">
          <template #reference>
            <button type="button" class="user-chip" :class="{ active: route.path === '/profile' }" aria-label="个人资料" @click="router.push('/profile')">
              <span>{{ auth.user?.nickname?.slice(0, 1) || '我' }}</span>{{ auth.user?.nickname || '我的账号' }}
            </button>
          </template>
          <div class="account-preview">
            <div class="account-preview-head">
              <span>{{ auth.user?.nickname?.slice(0, 1) || '我' }}</span>
              <div><strong>{{ auth.user?.nickname || '我的账号' }}</strong><small>{{ roleLabel(auth.user?.role) }}</small></div>
            </div>
            <div class="account-preview-row"><span>邮箱</span><strong>{{ auth.user?.email || '—' }}</strong></div>
          </div>
        </el-popover>
        <button type="button" class="logout-button" @click="logout">退出</button>
      </div>
    </header>
    <main class="workspace-main" :class="{ 'workspace-main--workbench': route.meta.layout === 'workspace' }"><router-view :key="route.fullPath" /></main>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { appName, navItems } from '@/config'
import { useAuthStore } from '@/stores/auth'
import { roleLabel } from '@/utils/displayLabels'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

async function logout() {
  await ElMessageBox.confirm('确认退出当前账号？', '退出登录', { type: 'warning' })
  await auth.logout()
  router.replace('/login')
}
</script>

<style>
/* ===== User端 全局字体与基础 ===== */
:root {
  color-scheme: light;
  --u-ink: #0f172a;
  --u-surface: #ffffff;
  --u-bg: #f7f8fa;
  --u-border: #e2e5ea;
  --u-muted: #64748b;
  --u-accent: #c0742a;
  --u-accent-light: #fef3e8;
  --u-accent-mid: #f5e4d1;
  --u-reach: #1a8a5c;
  --u-reach-bg: #ecfdf5;
  --u-pending: #c0742a;
  --u-pending-bg: #fef8f2;
  --u-danger: #9f3a3a;
  --u-danger-bg: #fdf2f2;
  font-family: 'Inter', 'Noto Sans SC', -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif;
}

/* ===== Layout Shell ===== */
.workspace-shell {
  min-height: 100vh;
  background: var(--u-bg);
  color: var(--u-ink);
}

/* ===== 顶部导航 ===== */
.workspace-header {
  height: 58px;
  padding: 0 max(24px, calc((100vw - 1400px) / 2));
  display: flex;
  align-items: center;
  gap: 0;
  border-bottom: 1px solid var(--u-border);
  background: rgba(255,255,255,0.98);
  backdrop-filter: blur(12px);
  position: sticky;
  top: 0;
  z-index: 100;
}

.workspace-brand {
  padding: 0;
  border: 0;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--u-ink);
  cursor: pointer;
  white-space: nowrap;
  flex: none;
  margin-right: 36px;
}
.workspace-brand span {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 7px;
  background: var(--u-ink);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.workspace-brand strong {
  font-size: 14px;
  font-weight: 600;
  color: var(--u-ink);
}

.workspace-nav {
  height: 100%;
  display: flex;
  align-items: center;
  gap: 0;
  flex: 1;
}
.workspace-nav button {
  height: 100%;
  padding: 0 16px;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--u-muted);
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: color 180ms, border-color 180ms;
}
.workspace-nav button .el-icon { font-size: 16px; }
.workspace-nav button:hover { color: var(--u-ink); }
.workspace-nav button.active {
  color: var(--u-accent);
  border-bottom-color: var(--u-accent);
}

.workspace-user {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-chip {
  padding: 5px 10px 5px 6px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--u-ink);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: background 160ms, border-color 160ms;
}
.user-chip:hover,
.user-chip.active {
  border-color: var(--u-border);
  background: var(--u-bg);
}
.user-chip span {
  width: 26px;
  height: 26px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--u-ink);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.logout-button {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--u-muted);
  cursor: pointer;
  font-size: 13px;
  font-weight: 400;
  transition: color 160ms;
}
.logout-button:hover { color: var(--u-danger); }

/* ===== 页面内容区 ===== */
.workspace-main {
  max-width: 1400px;
  margin: auto;
  padding: 28px 24px;
}

/* ===== 账号弹窗 ===== */
.el-popover.account-popover {
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--u-border);
  border-radius: 12px;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.12);
}
.account-preview { padding: 16px; }
.account-preview-head {
  display: flex;
  align-items: center;
  gap: 12px;
}
.account-preview-head > span {
  width: 40px;
  height: 40px;
  flex: none;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: var(--u-ink);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
}
.account-preview-head > div { min-width: 0; display: grid; gap: 2px; }
.account-preview-head strong {
  overflow: hidden;
  color: var(--u-ink);
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.account-preview-head small { color: var(--u-muted); font-size: 12px; }
.account-preview-row {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--u-border);
  display: grid;
  gap: 3px;
}
.account-preview-row span { color: var(--u-muted); font-size: 11px; }
.account-preview-row strong {
  overflow: hidden;
  color: var(--u-ink);
  font-size: 13px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== Workbench full-screen 模式 ===== */
.workspace-shell--workbench { height: 100vh; min-height: 0; overflow: hidden; }
.workspace-shell--workbench .workspace-header { position: relative; }
.workspace-shell--workbench > .workspace-main--workbench {
  width: 100%;
  max-width: none;
  height: calc(100vh - 58px);
  margin: 0;
  padding: 0;
  overflow: hidden;
}

@media (max-width: 700px) {
  .workspace-header { padding: 0 16px; gap: 8px; }
  .workspace-brand strong { display: none; }
  .workspace-nav button:not(.active) { font-size: 0; padding: 0 10px; }
  .workspace-nav button.active { font-size: 14px; }
  .workspace-user { gap: 6px; }
  .logout-button { display: none; }
  .workspace-main { padding: 16px 14px; }
}
</style>
