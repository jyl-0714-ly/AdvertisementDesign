const projectStatusLabels: Record<string, string> = {
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  PAUSED: '已暂停',
  CANCELLED: '已取消'
}

const stageStatusLabels: Record<string, string> = {
  TODO: '待发起',
  PENDING_CONFIRM: '等待确认',
  REACHED: '已达成',
  REJECTED: '已驳回'
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

function labelOf(labels: Record<string, string>, value: string | null | undefined, fallback: string) {
  if (!value) return '—'
  return labels[value] || (/[^\x00-\xff]/.test(value) ? value : fallback)
}

export const projectStatusLabel = (value?: string | null) => labelOf(projectStatusLabels, value, '未知状态')
export const stageStatusLabel = (value?: string | null) => labelOf(stageStatusLabels, value, '未知状态')
export const fileRoleLabel = (value?: string | null) => labelOf(fileRoleLabels, value, '其他文件')
export const roleLabel = (value?: string | null) => labelOf(roleLabels, value, '未知角色')
