<template>
  <div class="login-page">
    <div class="login-box bento-card">
      <div class="login-header">
        <div class="logo-circle">
          <el-icon size="24" color="#fff"><Checked /></el-icon>
        </div>
        <h2>XDR ELITE</h2>
        <p class="subtitle">安全管理运营中心</p>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleLogin" label-position="top">
        <el-form-item prop="username" label="账号登录">
          <el-input
            v-model="form.username"
            placeholder="用户名 / 身份标识"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="password" label="身份验证">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="安全口令 / 授权码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        
        <div class="form-actions">
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            class="login-btn"
          >
            安全登入系统
          </el-button>
        </div>
      </el-form>

      <div class="login-footer">
        <p>系统访问受限，仅限经授权的专业人员使用。</p>
        <p class="account-hint">演示账号: admin / admin123</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, type FormInstance } from 'element-plus'
import { User, Lock, Checked } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入身份标识', trigger: 'blur' }],
  password: [{ required: true, message: '需要进行身份验证', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(form.username, form.password)
    ElMessage({
      message: '认证成功，正在进入工作台',
      type: 'success',
      duration: 2000
    })
    router.push('/dashboard')
  } catch {
    ElMessage.error('身份验证失败：请检查凭据准确性')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: var(--bg-color);
  position: relative;
}

.login-box {
  width: 100%;
  max-width: 440px;
  padding: 48px;
  background: white;
  border-radius: 32px;
  box-shadow: var(--shadow-elite);
  cursor: default;
  transition: transform 0.4s ease;
}

.login-box:hover {
  transform: translateY(-2px);
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo-circle {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #4F46E5, #3B82F6);
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  box-shadow: 0 12px 24px rgba(79, 70, 229, 0.25);
}

h2 {
  font-size: 24px;
  margin: 0;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.subtitle {
  font-size: 13px;
  color: var(--text-light);
  margin-top: 4px;
  font-weight: 500;
}

:deep(.el-form-item__label) {
  font-weight: 700;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-muted);
  margin-bottom: 8px !important;
}

:deep(.el-input__wrapper) {
  background-color: #F1F5F9 !important;
  box-shadow: none !important;
  border-radius: 14px !important;
  padding: 4px 16px !important;
  border: 1px solid transparent !important;
  transition: all 0.2s;
}

:deep(.el-input__wrapper.is-focus) {
  background-color: white !important;
  border-color: var(--primary-color) !important;
  box-shadow: 0 0 0 4px var(--primary-glow) !important;
}

.form-actions {
  margin-top: 32px;
}

.login-btn {
  width: 100%;
}

.login-footer {
  text-align: center;
  margin-top: 32px;
  font-size: 12px;
  color: var(--text-light);
}

.account-hint {
  margin-top: 8px;
  color: var(--text-muted);
  font-family: monospace;
  background: #F8FAFC;
  padding: 4px 10px;
  border-radius: 8px;
  display: inline-block;
}
</style>
