<template>
  <div class="app-wrapper animate-fade-in">
    <!-- Sidebar (Fintech Style) -->
    <aside class="sidebar-container">
      <div class="logo">
        <div class="logo-box">
          <el-icon size="20" color="#fff"><Checked /></el-icon>
        </div>
        <span class="logo-text">XDR ELITE</span>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        router
        class="custom-menu"
        text-color="#64748b"
        active-text-color="#4F46E5"
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
            <el-breadcrumb-item>工作台</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="nav-right">
          <div class="search-trigger" @click="openSearch">
            <el-icon><Search /></el-icon>
            <span>搜索功能与命令...</span>
            <div class="kb-shortcut">⌘K</div>
          </div>

          <el-badge :value="5" class="notice-badge" is-dot>
            <el-icon><Bell /></el-icon>
          </el-badge>
          
          <div class="divider"></div>
          
          <el-dropdown @command="handleCommand">
            <div class="user-pill">
              <el-avatar :size="32" class="user-avatar">
                {{ username.charAt(0).toUpperCase() }}
              </el-avatar>
              <div class="user-info-text">
                <span class="username-text">{{ username }}</span>
                <span class="user-role">系统管理员</span>
              </div>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu class="fintech-dropdown">
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="settings">系统设置</el-dropdown-item>
                <el-dropdown-item divided command="logout" class="danger-item">安全退出</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content-viewport">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
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
  Platform, Warning, User, Monitor, SetUp, Search,
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
  width: 200px;
  background: #F8FAFC;
  border-right: 1px solid rgba(226, 232, 240, 0.6);
  display: flex;
  flex-direction: column;
  z-index: 100;
  box-shadow: inset -10px 0 20px -10px rgba(0,0,0,0.02);
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
}

.logo-box {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #4F46E5, #3B82F6);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 16px rgba(79, 70, 229, 0.2);
}

.logo-text {
  font-size: 16px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.custom-menu {
  border-right: none !important;
  flex: 1;
  padding: 0 16px;
}

.menu-section {
  padding: 24px 12px 12px;
  font-size: 11px;
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

:deep(.el-menu-item) {
  height: 44px !important;
  line-height: 44px !important;
  margin: 4px 0 !important;
  border-radius: 12px !important;
  font-size: 13px !important;
  font-weight: 500 !important;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1) !important;
  position: relative;
  overflow: hidden;
}

:deep(.el-menu-item.is-active) {
  background: white !important;
  color: #4F46E5 !important;
  font-weight: 800 !important;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.12), 0 0 1px rgba(79, 70, 229, 0.2) !important;
}

:deep(.el-menu-item.is-active)::after {
  content: "";
  position: absolute;
  left: 0;
  top: 25%;
  height: 50%;
  width: 3px;
  background: #4F46E5;
  border-radius: 4px;
}

:deep(.el-menu-item:hover:not(.is-active)) {
  background: #F8FAFC !important;
  transform: translateX(4px);
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  position: relative;
}

.top-nav {
  height: 64px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(226, 232, 240, 0.4);
  z-index: 90;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.search-trigger {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 240px;
  height: 40px;
  padding: 0 16px;
  background: #F1F5F9;
  border-radius: 12px;
  color: var(--text-muted);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.search-trigger:hover {
  background: #E2E8F0;
}

.kb-shortcut {
  margin-left: auto;
  background: white;
  padding: 2px 6px;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 700;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.notice-badge {
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 20px;
}

.divider {
  width: 1px;
  height: 24px;
  background: #E2E8F0;
}

.user-pill {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 12px;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.user-pill:hover { background: #F8FAFC; }

.user-avatar {
  background: linear-gradient(135deg, #4F46E5, #3B82F6) !important;
  box-shadow: 0 4px 8px rgba(79, 70, 229, 0.2);
}

.user-info-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.username-text {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.user-role {
  font-size: 11px;
  color: var(--text-muted);
}

.content-viewport {
  flex: 1;
  overflow-y: auto;
  background: var(--bg-color);
}

/* Breadcrumb Styles */
:deep(.el-breadcrumb__inner) { 
  color: var(--text-muted) !important; 
  font-size: 13px !important;
  font-weight: 500 !important;
}
:deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) { 
  color: var(--text-primary) !important; 
  font-weight: 700 !important; 
}

/* Dropdown Styles */
:deep(.fintech-dropdown) {
  border-radius: 16px !important;
  padding: 8px !important;
  box-shadow: 0 20px 40px -10px rgba(0,0,0,0.1) !important;
  border: 1px solid rgba(226, 232, 240, 0.6) !important;
}

:deep(.el-dropdown-menu__item) {
  border-radius: 8px !important;
  padding: 10px 16px !important;
  font-size: 13px !important;
  font-weight: 500 !important;
}

:deep(.el-dropdown-menu__item:hover) {
  background: #F1F5F9 !important;
  color: var(--text-primary) !important;
}

.danger-item:hover {
  background: #FEF2F2 !important;
  color: #EF4444 !important;
}

/* Page Transitions */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.animate-fade-in {
  animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
