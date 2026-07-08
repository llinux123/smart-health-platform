<template>
  <div class="bind-email-page">
    <van-nav-bar :title="pageTitle" left-arrow @click-left="router.back()" fixed placeholder />

    <div class="content">
      <div class="page-header">
        <div class="header-icon email-icon">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <rect x="2" y="4" width="20" height="16" rx="2" />
            <path d="M22 4L12 13L2 4" />
          </svg>
        </div>
        <h2 class="header-title">{{ pageTitle }}</h2>
        <p class="page-desc">{{ pageDescription }}</p>
      </div>

      <van-form @submit="onSubmit">
        <div class="form-card email-form-card">
          <van-field
            v-model="email"
            label="邮箱"
            :placeholder="isChangeMode ? '请输入新邮箱地址' : '请输入邮箱地址'"
            type="email"
            :rules="emailRules"
            clearable
            :disabled="codeSent"
          />
          <van-field
            v-model="code"
            v-if="codeSent"
            label="验证码"
            placeholder="请输入邮箱验证码"
            type="digit"
            maxlength="6"
            :rules="codeRules"
            clearable
          >
            <template #button>
              <van-button
                size="small"
                type="primary"
                :disabled="sendingCode"
                @click="handleSendCode"
              >
                {{ sendingCode ? '发送中...' : sendBtnText }}
              </van-button>
            </template>
          </van-field>
        </div>

        <div class="submit-actions">
          <van-button
            v-if="!codeSent"
            block
            type="primary"
            round
            size="large"
            :loading="sendingCode"
            :loading-text="sendLoadingText"
            class="email-submit-btn"
            @click="handleSendCode"
          >
            发送验证码
          </van-button>
          <van-button
            v-else
            block
            type="primary"
            native-type="submit"
            :loading="submitting"
            :loading-text="submitLoadingText"
            round
            size="large"
            class="email-submit-btn"
          >
            {{ submitText }}
          </van-button>
        </div>
      </van-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import type { FieldRule } from 'vant/es/field/types'
import { useUserStore } from '@/stores/user'
import { sendEmailCode, bindEmail } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const hasEmail = computed(() => !!userStore.profile?.email)
const isChangeMode = computed(() => hasEmail.value)

const pageTitle = computed(() => isChangeMode.value ? '更换邮箱' : '绑定邮箱')
const pageDescription = computed(() =>
  isChangeMode.value
    ? '更换用于接收通知和找回账号的邮箱地址'
    : '绑定邮箱后可用于接收通知和找回账号'
)
const submitText = computed(() => isChangeMode.value ? '确认更换' : '确认绑定')
const submitLoadingText = computed(() => isChangeMode.value ? '更换中...' : '绑定中...')
const sendLoadingText = computed(() => isChangeMode.value ? '发送中...' : '发送中...')

const initialEmail = userStore.profile?.email ?? ''
const email = ref(isChangeMode.value ? initialEmail : '')
const code = ref('')
const submitting = ref(false)
const sendingCode = ref(false)
const codeSent = ref(false)
const countdown = ref(0)

const emailRules: FieldRule[] = [
  { required: true, message: '请输入邮箱地址' },
  { pattern: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/, message: '邮箱格式不正确' }
]

const codeRules: FieldRule[] = [
  { required: true, message: '请输入验证码' },
  { pattern: /^\d{6}$/, message: '验证码为6位数字' }
]

const sendBtnText = computed(() => countdown.value > 0 ? `${countdown.value}s` : '发送验证码')

let countdownTimer: ReturnType<typeof setInterval> | null = null

function startCountdown() {
  countdown.value = 60
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      if (countdownTimer) clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

async function handleSendCode() {
  if (!email.value || !/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email.value)) {
    showToast('请输入正确的邮箱地址')
    return
  }
  sendingCode.value = true
  try {
    await sendEmailCode(email.value)
    codeSent.value = true
    startCountdown()
    showToast('验证码已发送到邮箱')
  } catch (e) {
    console.warn('[BindEmailPage] 发送验证码失败', e)
    const message = e instanceof Error ? e.message : '发送失败，请稍后重试'
    showToast(message)
  } finally {
    sendingCode.value = false
  }
}

async function onSubmit() {
  submitting.value = true
  try {
    const data = await bindEmail(email.value, code.value)
    userStore.setProfile(data)
    showToast(isChangeMode.value ? '邮箱更换成功' : '邮箱绑定成功')
    router.back()
  } catch (e) {
    console.warn('[BindEmailPage] 邮箱操作失败', e)
    const message = e instanceof Error ? e.message : '操作失败，请稍后重试'
    showToast(message)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.bind-email-page {
  min-height: 100vh;
  background: var(--color-bg);
  padding-bottom: 40px;
}

.content {
  padding: 16px;
}

.page-header {
  text-align: center;
  margin-bottom: 24px;
  padding-top: 16px;
}

.header-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  margin-bottom: 12px;
}

.email-icon {
  background: linear-gradient(135deg, #DBEAFE 0%, #BFDBFE 100%);
  color: #2563EB;
}

.header-title {
  font-size: var(--font-size-title);
  font-weight: var(--font-weight-bold);
  color: var(--color-text);
  margin: 0 0 6px;
}

.page-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0;
  padding: 0 16px;
  line-height: var(--line-height-normal);
}

.email-form-card {
  background: var(--color-card);
  border: 1px solid #BFDBFE;
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin-bottom: 16px;
}

.submit-actions {
  margin-top: 24px;
  padding: 0 4px;
}

.email-submit-btn {
  height: 48px;
  font-size: 16px;
  font-weight: var(--font-weight-semibold);
}

:deep(.van-field) {
  padding: 12px 16px;
}

:deep(.van-field__label) {
  width: 80px;
  color: var(--color-text);
  font-size: 14px;
}
</style>
