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
        <!-- 角色切换 Tab -->
        <div class="login-role-tabs">
          <button
            :class="['login-role-tab', { active: loginType === 'PATIENT' }]"
            @click="switchRole('PATIENT')"
          >患者登录</button>
          <button
            :class="['login-role-tab', { active: loginType === 'STAFF' }]"
            @click="switchRole('STAFF')"
          >员工登录</button>
        </div>

        <!-- ============ 患者登录 ============ -->
        <template v-if="loginType === 'PATIENT'">
          <!-- B站风格分段切换：短信登录 / 密码登录 -->
          <div class="login-mode-switch">
            <button
              :class="['switch-btn', { active: patientMode === 'sms' }]"
              @click="patientMode = 'sms'"
            >短信登录</button>
            <button
              :class="['switch-btn', { active: patientMode === 'password' }]"
              @click="patientMode = 'password'"
            >密码登录</button>
          </div>

          <!-- 短信登录表单 -->
          <van-form v-if="patientMode === 'sms'" @submit="onSmsLogin" class="login-form">
            <van-field
              v-model="smsForm.phone"
              label="手机号"
              placeholder="请输入手机号"
              type="tel"
              maxlength="11"
              :rules="[{ required: true, message: '请输入手机号' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }]"
              clearable
            />
            <van-field
              v-model="smsForm.code"
              label="验证码"
              placeholder="请输入验证码"
              maxlength="6"
              :rules="[{ required: true, message: '请输入验证码' }]"
            >
              <template #button>
                <button
                  type="button"
                  class="sms-code-btn"
                  :class="{ 'sms-code-btn--counting': counting }"
                  :disabled="counting || !smsForm.phone || !/^1[3-9]\d{9}$/.test(smsForm.phone)"
                  @click="sendCode"
                >
                  {{ counting ? `${countdown}s` : '获取验证码' }}
                </button>
              </template>
            </van-field>

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
            </div>
          </van-form>

          <!-- 密码登录表单 -->
          <van-form v-if="patientMode === 'password'" @submit="onPasswordLogin" class="login-form">
            <van-field
              v-model="pwdForm.username"
              label="用户名"
              placeholder="请输入用户名"
              :rules="[{ required: true, message: '请输入用户名' }]"
              clearable
            />
            <van-field
              v-model="pwdForm.password"
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
              <router-link to="/forgot-password" class="forgot-link">忘记密码？</router-link>
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
            </div>
          </van-form>

          <!-- 第三方登录入口 -->
          <div class="third-party-login">
            <div class="third-party-divider">
              <span class="third-party-divider-line"></span>
              <span class="third-party-divider-text">其他登录方式</span>
              <span class="third-party-divider-line"></span>
            </div>
            <div class="third-party-icons">
              <div class="third-party-icon" @click="showComingSoon">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
                  <rect width="24" height="24" rx="6" fill="#07C160"/>
                  <path d="M7.5 10.5C9 9 12 7 15 8s5 5 3.5 8-7 4-10 1S5.5 12.5 7.5 10.5Z" stroke="#fff" stroke-width="1.5" fill="none"/>
                  <path d="M10 13.5c0 .8.7 1.5 1.5 1.5s1.5-.7 1.5-1.5" stroke="#fff" stroke-width="1.2" fill="none"/>
                </svg>
                <span class="third-party-label">微信</span>
              </div>
              <div class="third-party-icon" @click="showComingSoon">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
                  <rect width="24" height="24" rx="6" fill="#1677FF"/>
                  <text x="12" y="16.5" text-anchor="middle" fill="#fff" font-size="14" font-weight="bold">支</text>
                </svg>
                <span class="third-party-label">支付宝</span>
              </div>
            </div>
          </div>
        </template>

        <!-- ============ 员工登录 ============ -->
        <template v-else>
          <van-form @submit="onPasswordLogin" class="login-form">
            <van-field
              v-model="pwdForm.username"
              label="用户名"
              placeholder="请输入用户名"
              :rules="[{ required: true, message: '请输入用户名' }]"
              clearable
            />
            <van-field
              v-model="pwdForm.password"
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
            </div>
          </van-form>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import { login, smsLogin, sendSmsCode } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { setRememberMe } from '@/utils/storage'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

/** 角色类型 */
const loginType = ref<'PATIENT' | 'STAFF'>('PATIENT')
/** 患者登录模式 */
const patientMode = ref<'sms' | 'password'>('sms')

/** 短信登录表单 */
const smsForm = reactive({ phone: '', code: '' })
/** 密码登录表单 */
const pwdForm = reactive({ username: '', password: '' })

const loading = ref(false)
const rememberMe = ref(false)

/** 倒计时 */
const counting = ref(false)
const countdown = ref(60)
let timer: ReturnType<typeof setInterval> | null = null

function switchRole(role: 'PATIENT' | 'STAFF') {
  loginType.value = role
  smsForm.phone = ''
  smsForm.code = ''
  pwdForm.username = ''
  pwdForm.password = ''
}

/** 发送短信验证码 */
async function sendCode() {
  if (!/^1[3-9]\d{9}$/.test(smsForm.phone)) {
    showToast('请输入正确的手机号')
    return
  }
  try {
    await sendSmsCode(smsForm.phone)
    showToast('验证码已发送')
    // 开始倒计时
    counting.value = true
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer!)
        counting.value = false
      }
    }, 1000)
  } catch {
    // 错误已在拦截器中处理
  }
}

/** 短信验证码登录 */
async function onSmsLogin() {
  if (!smsForm.phone || !smsForm.code) {
    showToast('请填写手机号和验证码')
    return
  }

  loading.value = true
  try {
    const data = (await smsLogin(smsForm.phone, smsForm.code)) as any
    setRememberMe(false)
    userStore.setLoginInfo(data)

    if (data.isNewUser && data.randomPassword) {
      showToast(`注册成功！初始密码：${data.randomPassword}`)
    } else {
      showToast('登录成功')
    }

    redirectToHome()
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

/** 密码登录（患者/员工通用） */
async function onPasswordLogin() {
  if (!pwdForm.username || !pwdForm.password) {
    showToast('请填写用户名和密码')
    return
  }

  loading.value = true
  try {
    const loginData = (await login({
      username: pwdForm.username,
      password: pwdForm.password,
      loginType: loginType.value
    })) as any
    setRememberMe(rememberMe.value)
    userStore.setLoginInfo(loginData)
    showToast('登录成功')
    redirectToHome()
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

function redirectToHome() {
  const rawRedirect = Array.isArray(route.query.redirect) ? route.query.redirect[0] : route.query.redirect
  const redirect = (rawRedirect && rawRedirect.startsWith('/') && !rawRedirect.startsWith('//')) ? rawRedirect : '/home'
  let targetQuery: Record<string, string> = {}
  const rqRaw = route.query._rq
  const rqStr = Array.isArray(rqRaw) ? rqRaw[0] : rqRaw
  if (rqStr) {
    try {
      targetQuery = JSON.parse(rqStr)
    } catch { /* ignore */ }
  }
  router.push({ path: redirect, query: targetQuery })
}

function showComingSoon() {
  showToast('功能即将上线')
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
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

/* ============ Body ============ */
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
  padding: 0 20px 24px;
  box-shadow: var(--shadow-lg);
}

/* ============ Role Tabs ============ */
.login-role-tabs {
  display: flex;
  padding: 16px 0 0;
  gap: 0;
}

.login-role-tab {
  flex: 1;
  padding: 10px 0;
  border: none;
  background: transparent;
  font-size: 15px;
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  cursor: pointer;
  position: relative;
  transition: color var(--transition-fast);
}

.login-role-tab.active {
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
}

.login-role-tab.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 32px;
  height: 3px;
  background: var(--color-primary);
  border-radius: 2px;
}

/* ============ Mode Switch (B站风格) ============ */
.login-mode-switch {
  display: flex;
  justify-content: center;
  gap: 0;
  padding: 20px 0 12px;
}

.switch-btn {
  flex: 1;
  max-width: 160px;
  padding: 8px 20px;
  border: 1px solid var(--color-primary);
  background: transparent;
  font-size: 13px;
  font-weight: var(--font-weight-medium);
  color: var(--color-primary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.switch-btn:first-child {
  border-radius: 6px 0 0 6px;
  border-right: none;
}

.switch-btn:last-child {
  border-radius: 0 6px 6px 0;
}

.switch-btn.active {
  background: var(--color-primary);
  color: #fff;
}

/* ============ Forms ============ */
.login-form :deep(.van-field) {
  padding: 14px 0;
}

.login-form :deep(.van-field__label) {
  width: 56px;
  color: var(--color-text-secondary);
  font-size: 14px;
}

/* SMS Code Button */
.sms-code-btn {
  padding: 6px 12px;
  border: 1px solid var(--color-primary);
  border-radius: 4px;
  background: transparent;
  color: var(--color-primary);
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: all var(--transition-fast);
}

.sms-code-btn:disabled {
  color: var(--color-text-tertiary);
  border-color: var(--color-text-tertiary);
  cursor: not-allowed;
}

.sms-code-btn--counting {
  color: var(--color-text-tertiary);
  border-color: var(--color-divider);
}

/* Options */
.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0 0;
}

.checkbox-label {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.forgot-link {
  font-size: 13px;
  color: var(--color-primary);
  text-decoration: none;
}

/* Actions */
.login-actions {
  padding-top: 20px;
}

.login-actions :deep(.van-button--primary) {
  height: 48px;
  font-size: 16px;
  font-weight: var(--font-weight-semibold);
  box-shadow: var(--shadow-warm);
}

/* ============ Third-Party Login ============ */
.third-party-login {
  margin-top: 28px;
}

.third-party-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.third-party-divider-line {
  flex: 1;
  height: 1px;
  background: var(--color-divider);
}

.third-party-divider-text {
  font-size: 12px;
  color: var(--color-text-tertiary);
  white-space: nowrap;
}

.third-party-icons {
  display: flex;
  justify-content: center;
  gap: 40px;
}

.third-party-icon {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: transform var(--transition-fast);
}

.third-party-icon:active {
  transform: scale(0.9);
}

.third-party-label {
  font-size: 11px;
  color: var(--color-text-secondary);
}

/* ============ Keyframes ============ */
@keyframes draw-growth {
  to { stroke-dashoffset: 0; }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}
</style>
