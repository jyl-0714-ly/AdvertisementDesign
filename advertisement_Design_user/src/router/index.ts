import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import LoginView from '@/views/LoginView.vue'
import MainLayout from '@/layouts/MainLayout.vue'
import ProjectsView from '@/views/ProjectsView.vue'
import ProjectView from '@/views/ProjectView.vue'
import ProfileView from '@/views/ProfileView.vue'
import PortfolioView from '@/views/PortfolioView.vue'
import HomeView from '@/views/HomeView.vue'
import CaseDetailView from '@/views/CaseDetailView.vue'
import WorkbenchView from '@/views/WorkbenchView.vue'
import ConsultantView from '@/views/ConsultantView.vue'
import ServiceDetailView from '@/views/ServiceDetailView.vue'
import { sanitizeInternalRedirect } from '@/utils/safeRedirect'

const routes = [
  { path: '/', component: HomeView, meta: { public: true } },
  { path: '/services/:slug', component: ServiceDetailView, meta: { public: true } },
  { path: '/cases/:id', component: CaseDetailView, meta: { public: true } },
  { path: '/login', component: LoginView, meta: { public: true, login: true } },
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: 'dashboard', redirect: '/projects' },
      { path: 'projects', component: ProjectsView, meta: { title: '项目' } },
      { path: 'projects/:id', component: ProjectView, meta: { title: '项目详情' } },
      { path: 'portfolio', component: PortfolioView, meta: { title: '作品集' } },
      { path: 'consultant', component: ConsultantView, meta: { title: '项目顾问' } },
      { path: 'workbench', component: WorkbenchView, meta: { title: '需求沟通' } },
      { path: 'profile', component: ProfileView, meta: { title: '我的' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, _from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) return { el: to.hash }
    return { top: 0 }
  }
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.bootstrapped) {
    await auth.bootstrap()
  }
  if (to.meta.public) {
    if (to.meta.login && auth.isLoggedIn) {
      return sanitizeInternalRedirect(to.query.redirect)
    }
    return true
  }
  if (!auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
