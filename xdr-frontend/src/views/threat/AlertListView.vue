<template>
  <div class="page-main">
    <!-- Elite Header & Filters -->
    <div class="bento-card page-header-elite mb-10 entrance-stagger" style="--delay: 0.1s">
      <div class="header-main">
        <div class="title-section">
          <div class="title-with-dot">
            <span class="pulse-dot warning"></span>
            <h3>威胁告警中心</h3>
          </div>
          <p class="subtitle-elite">实时监控、分析并响应全网威胁事件</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" plain class="elite-button" @click="loadData">
            <el-icon><Refresh /></el-icon> 刷新事件
          </el-button>
        </div>
      </div>
      
      <div class="divider-elite"></div>

      <el-form :inline="true" class="elite-filter-form">
        <el-form-item label="威胁级别">
          <el-select v-model="filters.level" placeholder="所有级别" clearable class="elite-select">
            <el-option label="严重" value="CRITICAL" />
            <el-option label="高危" value="HIGH" />
            <el-option label="中危" value="MEDIUM" />
            <el-option label="低危" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="filters.status" placeholder="所有状态" clearable class="elite-select">
            <el-option label="待处理" value="NEW" />
            <el-option label="进行中" value="ACKNOWLEDGED" />
            <el-option label="已闭环" value="RESOLVED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData" class="elite-button">查询</el-button>
          <el-button @click="resetFilters" class="elite-button secondary">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- Alert Table (Elite Bento) -->
    <div class="bento-card entrance-stagger" style="--delay: 0.2s">
      <div class="card-header-elite border-header">
        <div class="header-labels">
          <h3>事件响应列表</h3>
          <span class="count-badge">共 {{ total }} 个记录</span>
        </div>
      </div>

      <div class="table-wrapper-elite">
        <el-table :data="alerts" v-loading="loading" class="elite-table no-border-table">
          <el-table-column prop="title" label="威胁事件名称" min-width="240">
            <template #default="{ row }">
              <div class="event-title-cell">
                <span class="event-title-text">{{ row.title }}</span>
                <span class="event-id-sub">{{ row.threatType }}</span>
              </div>
            </template>
          </el-table-column>
          
          <el-table-column prop="level" label="危险指数" width="120">
            <template #default="{ row }">
              <div class="severity-pill" :class="row.level.toLowerCase()">
                <span class="severity-indicator"></span>
                {{ levelLabel(row.level) }}
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="status" label="响应状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" effect="light" class="elite-tag">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="agentId" label="受影响端点" width="180">
            <template #default="{ row }">
              <span class="text-muted-sm">{{ row.agentId }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="createdAt" label="发现时间" width="180" />

          <el-table-column label="管理操作" width="120" fixed="right" align="center">
            <template #default="{ row }">
              <div class="action-icons">
                <el-tooltip content="情报详情分析" placement="top">
                  <el-button circle size="small" @click="showDetail(row)">
                    <el-icon><Monitor /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-dropdown trigger="click" class="ml-2">
                  <el-tooltip content="更多处置动作" placement="top">
                    <el-button circle size="small">
                      <el-icon><MoreFilled /></el-icon>
                    </el-button>
                  </el-tooltip>
                  <template #dropdown>
                    <el-dropdown-menu class="elite-dropdown-menu">
                      <el-dropdown-item v-if="row.status === 'NEW'" @click="handleAlert(row.id, 'ACKNOWLEDGED')">
                        <el-icon><Check /></el-icon>标记已读
                      </el-dropdown-item>
                      <el-dropdown-item v-if="row.status !== 'RESOLVED'" @click="handleAlert(row.id, 'RESOLVED')">
                        <el-icon><Select /></el-icon>标记已解决
                      </el-dropdown-item>
                      <el-dropdown-item class="text-danger">
                        <el-icon><Warning /></el-icon>误报忽略
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          class="elite-pagination"
          @change="loadData"
        />
      </div>
    </div>

    <!-- Alert Detail (Elite) -->
    <el-dialog v-model="dialogVisible" title="告警情报详情" width="680px" class="elite-dialog">
      <template v-if="currentAlert">
        <div class="elite-detail-content">
          <div class="detail-hero" :class="currentAlert.level.toLowerCase()">
            <el-icon size="40"><WarningFilled /></el-icon>
            <div class="hero-info">
              <h4>{{ currentAlert.title }}</h4>
              <p>{{ currentAlert.agentId }} - {{ currentAlert.threatType }}</p>
            </div>
          </div>
          
          <el-descriptions :column="2" class="mt-6 elite-descriptions">
            <el-descriptions-item label="严重级别">
              <div class="severity-pill mini" :class="currentAlert.level.toLowerCase()">{{ levelLabel(currentAlert.level) }}</div>
            </el-descriptions-item>
            <el-descriptions-item label="当前状态">{{ statusLabel(currentAlert.status) }}</el-descriptions-item>
            <el-descriptions-item label="发现时间" :span="2">{{ currentAlert.createdAt }}</el-descriptions-item>
            <el-descriptions-item label="威胁描述" :span="2">
              <div class="description-box">{{ currentAlert.description }}</div>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </template>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="dialogVisible = false">调查取证</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getAlerts, updateAlertStatus } from '@/api/alert'
import { 
  Refresh, ArrowDown, Check, Select, Warning, 
  WarningFilled, Monitor, MoreFilled 
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const alerts = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const dialogVisible = ref(false)
const currentAlert = ref<any>(null)

const filters = reactive({ level: '', status: '', threatType: '' })

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await getAlerts({ page: page.value, size: pageSize.value, ...filters })
    alerts.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch { /* empty */ } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.level = ''
  filters.status = ''
  filters.threatType = ''
  loadData()
}

function showDetail(row: any) {
  currentAlert.value = row
  dialogVisible.value = true
}

async function handleAlert(id: string, status: string) {
  try {
    await updateAlertStatus(id, { status, operator: localStorage.getItem('username') || '' })
    ElMessage.success('状态已成功更新')
    loadData()
  } catch { /* error handled by interceptor */ }
}

function levelLabel(level: string) {
  const map: Record<string, string> = { CRITICAL: '严重', HIGH: '高危', MEDIUM: '中危', LOW: '低危' }
  return map[level] || level
}

function statusTagType(status: string) {
  const map: Record<string, string> = { NEW: 'danger', ACKNOWLEDGED: 'warning', RESOLVED: 'success', IGNORED: 'info' }
  return map[status] || 'info'
}

function statusLabel(status: string) {
  const map: Record<string, string> = { NEW: '待处理', ACKNOWLEDGED: '分析中', RESOLVED: '已闭环', IGNORED: '已忽略' }
  return map[status] || status
}
</script>

<style scoped>
.page-header-elite {
  padding: 32px;
}

.header-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.subtitle-elite {
  margin: 8px 0 0 0;
  color: var(--text-muted);
  font-size: 14px;
}

.elite-filter-form {
  margin-top: 24px;
}

.elite-filter-form :deep(.el-form-item__label) {
  color: var(--text-primary);
  font-weight: 600;
  font-size: 13px;
}

.elite-select :deep(.el-input__wrapper) {
  background: var(--bg-pale) !important;
  border-radius: 12px !important;
  box-shadow: none !important;
}

.border-header {
  padding: 24px 32px;
  border-bottom: 1px solid var(--card-border);
}

.header-labels {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.count-badge {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 600;
  background: var(--bg-pale);
  padding: 4px 12px;
  border-radius: 20px;
}

.table-wrapper-elite {
  padding: 0 16px;
}

.event-title-cell {
  display: flex;
  flex-direction: column;
}

.event-title-text {
  font-weight: 700;
  color: var(--text-primary);
}

.event-id-sub {
  font-size: 12px;
  color: var(--text-muted);
  font-family: monospace;
}

.severity-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
}

.severity-indicator {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.severity-pill.critical { background: #FEF2F2; color: #DC2626; }
.severity-pill.critical .severity-indicator { background: #DC2626; box-shadow: 0 0 8px rgba(220, 38, 38, 0.5); }

.severity-pill.high { background: #FFF7ED; color: #EA580C; }
.severity-pill.high .severity-indicator { background: #EA580C; }

.severity-pill.medium { background: #EFF6FF; color: #2563EB; }
.severity-pill.medium .severity-indicator { background: #2563EB; }

.severity-pill.low { background: #F8FAFC; color: #64748B; }
.severity-pill.low .severity-indicator { background: #64748B; }

.severity-pill.mini { padding: 4px 10px; font-size: 11px; }

.action-icons {
  display: flex;
  justify-content: center;
  align-items: center;
}

.table-ops {
  display: flex;
  align-items: center;
}

.op-btn { font-weight: 700; font-size: 13px; }

.pagination-container {
  padding: 24px 32px;
  display: flex;
  justify-content: flex-end;
}

.elite-detail-content {
  padding: 8px 0;
}

.detail-hero {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
  border-radius: 16px;
  margin-bottom: 24px;
}

.detail-hero.critical { background: linear-gradient(135deg, #FEF2F2, #FEE2E2); color: #991B1B; }
.detail-hero.high { background: linear-gradient(135deg, #FFF7ED, #FFEDD5); color: #9A3412; }
.detail-hero.medium { background: linear-gradient(135deg, #EFF6FF, #DBEAFE); color: #1E40AF; }

.hero-info h4 { margin: 0; font-size: 18px; font-weight: 800; }
.hero-info p { margin: 4px 0 0 0; opacity: 0.8; font-size: 13px; }

.description-box {
  background: var(--bg-pale);
  padding: 16px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-primary);
}

:deep(.elite-descriptions .el-descriptions__label) {
  color: var(--text-muted);
  font-weight: 600;
}

:deep(.elite-descriptions .el-descriptions__content) {
  color: var(--text-primary);
  font-weight: 700;
}
</style>
