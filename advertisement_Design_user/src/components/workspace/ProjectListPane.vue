<template>
  <aside class="project-list-pane" aria-label="项目会话列表">
    <header><div><small>我的项目</small><strong>项目会话</strong></div><button type="button" @click="router.push('/workspace/new')">新建需求</button></header>
    <p v-if="loading" class="pane-state">正在读取项目…</p>
    <p v-else-if="error" class="pane-state pane-state--error">{{ error }}<button type="button" @click="load">重新加载</button></p>
    <nav v-else-if="projects.length">
      <button v-for="project in projects" :key="project.id" type="button" :class="{ active: project.id === projectId }" @click="router.push(`/workspace/${project.id}`)">
        <strong>{{ project.name }}</strong><span>{{ statusText(project.status) }} · {{ dateText(project.updatedAt) }}</span>
      </button>
    </nav>
    <p v-else class="pane-state">发送第一条有效需求后，项目会显示在这里。</p>
  </aside>
</template>
<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listProjectSummaries } from '@/modules/project/api'
import type { ProjectSummary } from '@/modules/project/types'
defineProps<{ projectId: number }>()
const router = useRouter()
const projects = ref<ProjectSummary[]>([])
const loading = ref(false)
const error = ref('')
let controller: AbortController | null = null
function statusText(status: string) { return ({ ACTIVE: '进行中', PAUSED: '已暂停', COMPLETED: '已完成', TERMINATED: '已终止' } as Record<string, string>)[status] || '处理中' }
function dateText(value: string | null) { return value ? value.slice(5, 10).replace('-', '/') : '等待更新' }
async function load() { controller?.abort(); controller = new AbortController(); loading.value = true; error.value = ''; try { projects.value = await listProjectSummaries(controller.signal) } catch (reason) { if ((reason as DOMException)?.name !== 'AbortError') error.value = reason instanceof Error ? reason.message : '项目列表暂时无法读取。' } finally { loading.value = false } }
void load()
onBeforeUnmount(() => controller?.abort())
</script>
<style scoped>
.project-list-pane{min-height:0;padding:22px 14px;border-right:1px solid #dde0dc;background:#fafbf9;overflow:auto}.project-list-pane header{padding:0 6px 18px;display:flex;align-items:center;justify-content:space-between}.project-list-pane header div{display:grid;gap:2px}.project-list-pane header small{color:#9b632f;font-size:10px;font-weight:750;letter-spacing:.08em}.project-list-pane header strong{font-size:15px}.project-list-pane header button,.pane-state button{border:1px solid #dfd3c6;border-radius:7px;background:#fff;color:#87572d;padding:6px 9px;cursor:pointer}.project-list-pane nav{display:grid;gap:6px}.project-list-pane nav button{width:100%;padding:12px;border:1px solid transparent;border-radius:9px;background:transparent;display:grid;gap:5px;text-align:left;cursor:pointer}.project-list-pane nav button:hover,.project-list-pane nav button.active{border-color:#dfd5c9;background:#fff}.project-list-pane nav strong{overflow:hidden;font-size:13px;text-overflow:ellipsis;white-space:nowrap}.project-list-pane nav span,.pane-state{color:#7b827d;font-size:11px}.pane-state{padding:16px 8px;line-height:1.7}.pane-state--error{color:#8f3e39}.pane-state button{display:block;margin-top:10px}
</style>
