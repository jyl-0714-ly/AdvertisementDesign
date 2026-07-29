export interface ServiceDefinition {
  slug: string
  title: string
  shortTitle: string
  en: string
  mark: string
  introduction: string
  detail: string
  contents: { title: string; description: string }[]
  customers: string[]
  caseServiceTypes: string[]
}

export const serviceDefinitions: readonly ServiceDefinition[] = [
  {
    slug: 'brand-visual', title: '品牌视觉设计', shortTitle: '品牌视觉', en: 'BRAND VISUAL', mark: 'A',
    introduction: '建立具有辨识度与一致性的品牌视觉系统，让每一次品牌露出都传递相同的价值与气质。',
    detail: '我们从品牌定位、受众和竞争环境出发，将抽象的品牌策略转译为可识别、可延展、可执行的视觉语言，并提供团队可长期使用的规范。',
    contents: [{ title: '品牌定位', description: '梳理品牌核心价值、差异化与表达方向。' }, { title: 'Logo 与 VI', description: '完成标志、色彩、字体及基础识别系统。' }, { title: '视觉手册', description: '沉淀应用规则，保证跨场景表达一致。' }, { title: '品牌升级', description: '在保留品牌资产的基础上完成视觉焕新。' }],
    customers: ['新品牌从 0 到 1 建立专业形象', '现有视觉陈旧或缺少统一规范', '业务升级，需要匹配新的市场定位', '多渠道传播中品牌呈现不一致'],
    caseServiceTypes: ['品牌设计', 'VI 设计', '品牌视觉设计', 'Logo 设计', '品牌升级']
  },
  {
    slug: 'marketing', title: '营销传播设计', shortTitle: '营销传播', en: 'MARKETING', mark: 'M',
    introduction: '围绕传播目标构建有记忆点的活动与营销视觉，提高信息触达效率。',
    detail: '从活动主题、传播节奏和渠道规格出发，形成统一创意概念，并适配线上线下不同媒体触点。',
    contents: [{ title: '海报设计', description: '主视觉及系列宣传海报。' }, { title: '活动视觉', description: '活动主题与完整视觉延展。' }, { title: '社交媒体', description: '适配各类社媒平台的内容模板。' }, { title: '广告创意', description: '围绕卖点形成高识别传播创意。' }],
    customers: ['新品上市或品牌推广期', '需要提升活动传播辨识度', '社媒内容缺少统一视觉语言', '营销物料多但整体感不足'],
    caseServiceTypes: ['海报设计', '活动物料', '活动视觉设计', '宣传物料设计', '营销传播设计', '广告创意']
  },
  {
    slug: 'packaging', title: '产品包装设计', shortTitle: '包装设计', en: 'PACKAGING', mark: 'P',
    introduction: '让包装同时承担品牌识别、产品沟通与货架竞争力。',
    detail: '综合品类认知、消费场景、生产工艺和成本约束，打造兼顾审美与落地质量的包装系统。',
    contents: [{ title: '包装策略', description: '明确产品层级、消费场景和视觉机会。' }, { title: '包装设计', description: '完成包装结构表面与主视觉设计。' }, { title: '标签设计', description: '优化信息层级与法规信息布局。' }, { title: '系列化包装', description: '建立易扩展的产品家族识别系统。' }],
    customers: ['新品需要快速建立货架识别', '产品线扩张需要系列化管理', '现有包装难以体现产品价值', '电商展示与线下陈列需要统一'],
    caseServiceTypes: ['包装设计', '产品包装设计', '标签设计', '系列化包装']
  },
  {
    slug: 'spatial', title: '空间品牌设计', shortTitle: '空间品牌', en: 'SPATIAL', mark: 'S',
    introduction: '将品牌语言延伸到真实空间，让顾客在行走与停留中感知品牌。',
    detail: '通过空间视觉、导视与接触点设计，把品牌气质转化为连贯的环境体验。',
    contents: [{ title: '门店设计', description: '品牌门店视觉概念与触点设计。' }, { title: '展厅设计', description: '围绕叙事动线组织品牌展示。' }, { title: '导视系统', description: '建立清晰易用的空间信息系统。' }, { title: '空间视觉', description: '墙面、陈列及环境图形延展。' }],
    customers: ['新店开业或连锁门店升级', '企业展厅需要清晰叙事', '空间识别度不足或导视混乱', '线上品牌与线下体验不一致'],
    caseServiceTypes: ['空间设计', '空间品牌设计', '商业空间视觉设计', '门店设计', '展厅设计', '导视系统']
  },
  {
    slug: 'digital', title: '数字体验设计', shortTitle: '数字体验', en: 'DIGITAL', mark: 'D',
    introduction: '以清晰的信息架构和体验细节，构建兼具品牌感与易用性的数字产品。',
    detail: '从用户任务与业务目标出发，完成结构、界面、交互与响应式体验设计。',
    contents: [{ title: '官网设计', description: '品牌官网信息架构与视觉体验。' }, { title: '小程序设计', description: '围绕核心任务优化移动体验。' }, { title: 'UI/UX 设计', description: '从流程到组件建立完整界面系统。' }, { title: '数字产品', description: '支持复杂业务的产品体验设计。' }],
    customers: ['需要建立专业品牌官网', '产品功能复杂、使用路径不清晰', '旧界面体验与品牌形象脱节', '多端产品需要统一设计系统'],
    caseServiceTypes: ['数字体验设计', '官网设计', '小程序设计', 'UI/UX 设计', 'UI设计', '数字产品']
  }
]

export const defaultService = serviceDefinitions[0]
export const serviceBySlug = Object.fromEntries(serviceDefinitions.map(service => [service.slug, service])) as Record<string, ServiceDefinition>

function normalize(value: string) {
  return value.toLowerCase().replace(/[\s/&·_-]/g, '').replace(/设计$/u, '')
}

export function caseMatchesService(serviceType: string, service: ServiceDefinition) {
  const candidate = normalize(serviceType)
  return service.caseServiceTypes.some(type => normalize(type) === candidate)
}
