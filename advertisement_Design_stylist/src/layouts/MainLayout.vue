<template>
  <div class="designer-shell">
    <header class="designer-header">
      <button class="designer-brand" type="button" @click="router.push('/projects')"><span>AD</span><strong>{{ appName }}</strong></button>
      <nav class="designer-nav">
        <button v-for="item in navItems" :key="item.path" type="button" :class="{ active: route.path === item.path || route.path.startsWith(`${item.path}/`) }" @click="router.push(item.path)">
          <el-icon><component :is="item.icon" /></el-icon>{{ item.label }}
        </button>
      </nav>
      <div class="designer-user"><button type="button" @click="router.push('/profile')"><span>{{ auth.user?.nickname?.slice(0, 1) || '设' }}</span>{{ auth.user?.nickname || '设计师' }}</button><button type="button" class="designer-logout" @click="logout">退出</button></div>
    </header>
    <main class="designer-main"><router-view :key="route.fullPath" /></main>
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
.designer-shell { min-height: 100vh; background: #f6f8fb; color: #243247; }.designer-header { height: 64px; padding: 0 max(22px, calc((100vw - 1380px) / 2)); display: flex; align-items: center; gap: 40px; border-bottom: 1px solid #dde5ee; background: rgba(255,255,255,.97); position: sticky; top: 0; z-index: 30; }.designer-brand { padding: 0; border: 0; display: flex; align-items: center; gap: 9px; color: #263348; background: transparent; cursor: pointer; white-space: nowrap; }.designer-brand span { width: 29px; height: 29px; display: grid; place-items: center; border-radius: 8px; background: linear-gradient(135deg, #0d8d87, #2563eb); color: white; font-size: 11px; font-weight: 800; }.designer-brand strong { font-size: 14px; }.designer-nav { height: 100%; display: flex; align-items: center; gap: 7px; }.designer-nav button { height: 100%; padding: 0 12px; border: 0; border-bottom: 2px solid transparent; display: inline-flex; align-items: center; gap: 5px; background: transparent; color: #66758a; cursor: pointer; font-size: 13px; }.designer-nav .el-icon { font-size: 15px; }.designer-nav button.active, .designer-nav button:hover { border-color: #0d8d87; color: #087b76; }.designer-user { margin-left: auto; display: flex; align-items: center; gap: 13px; }.designer-user > button:first-child { padding: 0; border: 0; display: inline-flex; align-items: center; gap: 7px; background: transparent; color: #4a5a6f; cursor: pointer; font-size: 13px; }.designer-user > button:first-child span { width: 26px; height: 26px; display: grid; place-items: center; border-radius: 50%; background: #d8f2ed; color: #087b76; font-weight: 800; }.designer-logout { padding: 0; border: 0; background: transparent; color: #8492a3; cursor: pointer; font-size: 12px; }.designer-main { max-width: 1380px; margin: auto; padding: 23px; }
@media (max-width: 700px) { .designer-header { padding: 0 14px; gap: 9px; }.designer-brand strong, .designer-nav button:not(.active) { font-size: 0; }.designer-nav button.active { font-size: 13px; }.designer-user { gap: 7px; }.designer-logout { display: none; }.designer-main { padding: 14px; } }
</style>
