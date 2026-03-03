<template>
  <div class="animate-slide-up">
    <div class="card-header glass-card" style="padding: 24px; margin-bottom: 24px;">
      <div class="titles">
        <h3><el-icon><SetUp /></el-icon> 防护策略矩阵</h3>
        <p class="desc">分层策略体系：全局基准 -> 组织分组 -> 单机定制</p>
      </div>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon> 新增策略
      </el-button>
    </div>

    <div class="table-card glass-card">
      <el-table :data="policies" stripe v-loading="loading">
        <el-table-column prop="name" label="策略名称" min-width="180" />
        <el-table-column prop="level" label="生效层级" width="120">
          <template #default="{ row }">
            <el-tag :type="getLevelTag(row.level)" effect="dark">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetId" label="绑定对象" min-width="150">
          <template #default="{ row }">
            {{ row.level === 'GLOBAL' ? '全网生效' : row.targetId }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="最后修改" width="180" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">配置控制项</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 策略编辑对话框 -->
    <el-dialog v-model="dialogVisible" title="安全策略配置" width="600px" class="glass-dialog">
      <el-form :model="form" label-position="top">
        <el-form-item label="策略显示名称">
          <el-input v-model="form.name" placeholder="请输入策略名" />
        </el-form-item>
        <el-form-item label="管控等级">
          <el-select v-model="form.level" style="width: 100%">
            <el-option label="全局基准" value="GLOBAL" />
            <el-option label="组织分组" value="GROUP" />
            <el-option label="单机定制" value="AGENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="防护设置 (JSON)">
          <el-input 
            type="textarea" 
            v-model="form.content" 
            :rows="8" 
            placeholder='{"firewall": "on", "usb_allow": ["trust-link"]}' 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存并发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getPolicies, savePolicy } from '@/api/policy'
import { SetUp, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const policies = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const form = ref({ name: '', level: 'GLOBAL', content: '', targetId: '' })

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res: any = await getPolicies()
    policies.value = res.data || []
  } catch { /* empty */ } finally {
    loading.value = false
  }
}

function getLevelTag(level: string) {
  switch (level) {
    case 'GLOBAL': return 'success'
    case 'GROUP': return 'warning'
    default: return 'primary'
  }
}

function handleAdd() {
  form.value = { name: '', level: 'GLOBAL', content: '', targetId: '' }
  dialogVisible.value = true
}

function handleEdit(row: any) {
  form.value = { ...row }
  dialogVisible.value = true
}

async function submitForm() {
  await savePolicy(form.value)
  ElMessage.success('策略已更新并推送至相关Agent')
  dialogVisible.value = false
  loadData()
}

function handleDelete(row: any) {
  ElMessage.info('功能演示：此处将调用物理删除接口')
}
</script>

<style scoped>
.desc {
  margin: 4px 0 0 0;
  font-size: 0.85rem;
  color: var(--text-secondary);
}
</style>
