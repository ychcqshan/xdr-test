<template>
  <div class="download-container">
    <div class="page-header">
      <h2 class="elite-title">Agent 下载中心</h2>
      <p class="subtitle">请根据目标操作系统选择合适的探针版本进行下载安装</p>
    </div>

    <el-row :gutter="24" class="mt-6">
      <!-- Windows Agent -->
      <el-col :span="12">
        <el-card class="elite-card agent-card" shadow="hover">
          <div class="card-content">
            <div class="icon-wrapper windows-icon">
              <el-icon><Monitor /></el-icon>
            </div>
            <div class="info">
              <h3 class="agent-title">Windows 探针 (x64)</h3>
              <p class="agent-desc">支持 Windows 10/11, Windows Server 2016 及以上版本。内置自保护与内核级采集模块。</p>
              <div class="meta">
                <el-tag size="small" type="info">v1.1.0</el-tag>
                <span class="size">~45 MB</span>
              </div>
            </div>
          </div>
          <template #footer>
            <div class="actions">
              <el-button type="primary" class="elite-button" @click="downloadAgent('windows')">
                <el-icon class="mr-1"><Download /></el-icon>
                立即下载 (EXE)
              </el-button>
              <el-button class="elite-button secondary" @click="copyCommand('windows')">
                复制静默安装命令
              </el-button>
            </div>
          </template>
        </el-card>
      </el-col>

      <!-- Linux Agent -->
      <el-col :span="12">
        <el-card class="elite-card agent-card" shadow="hover">
          <div class="card-content">
            <div class="icon-wrapper linux-icon">
              <el-icon><Platform /></el-icon>
            </div>
            <div class="info">
              <h3 class="agent-title">Linux 探针 (x64)</h3>
              <p class="agent-desc">支持 Ubuntu 20.04+, CentOS 7+, Debian 11+。包含 eBPF 高级检测特性。</p>
              <div class="meta">
                <el-tag size="small" type="info">v1.1.0</el-tag>
                <span class="size">~38 MB</span>
              </div>
            </div>
          </div>
          <template #footer>
            <div class="actions">
              <el-button type="primary" class="elite-button" @click="downloadAgent('linux')">
                <el-icon class="mr-1"><Download /></el-icon>
                立即下载 (ZIP)
              </el-button>
              <el-button class="elite-button secondary" @click="copyCommand('linux')">
                复制安装脚本命令
              </el-button>
            </div>
          </template>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { Download, Monitor, Platform } from '@element-plus/icons-vue'

const downloadAgent = (os: string) => {
  // 假设我们将文件放在了后端的静态资源目录下，或者前端 public 目录下
  // 在实际环境中，应该向后端请求下载链接或者直接使用静态链接
  const fileUrl = os === 'windows' ? '/downloads/xdr-agent-win.exe' : '/downloads/xdr-agent-linux.zip'
  
  const link = document.createElement('a')
  link.href = fileUrl
  link.download = os === 'windows' ? 'xdr-agent-win.exe' : 'xdr-agent-linux.zip'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  
  ElMessage.success(`正在开始下载 ${os === 'windows' ? 'Windows' : 'Linux'} 探针...`)
}

const copyCommand = (os: string) => {
  let cmd = ''
  if (os === 'windows') {
    cmd = `certutil -urlcache -split -f "http://111.229.201.185/downloads/xdr-agent-win.exe" xdr-agent.exe && xdr-agent.exe --silent`
  } else {
    cmd = `curl -sL http://111.229.201.185/downloads/xdr-agent-linux.zip -o agent.zip && unzip agent.zip && sudo ./install.sh`
  }
  
  navigator.clipboard.writeText(cmd).then(() => {
    ElMessage.success('安装命令已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败，请手动选择复制')
  })
}
</script>

<style scoped>
.download-container {
  padding: 24px;
}

.page-header {
  margin-bottom: 32px;
}

.subtitle {
  color: #64748b;
  margin-top: 8px;
  font-size: 14px;
}

.agent-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-content {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  padding: 10px 0;
}

.icon-wrapper {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  flex-shrink: 0;
}

.windows-icon {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.linux-icon {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.info {
  flex: 1;
}

.agent-title {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.agent-desc {
  color: #64748b;
  font-size: 14px;
  line-height: 1.5;
  margin: 0 0 16px 0;
}

.meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.size {
  color: #94a3b8;
  font-size: 13px;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}
</style>
