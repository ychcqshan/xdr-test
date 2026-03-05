<template>
  <div class="page-main">
    <!-- Elite Header -->
    <div class="bento-card page-header-elite mb-10">
      <div class="header-main">
        <div class="title-section">
          <div class="title-with-dot">
            <span class="pulse-dot active"></span>
            <h3>端点远程升级</h3>
          </div>
          <p class="subtitle-elite">集中管理 Agent 升级包仓库，监控全网端点版本平滑演进</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" class="elite-button shadow-btn">
            <el-icon><Upload /></el-icon> 上传升级包
          </el-button>
        </div>
      </div>
    </div>

    <!-- Main Layout Grid -->
    <el-row :gutter="24">
      <!-- Upgrade Repository -->
      <el-col :span="10">
        <div class="bento-card">
          <div class="card-header-elite border-header">
            <h3>升级包中心</h3>
            <span class="count-badge">活跃版本: {{ packages.length }}</span>
          </div>
          
          <div class="table-wrapper-elite">
            <el-table :data="packages" class="elite-table no-border-table">
              <el-table-column prop="version" label="版本号" width="100">
                <template #default="{ row }">
                  <div class="version-tag-elite">v{{ row.version }}</div>
                </template>
              </el-table-column>
              <el-table-column prop="osType" label="目标平台" width="100">
                <template #default="{ row }">
                  <span class="text-primary-bold">{{ row.osType }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="变更说明" show-overflow-tooltip>
                <template #default="{ row }">
                  <span class="text-muted-sm">{{ row.description }}</span>
                </template>
              </el-table-column>
              <el-table-column label="动作" width="80" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" class="op-btn">发布</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-col>

      <!-- Mission Monitoring -->
      <el-col :span="14">
        <div class="bento-card">
          <div class="card-header-elite border-header">
            <div class="header-labels-flex">
              <h3>任务实时流</h3>
              <div class="header-right-actions">
                <el-input
                  v-model="searchQuery"
                  placeholder="搜索终端 ID..."
                  class="elite-search-input-sm mr-4"
                  clearable
                >
                  <template #prefix><el-icon><Search /></el-icon></template>
                </el-input>
                <div class="active-tasks-pill">
                  <span class="pulse-dot active small"></span>
                  {{ filteredTasks.length }} 个活跃任务
                </div>
              </div>
            </div>
          </div>

          <div class="table-wrapper-elite">
            <el-table :data="filteredTasks" class="elite-table no-border-table">
              <el-table-column prop="agentId" label="计算终端 ID" width="160">
                <template #default="{ row }">
                  <span class="text-primary-bold">{{ row.agentId }}</span>
                </template>
              </el-table-column>
              
              <el-table-column label="分发进度" min-width="240">
                <template #default="{ row }">
                  <div class="elite-progress-box">
                    <div class="progress-info-elite">
                      <span class="status-label-elite">{{ statusLabel(row.status) }}</span>
                      <span class="version-target">目标版本: {{ row.targetVersion }}</span>
                    </div>
                    <el-progress 
                      :percentage="row.progress" 
                      :status="getProgressStatus(row.status)"
                      :stroke-width="10"
                      :show-text="false"
                      class="elite-progress-bar-thick"
                    />
                  </div>
                </template>
              </el-table-column>

              <el-table-column prop="updatedAt" label="最近心跳" width="160">
                <template #default="{ row }">
                  <span class="text-muted-sm">{{ row.updatedAt }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getUpgradePackages } from '@/api/upgrade'
import { Upload, Monitor, Search } from '@element-plus/icons-vue'

const searchQuery = ref('')
const packages = ref([])
const tasks = ref([
  { agentId: 'AG-8829-X1', progress: 45, status: 'DOWNLOADING', targetVersion: 'v2.1.0', updatedAt: '刚才' },
  { agentId: 'AG-3102-Y4', progress: 100, status: 'SUCCESS', targetVersion: 'v2.1.0', updatedAt: '1分钟前' },
  { agentId: 'AG-9981-L2', progress: 15, status: 'INSTALLING', targetVersion: 'v2.0.8', updatedAt: '3分钟前' },
])

const filteredTasks = computed(() => {
  if (!searchQuery.value) return tasks.value
  return tasks.value.filter(t => t.agentId.toLowerCase().includes(searchQuery.value.toLowerCase()))
})

onMounted(async () => {
  try {
    const res: any = await getUpgradePackages()
    packages.value = res.data || []
  } catch { /* ignored */ }
})

function getProgressStatus(status: string) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'exception'
  return ''
}

function statusLabel(status: string) {
  const map: any = {
    'DOWNLOADING': '升级包下载中...',
    'INSTALLING': '解压与安装中...',
    'SUCCESS': '升级已完成',
    'FAILED': '升级失败'
  }
  return map[status] || status
}
</script>

<style scoped>
.page-header-elite { padding: 32px; }
.header-main { display: flex; justify-content: space-between; align-items: flex-start; }
.subtitle-elite { margin: 8px 0 0 0; color: var(--text-muted); font-size: 14px; }

.border-header { padding: 20px 32px; border-bottom: 1px solid var(--card-border); }
.header-labels-flex { display: flex; justify-content: space-between; align-items: center; }
.header-right-actions { display: flex; align-items: center; }

.count-badge { font-size: 12px; color: var(--text-muted); font-weight: 700; background: var(--bg-pale); padding: 4px 12px; border-radius: 20px; }

.active-tasks-pill { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--primary-color); font-weight: 800; background: var(--bg-pale); padding: 4px 12px; border-radius: 20px; }

.table-wrapper-elite { padding: 8px 16px; }

.version-tag-elite { display: inline-block; padding: 2px 8px; background: #F1F5F9; border-radius: 6px; font-size: 11px; font-weight: 800; color: #64748B; }

.text-primary-bold { font-weight: 700; color: var(--text-primary); }
.text-muted-sm { font-size: 12px; color: var(--text-muted); }

.elite-progress-box { padding: 8px 0; }
.progress-info-elite { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }

.status-label-elite { font-size: 11px; font-weight: 800; color: var(--text-primary); text-transform: uppercase; }
.version-target { font-size: 11px; font-weight: 600; color: var(--text-muted); }

.elite-progress-bar-thick :deep(.el-progress-bar__outer) { background: #F1F5F9 !important; border-radius: 6px; overflow: hidden; height: 10px !important; }

.elite-search-input-sm :deep(.el-input__wrapper) {
  background: var(--bg-pale) !important;
  border-radius: 12px !important;
  width: 180px;
}

.op-btn { font-weight: 700; font-size: 13px; }
</style>
