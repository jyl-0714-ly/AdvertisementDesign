import { ChatDotRound, FolderOpened, Service } from '@element-plus/icons-vue'

export const appName = '广告设计客户工作台'
export const appShortName = '客户端'
export const appSubTitle = '项目协作、案例浏览、消息沟通'

export const navItems = [
  { path: '/consultant', label: '项目顾问', icon: Service },
  { path: '/projects', label: '我的项目', icon: FolderOpened },
  { path: '/workbench', label: '需求沟通', icon: ChatDotRound }
] as const
