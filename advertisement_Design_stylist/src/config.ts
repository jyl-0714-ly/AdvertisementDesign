import { ChatDotRound, FolderOpened, PictureRounded, Service } from '@element-plus/icons-vue'

export const appName = '广告设计设计师工作台'
export const appShortName = '设计师端'
export const appSubTitle = '客户接待、项目协作、设计交付与案例沉淀'

export const navItems = [
  { path: '/reception', label: '客户接待', icon: Service },
  { path: '/projects', label: '项目管理', icon: FolderOpened },
  { path: '/workbench', label: '客户沟通', icon: ChatDotRound },
  { path: '/portfolio', label: '案例维护', icon: PictureRounded }
] as const
