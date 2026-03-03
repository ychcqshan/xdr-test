<template>
  <div class="animate-slide-up">
    <el-row :gutter="24">
      <!-- 升级包仓库 -->
      <el-col :span="10">
        <div class="table-card glass-card">
          <div class="card-header">
            <h3><el-icon><Upload /></el-icon> 升级包仓库</h3>
            <el-button type="primary" size="small">上传新版本</el-button>
          </div>
          <el-table :data="packages" size="small">
            <el-table-column prop="version" label="版本号" width="100" />
            <el-table-column prop="osType" label="平台" width="90" />
            <el-table-column prop="description" label="描述" show-overflow-tooltip />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button link type="primary">发布</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>

      <!-- 实时升级监控 -->
      <el-col :span="14">
        <div class="table-card glass-card">
          <div class="card-header">
            <h3><el-icon><Monitor /></el-icon> 任务实时监控</h3>
            <span class="status-badge processing">5 活跃中</span>
          </div>
          <el-table :data="tasks" stripe>
            <el-table-column prop="agentId" label="Agent ID" width="180" />
            <el-table-column label="进度" min-width="200">
              <template #default="{ row }">
                <div class="progress-wrapper">
                  <el-progress 
                    :percentage="row.progress" 
                    :status="getProgressStatus(row.status)"
                    :stroke-width="12"
                    striped
                    striped-flow
                  />
                  <div class="progress-desc">{{ row.status }} - {{ row.targetVersion }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="updatedAt" label="最后更新" width="160" />
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUpgradePackages } from '@/api/upgrade'
import { Upload, Monitor } from '@element-plus/icons-vue'

const packages = ref([])
const tasks = ref([
  { agentId: 'AG-8829-X1', progress: 45, status: 'DOWNLOADING', targetVersion: 'v2.1.0' },
  { agentId: 'AG-3102-Y4', progress: 100, status: 'SUCCESS', targetVersion: 'v2.1.0' },
  { agentId: 'AG-9981-L2', progress: 15, status: 'INSTALLING', targetVersion: 'v2.0.8' },
])

onMounted(async () => {
  try {
    const res: any = await getUpgradePackages()
    packages.value = res.data || []
  } catch { /* empty */ }
})

function getProgressStatus(status: string) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'exception'
  return ''
}
</script>

<style scoped>
.status-badge {
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
}
.status-badge.processing {
  background: rgba(99, 102, 241, 0.2);
  color: #818cf8;
}

.progress-wrapper {
  padding: 4px 0;
}
.progress-desc {
  font-size: 0.7rem;
  color: var(--text-secondary);
  margin-top: 4px;
}
</style>
