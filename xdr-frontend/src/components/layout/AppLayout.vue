<template>
  <div class="app-wrapper">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="logo">🛡️ XDR 安全中心</div>
      <el-menu
        :default-active="activeMenu"
        router
        text-color="#c0c4cc"
        active-text-color="#409eff"
        background-color="transparent"
      >
        <el-menu-item-group title="系统管理">
          <el-menu-item index="/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
        </el-menu-item-group>
        <el-menu-item-group title="分类管理">
          <el-menu-item index="/assets">
            <el-icon><Platform /></el-icon>
            <span>资产分组管理</span>
          </el-menu-item>
          <el-menu-item index="/alerts">
            <el-icon><Warning /></el-icon>
            <span>告警分类</span>
          </el-menu-item>
        </el-menu-item-group>
      </el-menu>
    </aside>

    <!-- 主内容 -->
    <div class="main-container">
      <header class="topbar">
        <!-- 顶部导航 -->
        <div class="top-nav-wrapper" style="flex:1;">
          <el-menu
            :default-active="activeMenu"
            router
            mode="horizontal"
            style="border-bottom: none;"
          >
            <el-menu-item index="/dashboard">仪表盘</el-menu-item>
            <el-menu-item index="/assets">资产</el-menu-item>
            <el-menu-item index="/alerts">威胁</el-menu-item>
            <el-menu-item index="/compliance">合规</el-menu-item>
            <el-menu-item index="/policy">策略</el-menu-item>
            <el-menu-item index="/upgrade">升级</el-menu-item>
          </el-menu>
        </div>
        <div style="display:flex;align-items:center;gap:16px;">
          <span style="color:#606266;">{{ username }}</span>
          <el-dropdown @command="handleCommand">
            <el-avatar :size="32" style="cursor:pointer;background:#409eff;">
              {{ username.charAt(0).toUpperCase() }}
            </el-avatar>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="page-content">
        <div class="breadcrumb-container" style="margin-bottom: 20px;">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { Platform, Warning, User } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => (route.meta.title as string) || '')
const username = computed(() => authStore.username || 'Admin')

function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.app-wrapper {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  width: 240px;
  background-color: #1f2229;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  background-color: #1a1c22;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
  min-width: 0;
}

.topbar {
  height: 60px;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.page-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}
</style>
