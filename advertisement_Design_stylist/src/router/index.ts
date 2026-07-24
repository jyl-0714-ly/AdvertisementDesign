import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import LoginView from '@/views/LoginView.vue'
import MainLayout from '@/layouts/MainLayout.vue'
import ProjectsView from '@/views/ProjectsView.vue'
import ProjectView from '@/views/ProjectView.vue'
import PortfolioView from '@/views/PortfolioView.vue'
import ProfileView from '@/views/ProfileView.vue'
import WorkbenchView from '@/views/WorkbenchView.vue'
import ReceptionCenterView from '@/views/ReceptionCenterView.vue'
import ReceptionChatView from '@/views/ReceptionChatView.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: LoginView, meta: { public: true } },
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: 'dashboard', redirect: '/reception' },
      { path: 'reception', component: ReceptionCenterView, meta: { title: '客户接待中心' } },
      { path: 'reception/:intakeId', component: ReceptionChatView, meta: { title: '客户接待' } },
      { path: 'projects', component: ProjectsView, meta: { title: '项目' } },
      { path: 'projects/:id', component: ProjectView, meta: { title: '项目详情' } },
      { path: 'workbench', component: WorkbenchView, meta: { title: '客户沟通' } },
      { path: 'portfolio', component: PortfolioView, meta: { title: '案例' } },
      { path: 'profile', component: ProfileView, meta: { title: '我的' } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.bootstrapped) await auth.bootstrap()
  if (to.meta.public) {
    if (auth.isLoggedIn) return '/dashboard'
    return true
  }
  if (!auth.isLoggedIn) return { path: '/login', query: { redirect: to.fullPath } }
  return true
})

export default router
