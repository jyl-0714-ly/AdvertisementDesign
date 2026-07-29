<template>
  <div class="requirement-workspace">
    <aside class="project-rail">
      <header><strong>项目会话</strong><button type="button" @click="startNewIntent">新建需求</button></header>
      <div v-if="projectListLoading" class="rail-hint">正在读取项目…</div>
      <button
        v-for="item in projects"
        :key="item.id"
        class="project-link"
        :class="{ active: item.id === activeProjectId }"
        type="button"
        @click="router.push(`/workspace/${item.id}`)"
      >
        <span>{{ item.name }}</span><small>{{ projectStatusText(item.status) }}</small>
      </button>
      <div v-if="!projectListLoading && !projects.length" class="rail-empty">首条有效需求发送后，项目会显示在这里。</div>
    </aside>

    <main class="conversation-panel">
      <div v-if="isNewWorkspace" class="new-requirement">
        <section class="welcome-card">
          <span class="team-mark">项目服务团队</span>
          <h1>先说说，这次想设计什么？</h1>
          <p>可以直接写完整需求，也可以跟随下方问题逐步整理。未发送的内容只保存在当前页面。</p>
        </section>

        <section class="prompt-card">
          <div><small>需求引导 · {{ templateIndex + 1 }}/{{ templates.length }}</small><strong>{{ activeTemplate.question }}</strong></div>
          <div class="prompt-actions">
            <button type="button" @click="applyTemplate">写入提示</button>
            <button type="button" @click="nextTemplate">换一个问题</button>
          </div>
        </section>

        <section class="composer" :class="{ busy: submitting }">
          <textarea v-model="content" :disabled="submitting" maxlength="5000" placeholder="例如：为秋季新品设计一张门店海报，尺寸 60×90cm，希望简洁、有质感……"></textarea>
          <div v-if="attachments.length" class="attachment-list">
            <article v-for="attachment in attachments" :key="attachment.localId">
              <div><strong>{{ attachment.file.name }}</strong><small :class="attachment.status.toLowerCase()">{{ uploadStatusText(attachment) }}</small></div>
              <button v-if="attachment.status === 'FAILED'" type="button" @click="retryUpload(attachment)">重试</button>
              <button type="button" :disabled="submitting" @click="removeAttachment(attachment.localId)">移除</button>
            </article>
          </div>
          <p v-if="guidance" class="guidance">{{ guidance }}</p>
          <footer>
            <label class="attach-button"><input type="file" multiple :disabled="submitting" @change="selectFiles">添加附件</label>
            <span>{{ content.length }}/5000</span>
            <button class="send-button" type="button" :disabled="!canSubmit" @click="submitRequirement">{{ submitting ? '正在创建项目…' : '发送需求' }}</button>
          </footer>
        </section>
      </div>

      <div v-else class="project-conversation" v-loading="projectLoading">
        <template v-if="project">
          <div class="context-line"><span>项目服务团队</span><small>{{ projectStatusText(project.status) }}</small></div>
          <section class="team-message"><strong>需求已进入项目服务流程</strong><p>我们已保留项目需求和相关资料。后续沟通、版本确认与阶段记录都会归入此项目。</p></section>
          <section v-if="project.description" class="customer-message"><small>我的首条需求</small><p>{{ project.description }}</p></section>
        </template>
      </div>
    </main>

    <aside class="context-rail">
      <template v-if="isNewWorkspace">
        <span class="context-label">开始前</span><h2>需求草稿</h2>
        <dl><div><dt>项目</dt><dd>发送有效需求后创建</dd></div><div><dt>当前内容</dt><dd>{{ draftSummary }}</dd></div><div><dt>附件</dt><dd>{{ attachments.length }} 个</dd></div></dl>
        <p>问候、表情或没有用途说明的附件不会创建项目。页面会保留内容并提示需要补充的信息。</p>
      </template>
      <template v-else-if="project">
        <span class="context-label">当前阶段</span><h2>需求引导</h2>
        <dl><div><dt>状态</dt><dd>{{ projectStatusText(project.status) }}</dd></div><div><dt>更新</dt><dd>{{ formatDate(project.updatedAt) }}</dd></div></dl>
        <p>项目将依次经过需求、合同预付款、调研、草图、定稿、交付和售后阶段。</p>
      </template>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listMyOrganizations } from '@/modules/identity/api'
import { createFromFirstRequirement, getProjectDetail, listProjectSummaries } from '@/modules/project/api'
import type { ProjectDetail, ProjectSummary } from '@/modules/project/types'
import { uploadFirstRequirementDraft } from '@/modules/storage/api'
import type { RequirementDraftAttachment } from '@/modules/storage/types'

const templates = [
  { question: '这次最需要设计的对象是什么？', hint: '需要设计：' },
  { question: '它会用在什么场景或渠道？', hint: '使用场景：' },
  { question: '尺寸、数量或交付格式有什么要求？', hint: '规格与交付：' },
  { question: '希望呈现怎样的风格或感受？', hint: '风格偏好：' }
]
const route = useRoute()
const router = useRouter()
const projects = ref<ProjectSummary[]>([])
const project = ref<ProjectDetail | null>(null)
const content = ref('')
const attachments = ref<RequirementDraftAttachment[]>([])
const templateIndex = ref(0)
const guidance = ref('')
const submitting = ref(false)
const projectLoading = ref(false)
const projectListLoading = ref(false)
let loadController: AbortController | null = null
let sendController: AbortController | null = null
let sendIntent = newSendIntent()

const isNewWorkspace = computed(() => route.name === 'workspace-new')
const activeProjectId = computed(() => Number(route.params.projectId) || null)
const activeTemplate = computed(() => templates[templateIndex.value])
const draftSummary = computed(() => content.value.trim() ? '已有未发送内容' : '尚未填写')
const canSubmit = computed(() => !submitting.value && attachments.value.every(item => item.status !== 'UPLOADING') && (content.value.trim().length > 0 || attachments.value.some(item => item.status === 'UPLOADED')))

function newSendIntent() {
  return { idempotencyKey: crypto.randomUUID(), clientMessageId: crypto.randomUUID() }
}
function projectStatusText(status: string) {
  return ({ ACTIVE: '进行中', PAUSED: '已暂停', COMPLETED: '已完成', TERMINATED: '已终止' } as Record<string, string>)[status] || status
}
function formatDate(value: string | null) {
  return value ? value.replace('T', ' ').slice(0, 16) : '—'
}
function uploadStatusText(item: RequirementDraftAttachment) {
  if (item.status === 'UPLOADING') return '上传中'
  if (item.status === 'UPLOADED') return '已上传'
  return item.error || '上传失败'
}
function nextTemplate() {
  templateIndex.value = (templateIndex.value + 1) % templates.length
}
function applyTemplate() {
  const hint = activeTemplate.value.hint
  if (!content.value.includes(hint)) content.value += `${content.value.trim() ? '\n' : ''}${hint}`
}
function startNewIntent() {
  if (!isNewWorkspace.value) void router.push('/workspace/new')
  else resetDraft()
}
function resetDraft() {
  content.value = ''
  attachments.value = []
  guidance.value = ''
  templateIndex.value = 0
  sendIntent = newSendIntent()
}

function selectFiles(event: Event) {
  const input = event.target as HTMLInputElement
  Array.from(input.files || []).slice(0, Math.max(0, 10 - attachments.value.length)).forEach(file => {
    const attachment: RequirementDraftAttachment = { localId: crypto.randomUUID(), file, status: 'UPLOADING', assetId: null, error: null }
    attachments.value.push(attachment)
    void upload(attachment)
  })
  input.value = ''
}
async function upload(attachment: RequirementDraftAttachment) {
  attachment.status = 'UPLOADING'
  attachment.error = null
  try {
    const asset = await uploadFirstRequirementDraft(attachment.file)
    attachment.assetId = asset.id
    attachment.status = 'UPLOADED'
  } catch (error) {
    attachment.status = 'FAILED'
    attachment.error = error instanceof Error ? error.message : '上传失败'
  }
}
function retryUpload(attachment: RequirementDraftAttachment) { void upload(attachment) }
function removeAttachment(localId: string) { attachments.value = attachments.value.filter(item => item.localId !== localId) }

async function submitRequirement() {
  if (!canSubmit.value) return
  const failed = attachments.value.filter(item => item.status === 'FAILED')
  if (failed.length) {
    ElMessage.warning('请重试或移除上传失败的附件')
    return
  }
  submitting.value = true
  guidance.value = ''
  sendController = new AbortController()
  try {
    const organizations = await listMyOrganizations(sendController.signal)
    if (!organizations.length) throw new Error('当前账号没有可用的客户组织')
    const result = await createFromFirstRequirement({
      organizationId: organizations[0].id,
      content: content.value,
      clientMessageId: sendIntent.clientMessageId,
      fileAssetIds: attachments.value.flatMap(item => item.assetId == null ? [] : [item.assetId])
    }, sendIntent.idempotencyKey, sendController.signal)
    if (result.status === 'INVALID_REQUIREMENT') {
      guidance.value = result.guidance || '请补充更具体的设计需求。'
      return
    }
    if (!result.projectId) throw new Error('建项结果缺少项目标识')
    await router.replace(`/workspace/${result.projectId}`)
  } catch (error) {
    if ((error as DOMException)?.name !== 'AbortError') ElMessage.error(error instanceof Error ? error.message : '发送失败，请重试')
  } finally {
    submitting.value = false
  }
}

async function loadRouteContext() {
  loadController?.abort()
  const controller = new AbortController()
  loadController = controller
  project.value = null
  projectListLoading.value = true
  const expectedProjectId = activeProjectId.value
  try {
    const listPromise = listProjectSummaries(controller.signal)
    const detailPromise = expectedProjectId ? getProjectDetail(expectedProjectId, controller.signal) : Promise.resolve(null)
    const [list, detail] = await Promise.all([listPromise, detailPromise])
    if (controller.signal.aborted || expectedProjectId !== activeProjectId.value) return
    projects.value = list
    project.value = detail
  } catch (error) {
    if ((error as DOMException)?.name !== 'AbortError') ElMessage.error(error instanceof Error ? error.message : '工作台加载失败')
  } finally {
    if (loadController === controller) {
      projectListLoading.value = false
      projectLoading.value = false
    }
  }
}

watch(() => route.fullPath, () => {
  projectLoading.value = !isNewWorkspace.value
  void loadRouteContext()
}, { immediate: true })
onBeforeUnmount(() => { loadController?.abort(); sendController?.abort() })
</script>

<style scoped>
.requirement-workspace { height: 100%; min-height: 0; display: grid; grid-template-columns: 248px minmax(480px, 1fr) 292px; background: #f3f4f2; color: #1f2522; }
.project-rail, .context-rail { min-height: 0; overflow: auto; background: #fafbf9; }
.project-rail { padding: 20px 14px; border-right: 1px solid #dde0dc; }
.project-rail header { margin-bottom: 18px; display: flex; align-items: center; justify-content: space-between; }
.project-rail header strong { font-size: 14px; }
.project-rail header button { padding: 6px 9px; border: 1px solid #d9c7b2; border-radius: 7px; background: #fffaf4; color: #8b582b; cursor: pointer; }
.project-link { width: 100%; margin-bottom: 7px; padding: 12px; border: 1px solid transparent; border-radius: 9px; background: transparent; display: grid; gap: 5px; text-align: left; cursor: pointer; }
.project-link:hover, .project-link.active { border-color: #dfd5c9; background: #fff; }
.project-link span { overflow: hidden; font-size: 13px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }.project-link small, .rail-hint, .rail-empty { color: #7b827d; font-size: 11px; }.rail-empty { padding: 16px 10px; line-height: 1.7; }
.conversation-panel { min-width: 0; min-height: 0; overflow: auto; padding: 34px clamp(24px, 5vw, 72px); background: #fff; }
.new-requirement { width: min(760px, 100%); min-height: 100%; margin: 0 auto; display: flex; flex-direction: column; justify-content: center; gap: 18px; }
.welcome-card { padding: 0 4px 10px; }.team-mark, .context-label { color: #9b632f; font-size: 11px; font-weight: 750; letter-spacing: .08em; }.welcome-card h1 { margin: 12px 0 9px; font-family: Georgia, 'Noto Serif SC', serif; font-size: clamp(28px, 3.4vw, 43px); font-weight: 500; letter-spacing: -.035em; }.welcome-card p { max-width: 620px; margin: 0; color: #747b76; font-size: 14px; line-height: 1.8; }
.prompt-card { padding: 17px 19px; border: 1px solid #ded8d0; border-left: 4px solid #b7783e; border-radius: 4px 12px 12px 4px; display: flex; align-items: center; justify-content: space-between; gap: 18px; background: #fbfaf7; }.prompt-card div:first-child { display: grid; gap: 7px; }.prompt-card small { color: #92704f; }.prompt-card strong { font-size: 15px; }.prompt-actions { flex: none; display: flex; gap: 8px; }.prompt-actions button { padding: 7px 9px; border: 1px solid #ded7cf; border-radius: 7px; background: #fff; color: #5e625f; cursor: pointer; }
.composer { border: 1px solid #d9dcd8; border-radius: 13px; overflow: hidden; box-shadow: 0 18px 50px rgba(44, 51, 46, .08); }.composer:focus-within { border-color: #b98b61; box-shadow: 0 18px 50px rgba(120, 79, 42, .12); }.composer textarea { width: 100%; min-height: 142px; padding: 18px; border: 0; outline: 0; resize: vertical; color: #262b28; font: inherit; line-height: 1.7; box-sizing: border-box; }.composer footer { min-height: 55px; padding: 9px 12px; border-top: 1px solid #eceeeb; display: flex; align-items: center; gap: 13px; background: #fafbf9; }.composer footer > span { margin-left: auto; color: #929791; font-size: 11px; }.attach-button, .send-button { padding: 8px 11px; border-radius: 7px; cursor: pointer; font-size: 12px; font-weight: 650; }.attach-button { border: 1px solid #d8dcd7; background: #fff; }.attach-button input { display: none; }.send-button { border: 0; background: #282e2a; color: #fff; }.send-button:disabled { cursor: not-allowed; opacity: .45; }
.attachment-list { padding: 0 12px 10px; display: grid; gap: 6px; }.attachment-list article { padding: 9px 10px; border-radius: 7px; display: flex; align-items: center; gap: 7px; background: #f4f5f3; }.attachment-list article div { min-width: 0; display: grid; gap: 3px; flex: 1; }.attachment-list strong { overflow: hidden; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }.attachment-list small { color: #69706b; font-size: 10px; }.attachment-list small.failed { color: #a53e38; }.attachment-list small.uploaded { color: #327153; }.attachment-list button { padding: 4px 7px; border: 0; background: transparent; color: #7d6044; cursor: pointer; font-size: 11px; }.guidance { margin: 0 14px 10px; padding: 10px 12px; border-radius: 7px; background: #fff5e8; color: #895626; font-size: 12px; line-height: 1.55; }
.context-rail { padding: 28px 22px; border-left: 1px solid #dde0dc; }.context-rail h2 { margin: 10px 0 24px; font-size: 20px; }.context-rail dl { margin: 0; display: grid; }.context-rail dl div { padding: 12px 0; border-bottom: 1px solid #e6e8e4; display: grid; gap: 5px; }.context-rail dt { color: #858b86; font-size: 11px; }.context-rail dd { margin: 0; font-size: 13px; font-weight: 650; }.context-rail p { margin: 22px 0 0; color: #7d837e; font-size: 12px; line-height: 1.75; }
.project-conversation { width: min(760px, 100%); margin: 0 auto; }.context-line { margin-bottom: 30px; display: flex; justify-content: space-between; color: #707872; font-size: 12px; }.context-line span { font-weight: 700; }.team-message, .customer-message { max-width: 72%; margin-bottom: 18px; padding: 16px 18px; border-radius: 4px 14px 14px; background: #f1f2ef; }.team-message strong { font-size: 14px; }.team-message p, .customer-message p { margin: 7px 0 0; font-size: 13px; line-height: 1.7; }.customer-message { margin-left: auto; border-radius: 14px 4px 14px 14px; background: #fff2e3; }.customer-message small { color: #8b623c; }
@media (max-width: 980px) { .requirement-workspace { grid-template-columns: 210px 1fr; }.context-rail { display: none; } }
@media (max-width: 720px) { .requirement-workspace { grid-template-columns: 1fr; }.project-rail { display: none; }.conversation-panel { padding: 24px 18px; }.prompt-card { align-items: flex-start; flex-direction: column; } }
@media (prefers-reduced-motion: reduce) { * { scroll-behavior: auto !important; transition: none !important; } }
</style>
