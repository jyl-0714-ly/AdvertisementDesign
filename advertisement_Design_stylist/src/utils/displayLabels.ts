const projectStatusLabels: Record<string, string> = {
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  PAUSED: '已暂停',
  CANCELLED: '已取消'
}

const portfolioStatusLabels: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  OFFLINE: '已下线'
}

const stageStatusLabels: Record<string, string> = {
  TODO: '待发起',
  PENDING_CONFIRM: '等待确认',
  REACHED: '已达成',
  REJECTED: '已驳回'
}

const stageActionStatusLabels: Record<string, string> = {
  PENDING: '等待确认',
  CONFIRMED: '已确认',
  REJECTED: '已驳回',
  CANCELLED: '已取消'
}

const stageCodeLabels: Record<string, string> = {
  REQUIREMENT_GUIDE: '需求引导',
  CONTRACT_PREPAYMENT: '签订合同预付款',
  RESEARCH_REPORT: '资料调研报告',
  SKETCH_STYLE: '草图风格敲定',
  REVIEW_FINAL: '审稿定稿',
  FINAL_PAYMENT: '交付尾款',
  AFTER_SALE_REPURCHASE: '售后复购'
}

const fileRoleLabels: Record<string, string> = {
  MATERIAL: '需求资料',
  REPORT: '调研报告',
  DRAFT: '草稿',
  FINAL: '定稿',
  CONTRACT: '合同',
  DELIVERABLE: '交付文件',
  OTHER: '其他文件'
}

const roleLabels: Record<string, string> = {
  CUSTOMER: '客户',
  DESIGNER: '设计师',
  SYSTEM: '系统'
}

const messageTypeLabels: Record<string, string> = {
  TEXT: '文字消息',
  IMAGE: '图片',
  FILE: '文件',
  EMOJI: '表情',
  SYSTEM: '系统消息'
}

const operationActionLabels: Record<string, string> = {
  CREATE: '创建',
  SEND: '发送',
  REQUEST_CONFIRM: '发起确认',
  CONFIRM: '确认',
  REJECT: '驳回'
}

const businessTypeLabels: Record<string, string> = {
  PROJECT: '项目',
  STAGE: '项目阶段',
  MESSAGE: '沟通消息'
}

function labelOf(labels: Record<string, string>, value: string | null | undefined, fallback: string) {
  if (!value) return '—'
  return labels[value] || (/[^\x00-\xff]/.test(value) ? value : fallback)
}

export const projectStatusLabel = (value?: string | null) => labelOf(projectStatusLabels, value, '未知状态')
export const portfolioStatusLabel = (value?: string | null) => labelOf(portfolioStatusLabels, value, '未知状态')
export const stageStatusLabel = (value?: string | null) => labelOf(stageStatusLabels, value, '未知状态')
export const stageActionStatusLabel = (value?: string | null) => labelOf(stageActionStatusLabels, value, '未知状态')
export const stageCodeLabel = (value?: string | null) => labelOf(stageCodeLabels, value, '其他阶段')
export const fileRoleLabel = (value?: string | null) => labelOf(fileRoleLabels, value, '其他文件')
export const roleLabel = (value?: string | null) => labelOf(roleLabels, value, '未知角色')
export const messageTypeLabel = (value?: string | null) => labelOf(messageTypeLabels, value, '其他消息')
export const operationActionLabel = (value?: string | null) => labelOf(operationActionLabels, value, '操作记录')
export const businessTypeLabel = (value?: string | null) => labelOf(businessTypeLabels, value, '业务记录')
