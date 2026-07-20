import { FolderOpened, House, PictureRounded, User } from '@element-plus/icons-vue'

export const appName = '广告设计客户工作台'
export const appShortName = '客户端'
export const appSubTitle = '项目协作、案例浏览、消息沟通'

export const navItems = [
  { path: '/dashboard', label: '首页', icon: House },
  { path: '/projects', label: '项目', icon: FolderOpened },
  { path: '/portfolio', label: '案例', icon: PictureRounded },
  { path: '/profile', label: '我的', icon: User }
] as const
