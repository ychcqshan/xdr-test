<template>
  <div class="page-main">
    <!-- Page Header & Search (Elite Glass Card) -->
    <header class="page-header bento-card search-section entrance-stagger" style="--delay: 0.1s">
      <div class="header-content">
        <div class="title-group">
          <h1>资产全量库</h1>
          <p class="subtitle">所有受控端点与云端资产的实时清单看板</p>
        </div>
        
        <el-form :inline="true" @submit.prevent="loadData" class="elite-search-form">
          <el-form-item label="资产身份">
            <el-input v-model="filters.keyword" placeholder="主机名 / IP / Agent ID" clearable />
          </el-form-item>
          <el-form-item label="操作系统">
            <el-select v-model="filters.osType" placeholder="全部平台" clearable style="width:140px;">
              <el-option label="Windows" value="WINDOWS" />
              <el-option label="Linux" value="LINUX" />
              <el-option label="麒麟 (Kylin)" value="KYLIN" />
              <el-option label="统信 (UOS)" value="UOS" />
            </el-select>
          </el-form-item>
          <el-form-item label="在线状态">
            <el-select v-model="filters.status" placeholder="全部状态" clearable style="width:120px;">
              <el-option label="在线" value="ONLINE" />
              <el-option label="离线" value="OFFLINE" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="loadData">
              <el-icon><Search /></el-icon>
              <span>执行查询</span>
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </header>

    <!-- Asset Table (Elite Bento Container) -->
    <div class="table-container bento-card entrance-stagger" style="--delay: 0.2s">
      <div class="table-header">
        <div class="header-left">
          <div class="indicator-dot"></div>
          <h3>资产清单列表</h3>
        </div>
        <div class="header-right">
          <span class="count-badge">当前共计 {{ total }} 台受控资产</span>
        </div>
      </div>
      
      <el-table 
        :data="assets" 
        v-loading="loading" 
        @row-click="showDetail"
        class="elite-table"
      >
        <el-table-column prop="hostname" label="受控端点名称" min-width="180">
          <template #default="{ row }">
            <div class="host-cell">
              <div class="os-icon" :class="row.osType.toLowerCase()">
                <el-icon><Platform /></el-icon>
              </div>
              <div class="host-info">
                <span class="hostname">{{ row.hostname }}</span>
                <span class="agent-id">标识: {{ row.agentId?.substring(0, 8) }}...</span>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="ipAddress" label="局域网地址" width="160">
          <template #default="{ row }">
            <span class="ip-text">{{ row.ipAddress }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="实时状态" width="120">
          <template #default="{ row }">
            <div class="status-pill" :class="row.status.toLowerCase()">
              <span class="status-dot" :class="row.status.toLowerCase()"></span>
              <span>{{ row.status === 'ONLINE' ? '在线' : '离线' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="riskScore" label="安全健康水位" width="180">
          <template #default="{ row }">
            <div class="risk-meter">
              <el-progress
                :percentage="row.riskScore || 0"
                :stroke-width="8"
                :show-text="false"
                :color="getRiskColor(row.riskScore)"
              />
              <span class="risk-value" :style="{ color: getRiskColor(row.riskScore) }">
                {{ row.riskScore || 0 }}%
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="lastHeartbeat" label="最后在线心跳" width="180">
          <template #default="{ row }">
            <span class="time-text">{{ formatTime(row.lastHeartbeat) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-footer">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[15, 30, 50]"
          layout="prev, pager, next, sizes"
          @change="loadData"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAssets } from '@/api/asset'
import { Platform, Search } from '@element-plus/icons-vue'

const router = useRouter()
const assets = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(15)
const loading = ref(false)
const filters = reactive({ keyword: '', osType: '', status: '' })

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await getAssets({ page: page.value, size: pageSize.value, ...filters })
    assets.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch { /* error handled by interceptor */ } finally {
    loading.value = false
  }
}

function showDetail(row: any) {
  router.push({
    name: 'AssetDetail',
    params: { id: row.id },
    query: { agentId: row.agentId }
  })
}

function getRiskColor(score: number) {
  if (score > 70) return '#EF4444'
  if (score > 40) return '#F59E0B'
  return '#10B981'
}

function formatTime(time: string) {
  if (!time) return '--'
  return time.split(' ')[1] || time // Just show time for brevity
}
</script>

<style scoped>
.page-header {
  margin-bottom: 40px; /* Standardize to mb-10 */
  padding: var(--space-xl);
}

.title-group h1 {
  font-size: 28px;
  margin: 0;
  color: var(--text-primary);
}

.subtitle {
  color: var(--text-muted);
  font-size: 14px;
  margin-top: 4px;
}

.elite-search-form {
  margin-top: 32px;
  display: flex;
  align-items: flex-end;
  gap: 16px;
}

:deep(.el-form-item) {
  margin-bottom: 0 !important;
  margin-right: 0 !important;
}

:deep(.el-form-item__label) {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--text-light);
  margin-bottom: 4px !important;
  line-height: 1 !important;
}

:deep(.el-input__wrapper), :deep(.el-select .el-input__wrapper) {
  background-color: #F1F5F9 !important;
  box-shadow: none !important;
  border-radius: 12px !important;
  border: 1px solid transparent !important;
  transition: all 0.2s;
}

:deep(.el-input__wrapper.is-focus) {
  background-color: white !important;
  border-color: var(--primary-color) !important;
  box-shadow: 0 0 0 4px var(--primary-glow) !important;
}

.table-container {
  padding: 0; /* Remove padding to let table bleed horizontally but we use bento-card */
}

/* Ensure bento-card in list view has internal padding for header but table overflows nicely */
.table-header {
  padding: var(--space-lg) var(--space-xl);
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--card-border);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.indicator-dot {
  width: 12px;
  height: 12px;
  background: var(--primary-color);
  border-radius: 4px;
  box-shadow: 0 0 10px var(--primary-glow);
}

h3 {
  margin: 0;
  font-size: 18px;
}

.count-badge {
  font-size: 12px;
  font-weight: 600;
  color: var(--primary-color);
  background: var(--primary-glow);
  padding: 6px 14px;
  border-radius: 12px;
}

.elite-table {
  padding: 0 var(--space-xl);
}

.host-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.os-icon {
  width: 36px;
  height: 36px;
  background: #F8FAFC;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
}

.os-icon.windows { color: #00A4EF; }
.os-icon.linux { color: #FCC624; }

.host-info {
  display: flex;
  flex-direction: column;
}

.hostname {
  font-weight: 700;
  color: var(--text-primary);
  font-size: 14px;
}

.agent-id {
  font-size: 11px;
  color: var(--text-light);
  font-family: monospace;
}

.ip-text, .time-text {
  font-weight: 500;
  color: var(--text-secondary);
  font-size: 13px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-pill.online {
  background: #ECFDF5;
  color: #065F46;
}

.status-pill.offline {
  background: #F1F5F9;
  color: #64748B;
}

.risk-meter {
  display: flex;
  align-items: center;
  gap: 12px;
}

:deep(.el-progress) {
  flex: 1;
}

.risk-value {
  font-weight: 800;
  font-size: 13px;
  width: 40px;
}

.pagination-footer {
  padding: var(--space-lg) var(--space-xl);
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--card-border);
}
</style>
