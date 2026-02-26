<template>
  <div class="table-card">
    <div class="card-header">
      <h3>基线策略管理</h3>
      <el-button type="primary" @click="handleLearning">启动基线学习</el-button>
    </div>

    <el-tabs v-model="activeType" @tab-change="fetchBaselines">
      <el-tab-pane label="进程基线" name="PROCESS" />
      <el-tab-pane label="网络端口" name="PORT" />
      <el-tab-pane label="登录行为" name="LOGIN" />
      <el-tab-pane label="外设基线" name="USB" />
    </el-tabs>

    <el-table :data="baselines" stripe v-loading="loading">
      <el-table-column prop="agentId" label="Agent ID" width="180" />
      <el-table-column prop="version" label="版本" width="80" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="最近更新" width="180" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewItems(row)">查看明细</el-button>
          <el-button link type="success" v-if="row.status === 'PENDING_REVIEW'" @click="handleApprove(row)">通过审核</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 基线内容对话框 -->
    <el-dialog v-model="dialogVisible" title="基线详细项" width="60%">
      <el-table :data="items" max-height="400">
        <el-table-column prop="itemKey" label="特征键" />
        <el-table-column label="详细数据">
          <template #default="{ row }">
            <pre class="data-preview">{{ row.itemData }}</pre>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getBaselines, approveBaseline, getBaselineItems } from '@/api/baseline'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeType = ref('PROCESS')
const baselines = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const items = ref([])

const fetchBaselines = async () => {
  loading.value = true
  try {
    const res: any = await getBaselines({ type: activeType.value })
    baselines.value = res.data || []
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const getStatusType = (status: string) => {
  switch (status) {
    case 'ACTIVE': return 'success'
    case 'LEARNING': return 'primary'
    case 'PENDING_REVIEW': return 'warning'
    default: return 'info'
  }
}

const viewItems = async (row: any) => {
  try {
    const res: any = await getBaselineItems(row.id)
    items.value = res.data || []
    dialogVisible.value = true
  } catch (err) {
    ElMessage.error('无法获取基线内容')
  }
}

const handleApprove = async (row: any) => {
  await ElMessageBox.confirm('确定通过该基线审核吗？通过后将作为安全标准生效。')
  try {
    await approveBaseline(row.agentId, row.type)
    ElMessage.success('审核通过')
    fetchBaselines()
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

const handleLearning = () => {
  ElMessage.info('功能开发中：通过下发指令至Agent启动学习任务')
}

onMounted(() => {
  fetchBaselines()
})
</script>

<style scoped>
.data-preview {
  margin: 0;
  font-size: 12px;
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
}
</style>
