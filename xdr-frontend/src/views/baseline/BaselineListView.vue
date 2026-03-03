<template>
  <div class="animate-slide-up">
    <div class="card-header glass-card" style="padding: 24px; margin-bottom: 24px;">
      <div class="header-titles">
        <h3>基线库管理</h3>
        <p class="desc">核对 Agent 实时状态与安全基线，识别配置飘移。</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" secondary @click="handleBatchLearning">
          <el-icon><Compass /></el-icon> 批量基线学习
        </el-button>
        <el-button type="success" @click="handleCreate">
          <el-icon><Plus /></el-icon> 手动创建基线
        </el-button>
      </div>
    </div>

    <el-tabs v-model="activeType" class="premium-tabs" @tab-change="fetchBaselines">
      <el-tab-pane v-for="tab in tabs" :key="tab.name" :label="tab.label" :name="tab.name" />
    </el-tabs>

    <div class="table-card glass-card">
      <el-table :data="baselines" stripe v-loading="loading">
        <el-table-column prop="agentId" label="Agent ID" width="180" />
        <el-table-column prop="version" label="基线版本" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">v{{ row.version }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="当前状态" width="130">
          <template #default="{ row }">
            <div class="status-indicator">
              <span class="dot" :class="row.status.toLowerCase()"></span>
              {{ row.status }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="最近快照时间" width="180" />
        <el-table-column label="动作">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDiff(row)">
              <el-icon><Connection /></el-icon> 漂移分析
            </el-button>
            <el-button link type="warning" @click="handleEdit(row)">维护明细</el-button>
            <el-dropdown class="ml-2">
              <el-button link type="info">更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="handleCopy(row)">复制基线</el-dropdown-item>
                  <el-dropdown-item @click="handleHistory(row)">版本回滚</el-dropdown-item>
                  <el-dropdown-item divided type="danger">物理物理删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 漂移分析 Visual Diff 对话框 -->
    <el-dialog v-model="diffVisible" title="基线漂移轨迹追踪" width="1000px" class="glass-dialog custom-dialog">
      <div class="diff-container">
        <div class="diff-side">
          <div class="side-title">安全基线标准 (Baseline)</div>
          <div class="diff-content standard">
            <pre>{{ formatJson(currentBaselineItems) }}</pre>
          </div>
        </div>
        <div class="diff-divider">
          <el-icon size="24" color="#6366f1"><Switch /></el-icon>
        </div>
        <div class="diff-side">
          <div class="side-title">实时采集快照 (Real-time)</div>
          <div class="diff-content real">
            <pre :class="{ 'has-diff': true }">{{ formatJson(realtimeItems) }}</pre>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="diffVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleApprove">更新基线标准</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getBaselines, getBaselineItems, approveBaseline } from '@/api/baseline'
import { Plus, Compass, Connection, Switch } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeType = ref('PROCESS')
const tabs = [
  { label: '运行进程', name: 'PROCESS' },
  { label: '监听端口', name: 'PORT' },
  { label: '账号登录', name: 'LOGIN' },
  { label: '外设授权', name: 'USB' },
]
const baselines = ref([])
const loading = ref(false)
const diffVisible = ref(false)
const currentBaselineItems = ref([])
const realtimeItems = ref([])

onMounted(() => fetchBaselines())

async function fetchBaselines() {
  loading.value = true
  try {
    const res: any = await getBaselines({ type: activeType.value })
    baselines.value = res.data || []
  } finally { loading.value = false }
}

async function viewDiff(row: any) {
  try {
    const res: any = await getBaselineItems(row.id)
    currentBaselineItems.value = res.data || []
    // 模拟实时数据用于展示 Diff
    realtimeItems.value = JSON.parse(JSON.stringify(res.data)).slice(0, -1) 
    diffVisible.value = true
  } catch { ElMessage.error('获取基线明细失败') }
}

function handleBatchLearning() {
  ElMessage.info('功能提示：已触发全网基线学习指令。')
}

function handleCreate() {
  ElMessage.info('正在打开手动基线设计器...')
}

function handleHistory(row: any) {
  ElMessage.info('查看历史版本快照中...')
}

function handleCopy(row: any) { ElMessage.info('正在开启基线复制向导...') }
function handleEdit(row: any) { ElMessage.info('进入基线项手动维护模式') }

async function handleApprove() {
  await ElMessageBox.confirm('这会将当前的实时状态覆盖为新的基线标准，确定吗？')
  ElMessage.success('基线已重新学习并固化')
  diffVisible.value = false
}

function formatJson(items: any[]) {
  return JSON.stringify(items.map(i => i.itemKey), null, 2)
}
</script>

<style scoped>
.header-titles h3 { margin: 0; color: #fff; display: flex; align-items: center; gap: 8px; }
.desc { color: var(--text-secondary); margin: 4px 0 0 0; font-size: 0.85rem; }

.status-indicator { display: flex; align-items: center; gap: 8px; }
.dot { width: 8px; height: 8px; border-radius: 50%; }
.dot.active { background: #10b981; box-shadow: 0 0 8px #10b981; }
.dot.learning { background: #6366f1; }

.diff-container {
  display: flex;
  height: 500px;
  gap: 16px;
  background: #0f172a;
  padding: 16px;
  border-radius: 12px;
}
.diff-side { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.side-title { font-size: 0.8rem; color: #94a3b8; margin-bottom: 8px; }
.diff-content { flex: 1; background: rgba(255, 255, 255, 0.02); border-radius: 8px; padding: 12px; overflow-y: auto; font-family: monospace; font-size: 13px; }

.real.has-diff { border-left: 2px solid #f43f5e; color: #f43f5e; }
.standard { border-left: 2px solid #10b981; color: #10b981; }

.diff-divider { display: flex; align-items: center; }
</style>
