<template>
  <div class="page-main">
    <!-- Elite Header -->
    <div class="bento-card page-header-elite mb-10">
      <div class="header-main">
        <div class="title-section">
          <div class="title-with-dot">
            <span class="pulse-dot active"></span>
            <h3>基线标准管理</h3>
          </div>
          <p class="subtitle-elite">实时核对端点配置，追踪系统配置漂移与基线偏差</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" plain class="elite-button" @click="handleBatchLearning">
            <el-icon><Compass /></el-icon> 基线全网学习
          </el-button>
          <el-button type="primary" class="elite-button shadow-btn" @click="handleCreate">
            <el-icon><Plus /></el-icon> 定义新基线
          </el-button>
        </div>
      </div>
    </div>

    <!-- Tabs Container -->
    <div class="elite-tabs-container mb-10">
      <el-tabs v-model="activeType" class="elite-tabs no-border-tabs" @tab-change="fetchBaselines">
        <el-tab-pane v-for="tab in tabs" :key="tab.name" :label="tab.label" :name="tab.name" />
      </el-tabs>
    </div>

    <!-- Main Table Card -->
    <div class="bento-card table-panel-elite">
      <div class="card-header-elite border-header">
        <div class="header-left">
          <h3>基线快照列表</h3>
          <span class="count-badge ml-4">活跃基线: {{ baselines.length }}</span>
        </div>
        <div class="header-filters">
          <el-form :inline="true" class="mini-search-form">
            <el-form-item label="所属单位">
              <el-input v-model="filters.unit" placeholder="单位" clearable @change="fetchBaselines" />
            </el-form-item>
            <el-form-item label="责任人">
              <el-input v-model="filters.responsiblePerson" placeholder="姓名" clearable @change="fetchBaselines" />
            </el-form-item>
          </el-form>
        </div>
      </div>

      <div class="table-wrapper-elite">
        <el-table :data="baselines" v-loading="loading" class="elite-table no-border-table">
          <el-table-column prop="agentId" label="关联 Agent ID" width="200">
            <template #default="{ row }">
              <span class="text-primary-bold">{{ row.agentId?.substring(0, 16) }}</span>
            </template>
          </el-table-column>
          
          <el-table-column prop="unit" label="所属单位" width="140" show-overflow-tooltip />
          <el-table-column prop="responsiblePerson" label="责任人" width="100" />
          
          <el-table-column prop="version" label="基线版本" width="120">
            <template #default="{ row }">
              <div class="version-badge">v{{ row.version }}</div>
            </template>
          </el-table-column>

          <el-table-column prop="status" label="合规状态" width="140">
            <template #default="{ row }">
              <div class="elite-status-pill" :class="row.status.toLowerCase()">
                <span class="status-dot"></span>
                {{ row.status === 'ACTIVE' ? '已固化' : (row.status === 'LEARNING' ? '学习中' : row.status) }}
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="updatedAt" label="最后快照时间" min-width="180">
            <template #default="{ row }">
              <span class="text-muted-sm">{{ row.updatedAt }}</span>
            </template>
          </el-table-column>

          <el-table-column label="交互操作" width="180" fixed="right">
            <template #default="{ row }">
              <div class="table-ops">
                <el-button link type="primary" class="op-btn" @click="viewDiff(row)">
                  <el-icon><Connection /></el-icon> 漂移分析
                </el-button>
                <el-divider direction="vertical" />
                <el-dropdown trigger="click">
                  <el-button link class="op-btn">管理</el-button>
                  <template #dropdown>
                    <el-dropdown-menu class="elite-dropdown-menu">
                      <el-dropdown-item @click="handleEdit(row)">维护明细</el-dropdown-item>
                      <el-dropdown-item @click="handleCopy(row)">克隆副本</el-dropdown-item>
                      <el-dropdown-item divided class="text-danger">删除基线</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- Diff Dialog (Elite) -->
    <el-dialog v-model="diffVisible" title="基线轨迹追踪分析" width="1080px" class="elite-dialog custom-large-dialog">
      <div class="elite-diff-layout">
        <div class="diff-pane">
          <div class="pane-header">
            <span class="dot standard"></span>
            <h4>基线内核标准</h4>
          </div>
          <div class="pane-content standard">
            <pre>{{ formatJson(currentBaselineItems) }}</pre>
          </div>
        </div>

        <div class="diff-visual-divider">
          <div class="arrow-line"></div>
          <el-icon size="24" color="var(--primary-color)"><Switch /></el-icon>
          <div class="arrow-line"></div>
        </div>

        <div class="diff-pane">
          <div class="pane-header">
            <span class="dot drift"></span>
            <h4>当前实时快照</h4>
          </div>
          <div class="pane-content realtime">
            <pre :class="{ 'has-drift': true }">{{ formatJson(realtimeItems) }}</pre>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="diffVisible = false" class="elite-button tertiary">关闭</el-button>
          <el-button type="primary" class="elite-button shadow-btn" @click="handleApprove">更新基线标准</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
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
const filters = reactive({
  unit: '',
  responsiblePerson: ''
})
const baselines = ref<any[]>([])
const loading = ref(false)
const diffVisible = ref(false)
const currentBaselineItems = ref([])
const realtimeItems = ref([])

onMounted(() => fetchBaselines())

async function fetchBaselines() {
  loading.value = true
  try {
    const res: any = await getBaselines({ 
      type: activeType.value,
      ...filters
    })
    baselines.value = res.data || []
  } finally { loading.value = false }
}

async function viewDiff(row: any) {
  try {
    const res: any = await getBaselineItems(row.id)
    currentBaselineItems.value = res.data || []
    realtimeItems.value = JSON.parse(JSON.stringify(res.data)).slice(0, -1) 
    diffVisible.value = true
  } catch { ElMessage.error('获取基线明细失败') }
}

function handleBatchLearning() { ElMessage.success('已触发全网基线学习任务') }
function handleCreate() { ElMessage.info('正在打开手动基线设计器...') }
function handleCopy(row: any) { ElMessage.info('基线副本已创建') }
function handleEdit(row: any) { ElMessage.info('正在加载基线维护界面') }

async function handleApprove() {
  await ElMessageBox.confirm('确定将当前实时快照固化为新的基线标准吗？', '操作确认', {
    confirmButtonText: '立即固化',
    cancelButtonText: '取消',
    type: 'warning',
    customClass: 'elite-message-box'
  })
  ElMessage.success('基线已重新固化')
  diffVisible.value = false
}

function formatJson(items: any[]) {
  return JSON.stringify(items.map(i => i.itemKey), null, 2)
}
</script>

<style scoped>
.page-header-elite { padding: 32px; }
.header-main { display: flex; justify-content: space-between; align-items: flex-start; }
.subtitle-elite { margin: 8px 0 0 0; color: var(--text-muted); font-size: 14px; }

.elite-tabs-container { padding: 0 32px; }

.border-header {
  padding: 16px 32px;
  border-bottom: 1px solid var(--card-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
}

.mini-search-form {
  display: flex;
  align-items: center;
}

.mini-search-form :deep(.el-form-item) {
  margin-bottom: 0 !important;
  margin-right: 16px !important;
}

.mini-search-form :deep(.el-form-item__label) {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
}

.mini-search-form :deep(.el-input__inner) {
  height: 32px;
  font-size: 12px;
}

.count-badge {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 700;
  background: var(--bg-pale);
  padding: 4px 12px;
  border-radius: 20px;
}

.table-wrapper-elite { padding: 8px 16px; }

.text-primary-bold { font-weight: 700; color: var(--text-primary); }
.text-muted-sm { font-size: 13px; color: var(--text-muted); }

.version-badge {
  display: inline-block;
  padding: 2px 8px;
  background: #F1F5F9;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 800;
  color: #64748B;
  border: 1px solid #E2E8F0;
}

.elite-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.elite-status-pill .status-dot { width: 6px; height: 6px; border-radius: 50%; }

.elite-status-pill.active { background: #ECFDF5; color: #059669; }
.elite-status-pill.active .status-dot { background: #10B981; box-shadow: 0 0 8px #10B981; }

.elite-status-pill.learning { background: #EEF2FF; color: #4F46E5; }
.elite-status-pill.learning .status-dot { background: #6366F1; }

.table-ops { display: flex; align-items: center; }
.op-btn { font-weight: 700; font-size: 13px; }

.elite-diff-layout {
  display: flex;
  height: 540px;
  gap: 20px;
  padding: 10px 0;
}

.diff-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-pale);
  border-radius: 20px;
  padding: 24px;
  border: 1px solid var(--card-border);
}

.pane-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.pane-header h4 { margin: 0; font-size: 14px; font-weight: 800; color: var(--text-primary); }

.pane-header .dot { width: 8px; height: 8px; border-radius: 50%; }
.pane-header .dot.standard { background: #10B981; }
.pane-header .dot.drift { background: #F43F5E; }

.pane-content {
  flex: 1;
  background: white;
  border-radius: 12px;
  padding: 16px;
  overflow-y: auto;
  font-family: 'JetBrains Mono', monospace;
  font-size: 13px;
  border: 1px solid var(--card-border);
}

.pane-content.standard { color: #059669; }
.pane-content.realtime.has-drift { color: #DC2626; border-color: rgba(244, 63, 94, 0.2); }

.diff-visual-divider {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.arrow-line { width: 1px; height: 60px; background: linear-gradient(to bottom, transparent, var(--card-border), transparent); }

.pagination-container { padding: 24px 32px; display: flex; justify-content: flex-end; }
</style>
