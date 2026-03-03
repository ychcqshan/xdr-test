<template>
  <div class="animate-slide-up">
    <!-- 合规总览 -->
    <div class="table-card glass-card mb-6" style="padding: 32px;">
      <el-row :gutter="40" align="middle">
        <el-col :span="6">
          <div class="pass-rate-panel">
            <el-progress type="dashboard" :percentage="82" :color="colors" :stroke-width="12">
              <template #default="{ percentage }">
                <div class="rate-content">
                  <span class="pct">{{ percentage }}%</span>
                  <span class="lab">合规通过率</span>
                </div>
              </template>
            </el-progress>
          </div>
        </el-col>
        <el-col :span="18">
          <div class="compliance-summary">
            <h4>等保 2.0 (三级) 合规状态</h4>
            <p>最近扫描时间: 2026-03-02 20:00:01 | 总检查项: 45 | 待修复: 8</p>
            <div class="tags-row mt-4">
              <el-tag type="success" effect="dark">身份鉴别 (Pass)</el-tag>
              <el-tag type="success" effect="dark">恶意代码防范 (Pass)</el-tag>
              <el-tag type="danger" effect="dark">访问控制 (Fail)</el-tag>
              <el-tag type="warning" effect="dark">安全审计 (Risk)</el-tag>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 检查标准列表 -->
    <div class="table-card glass-card">
      <div class="card-header">
        <h3><el-icon><Stamp /></el-icon> 核心合规指标</h3>
      </div>
      <el-table :data="standards" stripe>
        <el-table-column prop="category" label="分类" width="150" />
        <el-table-column prop="name" label="指标项" min-width="250" />
        <el-table-column prop="standard" label="标准引用" width="150">
          <template #default="{ row }">
            <el-tag link size="small" type="info">{{ row.standard }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PASS' ? 'success' : 'danger'">
              {{ row.status === 'PASS' ? '符合' : '不符合' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default>
            <el-button link type="primary">查看修复建议</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Stamp } from '@element-plus/icons-vue'

const colors = [
  { color: '#f56c6c', percentage: 20 },
  { color: '#e6a23c', percentage: 40 },
  { color: '#5cb87a', percentage: 60 },
  { color: '#1989fa', percentage: 80 },
  { color: '#6f7ad3', percentage: 100 },
]

const standards = ref([
  { category: '身份鉴别', name: '应提供专用的登录控制控制台或终端', standard: 'GB/T 22239', status: 'PASS' },
  { category: '访问控制', name: '应限制默认网络服务的开启', standard: 'GB/T 22239', status: 'FAIL' },
  { category: '安全审计', name: '审计记录应包括事件的时间、类型、主体', standard: 'GB/T 22239', status: 'PASS' },
  { category: '恶意代码防范', name: '应安装防病毒软件并及时更新', standard: 'GB/T 22239', status: 'PASS' },
])
</script>

<style scoped>
.mb-6 { margin-bottom: 24px; }
.mt-4 { margin-top: 16px; }

.rate-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.rate-content .pct {
  font-size: 2rem;
  font-weight: 700;
  color: #fff;
}
.rate-content .lab {
  font-size: 0.8rem;
  color: var(--text-secondary);
}

.compliance-summary h4 {
  margin: 0 0 12px 0;
  font-size: 1.2rem;
}
.compliance-summary p {
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.tags-row {
  display: flex;
  gap: 12px;
}
</style>
