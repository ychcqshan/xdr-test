<template>
  <div class="page-main">
    <!-- Elite Header -->
    <div class="bento-card page-header-elite mb-10">
      <div class="header-main">
        <div class="title-section">
          <div class="title-with-dot">
            <span class="pulse-dot active"></span>
            <h3>合规治理中心</h3>
          </div>
          <p class="subtitle-elite">追踪端点合规水位线，基于等保 2.0 及 ISO 27001 标准自动化评估 Agent 原子项</p>
        </div>
        <div class="header-actions">
          <el-button class="elite-button secondary">
            <el-icon><Download /></el-icon> 导出合规报告
          </el-button>
          <el-button type="primary" class="elite-button shadow-btn" @click="startFullAssessment">
            <el-icon><Refresh /></el-icon> 启动全量扫描
          </el-button>
        </div>
      </div>
    </div>

    <!-- Compliance Stats Bento -->
    <el-row :gutter="24" class="mb-10">
      <el-col :span="8">
        <div class="bento-card stats-card-elite">
          <div class="card-header-elite">
            <h3>等保 2.0 (GB/T 22239)</h3>
            <span class="status-badge success">合规度: 78%</span>
          </div>
          <div class="chart-container">
            <el-progress type="dashboard" :percentage="78" :color="colors" :stroke-width="12" />
            <div class="chart-info">
              <span class="count">24 / 31</span>
              <span class="label">检查项通过率</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="bento-card stats-card-elite">
          <div class="card-header-elite">
            <h3>ISO/IEC 27001</h3>
            <span class="status-badge warning">合规度: 62%</span>
          </div>
          <div class="chart-container">
            <el-progress type="dashboard" :percentage="62" :color="colors" :stroke-width="12" />
            <div class="chart-info">
              <span class="count">85 / 138</span>
              <span class="label">控制项符合率</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="bento-card stats-card-elite">
          <div class="card-header-elite">
            <h3>单位节点整体状态</h3>
            <span class="status-badge">在线率: 91%</span>
          </div>
          <div class="chart-container">
            <el-progress type="dashboard" :percentage="91" :color="colors" :stroke-width="12" />
            <div class="chart-info">
              <span class="count">42 / 46</span>
              <span class="label">合规资产数</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- Detailed Finding Table -->
    <div class="bento-card">
      <div class="card-header-elite border-header">
        <div class="header-left">
          <h3>合规风险项与修复建议 (Findings)</h3>
          <el-input
            v-model="searchQuery"
            placeholder="搜索风险项信息..."
            class="elite-search-input ml-6"
            clearable
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
        <div class="header-right">
          <span class="text-muted-xs mr-4">自动评估周期: 24h</span>
          <el-radio-group v-model="filterType" size="small">
            <el-radio-button label="ALL">全部</el-radio-button>
            <el-radio-button label="RISK">高风险</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <div class="table-wrapper-elite">
        <el-table :data="filteredFindings" class="elite-table no-border-table">
          <el-table-column prop="title" label="检查项/风险说明" min-width="280">
            <template #default="{ row }">
              <div class="finding-title">
                <el-icon :class="row.severity.toLowerCase()"><CircleCheckFilled v-if="row.status === 'PASS'" /><CircleCloseFilled v-else /></el-icon>
                <span>{{ row.title }}</span>
              </div>
            </template>
          </el-table-column>
          
          <el-table-column prop="standards" label="关联标准" width="180">
            <template #default="{ row }">
              <div class="tags-flex">
                <el-tag v-for="tag in row.standards" :key="tag" size="small" effect="plain" class="elite-tag-sm">{{ tag }}</el-tag>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="suggestion" label="修复路径" min-width="200">
            <template #default="{ row }">
              <span class="text-muted-sm">{{ row.suggestion }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="agentCount" label="受影响资产" width="120" align="center">
            <template #default="{ row }">
              <span class="asset-count" @click="ElMessage.info('资产详情过滤开发中')">{{ row.agentCount }} 台</span>
            </template>
          </el-table-column>

          <el-table-column label="动作" width="120" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" class="op-btn" @click="quickFix(row)">一键加固</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { 
  Refresh, Download, Search, CircleCheckFilled, 
  CircleCloseFilled, InfoFilled 
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const searchQuery = ref('')
const filterType = ref('ALL')

const colors = [
  { color: '#EF4444', percentage: 20 },
  { color: '#F59E0B', percentage: 40 },
  { color: '#10B981', percentage: 80 },
  { color: '#4F46E5', percentage: 100 }
]

const findings = ref([
  { 
    title: '系统口令复杂度不足', 
    status: 'FAIL',
    severity: 'HIGH', 
    standards: ['等保三级-S3', 'ISO 5.17'], 
    suggestion: '通过策略中心强制开启密码复杂度。',
    agentCount: 12
  },
  { 
    title: '恶意代码防范定义更新过时', 
    status: 'PASS',
    severity: 'LOW', 
    standards: ['等保三级-S3'], 
    suggestion: '检查资产 10.0.2.15 的离线时间。',
    agentCount: 1
  },
  { 
    title: '未启用登录失败处理机制', 
    status: 'FAIL',
    severity: 'HIGH', 
    standards: ['等保三级-S3'], 
    suggestion: '配置超过5次登录失败锁定账户。',
    agentCount: 8
  },
  { 
    title: '终端发现明文密码记录文件', 
    status: 'FAIL',
    severity: 'CRITICAL', 
    standards: ['ISO 14.1'], 
    suggestion: '立即清理桌面 password.txt。',
    agentCount: 3
  }
])

const filteredFindings = computed(() => {
  let list = findings.value
  if (filterType.value === 'RISK') {
    list = list.filter(f => f.status === 'FAIL')
  }
  if (searchQuery.value) {
    list = list.filter(f => f.title.includes(searchQuery.value) || f.suggestion.includes(searchQuery.value))
  }
  return list
})

function startFullAssessment() {
  ElMessage.info('全网即时扫描指令已通过网关异步下发...')
}

function quickFix(row: any) {
  ElMessage.success(`正在为 ${row.agentCount} 台主机执行批量修复指令 [${row.title}]`)
}
</script>

<style scoped>
.page-header-elite { padding: 32px; }
.header-main { display: flex; justify-content: space-between; align-items: center; }

.stats-card-elite { padding: 24px; }
.card-header-elite { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 32px; }
.status-badge { font-size: 11px; font-weight: 800; background: var(--bg-pale); padding: 4px 12px; border-radius: 20px; color: var(--text-muted); }
.status-badge.success { color: #10B981; background: rgba(16, 185, 129, 0.08); }
.status-badge.warning { color: #F59E0B; background: rgba(245, 158, 11, 0.08); }

.chart-container { display: flex; flex-direction: column; align-items: center; justify-content: center; position: relative; }
.chart-info { position: absolute; top: 50%; transform: translateY(-30%); display: flex; flex-direction: column; align-items: center; }
.chart-info .count { font-size: 20px; font-weight: 800; color: var(--text-primary); }
.chart-info .label { font-size: 10px; color: var(--text-muted); font-weight: 600; text-transform: uppercase; margin-top: 4px; }

.border-header { padding: 24px 32px; border-bottom: 1px solid var(--card-border); display: flex; justify-content: space-between; align-items: center; }
.header-left { display: flex; align-items: center; }
.elite-search-input :deep(.el-input__wrapper) { background: var(--bg-pale) !important; border-radius: 12px !important; width: 240px; }

.table-wrapper-elite { padding: 12px 16px; }
.finding-title { display: flex; align-items: center; gap: 12px; font-weight: 700; color: var(--text-primary); font-size: 14px; }
.finding-title .high { color: #EF4444; }
.finding-title .critical { color: #B91C1C; }
.finding-title .low { color: #10B981; }

.tags-flex { display: flex; flex-wrap: wrap; gap: 4px; }
.elite-tag-sm { font-size: 10px; font-weight: 700; border-radius: 4px; }

.text-muted-sm { font-size: 13px; color: var(--text-muted); }
.text-muted-xs { font-size: 11px; color: var(--text-muted); font-weight: 600; }

.asset-count { cursor: pointer; color: var(--primary-color); font-weight: 800; text-decoration: underline; text-underline-offset: 4px; }
.op-btn { font-weight: 800; font-size: 13px; }
</style>
