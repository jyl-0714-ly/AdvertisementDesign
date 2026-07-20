import { FolderOpened, House, PictureRounded, User } from '@element-plus/icons-vue'

export const appName = '广告设计设计师工作台'
export const appShortName = '设计师端'
export const appSubTitle = '项目创建、阶段确认、文件归档、案例维护'

export const navItems = [
  { path: '/dashboard', label: '首页', icon: House },
  { path: '/projects', label: '项目', icon: FolderOpened },
  { path: '/portfolio', label: '案例', icon: PictureRounded },
  { path: '/profile', label: '我的', icon: User }
] as const
