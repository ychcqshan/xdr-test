<template>
  <div class="asset-detail-container">
    <div class="detail-header glass-card">
      <div class="asset-info-main">
        <el-avatar :size="64" class="avatar-gradient">
          <el-icon :size="32"><Platform /></el-icon>
        </el-avatar>
        <div class="titles">
          <h2>{{ asset?.hostname || '未知主机' }}</h2>
          <div class="sub-badges">
            <el-tag size="small" effect="plain">{{ asset?.ipAddress }}</el-tag>
            <el-tag size="small" type="info" effect="plain">{{ asset?.osType }}</el-tag>
            <el-tag size="small" :type="asset?.status === 'ONLINE' ? 'success' : 'info'">
              {{ asset?.status === 'ONLINE' ? '在线' : '离线' }}
            </el-tag>
          </div>
        </div>
      </div>
      <div class="actions">
        <el-button type="primary" @click="fetchData">
          <el-icon><Refresh /></el-icon> 刷新数据
        </el-button>
        <el-button @click="$router.back()">返回</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="detail-tabs">
      <el-tab-pane label="基础概览" name="overview">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-card class="info-card">
              <template #header>系统详情</template>
              <el-descriptions :column="1">
                <el-descriptions-item label="Agent ID">{{ asset?.agentId }}</el-descriptions-item>
                <el-descriptions-item label="内核版本">{{ details?.baseInfo?.osVersion }}</el-descriptions-item>
                <el-descriptions-item label="CPU型号">{{ details?.baseInfo?.cpuModel }}</el-descriptions-item>
                <el-descriptions-item label="内存容量">{{ formatBytes(details?.baseInfo?.memoryTotal) }}</el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>
          <el-col :span="16">
            <el-card class="info-card">
              <template #header>所属部门与用户</template>
              <el-descriptions :column="2">
                <el-descriptions-item label="真实姓名">{{ details?.userInfo?.realName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="所属门">{{ details?.userInfo?.department || '-' }}</el-descriptions-item>
                <el-descriptions-item label="工号">{{ details?.userInfo?.employeeId || '-' }}</el-descriptions-item>
                <el-descriptions-item label="电话">{{ details?.userInfo?.phone || '-' }}</el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="实时进程" name="processes">
        <div class="table-card">
          <el-table :data="paginatedProcesses" stripe>
            <el-table-column prop="pid" label="PID" width="100" />
            <el-table-column prop="name" label="进程名" width="200" />
            <el-table-column prop="path" label="可执行文件路径" show-overflow-tooltip />
            <el-table-column prop="startTime" label="启动时间" width="180" />
          </el-table>
          <el-pagination
            v-model:current-page="processPage"
            v-model:page-size="pageSize"
            :total="details?.processes?.length || 0"
            layout="total, prev, pager, next"
            class="mt-4"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="网络连接" name="network">
        <div class="table-card">
          <el-table :data="paginatedNetwork" stripe>
            <el-table-column prop="protocol" label="协议" width="100" />
            <el-table-column prop="localAddr" label="本地地址" />
            <el-table-column prop="remoteAddr" label="远程地址" />
            <el-table-column prop="status" label="状态" width="120" />
          </el-table>
          <el-pagination
            v-model:current-page="networkPage"
            v-model:page-size="pageSize"
            :total="details?.network?.length || 0"
            layout="total, prev, pager, next"
            class="mt-4"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="登录审计" name="logins">
        <div class="table-card">
          <el-table :data="details?.logins" stripe>
            <el-table-column prop="user" label="用户名" width="150" />
            <el-table-column prop="sourceIp" label="登录源IP" width="150" />
            <el-table-column prop="loginTime" label="登录时间" width="200" />
            <el-table-column prop="type" label="登录方式" />
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getAssetDetail, getAssetDetails } from '@/api/asset'
import { Platform, Refresh } from '@element-plus/icons-vue'

const route = useRoute()
const assetId = route.params.id as string
const agentId = route.query.agentId as string

const asset = ref<any>(null)
const details = ref<any>(null)
const activeTab = ref('overview')
const pageSize = ref(15)

// 分页逻辑
const processPage = ref(1)
const paginatedProcesses = computed(() => {
  if (!details.value?.processes) return []
  const start = (processPage.value - 1) * pageSize.value
  return details.value.processes.slice(start, start + pageSize.value)
})

const networkPage = ref(1)
const paginatedNetwork = computed(() => {
  if (!details.value?.network) return []
  const start = (networkPage.value - 1) * pageSize.value
  return details.value.network.slice(start, start + pageSize.value)
})

onMounted(() => fetchData())

async function fetchData() {
  try {
    const [resAsset, resDetails] = await Promise.all([
      getAssetDetail(assetId),
      getAssetDetails(agentId)
    ])
    asset.value = resAsset.data
    details.value = resDetails.data
  } catch { /* error handled by interceptor */ }
}

function formatBytes(bytes: number) {
  if (!bytes) return '-'
  const gb = bytes / (1024 * 1024 * 1024)
  return gb.toFixed(1) + ' GB'
}
</script>

<style scoped>
.asset-detail-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.detail-header {
  padding: 24px 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.asset-info-main {
  display: flex;
  align-items: center;
  gap: 20px;
}

.titles h2 {
  margin: 0 0 8px 0;
  font-size: 1.5rem;
  color: #fff;
}

.sub-badges {
  display: flex;
  gap: 8px;
}

.detail-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: var(--border-color);
}

.detail-tabs :deep(.el-tabs__item) {
  color: var(--text-secondary);
  font-size: 1rem;
  height: 50px;
}

.detail-tabs :deep(.el-tabs__item.is-active) {
  color: var(--primary-color);
  font-weight: 600;
}

.info-card {
  height: 100%;
}

.mt-4 {
  margin-top: 16px;
}

:deep(.el-descriptions__label) {
  color: var(--text-secondary);
  font-weight: normal;
}

:deep(.el-descriptions__content) {
  color: var(--text-primary);
  font-weight: 500;
}
</style>
