<template>
  <div class="page-main">
    <!-- Elite Header -->
    <div class="bento-card page-header-elite mb-10">
      <div class="header-main">
        <div class="title-section">
          <div class="title-with-dot">
            <span class="pulse-dot active"></span>
            <h3>防御策略中心</h3>
          </div>
          <p class="subtitle-elite">下发原子级终端安全基座策略，实现勒索防护、合规加固与入侵阻断</p>
        </div>
        <div class="header-actions">
          <div class="sync-status">
            <el-icon><Refresh /></el-icon>
            <span>默认拉取频率: 24h</span>
          </div>
          <el-button type="primary" class="elite-button shadow-btn" @click="saveAllPolicies">
            <el-icon><Check /></el-icon> 应用并下发
          </el-button>
        </div>
      </div>
    </div>

    <!-- Policy Categories Grid -->
    <el-row :gutter="24">
      <!-- Ransomware Protection -->
      <el-col :span="12" class="mb-10">
        <div class="bento-card policy-card">
          <div class="card-header-elite border-header">
            <div class="header-with-icon">
              <div class="icon-box warning"><el-icon><Lock /></el-icon></div>
              <div class="text">
                <h3>勒索病毒防御 (Anti-Ransomware)</h3>
                <span class="text-muted-xs">监控文件加密行为与诱饵文件触发</span>
              </div>
            </div>
            <el-switch v-model="policyState.ransomware.enabled" />
          </div>
          <div class="policy-body">
            <el-form label-position="top">
              <el-form-item label="诱饵文件路径 (Honeyfiles)">
                <el-input v-model="policyState.ransomware.honeyfilePath" placeholder="C:\Users\Public\Documents\Important.docx" />
              </el-form-item>
              <el-form-item label="加密频率阈值 (文件/秒)">
                <el-slider v-model="policyState.ransomware.threshold" :max="100" show-input />
              </el-form-item>
              <el-form-item label="响应动作">
                <el-checkbox-group v-model="policyState.ransomware.actions">
                  <el-checkbox label="TERM_PROCESS">终止进程</el-checkbox>
                  <el-checkbox label="ISOLATE_HOST">隔离主机</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </el-col>

      <!-- Peripheral Control -->
      <el-col :span="12" class="mb-10">
        <div class="bento-card policy-card">
          <div class="card-header-elite border-header">
            <div class="header-with-icon">
              <div class="icon-box info"><el-icon><Usb /></el-icon></div>
              <div class="text">
                <h3>外设准入控制 (USB Control)</h3>
                <span class="text-muted-xs">审计并限制外部存储设备的接入</span>
              </div>
            </div>
            <el-switch v-model="policyState.usb.enabled" />
          </div>
          <div class="policy-body">
            <el-form label-position="top">
              <el-form-item label="管控范围">
                <el-checkbox-group v-model="policyState.usb.scope">
                  <el-checkbox label="DISK_DRIVE">移动硬盘</el-checkbox>
                  <el-checkbox label="CD_ROM">光驱/刻录机</el-checkbox>
                  <el-checkbox label="PORTABLE_DEVICE">便携式 MTP 设备</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item label="准入策略">
                <el-radio-group v-model="policyState.usb.mode">
                  <el-radio label="READ_ONLY">只读模式</el-radio>
                  <el-radio label="BLOCK">完全阻断</el-radio>
                  <el-radio label="AUDIT_ONLY">仅审计</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </el-col>

      <!-- Advanced Command Monitoring -->
      <el-col :span="24">
        <div class="bento-card policy-card">
          <div class="card-header-elite border-header">
            <div class="header-with-icon">
              <div class="icon-box primary"><el-icon><Terminal /></el-icon></div>
              <div class="text">
                <h3>高危指令审计与阻断 (Command EDR)</h3>
                <span class="text-muted-xs">基于 ATT&CK 框架实时拦截可疑命令执行</span>
              </div>
            </div>
            <el-button link type="primary" @click="addRule">新增规则</el-button>
          </div>
          <div class="policy-body">
            <el-table :data="policyState.commands.rules" class="elite-table compact-table">
              <el-table-column prop="name" label="规则名称" width="180" />
              <el-table-column prop="pattern" label="匹配正则 (Regex)" />
              <el-table-column prop="severity" label="严重程度" width="120">
                <template #default="{ row }">
                  <el-tag :type="tagType(row.severity)" size="small">{{ row.severity }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="action" label="拦截响应" width="120" />
              <el-table-column label="操作" width="80" align="center">
                <template #default="{ $index }">
                  <el-button link type="danger" @click="policyState.commands.rules.splice($index, 1)"><el-icon><Delete /></el-icon></el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { 
  Lock, Refresh, Check, Warning, Setting,
  Delete, Plus, SetUp, Search, Connection, Monitor
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

// Using local icons as fallback or defining them
const Usb = Connection
const Terminal = Monitor

const policyState = reactive({
  ransomware: {
    enabled: true,
    honeyfilePath: 'C:\\Users\\Public\\Security\\Honeyfile.dat',
    threshold: 15,
    actions: ['TERM_PROCESS']
  },
  usb: {
    enabled: false,
    scope: ['DISK_DRIVE', 'CD_ROM'],
    mode: 'READ_ONLY'
  },
  commands: {
    rules: [
      { name: 'PsExec 探测', pattern: 'psexec', severity: 'HIGH', action: 'ALERT' },
      { name: 'Mimikatz 执行', pattern: 'mimikatz|sekurlsa', severity: 'CRITICAL', action: 'BLOCK' },
      { name: '编码 PowerShell', pattern: 'powershell.*-enc', severity: 'MEDIUM', action: 'ALERT' }
    ]
  }
})

function saveAllPolicies() {
  ElMessage.success('安全策略已成功合并下发，版本: v' + Date.now().toString().slice(-6))
}

function addRule() {
  policyState.commands.rules.push({
    name: '新建规则',
    pattern: '.*',
    severity: 'LOW',
    action: 'ALERT'
  })
}

function tagType(s: string) {
  if (s === 'CRITICAL') return 'danger'
  if (s === 'HIGH') return 'warning'
  return 'info'
}

onMounted(() => {
  // Fetch from /api/v1/policies
})
</script>

<style scoped>
.page-header-elite { padding: 32px; }
.header-main { display: flex; justify-content: space-between; align-items: center; }
.sync-status { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--text-muted); margin-right: 24px; font-weight: 600; }

.header-with-icon { display: flex; align-items: center; gap: 16px; }
.icon-box {
  width: 48px; height: 48px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px;
}
.icon-box.warning { background: #FEF2F2; color: #EF4444; }
.icon-box.info { background: #F0F9FF; color: #0EA5E9; }
.icon-box.primary { background: #EEF2FF; color: #4F46E5; }

.policy-card { padding: 0; }
.border-header { padding: 20px 32px; border-bottom: 1px solid var(--card-border); display: flex; justify-content: space-between; align-items: center; }
.policy-body { padding: 24px 32px; }

.text-muted-xs { font-size: 11px; color: var(--text-muted); display: block; margin-top: 2px; }
.text-muted-sm { font-size: 13px; color: var(--text-muted); }

:deep(.el-form-item__label) { font-weight: 700; color: var(--text-primary); font-size: 13px; margin-bottom: 8px !important; }
:deep(.el-slider) { margin-left: 10px; margin-right: 15px; }
</style>
