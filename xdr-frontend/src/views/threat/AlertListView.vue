<template>
  <div>
    <!-- 筛选栏 -->
    <div class="table-card" style="margin-bottom: 20px;">
      <el-form :inline="true" @submit.prevent="loadData">
        <el-form-item label="级别">
          <el-select v-model="filters.level" placeholder="全部" clearable style="width:120px;">
            <el-option label="严重" value="CRITICAL" />
            <el-option label="高危" value="HIGH" />
            <el-option label="中危" value="MEDIUM" />
            <el-option label="低危" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width:130px;">
            <el-option label="待处理" value="NEW" />
            <el-option label="已确认" value="ACKNOWLEDGED" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已忽略" value="IGNORED" />
          </el-select>
        </el-form-item>
        <el-form-item label="威胁类型">
          <el-select v-model="filters.threatType" placeholder="全部" clearable style="width:160px;">
            <el-option label="勒索软件" value="RANSOMWARE" />
            <el-option label="横向移动" value="LATERAL_MOVEMENT" />
            <el-option label="无文件攻击" value="FILELESS" />
            <el-option label="基线偏差" value="BASELINE_VIOLATION" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 告警表格 -->
    <div class="table-card">
      <div class="card-header">
        <h3>告警列表</h3>
        <span style="color: #909399; font-size: 13px;">共 {{ total }} 项</span>
      </div>
      <el-table :data="alerts" stripe v-loading="loading">
        <el-table-column prop="title" label="告警标题" min-width="220" />
        <el-table-column prop="level" label="级别" width="100">
          <template #default="{ row }">
            <el-tag
              :type="levelTagType(row.level)"
              effect="dark"
              size="small"
            >{{ levelLabel(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="threatType" label="威胁类型" width="130" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="agentId" label="Agent" width="170" />
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="showDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 'NEW'"
              text type="warning" size="small"
              @click="handleAlert(row.id, 'ACKNOWLEDGED')"
            >确认</el-button>
            <el-button
              v-if="row.status !== 'RESOLVED'"
              text type="success" size="small"
              @click="handleAlert(row.id, 'RESOLVED')"
            >解决</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end;"
        @change="loadData"
      />
    </div>

    <!-- 告警详情弹窗 -->
    <el-dialog v-model="dialogVisible" title="告警详情" width="600px">
      <template v-if="currentAlert">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="标题" :span="2">{{ currentAlert.title }}</el-descriptions-item>
          <el-descriptions-item label="级别">
            <span :class="'level-' + currentAlert.level?.toLowerCase()">{{ levelLabel(currentAlert.level) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusLabel(currentAlert.status) }}</el-descriptions-item>
          <el-descriptions-item label="威胁类型">{{ currentAlert.threatType }}</el-descriptions-item>
          <el-descriptions-item label="Agent">{{ currentAlert.agentId }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ currentAlert.description }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentAlert.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ currentAlert.resolvedAt || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getAlerts, updateAlertStatus } from '@/api/alert'
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
    ElMessage.success('操作成功')
    loadData()
  } catch { /* error handled by interceptor */ }
}

function levelTagType(level: string) {
  const map: Record<string, string> = { CRITICAL: 'danger', HIGH: 'warning', MEDIUM: '', LOW: 'info' }
  return map[level] || 'info'
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
  const map: Record<string, string> = { NEW: '待处理', ACKNOWLEDGED: '已确认', RESOLVED: '已解决', IGNORED: '已忽略' }
  return map[status] || status
}
</script>
