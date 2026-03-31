<template>
  <div class="intrusion-report-container">
    <!-- Header Summary -->
    <div class="report-header-elite mb-6">
      <div class="header-left">
        <h3 class="flex items-center">
          <el-icon class="mr-2 text-primary"><Search /></el-icon>
          终端入侵痕迹深度排查
        </h3>
        <p class="text-muted-sm mt-1">
          通过 Agent 执行多维度取证扫描，识别隐藏的持久化、恶意进程伪装及反弹 Shell 行为。
        </p>
      </div>
      <div class="header-actions">
        <el-button 
          type="primary" 
          class="elite-button shadow-btn" 
          :loading="scanning"
          @click="handleTriggerScan"
        >
          <el-icon class="mr-1"><Pointer /></el-icon>
          立即执行深度排查
        </el-button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="flex flex-col items-center justify-center py-20">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- Empty State -->
    <div v-else-if="!latestReport" class="empty-report-wrapper py-16">
      <el-empty description="暂无排查报告，请点击上方按钮开始扫描" />
    </div>

    <!-- Report Content -->
    <div v-else class="report-content animate__animated animate__fadeIn">
      <!-- Summary Row -->
      <div class="bento-grid-mini mb-6">
        <div class="bento-stat-card danger" v-if="latestReport.summary.foundAnomalies > 0">
          <div class="stat-value">{{ latestReport.summary.foundAnomalies }}</div>
          <div class="stat-label">发现异常项</div>
        </div>
        <div class="bento-stat-card success" v-else>
          <div class="stat-value">0</div>
          <div class="stat-label">未见异常</div>
        </div>
        <div class="bento-stat-card">
          <div class="stat-value-sm">{{ formatDate(latestReport.summary.scanTime) }}</div>
          <div class="stat-label">最后扫描时间</div>
        </div>
        <div class="bento-stat-card">
          <div class="stat-value-sm">{{ latestReport.summary.scanDuration }}</div>
          <div class="stat-label">扫描耗时</div>
        </div>
      </div>

      <!-- Main Analysis Grid -->
      <div class="forensic-grid">
        <!-- Persistence -->
        <div class="bento-card forensic-card">
          <div class="card-header-iconic">
            <el-icon class="icon"><Timer /></el-icon>
            <span>持久化机制 (Persistence)</span>
          </div>
          <div class="card-body-lite">
            <div v-if="persistenceList.length === 0" class="empty-sm">未发现可疑持久化</div>
            <div v-for="(item, idx) in persistenceList" :key="idx" class="forensic-item" :class="{ warning: item.isSuspicious }">
              <div class="item-main">
                <span class="type-tag">{{ item.type }}</span>
                <span class="item-name" :title="item.path">{{ item.name || item.key }}</span>
              </div>
              <div class="item-value text-muted-sm truncate">{{ item.action || item.value }}</div>
              <div v-if="item.isSuspicious" class="risk-info mt-2">
                <el-icon><Warning /></el-icon> {{ item.reason }}
                <div v-if="item.decodedCommand" class="decoded-box mt-1">
                  <strong>解码载荷:</strong> {{ item.decodedCommand }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Processes -->
        <div class="bento-card forensic-card">
          <div class="card-header-iconic">
            <el-icon class="icon"><Management /></el-icon>
            <span>异常进程分析 (Processes)</span>
          </div>
          <div class="card-body-lite">
            <div v-if="latestReport.processes.length === 0" class="empty-sm">未发现进程异常</div>
            <div v-for="(p, idx) in latestReport.processes" :key="idx" class="forensic-item warning">
              <div class="item-main">
                <span class="pid-tag">PID: {{ p.pid }}</span>
                <span class="item-name">{{ p.name }}</span>
              </div>
              <div class="item-value text-muted-sm">{{ p.path || p.cmdline }}</div>
              <div class="risk-info mt-1">
                <el-icon><Warning /></el-icon> {{ p.reason || '可疑命令行关键词命中' }}
                <div v-if="p.decodedCommand" class="decoded-box mt-1">
                  <strong>载荷还原:</strong> {{ p.decodedCommand }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Network -->
        <div class="bento-card forensic-card">
          <div class="card-header-iconic">
            <el-icon class="icon"><Connection /></el-icon>
            <span>网络连接审计 (Network)</span>
          </div>
          <div class="card-body-lite">
            <div v-if="latestReport.network.length === 0" class="empty-sm">未发现可疑网络连接</div>
            <div v-for="(n, idx) in latestReport.network" :key="idx" class="forensic-item danger">
              <div class="item-main">
                <span class="status-pill danger">REVERSE SHELL</span>
                <span class="item-name">{{ n.processName }} ({{ n.pid }})</span>
              </div>
              <div class="item-value">{{ n.localAddr }} -> {{ n.remoteAddr }}</div>
              <div class="risk-info mt-1">发现反弹 Shell 特征：交互式进程正在进行外部网络通信。</div>
            </div>
          </div>
        </div>

        <!-- File Residue -->
        <div class="bento-card forensic-card">
          <div class="card-header-iconic">
            <el-icon class="icon"><FolderOpened /></el-icon>
            <span>高危目录文件残留 (Files)</span>
          </div>
          <div class="card-body-lite">
            <div v-if="latestReport.fileResidue.length === 0" class="empty-sm">未发现近期可疑文件</div>
            <div v-for="(f, idx) in latestReport.fileResidue" :key="idx" class="forensic-item warning">
              <div class="item-main">
                <span class="ext-tag">{{ f.name.split('.').pop() }}</span>
                <span class="item-name">{{ f.name }}</span>
              </div>
              <div class="item-value text-muted-sm">{{ f.path }}</div>
              <div class="item-meta">
                <span>创建时间: {{ f.creationTime }}</span>
                <span class="ml-4">大小: {{ (f.size / 1024).toFixed(1) }} KB</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getIntrusionReports } from '@/api/asset'
import { issueCommand } from '@/api/policy'
import { ElMessage, ElNotification } from 'element-plus'

const props = defineProps({
  agentId: {
    type: String,
    required: true
  }
})

const loading = ref(false)
const scanning = ref(false)
const reports = ref([])
const latestReport = computed(() => reports.value[0] || null)

// 将 persistence 字典转为统一列表显示
const persistenceList = computed(() => {
  if (!latestReport.value) return []
  const list = []
  const { scheduledTasks, registry, wmiSubscriptions } = latestReport.value.persistence
  
  if (scheduledTasks) {
    scheduledTasks.forEach(t => list.push({ ...t, type: '计划任务' }))
  }
  if (registry) {
    registry.forEach(r => list.push({ ...r, type: '注册表' }))
  }
  if (wmiSubscriptions) {
    wmiSubscriptions.forEach(w => list.push({ ...w, type: 'WMI订阅' }))
  }
  return list
})

const fetchReports = async () => {
  loading.value = true
  try {
    const res = await getIntrusionReports(props.agentId)
    if (res.code === 200) {
      reports.value = res.data
    }
  } catch (err) {
    console.error('Fetch intrusion reports failed', err)
  } finally {
    loading.value = false
  }
}

const handleTriggerScan = async () => {
  scanning.value = true
  try {
    const res = await issueCommand({
      agentId: props.agentId,
      commandType: 'INTRUSION_SCAN',
      commandData: '{}'
    })
    
    if (res.code === 200) {
      ElNotification({
        title: '扫描指令已下发',
        message: 'Agent 正在进行深度取证排查，报告生成通常需要 30-60 秒，请稍后刷新。',
        type: 'success',
        duration: 5000
      })
      
      // 开启轮询，由于扫描较慢，我们简单等待后再刷一次
      setTimeout(fetchReports, 30000)
    }
  } catch (err) {
    ElMessage.error('下发扫描指令失败')
  } finally {
    scanning.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

onMounted(fetchReports)
</script>

<style scoped>
.intrusion-report-container {
  padding: 10px;
}

.report-header-elite {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px;
  background: rgba(var(--el-color-primary-rgb), 0.05);
  border-radius: 12px;
  border-left: 4px solid var(--el-color-primary);
}

.header-left h3 {
  margin: 0;
  font-size: 1.25rem;
  color: #1a1a1a;
}

/* Bento Stats */
.bento-grid-mini {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
}

.bento-stat-card {
  background: white;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
  text-align: center;
}

.bento-stat-card.danger { border-bottom: 3px solid var(--el-color-danger); }
.bento-stat-card.success { border-bottom: 3px solid var(--el-color-success); }

.stat-value { font-size: 2rem; font-weight: 800; color: #1a1a1a; }
.stat-value-sm { font-size: 1rem; font-weight: 600; padding: 10px 0; }
.stat-label { font-size: 0.8rem; color: #909399; margin-top: 4px; }

/* Forensic Grid */
.forensic-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.forensic-card {
  margin-bottom: 0;
  min-height: 300px;
}

.card-header-iconic {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  font-weight: 700;
  font-size: 0.95rem;
  color: #303133;
}

.card-header-iconic .icon {
  font-size: 1.2rem;
  margin-right: 8px;
  color: var(--el-color-primary);
}

.card-body-lite {
  max-height: 400px;
  overflow-y: auto;
}

.forensic-item {
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 12px;
  border: 1px solid #edf2f7;
  transition: all 0.2s;
}

.forensic-item.warning { border-left: 4px solid var(--el-color-warning); background: #fffcf0; }
.forensic-item.danger { border-left: 4px solid var(--el-color-danger); background: #fff5f5; }

.item-main {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}

.item-name {
  font-weight: 600;
  font-size: 0.9rem;
  margin-left: 8px;
}

.type-tag, .pid-tag, .ext-tag {
  font-size: 0.7rem;
  padding: 2px 6px;
  background: #e2e8f0;
  border-radius: 4px;
  color: #4a5568;
}

.status-pill.danger {
  background: var(--el-color-danger);
  color: white;
  font-size: 0.7rem;
  padding: 2px 8px;
  border-radius: 20px;
  font-weight: 700;
}

.item-value {
  font-family: 'Fira Code', monospace;
  font-size: 0.8rem;
  color: #718096;
}

.risk-info {
  font-size: 0.8rem;
  color: var(--el-color-danger);
  display: flex;
  flex-direction: column;
}

.decoded-box {
  background: #2d3748;
  color: #a0aec0;
  padding: 8px;
  border-radius: 4px;
  font-family: monospace;
  margin-top: 5px;
  word-break: break-all;
}

.item-meta {
  font-size: 0.75rem;
  color: #a0aec0;
  margin-top: 8px;
}

.empty-sm {
  text-align: center;
  padding: 30px;
  color: #cbd5e0;
  font-size: 0.9rem;
}

/* Animations */
.animate__animated {
  animation-duration: 0.5s;
}
</style>
