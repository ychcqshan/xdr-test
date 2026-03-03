<template>
  <div class="animate-slide-up">
    <!-- 搜索栏 (玻璃拟态) -->
    <div class="table-card glass-card" style="margin-bottom: 24px;">
      <el-form :inline="true" @submit.prevent="loadData">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="主机名/IP/AgentID" clearable style="width:220px;" />
        </el-form-item>
        <el-form-item label="操作系统">
          <el-select v-model="filters.osType" placeholder="全部" clearable style="width:140px;">
            <el-option label="Windows" value="WINDOWS" />
            <el-option label="Linux" value="LINUX" />
            <el-option label="Kylin" value="KYLIN" />
            <el-option label="UOS" value="UOS" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width:120px;">
            <el-option label="在线" value="ONLINE" />
            <el-option label="离线" value="OFFLINE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">
            <el-icon><Search /></el-icon> 搜索
          </el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 资产表格 (深色玻璃) -->
    <div class="table-card glass-card">
      <div class="card-header">
        <h3>资产库清单</h3>
        <span class="count-badge">Total {{ total }} Items</span>
      </div>
      
      <el-table 
        :data="assets" 
        stripe 
        v-loading="loading" 
        @row-click="showDetail"
        class="premium-table"
      >
        <el-table-column prop="hostname" label="主机名" min-width="160">
          <template #default="{ row }">
            <div class="host-cell">
              <el-icon :color="row.status === 'ONLINE' ? '#10b981' : '#94a3b8'"><Platform /></el-icon>
              <span>{{ row.hostname }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP地址" width="140" />
        <el-table-column prop="osType" label="操作系统" width="140">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" type="info">{{ row.osType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="agentVersion" label="Agent版本" width="110" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <div class="status-dot-wrapper">
              <span class="status-dot" :class="row.status.toLowerCase()"></span>
              <span>{{ row.status === 'ONLINE' ? '在线' : '离线' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="riskScore" label="风险评分" width="120">
          <template #default="{ row }">
            <el-progress
              :percentage="row.riskScore || 0"
              :color="row.riskScore > 70 ? '#f43f5e' : row.riskScore > 40 ? '#f59e0b' : '#10b981'"
              :stroke-width="6"
              :show-text="false"
              style="width: 80px;"
            />
          </template>
        </el-table-column>
        <el-table-column prop="lastHeartbeat" label="最后心跳" width="170" />
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        class="premium-pagination"
        @change="loadData"
      />
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
const pageSize = ref(20)
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

function resetFilters() {
  filters.keyword = ''
  filters.osType = ''
  filters.status = ''
  loadData()
}

function showDetail(row: any) {
  router.push({
    name: 'AssetDetail',
    params: { id: row.id },
    query: { agentId: row.agentId }
  })
}
</script>

<style scoped>
.count-badge {
  font-size: 0.8rem;
  color: var(--text-secondary);
  background: rgba(255, 255, 255, 0.05);
  padding: 4px 12px;
  border-radius: 20px;
}

.host-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.status-dot-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.status-dot.online { background-color: #10b981; box-shadow: 0 0 8px #10b981; }
.status-dot.offline { background-color: #94a3b8; }

.premium-table :deep(.el-table__row) {
  cursor: pointer;
}

.premium-pagination {
  margin-top: 24px;
  justify-content: flex-end;
}
</style>
