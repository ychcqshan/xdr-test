<template>
  <div class="org-user-container animate-slide-up">
    <el-row :gutter="24">
      <!-- 组织架构树 -->
      <el-col :span="6">
        <div class="glass-card tree-panel">
          <div class="panel-header">
            <h4><el-icon><Share /></el-icon> 组织架构</h4>
            <el-button link type="primary" @click="addOrg"><el-icon><Plus /></el-icon></el-button>
          </div>
          <el-input v-model="filterText" placeholder="搜索分支..." class="mb-4" size="small" />
          <el-tree
            :data="orgData"
            :props="defaultProps"
            highlight-current
            default-expand-all
            class="premium-tree"
          >
            <template #default="{ node, data }">
              <span class="custom-tree-node">
                <span>{{ node.label }}</span>
                <span class="node-ops">
                  <el-button link type="primary" size="small" @click.stop="editOrg(data)">改</el-button>
                </span>
              </span>
            </template>
          </el-tree>
        </div>
      </el-col>

      <!-- 用户列表 -->
      <el-col :span="18">
        <div class="table-card glass-card">
          <div class="card-header">
            <h3>用户账号管理</h3>
            <el-button type="primary" @click="addUser">
              <el-icon><User /></el-icon> 创建账号
            </el-button>
          </div>
          <el-table :data="users" stripe>
            <el-table-column prop="username" label="用户名" width="150" />
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column prop="role" label="角色" width="120">
              <template #default="{ row }">
                <el-tag size="small">{{ row.role }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="department" label="所属机构" min-width="150" />
            <el-table-column prop="lastLogin" label="最后登录" width="180" />
            <el-table-column label="操作" width="150">
              <template #default>
                <el-button link type="primary">编辑</el-button>
                <el-button link type="danger">禁用</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Share, Plus, User } from '@element-plus/icons-vue'

const filterText = ref('')
const orgData = ref([
  {
    label: '集团总部',
    children: [
      { label: '研发中心', children: [{ label: '后端组' }, { label: '前端组' }] },
      { label: '运维部' },
      { label: '安全审计部' },
    ],
  },
])

const defaultProps = { children: 'children', label: 'label' }

const users = ref([
  { username: 'admin', realName: '超管', role: 'SuperAdmin', department: '集团总部', lastLogin: '2026-03-02 21:05' },
  { username: 'dev_user', realName: '张三', role: 'Developer', department: '研发中心/后端组', lastLogin: '2026-03-02 10:33' },
  { username: 'sec_op', realName: '李四', role: 'SecOperator', department: '安全审计部', lastLogin: '2026-03-01 15:20' },
])

function addOrg() {}
function editOrg(data: any) {}
function addUser() {}
</script>

<style scoped>
.tree-panel {
  padding: 20px;
  min-height: calc(100vh - 160px);
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.panel-header h4 { margin: 0; color: #fff; }

.mb-4 { margin-bottom: 16px; }

.premium-tree {
  background: transparent !important;
  color: var(--text-secondary) !important;
}

:deep(.el-tree-node__content:hover) {
  background: rgba(255, 255, 255, 0.05) !important;
}
:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: rgba(99, 102, 241, 0.1) !important;
  color: #fff !important;
}

.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-right: 8px;
}
.node-ops { opacity: 0; transition: opacity 0.2s; }
.custom-tree-node:hover .node-ops { opacity: 1; }
</style>
