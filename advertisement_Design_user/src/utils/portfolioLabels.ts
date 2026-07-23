import type { PortfolioCategory } from '@/models'

export const portfolioCategoryOptions: Array<{ value: PortfolioCategory | ''; label: string }> = [
  { value: '', label: '全部案例' },
  { value: 'BRAND', label: '品牌系统' },
  { value: 'DIGITAL', label: '线上传播' },
  { value: 'OFFLINE', label: '线下物料' }
]

export function portfolioCategoryLabel(value: PortfolioCategory) {
  return portfolioCategoryOptions.find((item) => item.value === value)?.label || '其他案例'
}
