<template>
  <div class="app-wrapper animate-slide-up">
    <!-- Sidebar (Linear Style) -->
    <aside class="sidebar-container">
      <div class="logo font-mono">
        <div class="logo-box">
          <el-icon size="20" color="#fff"><Checked /></el-icon>
        </div>
        <span>XDR CONSOLE</span>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        router
        class="custom-menu"
        text-color="#64748b"
        active-text-color="#fff"
        background-color="transparent"
      >
        <div class="menu-section">监控</div>
        <el-menu-item index="/dashboard">
          <el-icon><Monitor /></el-icon>
          <span>安全概览</span>
        </el-menu-item>

        <div class="menu-section">资产</div>
        <el-menu-item index="/assets">
          <el-icon><Platform /></el-icon>
          <span>资产管理</span>
        </el-menu-item>
        <el-menu-item index="/alerts">
          <el-icon><Warning /></el-icon>
          <span>威胁告警</span>
        </el-menu-item>

        <div class="menu-section">治理</div>
        <el-menu-item index="/baselines">
          <el-icon><Memo /></el-icon>
          <span>基线审计</span>
        </el-menu-item>
        <el-menu-item index="/policy">
          <el-icon><SetUp /></el-icon>
          <span>防护策略</span>
        </el-menu-item>
        <el-menu-item index="/compliance">
          <el-icon><Stamp /></el-icon>
          <span>合规建议</span>
        </el-menu-item>
        <el-menu-item index="/upgrade">
          <el-icon><Upload /></el-icon>
          <span>远程升级</span>
        </el-menu-item>

        <div class="menu-section">系统</div>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <span>账户管理</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <!-- Main Content -->
    <div class="main-container">
      <header class="top-nav">
        <div class="nav-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item class="font-mono">WORKSPACE</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="nav-right">
          <div class="search-trigger font-mono" @click="openSearch">
            <span>Search...</span>
            <span class="kb-shortcut">⌘K</span>
          </div>

          <el-badge :value="5" class="notice-badge" is-dot>
            <el-icon><Bell /></el-icon>
          </el-badge>
          
          <div class="divider"></div>
          
          <el-dropdown @command="handleCommand">
            <div class="user-pill">
              <el-avatar :size="24" class="user-avatar">
                {{ username.charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="username-text">{{ username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu class="cyber-dropdown">
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="settings">设置</el-dropdown-item>
                <el-dropdown-item divided command="logout" class="danger-item">安全退出</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content-viewport">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { 
  Platform, Warning, User, Monitor, SetUp, 
  Stamp, Upload, Memo, Checked, Bell, ArrowDown 
} from '@element-plus/icons-vue'

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

function openSearch() {
  console.log('Open Command Palette')
}
</script>

<style scoped>
.app-wrapper {
  display: flex;
  height: 100vh;
  width: 100vw;
  background: var(--bg-color);
}

.sidebar-container {
  width: 240px;
  background: #020617;
  border-right: 1px solid var(--card-border);
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  font-size: 0.85rem;
  letter-spacing: 1.5px;
  color: #fff;
}

.logo-box {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #16a34a, #22c55e);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 10px rgba(34, 197, 94, 0.4);
}

.custom-menu {
  border-right: none !important;
  flex: 1;
  padding: 0 12px;
}

.menu-section {
  padding: 24px 8px 8px;
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 1px;
}

:deep(.el-menu-item) {
  height: 40px !important;
  line-height: 40px !important;
  margin: 2px 0 !important;
  border-radius: 6px !important;
  font-size: 0.85rem !important;
}

:deep(.el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.05) !important;
  color: #fff !important;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.05);
}

:deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.03) !important;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.top-nav {
  height: 60px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  border-bottom: 1px solid var(--card-border);
  background: rgba(2, 6, 23, 0.8);
  backdrop-filter: blur(8px);
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.search-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 180px;
  height: 32px;
  padding: 0 12px;
  background: rgba(30, 41, 59, 0.5);
  border: 1px solid var(--card-border);
  border-radius: 6px;
  color: var(--text-muted);
  font-size: 0.75rem;
  cursor: pointer;
}

.kb-shortcut {
  background: rgba(255, 255, 255, 0.1);
  padding: 1px 4px;
  border-radius: 4px;
  font-size: 0.65rem;
}

.notice-badge {
  cursor: pointer;
  color: var(--text-secondary);
}

.divider {
  width: 1px;
  height: 20px;
  background: var(--card-border);
}

.user-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.user-pill:hover { background: rgba(255, 255, 255, 0.05); }

.user-avatar {
  background: var(--primary-color) !important;
  font-weight: bold;
  font-size: 10px;
}

.username-text {
  font-size: 0.85rem;
  color: var(--text-primary);
}

.content-viewport {
  flex: 1;
  overflow-y: auto;
  background: radial-gradient(circle at 50% 10%, #0f172a, #020617);
  padding: 24px;
}

/* Breadcrumb Styles */
:deep(.el-breadcrumb__inner) { color: var(--text-muted) !important; font-size: 0.8rem; }
:deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) { color: var(--text-primary) !important; font-weight: 500 !important; }

/* Dropdown Styles */
:deep(.cyber-dropdown) {
  background: #0f172a !important;
  border: 1px solid var(--card-border) !important;
  padding: 4px !important;
}

:deep(.el-dropdown-menu__item) {
  color: var(--text-secondary) !important;
  font-size: 0.8rem !important;
  border-radius: 4px !important;
}

:deep(.el-dropdown-menu__item:hover) {
  background: rgba(255, 255, 255, 0.05) !important;
  color: #fff !important;
}

.danger-item:hover {
  background: rgba(239, 68, 68, 0.1) !important;
  color: #ef4444 !important;
}
</style>
