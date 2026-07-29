import { ChatDotRound, FolderOpened, PictureRounded } from '@element-plus/icons-vue'

export const appName = '广告设计设计师工作台'
export const appShortName = '设计师端'
export const appSubTitle = '项目协作、客户沟通、设计交付与案例沉淀'

export const navItems = [
  { path: '/projects', label: '项目管理', icon: FolderOpened },
  { path: '/workbench', label: '客户沟通', icon: ChatDotRound },
  { path: '/portfolio', label: '案例维护', icon: PictureRounded }
] as const
