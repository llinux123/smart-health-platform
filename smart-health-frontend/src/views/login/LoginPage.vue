<template>
  <div class="login-page">
    <div class="login-header">
      <div class="login-decoration">
        <svg class="login-growth-line" viewBox="0 0 360 24" fill="none" preserveAspectRatio="none">
          <path d="M0,16 C36,16 54,4 90,4 C126,4 144,18 180,18 C216,18 234,6 270,6 C306,6 324,14 360,14"
                stroke="var(--color-primary)" stroke-width="1.4" stroke-linecap="round"/>
        </svg>
      </div>
      <div class="login-brand">
        <div class="login-logo">
          <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
            <rect x="4" y="10" width="8" height="20" rx="2" fill="var(--color-primary)" opacity="0.7"/>
            <rect x="16" y="4" width="8" height="32" rx="2" fill="var(--color-primary)"/>
            <rect x="28" y="14" width="8" height="12" rx="2" fill="var(--color-primary)" opacity="0.7"/>
          </svg>
        </div>
        <h1 class="login-title">智慧健康</h1>
        <p class="login-subtitle">您的智能健康管理伙伴</p>
      </div>
    </div>

    <div class="login-body">
      <div class="login-card">
        <van-form @submit="onSubmit" class="login-form">
          <van-field
            v-model="form.username"
            label="用户名"
            placeholder="请输入用户名"
            :rules="[{ required: true, message: '请输入用户名' }]"
            clearable
          />
          <van-field
            v-model="form.password"
            type="password"
            label="密码"
            placeholder="请输入密码"
            :rules="[{ required: true, message: '请输入密码' }]"
            clearable
          />

          <div class="login-options">
            <van-checkbox v-model="rememberMe" shape="round" icon-size="16px">
              <span class="checkbox-label">记住我</span>
            </van-checkbox>
          </div>

          <div class="login-actions">
            <van-button
              block
              type="primary"
              native-type="submit"
              :loading="loading"
              loading-text="登录中..."
              round
              size="large"
            >
              登录
            </van-button>
            <p class="login-register-link">
              还没有账号？
              <router-link to="/register">立即注册</router-link>
            </p>
          </div>
        </van-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { setRememberMe } from '@/utils/storage'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const form = reactive({
  username: '',
  password: ''
})
const loading = ref(false)
const rememberMe = ref(false)

async function onSubmit() {
  if (!form.username || !form.password) {
    showToast('请填写用户名和密码')
    return
  }

  loading.value = true
  try {
    const data = await login(form)
    setRememberMe(rememberMe.value)
    userStore.setLoginInfo(data)
    showToast('登录成功')
    const redirect = (Array.isArray(route.query.redirect) ? route.query.redirect[0] : route.query.redirect) || '/home'
    // 恢复原始路由的 query 参数
    let targetQuery: Record<string, string> = {}
    const rqRaw = route.query._rq
    const rqStr = Array.isArray(rqRaw) ? rqRaw[0] : rqRaw
    if (rqStr) {
      try {
        targetQuery = JSON.parse(rqStr)
      } catch {
        // 解析失败则忽略，仅跳转基础路径
      }
    }
    router.push({ path: redirect, query: targetQuery })
  } catch (err) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
}

/* ============ Header ============ */
.login-header {
  background: var(--color-primary-light);
  padding: 0 0 32px;
  position: relative;
  overflow: hidden;
}

.login-decoration {
  height: 24px;
  display: flex;
  align-items: flex-end;
  padding: 0 24px;
  margin-bottom: 8px;
}

.login-growth-line {
  width: 100%;
  height: 24px;
  color: var(--color-primary);
  stroke-dasharray: 420;
  stroke-dashoffset: 420;
  animation: draw-growth 2s ease-out 0.1s forwards;
  opacity: 0.5;
}

.login-brand {
  text-align: center;
  padding: 0 24px;
}

.login-logo {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.login-logo svg {
  animation: float 3s ease-in-out infinite;
}

.login-title {
  font-size: 28px;
  font-weight: var(--font-weight-bold);
  color: var(--color-primary-dark);
  margin-bottom: 6px;
  letter-spacing: var(--letter-spacing-wide);
}

.login-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
}

/* ============ Card ============ */
.login-body {
  flex: 1;
  padding: 0 20px;
  margin-top: -20px;
  position: relative;
  z-index: 1;
}

.login-card {
  background: var(--color-card);
  border: 1px solid var(--color-card-border);
  border-radius: var(--radius-xl);
  padding: 8px 20px 24px;
  box-shadow: var(--shadow-lg);
}

.login-form :deep(.van-field) {
  padding: 14px 0;
}

.login-form :deep(.van-field__label) {
  width: 56px;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.login-options {
  padding: 4px 0 0;
}

.checkbox-label {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.login-actions {
  padding-top: 20px;
}

.login-actions :deep(.van-button--primary) {
  height: 48px;
  font-size: 16px;
  font-weight: var(--font-weight-semibold);
  box-shadow: var(--shadow-warm);
}

.login-register-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.login-register-link a {
  color: var(--color-primary);
  font-weight: var(--font-weight-medium);
}
</style>
