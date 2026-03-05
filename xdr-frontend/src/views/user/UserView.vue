<template>
  <div class="page-main">
    <!-- Filter Row (Elite Search) -->
    <div class="bento-card filter-panel-elite mb-10">
      <el-form :inline="true" :model="searchQuery" class="elite-form-inline">
        <el-form-item>
          <el-input 
            v-model="searchQuery.keyword" 
            placeholder="搜索用户名/姓名..." 
            class="elite-search-input-lg"
            :prefix-icon="Search"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-select v-model="searchQuery.role" placeholder="所有角色" clearable class="elite-select">
            <el-option label="超级管理员" value="Admin" />
            <el-option label="审计员" value="Auditor" />
            <el-option label="运维人员" value="Operator" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="elite-button" @click="handleFilter">
            <el-icon><Filter /></el-icon> 过滤
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-row :gutter="32" class="elite-row">
      <!-- 组织架构树 (Elite Sidebar) -->
      <el-col :span="6">
        <div class="bento-card tree-panel-elite">
          <div class="card-header-elite compact">
            <div class="title-with-dot">
              <span class="pulse-dot"></span>
              <h3>组织架构</h3>
            </div>
            <el-button link type="primary" @click="addOrg" class="header-action-btn">
              <el-icon><Plus /></el-icon>
            </el-button>
          </div>
          
          <el-tree
            :data="orgData"
            :props="defaultProps"
            highlight-current
            default-expand-all
            class="elite-tree"
            :expand-on-click-node="false"
          >
            <template #default="{ node, data }">
              <span class="elite-tree-node">
                <span class="node-label text-truncate" :title="node.label">
                  <el-icon class="folder-icon"><FolderOpened v-if="node.expanded" /><Folder v-else /></el-icon>
                  {{ node.label }}
                </span>
                <span class="node-actions-fade">
                  <el-button link @click.stop="handleEditOrg(data)" class="action-icon">
                    <el-icon><EditPen /></el-icon>
                  </el-button>
                  <el-button link type="danger" @click.stop="deleteOrg(data)" class="action-icon delete">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </span>
              </span>
            </template>
          </el-tree>
        </div>
      </el-col>

      <!-- 用户列表 (Main Bento Content) -->
      <el-col :span="18">
        <div class="bento-card table-panel-elite">
          <div class="card-header-elite border-header">
            <h3>活跃账户列表</h3>
            <el-button type="primary" class="elite-button shadow-btn" @click="addUser">
              <el-icon><Plus /></el-icon> 新增账户
            </el-button>
          </div>
          
          <div class="table-wrapper">
            <el-table :data="users" stripe border class="elite-table compact-table">
              <el-table-column prop="username" label="账户名" width="140">
                <template #default="{ row }">
                  <div class="user-cell">
                    <el-avatar :size="24" class="mr-2">{{ row.username.charAt(0).toUpperCase() }}</el-avatar>
                    <span class="text-primary-bold">{{ row.username }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="realName" label="真实姓名" width="100" />
              <el-table-column prop="role" label="角色级别" width="130">
                <template #default="{ row }">
                  <el-tag :type="getRoleType(row.role)" size="small" effect="light" class="elite-tag">
                    {{ row.role }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="department" label="所属分支" min-width="150" show-overflow-tooltip>
                <template #default="{ row }">
                  <span class="text-muted-sm">{{ row.department }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="lastLogin" label="最后活跃" width="160" />
              <el-table-column label="服务状态" width="100" align="center">
                <template #default="{ row }">
                  <el-switch 
                    v-model="row.active" 
                    inline-prompt
                    active-text="启用"
                    inactive-text="禁用"
                    @change="toggleUser(row)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="管理操作" width="120" align="center">
                <template #default="{ row }">
                  <div class="action-icons">
                    <el-tooltip content="修改信息" placement="top">
                      <el-button circle size="small" @click="editUser(row)">
                        <el-icon><Edit /></el-icon>
                      </el-button>
                    </el-tooltip>
                    <el-tooltip content="删除账户" placement="top">
                      <el-button circle size="small" type="danger" plain @click="confirmDelete(row)">
                        <el-icon><Delete /></el-icon>
                      </el-button>
                    </el-tooltip>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="pagination-container">
            <el-pagination
              layout="total, prev, pager, next"
              :total="users.length"
              :page-size="10"
              class="elite-pagination"
            />
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- Org Edit Dialog -->
    <el-dialog
      v-model="orgDialogVisible"
      title="修改部门名称"
      width="400px"
      class="elite-dialog"
    >
      <el-form :model="editOrgForm">
        <el-form-item label="部门名称">
          <el-input v-model="editOrgForm.label" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="orgDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveOrg">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { 
  Share, Plus, User, Search, Folder, FolderOpened, 
  EditPen, Delete, MoreFilled 
} from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'

const searchQuery = ref({ keyword: '', role: '' })
const orgDialogVisible = ref(false)
const editOrgForm = ref({ label: '', id: '' })

const orgData = ref([
  {
    id: '1',
    label: '集团总部',
    children: [
      { id: '2', label: '研发中心', children: [{ id: '4', label: '后端组' }, { id: '5', label: '前端组' }] },
      { id: '3', label: '运维部' },
      { id: '6', label: '安全审计部' },
    ],
  },
])

const defaultProps = { children: 'children', label: 'label' }

const users = ref([
  { username: 'admin', realName: '超管', role: '超级管理员', department: '集团总部', lastLogin: '2026-03-03 10:05', active: true },
  { username: 'dev_user', realName: '张三', role: '研发人员', department: '研发中心/后端组', lastLogin: '2026-03-03 14:33', active: true },
  { username: 'audit_x', realName: '李四', role: '审计人员', department: '安全审计部', lastLogin: '2026-03-02 15:20', active: true },
])

function addOrg() { ElMessage.info('新增部门功能开发中...') }
function handleEditOrg(data: any) {
  editOrgForm.value = { ...data }
  orgDialogVisible.value = true
}
function saveOrg() {
  const updateNode = (data: any[]) => {
    for (let i = 0; i < data.length; i++) {
      if (data[i].id === editOrgForm.value.id) {
        data[i].label = editOrgForm.value.label
        return true
      }
      if (data[i].children && updateNode(data[i].children)) return true
    }
    return false
  }
  updateNode(orgData.value)
  ElMessage.success(`部门名称已成功修改`)
  orgDialogVisible.value = false
}
function deleteOrg(data: any) { ElMessage.warning(`正在删除: ${data.label}`) }
function addUser() { ElMessage.info('新增账户功能开发中...') }
function editUser(user: any) { ElMessage.info(`编辑用户: ${user.username}`) }
function handleFilter() { ElMessage.info('数据过滤中...') }
function toggleUser(user: any) { ElMessage.success(`${user.active ? '启用' : '禁用'}成功`) }

function getRoleType(role: string) {
  if (role.includes('管理')) return 'danger'
  if (role.includes('研发')) return 'success'
  return 'info'
}

function confirmDelete(user: any) {
  ElMessageBox.confirm(`确定要永久删除用户 ${user.username} 吗？`, '安全警告', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning',
    customClass: 'elite-message-box'
  }).then(() => ElMessage.success('用户已移除'))
}
</script>

<style scoped>
.filter-panel-elite {
  padding: 24px;
}

.elite-form-inline {
  display: flex;
  align-items: center;
  gap: 16px;
}

.elite-search-input-lg :deep(.el-input__wrapper) {
  width: 280px;
  border-radius: 12px !important;
  background: var(--bg-pale) !important;
}

.elite-select :deep(.el-input__wrapper) {
  width: 160px;
  border-radius: 12px !important;
}

.tree-panel-elite {
  min-height: calc(100vh - 240px);
  padding: 6px 8px;
}

.table-panel-elite {
  min-height: calc(100vh - 240px);
  padding: 0;
}

.elite-search-input :deep(.el-input__wrapper) {
  background: var(--bg-pale) !important;
  border-radius: 12px !important;
  box-shadow: none !important;
  padding: 0 16px !important;
}

.elite-tree {
  margin-top: 12px;
}

.elite-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 4px;
}

.node-label {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.folder-icon {
  color: #94A3B8;
  font-size: 16px;
}

.node-actions-fade {
  opacity: 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  gap: 8px;
  transform: translateX(10px);
}

.elite-tree-node:hover .node-actions-fade {
  opacity: 1;
  transform: translateX(0);
}

.user-cell {
  display: flex;
  align-items: center;
}

.action-icons {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.elite-tag {
  border-radius: 6px;
  font-weight: 700;
  font-size: 11px;
}

.border-header {
  padding: var(--space-xl);
  border-bottom: 1px solid var(--card-border);
  margin-bottom: 20px;
}

.table-wrapper {
  padding: 0 var(--space-xl);
}

.text-primary-bold {
  font-weight: 700;
  color: var(--text-primary);
}

.text-muted-sm {
  font-size: 13px;
  color: var(--text-muted);
}

.role-badge {
  display: inline-flex;
  padding: 4px 12px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

.role-badge.superadmin { background: #EEF2FF; color: #4F46E5; }
.role-badge.developer { background: #ECFDF5; color: #10B981; }
.role-badge.secoperator { background: #FFFBEB; color: #D97706; }

.table-ops {
  display: flex;
  align-items: center;
}

.op-btn {
  font-weight: 700;
  font-size: 13px;
}

.pagination-container {
  padding: 24px var(--space-xl);
  display: flex;
  justify-content: flex-end;
}

/* Tree Override */
:deep(.el-tree-node__content) {
  height: auto !important;
  border-radius: 10px;
  margin: 1px 0;
  transition: all 0.2s;
}

:deep(.el-tree-node__content:hover) {
  background: var(--bg-pale) !important;
}

:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: rgba(79, 70, 229, 0.08) !important;
}

:deep(.el-tree-node.is-current > .el-tree-node__content .node-label) {
  color: var(--primary-color);
  font-weight: 700;
}
</style>
