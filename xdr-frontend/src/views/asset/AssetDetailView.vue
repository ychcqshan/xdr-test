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
          <!-- Multi-Mode Switcher -->
          <div class="mode-switcher-elite mr-4">
            <div 
              class="mode-pill" 
              :class="{ active: !isTimeMachineActive }"
              @click="resetToRealtime"
            >
              <el-icon><Monitor /></el-icon>
              <span>实时状态</span>
            </div>
            <div 
              class="mode-pill warning" 
              :class="{ active: isTimeMachineActive }"
              @click="toggleTimeMachine"
            >
              <el-icon><Calendar /></el-icon>
              <span>时光机</span>
              <div v-if="isTimeMachineActive" class="pulse-warning"></div>
            </div>
          </div>

          <transition name="fade">
            <div v-if="isTimeMachineActive" class="time-picker-popover">
              <el-date-picker
                v-model="selectedTime"
                type="datetime"
                placeholder="选择回溯时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DDTHH:mm:ss"
                @change="fetchHistoryData"
                class="elite-date-picker-mini"
              />
            </div>
          </transition>

          <el-divider direction="vertical" />

          <el-button @click="$router.back()" class="elite-button secondary px-4">
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          
          <el-button type="primary" class="elite-button shadow-btn" @click="fetchData" :loading="loading">
            <el-icon class="mr-1"><Refresh /></el-icon>
            同步
          </el-button>
        </div>
      </div>
      
      <!-- Time Machine Banner -->
      <transition name="slide-down">
        <div v-if="isTimeMachineActive && selectedTime" class="history-banner">
          <el-icon class="mr-2"><InfoFilled /></el-icon>
          当前正在查看 <strong>{{ formatDateTime(selectedTime) }}</strong> 的历史快照。部分实时监控指标已停用。
        </div>
      </transition>
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
                  <el-table-column prop="username" label="用户" width="120" />
                  <el-table-column prop="path" label="路径" show-overflow-tooltip>
                    <template #default="{ row }">
                      <span class="text-muted-sm">{{ row.path || '-' }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="cpuPercent" label="CPU(%)" width="100">
                    <template #default="{ row }">
                      {{ typeof row.cpuPercent === 'number' ? row.cpuPercent.toFixed(1) : '0.0' }}%
                    </template>
                  </el-table-column>
                  <el-table-column prop="createTime" label="启动于" width="180">
                    <template #default="{ row }">
                      {{ row.createTime ? new Date(row.createTime).toLocaleString() : '-' }}
                    </template>
                  </el-table-column>
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

            <el-tab-pane label="网络连接" name="network">
              <div class="table-container-elite">
                <el-table :data="paginatedNetwork" class="elite-table no-border-table">
                  <el-table-column prop="localAddr" label="本地地址" width="180" />
                  <el-table-column prop="remoteAddr" label="对端地址" width="180" />
                  <el-table-column prop="protocol" label="协议" width="100" />
                  <el-table-column prop="status" label="状态" width="150" />
                  <el-table-column prop="pid" label="相关进程 (PID)" />
                </el-table>
                <div class="pagination-sub">
                  <el-pagination
                    v-model:current-page="networkPage"
                    v-model:page-size="pageSize"
                    :total="details?.ports?.length || 0"
                    layout="total, prev, pager, next"
                    class="elite-pagination sm"
                  />
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="软件资产" name="software">
              <div class="table-container-elite">
                <el-table :data="paginatedSoftware" class="elite-table no-border-table">
                  <el-table-column prop="name" label="软件名称" />
                  <el-table-column prop="version" label="版本" width="180" />
                  <el-table-column prop="installDate" label="安装时间" width="200" />
                  <el-table-column prop="publisher" label="发布者" />
                </el-table>
                <div class="pagination-sub">
                  <el-pagination
                    v-model:current-page="softwarePage"
                    v-model:page-size="pageSize"
                    :total="details?.softwares?.length || 0"
                    layout="total, prev, pager, next"
                    class="elite-pagination sm"
                  />
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="外设记录" name="usb">
              <div class="table-container-elite">
                <div class="tab-header-actions mb-4 flex justify-between items-center">
                  <span class="text-muted-sm">外设插入审计历史 (按需筛选范围)</span>
                  <el-date-picker
                    v-model="auditTimeRange"
                    type="datetimerange"
                    start-placeholder="起始时间"
                    end-placeholder="结束时间"
                    size="small"
                    class="elite-date-range-picker"
                    @change="fetchData"
                    :disabled="isTimeMachineActive"
                  />
                </div>
                <el-table :data="paginatedUsb" class="elite-table no-border-table">
                  <el-table-column prop="name" label="设备名称" min-width="150" />
                  <el-table-column prop="deviceId" label="硬件 ID" min-width="220" show-overflow-tooltip />
                  <el-table-column prop="lastSeen" label="最后发现" width="200">
                    <template #default="{ row }">
                      {{ row.lastSeen ? formatDateTime(row.lastSeen) : '-' }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="status" label="当前状态" width="120">
                    <template #default="{ row }">
                      <el-tag :type="row.status === 'CONNECTED' ? 'success' : 'info'" size="small">
                        {{ row.status === 'CONNECTED' ? '已连接' : '历史记录' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                </el-table>
                <div class="pagination-sub">
                  <el-pagination
                    v-model:current-page="usbPage"
                    v-model:page-size="pageSize"
                    :total="details?.usbDevices?.length || 0"
                    layout="total, prev, pager, next"
                    class="elite-pagination sm"
                  />
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="登录审计" name="logins">
              <div class="table-container-elite">
                <div class="tab-header-actions mb-4 flex justify-between items-center">
                  <span class="text-muted-sm">系统登录审计日志 (历史全量回溯)</span>
                  <el-date-picker
                    v-model="auditTimeRange"
                    type="datetimerange"
                    start-placeholder="起始时间"
                    end-placeholder="结束时间"
                    size="small"
                    class="elite-date-range-picker"
                    @change="fetchData"
                    :disabled="isTimeMachineActive"
                  />
                </div>
                <el-table :data="paginatedLogins" class="elite-table no-border-table">
                  <el-table-column prop="userName" label="登录账户" width="180">
                    <template #default="{ row }">
                      <div class="user-cell">
                        <el-icon class="mr-2"><UserFilled /></el-icon>
                        <span class="text-primary-bold">{{ row.userName }}</span>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="host" label="源 IP / 地址" width="180" />
                  <el-table-column prop="loginTime" label="登录时间" width="200">
                    <template #default="{ row }">
                      {{ row.loginTime ? formatDateTime(row.loginTime) : '-' }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="status" label="类型" width="120">
                    <template #default="{ row }">
                      <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'warning'" size="small" effect="dark">
                        {{ row.status === 'ACTIVE' ? '当前在线' : '历史登录' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="terminal" label="会话方式">
                    <template #default="{ row }">
                      <span class="text-muted-sm">{{ row.terminal || '系统默认' }}</span>
                    </template>
                  </el-table-column>
                </el-table>
                <div class="pagination-sub">
                  <el-pagination
                    v-model:current-page="loginPage"
                    v-model:page-size="pageSize"
                    :total="details?.logins?.length || 0"
                    layout="total, prev, pager, next"
                    class="elite-pagination sm"
                  />
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="网络流量" name="traffic">
              <div class="table-container-elite">
                <div class="tab-header-actions mb-4 flex justify-between items-center">
                  <span class="text-muted-sm">流量会话审计 (按连接时间回溯)</span>
                  <el-date-picker
                    v-model="auditTimeRange"
                    type="datetimerange"
                    start-placeholder="起始时间"
                    end-placeholder="结束时间"
                    size="small"
                    class="elite-date-range-picker"
                    @change="fetchData"
                    :disabled="isTimeMachineActive"
                  />
                </div>
                <el-table :data="paginatedTraffic" class="elite-table no-border-table">
                  <el-table-column prop="timestamp" label="采集时间" width="200">
                    <template #default="{ row }">
                      {{ row.timestamp ? formatDateTime(row.timestamp) : '-' }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="srcIp" label="源 IP" width="160" />
                  <el-table-column prop="dstIp" label="目的 IP" width="160" />
                  <el-table-column prop="dstPort" label="端口" width="100" />
                  <el-table-column prop="protocol" label="协议" width="100">
                    <template #default="{ row }">
                      <el-tag size="small">{{ row.protocol === 6 ? 'TCP' : (row.protocol === 17 ? 'UDP' : row.protocol) }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="count" label="连接计数" width="120" sortable />
                </el-table>
                <div class="pagination-sub">
                  <el-pagination
                    v-model:current-page="trafficPage"
                    v-model:page-size="pageSize"
                    :total="details?.traffic?.length || 0"
                    layout="total, prev, pager, next"
                    class="elite-pagination sm"
                  />
                </div>
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
import { getAssetDetail, getAssetDetails, getAssetTimeline } from '@/api/asset'
import { 
  Platform, Refresh, ArrowLeft, UserFilled,
  InfoFilled, Calendar, Connection, Monitor
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const route = useRoute()
const assetId = route.params.id as string
const agentId = route.query.agentId as string

const asset = ref<any>(null)
const details = ref<any>(null)
const activeTab = ref('overview')
const pageSize = ref(12)
const loading = ref(false)

// Time Machine State
const isTimeMachineActive = ref(false)
const selectedTime = ref('')

const processPage = ref(1)
const networkPage = ref(1)
const softwarePage = ref(1)
const usbPage = ref(1)
const loginPage = ref(1)
const trafficPage = ref(1)
const auditTimeRange = ref<any>([])

const paginatedProcesses = computed(() => {
  if (!details.value?.processes) return []
  const start = (processPage.value - 1) * pageSize.value
  return details.value.processes.slice(start, start + pageSize.value)
})

const paginatedNetwork = computed(() => {
  if (!details.value?.ports) return []
  const start = (networkPage.value - 1) * pageSize.value
  return details.value.ports.slice(start, start + pageSize.value)
})

const paginatedSoftware = computed(() => {
  if (!details.value?.softwares) return []
  const start = (softwarePage.value - 1) * pageSize.value
  return details.value.softwares.slice(start, start + pageSize.value)
})

const paginatedUsb = computed(() => {
  if (!details.value?.usbDevices) return []
  const start = (usbPage.value - 1) * pageSize.value
  return details.value.usbDevices.slice(start, start + pageSize.value)
})

const paginatedLogins = computed(() => {
  if (!details.value?.logins) return []
  const start = (loginPage.value - 1) * pageSize.value
  return details.value.logins.slice(start, start + pageSize.value)
})

const paginatedTraffic = computed(() => {
  if (!details.value?.traffic) return []
  const start = (trafficPage.value - 1) * pageSize.value
  return details.value.traffic.slice(start, start + pageSize.value)
})

onMounted(() => fetchData())

async function fetchData() {
  try {
    loading.value = true
    let startTime = ''
    let endTime = ''
    if (auditTimeRange.value && auditTimeRange.value.length === 2) {
      startTime = dayjs(auditTimeRange.value[0]).format('YYYY-MM-DDTHH:mm:ss')
      endTime = dayjs(auditTimeRange.value[1]).format('YYYY-MM-DDTHH:mm:ss')
    }

    const [resAsset, resDetails] = await Promise.all([
      getAssetDetail(assetId),
      getAssetDetails(agentId, startTime, endTime)
    ])
    asset.value = resAsset.data
    // Only update if not in history mode
    if (!isTimeMachineActive.value) {
      details.value = resDetails.data
    }
  } catch { /* ignored */ } finally {
    loading.value = false
  }
}

function toggleTimeMachine() {
  isTimeMachineActive.value = !isTimeMachineActive.value
  if (!isTimeMachineActive.value) {
    resetToRealtime()
  }
}

async function fetchHistoryData() {
  if (!selectedTime.value) return
  try {
    const res = await getAssetTimeline(agentId, selectedTime.value)
    // Convert HostAssetRecord list to details format
    const historyRecords = res.data
    const newDetails = {
      baseInfo: asset.value,
      userInfo: details.value?.userInfo, // Keep current user info for context
      processes: [],
      ports: [],
      softwares: [],
      usbDevices: [],
      logins: [],
      traffic: []
    } as any

    historyRecords.forEach((record: any) => {
      try {
        const itemData = JSON.parse(record.assetData)
        switch (record.assetType) {
          case 'HOST_INFO':
            newDetails.baseInfo = itemData
            break
          case 'PROCESS': newDetails.processes.push(itemData); break
          case 'NETWORK': newDetails.ports.push(itemData); break
          case 'SOFTWARE': newDetails.softwares.push(itemData); break
          case 'USB': newDetails.usbDevices.push(itemData); break
          case 'LOGIN': newDetails.logins.push(itemData); break
          case 'TRAFFIC': newDetails.traffic.push(itemData); break
        }
      } catch (e) { /* skip */ }
    })
    details.value = newDetails
    processPage.value = 1
  } catch (e) {
    console.error('Failed to fetch history', e)
  }
}

function resetToRealtime() {
  selectedTime.value = ''
  isTimeMachineActive.value = false
  fetchData()
}

function formatDateTime(val: string) {
  return dayjs(val).format('YYYY-MM-DD HH:mm:ss')
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

/* Mode Switcher & Time Machine Elite Styles */
.mode-switcher-elite {
  display: flex;
  background: #F1F5F9;
  padding: 4px;
  border-radius: 14px;
  border: 1px solid #E2E8F0;
  gap: 4px;
}

.mode-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 13px;
  font-weight: 700;
  color: #64748B;
  position: relative;
}

.mode-pill .el-icon {
  font-size: 16px;
}

/* Audit Filter Elite Styles */
.elite-date-range-picker {
  background: var(--bg-pale) !important;
  border-radius: 12px !important;
  border: 1px solid var(--card-border) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05) !important;
  transition: all 0.3s ease;
}

.elite-date-range-picker:hover {
  border-color: var(--primary-glow) !important;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.1) !important;
}

:deep(.elite-date-range-picker .el-range-input) {
  background: transparent !important;
  font-weight: 600;
  color: var(--text-primary);
}

:deep(.elite-date-range-picker .el-range-separator) {
  color: var(--text-muted);
}

.tab-header-actions {
  padding: 8px 12px;
  background: rgba(248, 250, 252, 0.5);
  border-radius: 16px;
  backdrop-filter: blur(4px);
}

.mode-pill:hover {
  background: white;
  color: var(--primary-color);
}

.mode-pill.active {
  background: white;
  color: var(--primary-color);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.mode-pill.warning.active {
  color: #D97706;
  background: #FFFBEB;
}

.pulse-warning {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 6px;
  height: 6px;
  background: #F59E0B;
  border-radius: 50%;
  box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.7);
  animation: pulse-warning 2s infinite;
}

@keyframes pulse-warning {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 8px rgba(245, 158, 11, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(245, 158, 11, 0); }
}

.time-picker-popover {
  margin-left: 12px;
  animation: slideInLeft 0.3s ease-out;
}

@keyframes slideInLeft {
  from { opacity: 0; transform: translateX(-10px); }
  to { opacity: 1; transform: translateX(0); }
}

.elite-date-picker-mini {
  width: 200px !important;
}

.history-banner {
  margin-top: 20px;
  padding: 14px 24px;
  background: linear-gradient(90deg, rgba(255, 251, 235, 0.9) 0%, rgba(254, 243, 199, 0.9) 100%);
  border: 1px solid #FDE68A;
  border-radius: 12px;
  color: #92400E;
  font-size: 14px;
  display: flex;
  align-items: center;
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.1);
}

/* Animations */
@keyframes slideDown {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

.slide-down-enter-active, .slide-down-leave-active {
  transition: all 0.4s ease;
}
.slide-down-enter-from, .slide-down-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}
</style>
