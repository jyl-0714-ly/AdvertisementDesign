<template>
  <div class="designer-shell" :class="{ 'designer-shell--workbench': route.path === '/workbench' }">
    <header class="designer-header">
      <button class="designer-brand" type="button" @click="router.push('/projects')"><span>AD</span><strong>{{ appName }}</strong></button>
      <nav class="designer-nav">
        <button v-for="item in navItems" :key="item.path" type="button" :class="{ active: route.path === item.path || route.path.startsWith(`${item.path}/`) }" @click="router.push(item.path)">
          <el-icon><component :is="item.icon" /></el-icon>{{ item.label }}
        </button>
      </nav>
      <div class="designer-user">
        <el-popover placement="bottom-end" trigger="hover" :width="260" :show-after="220" popper-class="account-popover">
          <template #reference>
            <button type="button" class="designer-user-chip" :class="{ active: route.path === '/profile' }" aria-label="个人资料" @click="router.push('/profile')">
              <span>{{ auth.user?.nickname?.slice(0, 1) || '设' }}</span>{{ auth.user?.nickname || '设计师' }}
            </button>
          </template>
          <div class="account-preview">
            <div class="account-preview-head">
              <span>{{ auth.user?.nickname?.slice(0, 1) || '设' }}</span>
              <div><strong>{{ auth.user?.nickname || '设计师' }}</strong><small>{{ roleLabel(auth.user?.role) }}</small></div>
            </div>
            <div class="account-preview-row"><span>邮箱</span><strong>{{ auth.user?.email || '—' }}</strong></div>
          </div>
        </el-popover>
        <button type="button" class="designer-logout" @click="logout">退出</button>
      </div>
    </header>
    <main class="designer-main" :class="{ 'designer-main--workbench': route.path === '/workbench' }"><router-view :key="route.fullPath" /></main>
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
/* ===== Stylist端 全局字体与基础 ===== */
:root {
  color-scheme: light;
  --s-ink: #111827;
  --s-surface: #ffffff;
  --s-bg: #f7f8fa;
  --s-border: rgba(15, 23, 42, 0.08);
  --s-muted: #64748b;
  --s-accent: #D97745;
  --s-accent-light: #fff5ef;
  --s-accent-mid: #F8DED0;
  --s-reach: #1a8a5c;
  --s-pending: #D97745;
  --s-danger: #9f3a3a;
  font-family: 'Inter', 'PingFang SC', -apple-system, BlinkMacSystemFont, "Segoe UI", "Microsoft YaHei", sans-serif;
}

.designer-shell {
  min-height: 100vh;
  background: var(--s-bg);
  color: var(--s-ink);
}

/* ===== 顶部导航 ===== */
.designer-header {
  height: 58px;
  padding: 0 max(24px, calc((100vw - 1400px) / 2));
  display: flex;
  align-items: center;
  gap: 0;
  border-bottom: 1px solid var(--s-border);
  background: rgba(255,255,255,0.98);
  backdrop-filter: blur(12px);
  position: sticky;
  top: 0;
  z-index: 100;
}

.designer-brand {
  padding: 0;
  border: 0;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--s-ink);
  cursor: pointer;
  white-space: nowrap;
  flex: none;
  margin-right: 36px;
}
.designer-brand span {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 7px;
  background: var(--s-ink);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.designer-brand strong {
  font-size: 14px;
  font-weight: 600;
  color: var(--s-ink);
}

.designer-nav {
  height: 100%;
  display: flex;
  align-items: center;
  gap: 0;
  flex: 1;
}
.designer-nav button {
  height: 100%;
  padding: 0 16px;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--s-muted);
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: color 180ms, border-color 180ms;
}
.designer-nav .el-icon { font-size: 16px; }
.designer-nav button:hover { color: var(--s-ink); }
.designer-nav button.active {
  color: var(--s-accent);
  border-bottom-color: var(--s-accent);
}

.designer-user {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}

.designer-user-chip {
  padding: 5px 10px 5px 6px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--s-ink);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: background 160ms, border-color 160ms;
}
.designer-user-chip:hover,
.designer-user-chip.active {
  border-color: var(--s-border);
  background: var(--s-bg);
}
.designer-user-chip span {
  width: 26px;
  height: 26px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--s-ink);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.designer-logout {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--s-muted);
  cursor: pointer;
  font-size: 13px;
  transition: color 160ms;
}
.designer-logout:hover { color: var(--s-danger); }

.designer-main {
  max-width: 1400px;
  margin: auto;
  padding: 28px 24px;
}

/* ===== Workbench full-screen 模式 ===== */
.designer-shell--workbench { height: 100vh; min-height: 0; overflow: hidden; }
.designer-shell--workbench .designer-header { position: relative; }
.designer-shell--workbench > .designer-main--workbench {
  width: 100%;
  max-width: none;
  height: calc(100vh - 58px);
  margin: 0;
  padding: 0;
  overflow: hidden;
}

/* ===== 账号弹窗（复用 user 端写好的 account-popover） ===== */
.el-popover.account-popover {
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--s-border);
  border-radius: 12px;
  box-shadow: 0 20px 50px rgba(10, 22, 40, 0.12);
}
.account-preview { padding: 16px; }
.account-preview-head { display: flex; align-items: center; gap: 12px; }
.account-preview-head > span {
  width: 40px;
  height: 40px;
  flex: none;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: var(--s-ink);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
}
.account-preview-head > div { min-width: 0; display: grid; gap: 2px; }
.account-preview-head strong {
  overflow: hidden;
  color: var(--s-ink);
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.account-preview-head small { color: var(--s-muted); font-size: 12px; }
.account-preview-row {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--s-border);
  display: grid;
  gap: 3px;
}
.account-preview-row span { color: var(--s-muted); font-size: 11px; }
.account-preview-row strong {
  overflow: hidden;
  color: var(--s-ink);
  font-size: 13px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 700px) {
  .designer-header { padding: 0 16px; gap: 8px; }
  .designer-brand strong { display: none; }
  .designer-nav button:not(.active) { font-size: 0; padding: 0 10px; }
  .designer-nav button.active { font-size: 14px; }
  .designer-user { gap: 6px; }
  .designer-logout { display: none; }
  .designer-main { padding: 16px 14px; }
}
</style>
