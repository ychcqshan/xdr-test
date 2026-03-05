<template>
  <div class="page-main">
    <!-- Elite Header Section -->
    <div class="bento-card page-header-elite mb-10">
      <div class="header-main-alt">
        <div class="asset-info-wrapper">
          <div class="elite-avatar-glow">
            <el-icon size="28" color="white"><Platform /></el-icon>
          </div>
          <div class="titles-elite">
            <div class="title-row">
              <h2>{{ asset?.hostname || '未知设备' }}</h2>
              <div class="status-indicator-pills">
                <span class="status-dot-blink" :class="{ online: asset?.status === 'ONLINE' }"></span>
                <span class="status-text">{{ asset?.status === 'ONLINE' ? '实时在线' : '离线中' }}</span>
              </div>
            </div>
            <div class="sub-badges-elite">
              <span class="info-tag">{{ asset?.ipAddress }}</span>
              <span class="divider-dot"></span>
              <span class="info-tag secondary">{{ asset?.osType || '未知操作系统' }}</span>
              <span class="divider-dot"></span>
              <span class="info-tag secondary">Agent 标识: {{ asset?.agentId?.substring(0, 8) }}...</span>
            </div>
          </div>
        </div>
        <div class="header-actions">
          <el-button @click="$router.back()" class="elite-button secondary">
            <el-icon><ArrowLeft /></el-icon> 返回列表
          </el-button>
          <el-button type="primary" class="elite-button shadow-btn" @click="fetchData">
            <el-icon><Refresh /></el-icon> 同步状态
          </el-button>
        </div>
      </div>
    </div>

    <!-- Layout Grid -->
    <el-row :gutter="24">
      <!-- Left Column: Tabs Content -->
      <el-col :span="18">
        <div class="bento-card tab-content-panel">
          <el-tabs v-model="activeTab" class="elite-tabs">
            <el-tab-pane label="性能与配置" name="overview">
              <div class="tab-padding">
                <el-row :gutter="20">
                  <el-col :span="12">
                    <div class="info-section-elite">
                      <h4 class="section-title">硬件参数</h4>
                      <el-descriptions :column="1" class="elite-desc">
                        <el-descriptions-item label="核心型号">{{ details?.baseInfo?.cpuModel || 'Intel(R) Core(TM) i7-10700K' }}</el-descriptions-item>
                        <el-descriptions-item label="物理内存">{{ formatBytes(details?.baseInfo?.memoryTotal) }}</el-descriptions-item>
                        <el-descriptions-item label="内核发行版">{{ details?.baseInfo?.osVersion || '暂无数据' }}</el-descriptions-item>
                        <el-descriptions-item label="架构类型">x86_64 / x64</el-descriptions-item>
                      </el-descriptions>
                    </div>
                  </el-col>
                  <el-col :span="12">
                    <div class="info-section-elite">
                      <h4 class="section-title">权属与归属</h4>
                      <el-descriptions :column="1" class="elite-desc">
                        <el-descriptions-item label="负责人">{{ details?.userInfo?.realName || '公共设备' }}</el-descriptions-item>
                        <el-descriptions-item label="所属组织">{{ details?.userInfo?.department || '未分配部门' }}</el-descriptions-item>
                        <el-descriptions-item label="联系方式">{{ details?.userInfo?.phone || '-' }}</el-descriptions-item>
                        <el-descriptions-item label="最后上报">刚刚</el-descriptions-item>
                      </el-descriptions>
                    </div>
                  </el-col>
                </el-row>
              </div>
            </el-tab-pane>

            <el-tab-pane label="实时进程" name="processes">
              <div class="table-container-elite">
                <el-table :data="paginatedProcesses" class="elite-table no-border-table">
                  <el-table-column prop="pid" label="PID" width="100" />
                  <el-table-column prop="name" label="进程名" width="200">
                    <template #default="{ row }">
                      <span class="text-primary-bold">{{ row.name }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="path" label="路径" show-overflow-tooltip>
                    <template #default="{ row }">
                      <span class="text-muted-sm">{{ row.path }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="startTime" label="启动于" width="180" />
                </el-table>
                <div class="pagination-sub">
                  <el-pagination
                    v-model:current-page="processPage"
                    v-model:page-size="pageSize"
                    :total="details?.processes?.length || 0"
                    layout="total, prev, pager, next"
                    class="elite-pagination sm"
                  />
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="登录审计" name="logins">
              <div class="table-container-elite">
                <el-table :data="details?.logins" class="elite-table">
                  <el-table-column prop="user" label="登录账户" width="180">
                    <template #default="{ row }">
                      <div class="user-cell">
                        <el-icon class="mr-2"><UserFilled /></el-icon>
                        <span class="text-primary-bold">{{ row.user }}</span>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="sourceIp" label="源 IP 地址" width="180" />
                  <el-table-column prop="loginTime" label="登录时间" width="200" />
                  <el-table-column prop="type" label="会话方式">
                    <template #default="{ row }">
                      <el-tag size="small" effect="plain">{{ row.type }}</el-tag>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-col>

      <!-- Right Column: Sidebar Stats -->
      <el-col :span="6">
        <div class="bento-card sidebar-stat-card mb-4">
          <h4 class="sidebar-title">Agent 活跃度</h4>
          <div class="stat-main">
            <span class="stat-value">99.8%</span>
            <span class="stat-trend up">+0.2%</span>
          </div>
          <div class="stat-chart-mini">
            <div class="bar-progress">
              <div class="bar-fill" style="width: 100%"></div>
            </div>
            <p class="stat-lbl-sm">本月累计运行时长: 720h</p>
          </div>
        </div>
        
        <div class="bento-card sidebar-stat-card">
          <h4 class="sidebar-title">安全评分</h4>
          <div class="score-circle-wrapper">
            <div class="score-circle">
              <span class="score-num">85</span>
              <span class="score-unit">优秀</span>
            </div>
          </div>
          <ul class="score-risk-list">
            <li><span class="bullet warn"></span> 基线偏差: 2</li>
            <li><span class="bullet secure"></span> 未处理告警: 0</li>
          </ul>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getAssetDetail, getAssetDetails } from '@/api/asset'
import { 
  Platform, Refresh, ArrowLeft, UserFilled,
  InfoFilled
} from '@element-plus/icons-vue'

const route = useRoute()
const assetId = route.params.id as string
const agentId = route.query.agentId as string

const asset = ref<any>(null)
const details = ref<any>(null)
const activeTab = ref('overview')
const pageSize = ref(12)

const processPage = ref(1)
const paginatedProcesses = computed(() => {
  if (!details.value?.processes) return []
  const start = (processPage.value - 1) * pageSize.value
  return details.value.processes.slice(start, start + pageSize.value)
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
  } catch { /* ignored */ }
}

function formatBytes(bytes: number) {
  if (!bytes) return '-'
  const gb = bytes / (1024 * 1024 * 1024)
  return gb.toFixed(1) + ' GB'
}
</script>

<style scoped>
.page-header-elite {
  padding: 32px;
}

.header-main-alt {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.asset-info-wrapper {
  display: flex;
  align-items: center;
  gap: 24px;
}

.elite-avatar-glow {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-glow));
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 16px var(--primary-glow);
}

.titles-elite h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  color: var(--text-primary); /* Fixed Contrast */
}

.title-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}

.status-indicator-pills {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #F1F5F9;
  padding: 4px 12px;
  border-radius: 12px;
}

.status-dot-blink {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #94A3B8;
}

.status-dot-blink.online {
  background: #10B981;
  box-shadow: 0 0 8px #10B981;
}

.status-text {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-primary);
  text-transform: uppercase;
}

.sub-badges-elite {
  display: flex;
  align-items: center;
  gap: 12px;
}

.info-tag {
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 600;
}

.info-tag.secondary {
  color: var(--text-muted);
}

.divider-dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #CBD5E1;
}

.tab-content-panel {
  padding: 0;
  min-height: 500px;
}

.tab-padding {
  padding: 24px 32px;
}

.info-section-elite {
  background: var(--bg-pale);
  padding: 24px;
  border-radius: 20px;
  height: 100%;
}

.section-title {
  margin: 0 0 20px 0;
  font-size: 15px;
  font-weight: 800;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title::before {
  content: '';
  width: 4px;
  height: 14px;
  background: var(--primary-color);
  border-radius: 2px;
}

.table-container-elite {
  padding: 8px 16px;
}

.text-primary-bold {
  font-weight: 700;
  color: var(--text-primary);
}

.text-muted-sm {
  font-size: 13px;
  color: var(--text-muted);
}

.sidebar-stat-card {
  padding: 24px;
}

.sidebar-title {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: var(--text-muted);
  font-weight: 700;
}

.stat-main {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 20px;
}

.stat-value {
  font-size: 32px;
  font-weight: 900;
  color: var(--text-primary);
}

.stat-trend.up {
  font-size: 13px;
  color: #10B981;
  font-weight: 700;
}

.bar-progress {
  height: 6px;
  background: #F1F5F9;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 12px;
}

.bar-fill {
  height: 100%;
  background: var(--primary-color);
  border-radius: 3px;
}

.stat-lbl-sm {
  font-size: 12px;
  color: var(--text-muted);
}

.score-circle-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.score-circle {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 0 24px rgba(79, 70, 229, 0.1);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 6px solid #F1F5F9;
}

.score-num {
  font-size: 32px;
  font-weight: 900;
  color: var(--primary-color);
}

.score-unit {
  font-size: 11px;
  font-weight: 700;
  color: #10B981;
}

.score-risk-list {
  list-style: none;
  padding: 0;
  margin: 16px 0 0 0;
}

.score-risk-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  margin-bottom: 8px;
  color: var(--text-primary);
  font-weight: 600;
}

.bullet {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.bullet.warn { background: #F59E0B; }
.bullet.secure { background: #10B981; }

.pagination-sub {
  padding: 24px 0;
  display: flex;
  justify-content: flex-end;
}

:deep(.elite-tabs .el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.elite-tabs .el-tabs__header) {
  margin-bottom: 0;
  padding: 0 32px;
  border-bottom: 1px solid var(--card-border);
}

:deep(.elite-tabs .el-tabs__item) {
  height: 60px;
  font-weight: 700;
  color: var(--text-muted);
}

:deep(.elite-tabs .el-tabs__item.is-active) {
  color: var(--primary-color);
}

:deep(.elite-desc .el-descriptions__label) {
  color: var(--text-muted);
  font-weight: 600;
}

:deep(.elite-desc .el-descriptions__content) {
  color: var(--text-primary);
  font-weight: 700;
}
</style>
