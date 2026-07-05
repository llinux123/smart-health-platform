<template>
  <div class="forgot-page">
    <van-nav-bar
      title="忘记密码"
      left-arrow
      @click-left="$router.back()"
    />

    <div class="forgot-content">
      <!-- 步骤指示器 -->
      <div class="steps">
        <div class="step" :class="{ active: currentStep >= 1, done: currentStep > 1 }">
          <div class="step-circle">1</div>
          <span class="step-label">验证身份</span>
        </div>
        <div class="step-line" :class="{ active: currentStep > 1 }"></div>
        <div class="step" :class="{ active: currentStep >= 2 }">
          <div class="step-circle">2</div>
          <span class="step-label">设置新密码</span>
        </div>
      </div>

      <!-- 步骤 1：验证手机号 -->
      <van-form v-if="currentStep === 1" ref="formRef1" @submit="onVerifyPhone" class="forgot-form">
        <div class="form-card">
          <van-field
            v-model="form.phone"
            label="手机号"
            placeholder="请输入注册手机号"
            type="tel"
            maxlength="11"
            :rules="[{ required: true, message: '请输入手机号' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }]"
            clearable
          />
          <van-field
            v-model="form.verifyCode"
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
                :disabled="counting || !isPhoneValid"
                @click="sendVerifyCode"
              >
                {{ counting ? `${countdown}s` : '获取验证码' }}
              </button>
            </template>
          </van-field>
        </div>

        <div class="forgot-actions">
          <van-button block type="primary" native-type="submit" :loading="loading" round size="large">
            验证
          </van-button>
        </div>
      </van-form>

      <!-- 步骤 2：设置新密码 -->
      <van-form v-if="currentStep === 2" ref="formRef2" @submit="onResetPassword" class="forgot-form">
        <div class="form-card">
          <van-field
            v-model="form.newPassword"
            type="password"
            label="新密码"
            placeholder="6-20位，含字母和数字"
            :rules="[
              { required: true, message: '请输入新密码' },
              { pattern: /^(?=.*[a-zA-Z])(?=.*\d).{6,20}$/, message: '密码需包含字母和数字，6-20位' }
            ]"
            clearable
          />
          <van-field
            v-model="form.confirmPassword"
            type="password"
            label="确认密码"
            placeholder="请再次输入密码"
            :rules="[
              { required: true, message: '请确认密码' },
              { validator: (val: string) => val === form.newPassword, message: '两次密码不一致' }
            ]"
            clearable
          />
        </div>

        <div class="forgot-actions">
          <van-button block type="primary" native-type="submit" :loading="loading" round size="large">
            重置密码
          </van-button>
        </div>
      </van-form>

      <!-- 成功提示 -->
      <div v-if="currentStep === 3" class="forgot-success">
        <div class="success-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="var(--color-primary)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="16 10 11 15 8 12"/>
          </svg>
        </div>
        <h3 class="success-title">密码重置成功</h3>
        <p class="success-desc">请使用新密码重新登录</p>
        <van-button block type="primary" @click="$router.push('/login')" round size="large">
          返回登录
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onUnmounted } from 'vue'
import { showToast } from 'vant'
import { sendSmsCode, resetPassword } from '@/api/auth'

const currentStep = ref(1)

const form = reactive({
  phone: '',
  verifyCode: '',
  newPassword: '',
  confirmPassword: ''
})

const loading = ref(false)
const counting = ref(false)
const countdown = ref(60)
let timer: ReturnType<typeof setInterval> | null = null

const isPhoneValid = computed(() => /^1[3-9]\d{9}$/.test(form.phone))

/** 发送验证码 */
async function sendVerifyCode() {
  if (!isPhoneValid.value) {
    showToast('请输入正确的手机号')
    return
  }
  try {
    await sendSmsCode(form.phone)
    showToast('验证码已发送')
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

/** 步骤1：验证手机号 */
function onVerifyPhone() {
  if (!form.phone || !form.verifyCode) {
    showToast('请填写完整信息')
    return
  }
  // 验证码验证将在步骤2提交时一起校验
  currentStep.value = 2
}

/** 步骤2：重置密码 */
async function onResetPassword() {
  if (!form.newPassword || !form.confirmPassword) {
    showToast('请填写完整信息')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    showToast('两次密码不一致')
    return
  }

  loading.value = true
  try {
    await resetPassword({
      phone: form.phone,
      verifyCode: form.verifyCode,
      newPassword: form.newPassword
    })
    currentStep.value = 3
  } catch {
    // 如果是因为验证码错误，回到步骤1
    currentStep.value = 1
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.forgot-page {
  min-height: 100vh;
  background: var(--color-bg-gradient);
}

.forgot-content {
  animation: fade-in 0.4s ease;
  padding: 20px;
}

/* ============ Steps ============ */
.steps {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 0 28px;
  gap: 0;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.step-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-divider);
  color: #fff;
  font-size: 14px;
  font-weight: var(--font-weight-semibold);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background var(--transition-fast);
}

.step.active .step-circle,
.step.done .step-circle {
  background: var(--color-primary);
}

.step-label {
  font-size: 12px;
  color: var(--color-text-tertiary);
  transition: color var(--transition-fast);
}

.step.active .step-label,
.step.done .step-label {
  color: var(--color-primary);
  font-weight: var(--font-weight-medium);
}

.step-line {
  width: 60px;
  height: 2px;
  background: var(--color-divider);
  margin: 0 8px;
  margin-bottom: 20px;
  transition: background var(--transition-fast);
}

.step-line.active {
  background: var(--color-primary);
}

/* ============ Form ============ */
.forgot-form {
  margin-top: 8px;
}

.form-card {
  background: var(--color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  padding: 4px 0;
}

.forgot-form :deep(.van-field) {
  padding: 14px 16px;
}

.forgot-form :deep(.van-field__label) {
  width: 56px;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.sms-code-btn {
  padding: 6px 12px;
  border: 1px solid var(--color-primary);
  border-radius: 4px;
  background: transparent;
  color: var(--color-primary);
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
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

.forgot-actions {
  margin-top: 24px;
}

.forgot-actions :deep(.van-button--primary) {
  height: 48px;
  font-size: 16px;
  font-weight: var(--font-weight-semibold);
  box-shadow: var(--shadow-elevated);
}

/* ============ Success ============ */
.forgot-success {
  text-align: center;
  padding-top: 40px;
}

.success-icon {
  margin-bottom: 20px;
}

.success-title {
  font-size: 20px;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text);
  margin-bottom: 8px;
}

.success-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 32px;
}

.forgot-success :deep(.van-button--primary) {
  height: 48px;
  font-size: 16px;
  font-weight: var(--font-weight-semibold);
}
</style>
