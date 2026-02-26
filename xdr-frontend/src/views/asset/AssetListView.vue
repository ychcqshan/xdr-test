<template>
  <div>
    <!-- 搜索栏 -->
    <div class="table-card" style="margin-bottom: 20px;">
      <el-form :inline="true" @submit.prevent="loadData">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="主机名/IP/AgentID" clearable style="width:200px;" />
        </el-form-item>
        <el-form-item label="操作系统">
          <el-select v-model="filters.osType" placeholder="全部" clearable style="width:130px;">
            <el-option label="Windows" value="WINDOWS" />
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
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 资产表格 -->
    <div class="table-card">
      <div class="card-header">
        <h3>资产列表</h3>
        <span style="color: #909399; font-size: 13px;">共 {{ total }} 项</span>
      </div>
      <el-table :data="assets" stripe v-loading="loading" @row-click="showDetail">
        <el-table-column prop="hostname" label="主机名" min-width="140" />
        <el-table-column prop="ipAddress" label="IP地址" width="140" />
        <el-table-column prop="osType" label="操作系统" width="120" />
        <el-table-column prop="agentVersion" label="Agent版本" width="110" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ONLINE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ONLINE' ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="riskScore" label="风险评分" width="100">
          <template #default="{ row }">
            <el-progress
              :percentage="row.riskScore || 0"
              :color="row.riskScore > 70 ? '#f56c6c' : row.riskScore > 40 ? '#e6a23c' : '#67c23a'"
              :stroke-width="8"
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
        style="margin-top: 16px; justify-content: flex-end;"
        @change="loadData"
      />
    </div>

    <!-- 资产详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="资产详情" size="450px">
      <template v-if="currentAsset">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="Agent ID">{{ currentAsset.agentId }}</el-descriptions-item>
          <el-descriptions-item label="主机名">{{ currentAsset.hostname }}</el-descriptions-item>
          <el-descriptions-item label="IP">{{ currentAsset.ipAddress }}</el-descriptions-item>
          <el-descriptions-item label="操作系统">{{ currentAsset.osType }} {{ currentAsset.osVersion }}</el-descriptions-item>
          <el-descriptions-item label="CPU">{{ currentAsset.cpuModel }} ({{ currentAsset.cpuArch }})</el-descriptions-item>
          <el-descriptions-item label="内存">{{ formatBytes(currentAsset.memoryTotal) }}</el-descriptions-item>
          <el-descriptions-item label="磁盘">{{ formatBytes(currentAsset.diskTotal) }}</el-descriptions-item>
          <el-descriptions-item label="Agent版本">{{ currentAsset.agentVersion }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentAsset.status === 'ONLINE' ? 'success' : 'info'">{{ currentAsset.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="风险评分">{{ currentAsset.riskScore }}</el-descriptions-item>
          <el-descriptions-item label="最后心跳">{{ currentAsset.lastHeartbeat }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getAssets, getAssetDetail } from '@/api/asset'

const assets = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const drawerVisible = ref(false)
const currentAsset = ref<any>(null)

const filters = reactive({ keyword: '', osType: '', status: '' })

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await getAssets({ page: page.value, size: pageSize.value, ...filters })
    assets.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch { /* empty */ } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.keyword = ''
  filters.osType = ''
  filters.status = ''
  loadData()
}

async function showDetail(row: any) {
  try {
    const res = await getAssetDetail(row.id)
    currentAsset.value = res.data
  } catch {
    currentAsset.value = row
  }
  drawerVisible.value = true
}

function formatBytes(bytes: number) {
  if (!bytes) return '-'
  const gb = bytes / (1024 * 1024 * 1024)
  return gb.toFixed(1) + ' GB'
}
</script>
