<template>
  <div class="app-shell">
    <aside class="sidebar">
      <BrandMark :title="appName" :subtitle="appSubTitle" initials="AD" />
      <div class="surface pad">
        <div class="muted" style="font-size: 12px">当前账号</div>
        <div style="font-weight: 600; margin-top: 4px">{{ auth.user?.nickname || '未登录' }}</div>
        <div class="muted" style="margin-top: 4px">{{ auth.user?.email || '—' }}</div>
      </div>
      <div class="menu-group">
        <button
          v-for="item in navItems"
          :key="item.path"
          class="menu-link"
          :class="{ active: route.path === item.path || route.path.startsWith(`${item.path}/`) }"
          @click="router.push(item.path)"
        >
          <component :is="item.icon" style="font-size: 16px" />
          <span>{{ item.label }}</span>
        </button>
      </div>
      <div style="margin-top: auto" class="stack">
        <button class="menu-link" @click="reload">刷新数据</button>
        <button class="menu-link" @click="logout">退出登录</button>
      </div>
    </aside>
    <main class="main">
      <div class="topbar">
        <div>
          <h1 class="title">{{ pageTitle }}</h1>
          <p class="subtitle">{{ pageSubTitle }}</p>
        </div>
        <div class="badge primary">{{ auth.user?.role || 'GUEST' }}</div>
      </div>
      <router-view :key="route.fullPath" />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { appName, appSubTitle, navItems } from '@/config'
import BrandMark from '@/components/BrandMark.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const pageTitle = computed(() => (route.meta.title as string) || appName)
const pageSubTitle = computed(() => '设计师端协作工作台')

async function logout() {
  await ElMessageBox.confirm('确认退出当前账号？', '退出登录', { type: 'warning' })
  await auth.logout()
  router.replace('/login')
}

function reload() {
  window.location.reload()
}
</script>
