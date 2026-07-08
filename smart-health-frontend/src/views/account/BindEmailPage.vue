<template>
  <div class="bind-email-page">
    <van-nav-bar :title="pageTitle" left-arrow @click-left="router.back()" fixed placeholder />

    <div class="content">
      <!-- 邮箱图标头部 -->
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
          />
        </div>

        <div class="submit-actions">
          <van-button block type="primary" native-type="submit" :loading="submitting" :loading-text="submitLoadingText" round size="large" class="email-submit-btn">
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
import { bindEmail } from '@/api/auth'

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

const initialEmail = userStore.profile?.email ?? ''
const email = ref(isChangeMode.value ? initialEmail : '')
const submitting = ref(false)

const emailRules: FieldRule[] = [
  { required: true, message: '请输入邮箱地址' },
  { pattern: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/, message: '邮箱格式不正确' }
]

async function onSubmit() {
  submitting.value = true
  try {
    const data = await bindEmail(email.value)
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

/* 邮箱页面专属头部 */
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

/* 邮箱专属表单卡片 */
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
