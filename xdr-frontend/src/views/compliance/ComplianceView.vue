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
          <p class="subtitle-elite">追踪端点合规水位线，评估网络安全等级保护 (等保 2.0) 落实情况</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" class="elite-button shadow-btn">
            <el-icon><Refresh /></el-icon> 启动全量扫描
          </el-button>
        </div>
      </div>
    </div>

    <!-- Compliance Stats Bento -->
    <div class="bento-card summary-panel-elite mb-10">
      <el-row :gutter="40" align="middle">
        <el-col :span="6">
          <div class="pass-rate-visual">
            <div class="canvas-wrapper">
              <el-progress 
                type="dashboard" 
                :percentage="82" 
                :color="colors" 
                :stroke-width="12"
                :width="160"
              >
                <template #default="{ percentage }">
                  <div class="elite-rate-content">
                    <span class="pct">{{ percentage }}%</span>
                    <span class="lab">合规通过率</span>
                  </div>
                </template>
              </el-progress>
            </div>
          </div>
        </el-col>
        <el-col :span="18">
          <div class="compliance-details-elite">
            <div class="standard-title">
              <h4>等保 2.0 (三级) 标准评估计划</h4>
              <span class="status-badge success">常规扫描中</span>
            </div>
            <div class="meta-row">
              <span class="meta-item"><span>总检查项:</span> 45</span>
              <span class="meta-divider"></span>
              <span class="meta-item warning"><span>待修复项:</span> 8</span>
              <span class="meta-divider"></span>
              <span class="meta-item"><span>最近扫描:</span> 2026-03-02 20:00:01</span>
            </div>
            <div class="domain-badges mt-6">
              <div class="domain-pill pass">身份鉴别 (符合)</div>
              <div class="domain-pill pass">恶意代码防范 (符合)</div>
              <div class="domain-pill fail">访问控制 (违规)</div>
              <div class="domain-pill risk">安全审计 (风险)</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- Indicator Table -->
    <div class="bento-card">
      <div class="card-header-elite border-header">
        <div class="header-left">
          <h3>核心合规指标明细</h3>
          <el-input
            v-model="searchQuery"
            placeholder="搜索检查项..."
            class="elite-search-input ml-6"
            clearable
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
        <el-button link type="primary" class="elite-text-btn">导出合规报告</el-button>
      </div>

      <div class="table-wrapper-elite">
        <el-table :data="standards" class="elite-table no-border-table">
          <el-table-column prop="category" label="分类协议" width="160">
            <template #default="{ row }">
              <span class="category-text">{{ row.category }}</span>
            </template>
          </el-table-column>
          
          <el-table-column prop="name" label="合规检查项名称" min-width="300">
            <template #default="{ row }">
              <span class="text-primary-bold">{{ row.name }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="standard" label="标准号" width="140">
            <template #default="{ row }">
              <span class="standard-ref">{{ row.standard }}</span>
            </template>
          </el-table-column>
          
          <el-table-column prop="status" label="合规结论" width="120">
            <template #default="{ row }">
              <div class="status-pill-minimal" :class="row.status.toLowerCase()">
                <el-icon class="mr-1"><CircleCheckFilled v-if="row.status === 'PASS'" /><CircleCloseFilled v-else /></el-icon>
                {{ row.status === 'PASS' ? '完全符合' : '不符合' }}
              </div>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="100" fixed="right" align="center">
            <template #default>
              <el-tooltip content="查看修复建议" placement="top">
                <el-button circle size="small">
                  <el-icon><InfoFilled /></el-icon>
                </el-button>
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Stamp, Refresh, Search, CircleCheckFilled, CircleCloseFilled, InfoFilled } from '@element-plus/icons-vue'

const searchQuery = ref('')
const colors = [
  { color: '#f56c6c', percentage: 20 },
  { color: '#e6a23c', percentage: 40 },
  { color: '#10B981', percentage: 100 },
]

const standardsFull = [
  { category: '身份鉴别', name: '应提供专用的登录控制控制台或终端', standard: 'GB/T 22239', status: 'PASS' },
  { category: '访问控制', name: '应限制默认网络服务的开启', standard: 'GB/T 22239', status: 'FAIL' },
  { category: '安全审计', name: '审计记录应包括事件的时间、类型、主体', standard: 'GB/T 22239', status: 'PASS' },
  { category: '恶意代码防范', name: '应安装防病毒软件并及时更新', standard: 'GB/T 22239', status: 'PASS' },
]

const standards = computed(() => {
  if (!searchQuery.value) return standardsFull
  return standardsFull.filter(s => 
    s.name.includes(searchQuery.value) || s.category.includes(searchQuery.value)
  )
})
</script>

<style scoped>
.summary-panel-elite { padding: 40px; }

.elite-rate-content { display: flex; flex-direction: column; align-items: center; }
.elite-rate-content .pct { font-size: 32px; font-weight: 900; color: var(--text-primary); }
.elite-rate-content .lab { font-size: 12px; color: var(--text-muted); font-weight: 600; margin-top: 4px; }

.compliance-details-elite .standard-title { display: flex; align-items: center; gap: 16px; margin-bottom: 12px; }
.compliance-details-elite h4 { margin: 0; font-size: 20px; font-weight: 800; color: var(--text-primary); }

.status-badge { font-size: 11px; font-weight: 800; padding: 4px 10px; border-radius: 8px; text-transform: uppercase; }
.status-badge.success { background: #ECFDF5; color: #059669; }

.meta-row { display: flex; align-items: center; gap: 16px; margin-top: 8px; }
.meta-item { font-size: 14px; color: var(--text-muted); font-weight: 500; }
.meta-item span { font-weight: 700; color: var(--text-primary); margin-right: 4px; }
.meta-item.warning span { color: #DC2626; }
.meta-divider { width: 4px; height: 4px; border-radius: 50%; background: #CBD5E1; }

.domain-badges { display: flex; gap: 12px; flex-wrap: wrap; }
.domain-pill { padding: 6px 14px; border-radius: 12px; font-size: 12px; font-weight: 700; border: 1px solid transparent; }
.domain-pill.pass { background: #F1F5F9; color: #64748B; border-color: #E2E8F0; } /* Subtle pass */
.domain-pill.fail { background: #FEF2F2; color: #991B1B; border-color: #FEE2E2; }
.domain-pill.risk { background: #FFF7ED; color: #9A3412; border-color: #FFEDD5; }

.border-header { padding: 24px 32px; border-bottom: 1px solid var(--card-border); display: flex; justify-content: space-between; align-items: center; }

.category-text { font-size: 13px; color: var(--text-muted); font-weight: 600; }
.text-primary-bold { font-weight: 700; color: var(--text-primary); }
.standard-ref { font-family: monospace; font-size: 11px; color: var(--text-muted); background: var(--bg-pale); padding: 2px 8px; border-radius: 4px; }

.elite-search-input :deep(.el-input__wrapper) {
  background: var(--bg-pale) !important;
  border-radius: 12px !important;
  width: 240px;
}

.header-left {
  display: flex;
  align-items: center;
}

.status-pill-minimal { font-size: 12px; font-weight: 800; }
.status-pill-minimal.pass { color: #10B981; }
.status-pill-minimal.fail { color: #DC2626; }

.op-btn { font-weight: 700; }
</style>
