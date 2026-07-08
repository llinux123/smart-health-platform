<template>
  <div class="bind-identity-step1">
    <van-nav-bar title="实名认证" left-arrow @click-left="router.back()" fixed placeholder />

    <div class="content">
      <!-- 身份认证图标头部 -->
      <div class="page-header">
        <div class="header-icon identity-icon">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            <path d="M9 12l2 2 4-4" />
          </svg>
        </div>
        <h2 class="header-title">实名认证</h2>
        <p class="page-desc">请填写真实信息，用于后续诊疗服务（步骤 1/2）</p>
      </div>

      <van-form @submit="onNext">
        <div class="form-card identity-form-card">
          <van-field
            v-model="form.realName"
            label="真实姓名"
            placeholder="请输入真实姓名"
            :rules="[{ required: true, message: '请输入真实姓名' }]"
            clearable
          />
          <van-field
            v-model="form.idCard"
            label="身份证号"
            placeholder="请输入身份证号"
            maxlength="18"
            :rules="idCardRules"
            clearable
          />
          <van-field name="gender" label="性别">
            <template #input>
              <van-radio-group v-model="form.gender" direction="horizontal">
                <van-radio :name="1">男</van-radio>
                <van-radio :name="2">女</van-radio>
              </van-radio-group>
            </template>
          </van-field>
        </div>

        <!-- 隐私协议勾选（刚性合规要求） -->
        <div class="consent-section">
          <van-checkbox v-model="form.consentAccepted" shape="square">
            我已阅读并同意
            <span class="consent-link" @click.stop.prevent="showProtocol = true">
              《个人信息收集与隐私保护协议》
            </span>
          </van-checkbox>
        </div>

        <div class="submit-actions">
          <van-button block type="primary" native-type="submit"
                      :disabled="!form.consentAccepted"
                      :loading="loading" loading-text="跳转中..."
                      round size="large" class="identity-submit-btn">
            下一步
          </van-button>
        </div>
      </van-form>
    </div>

    <!-- 隐私协议弹窗 -->
    <van-dialog v-model:show="showProtocol" title="个人信息收集与隐私保护协议"
                :show-confirm-button="true" confirm-button-text="我已了解">
      <div class="protocol-content">
        <p>根据《个人信息保护法》，采集您的生物识别信息（人脸）和身份证件属于敏感个人信息处理行为。</p>
        <p>1. 采集目的：实名认证，用于诊疗服务身份核验</p>
        <p>2. 采集内容：姓名、身份证号、人脸图像、身份证照片</p>
        <p>3. 存储方式：加密存储于医疗平台服务器</p>
        <p>4. 您有权随时撤回授权并要求删除相关信息</p>
      </div>
    </van-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useIdentityVerificationStore } from '@/stores/identityVerification'

const router = useRouter()
const store = useIdentityVerificationStore()

const loading = ref(false)
const showProtocol = ref(false)

const form = reactive({
  realName: '',
  idCard: '',
  gender: 1,
  consentAccepted: false
})

const idCardRules = [
  { required: true, message: '请输入身份证号' },
  { pattern: /^\d{17}[\dXx]$/, message: '身份证号格式不正确' }
]

function onNext() {
  if (!form.consentAccepted) {
    showToast('请先阅读并同意隐私保护协议')
    return
  }
  loading.value = true
  // 暂存到 Pinia Store（仅内存，不持久化）
  store.setStep1Data({
    realName: form.realName,
    idCard: form.idCard,
    gender: form.gender,
    consentAccepted: form.consentAccepted
  })
  // 跳转页面2（不在 URL 中暴露任何个人信息）
  router.push('/account/bind-identity/verify')
  loading.value = false
}
</script>

<style scoped>
.bind-identity-step1 {
  min-height: 100vh;
  background: var(--color-bg);
  padding-bottom: 40px;
}

.content {
  padding: 16px;
}

/* 身份认证页面专属头部 */
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

.identity-icon {
  background: linear-gradient(135deg, #FEF3C7 0%, #FDE68A 100%);
  color: #D97706;
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

/* 表单卡片 */
.identity-form-card {
  background: var(--color-card);
  border: 1px solid #FDE68A;
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin-bottom: 16px;
}

/* 隐私协议勾选区 */
.consent-section {
  margin: 16px 4px;
  padding: 0 4px;
}

.consent-section :deep(.van-checkbox__label) {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: var(--line-height-normal);
}

.consent-link {
  color: var(--color-primary);
  text-decoration: underline;
}

.protocol-content {
  padding: 16px;
  max-height: 300px;
  overflow-y: auto;
}

.protocol-content p {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: var(--line-height-relaxed);
  margin: 0 0 8px;
}

.submit-actions {
  margin-top: 24px;
  padding: 0 4px;
}

.identity-submit-btn {
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
