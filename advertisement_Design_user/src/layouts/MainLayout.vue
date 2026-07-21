<template>
  <div class="workspace-shell">
    <header class="workspace-header">
      <button class="workspace-brand" type="button" @click="router.push('/')">
        <span>AD</span><strong>{{ appName }}</strong>
      </button>
      <nav class="workspace-nav">
        <button type="button" :class="{ active: route.path === '/portfolio' }" @click="router.push('/portfolio')">作品集</button>
        <button v-for="item in navItems" :key="item.path" type="button" :class="{ active: route.path === item.path || route.path.startsWith(`${item.path}/`) }" @click="router.push(item.path)">
          <el-icon><component :is="item.icon" /></el-icon>{{ item.label }}
        </button>
      </nav>
      <div class="workspace-user">
        <button type="button" class="user-chip" @click="router.push('/profile')"><span>{{ auth.user?.nickname?.slice(0, 1) || '我' }}</span>{{ auth.user?.nickname || '我的账号' }}</button>
        <button type="button" class="logout-button" @click="logout">退出</button>
      </div>
    </header>
    <main class="workspace-main"><router-view :key="route.fullPath" /></main>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { appName, navItems } from '@/config'
import { useAuthStore } from '@/stores/auth'

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
.workspace-shell { min-height: 100vh; background: #f5f8fc; color: #243247; }.workspace-header { height: 64px; padding: 0 max(22px, calc((100vw - 1380px) / 2)); display: flex; align-items: center; gap: 42px; border-bottom: 1px solid #e0e8f1; background: rgba(255,255,255,.96); position: sticky; top: 0; z-index: 30; }.workspace-brand { padding: 0; border: 0; background: transparent; display: flex; align-items: center; gap: 9px; color: #243247; cursor: pointer; white-space: nowrap; }.workspace-brand span { width: 29px; height: 29px; display: grid; place-items: center; border-radius: 8px; color: #fff; background: #1367d1; font-size: 11px; font-weight: 800; }.workspace-brand strong { font-size: 14px; }.workspace-nav { height: 100%; display: flex; align-items: center; gap: 7px; }.workspace-nav button { height: 100%; padding: 0 12px; border: 0; border-bottom: 2px solid transparent; background: transparent; display: inline-flex; align-items: center; gap: 5px; color: #65758a; cursor: pointer; font-size: 13px; }.workspace-nav button .el-icon { font-size: 15px; }.workspace-nav button.active, .workspace-nav button:hover { color: #1367d1; border-bottom-color: #1367d1; }.workspace-user { margin-left: auto; display: flex; align-items: center; gap: 13px; }.user-chip { padding: 0; border: 0; background: transparent; display: inline-flex; align-items: center; gap: 7px; color: #46576d; cursor: pointer; font-size: 13px; }.user-chip span { width: 26px; height: 26px; display: grid; place-items: center; border-radius: 50%; background: #dbeeff; color: #1367d1; font-weight: 800; }.logout-button { padding: 0; border: 0; background: transparent; color: #8290a1; cursor: pointer; font-size: 12px; }.workspace-main { max-width: 1380px; margin: auto; padding: 23px; }
@media (max-width: 700px) { .workspace-header { padding: 0 14px; gap: 10px; }.workspace-brand strong, .workspace-nav button:not(.active) .el-icon, .workspace-nav button:not(.active) { font-size: 0; }.workspace-nav button.active { font-size: 13px; }.workspace-user { gap: 7px; }.logout-button { display: none; }.workspace-main { padding: 14px; } }
</style>
