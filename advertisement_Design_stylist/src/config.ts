import { ChatDotRound, FolderOpened, PictureRounded } from '@element-plus/icons-vue'

export const appName = '广告设计设计师工作台'
export const appShortName = '设计师端'
export const appSubTitle = '项目创建、阶段确认、文件归档、案例维护'

export const navItems = [
  { path: '/projects', label: '项目管理', icon: FolderOpened },
  { path: '/workbench', label: '客户沟通', icon: ChatDotRound },
  { path: '/portfolio', label: '案例维护', icon: PictureRounded }
] as const
