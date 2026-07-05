<template>
  <div class="bind-identity-page">
    <van-nav-bar title="实名认证" left-arrow @click-left="router.back()" fixed placeholder />

    <div class="content">
      <p class="page-desc">请填写真实信息，用于后续诊疗服务</p>

      <van-form @submit="onSubmit">
        <div class="form-card">
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
          <van-field
            v-model="form.email"
            label="邮箱"
            placeholder="请输入邮箱（选填）"
            type="email"
            clearable
          />
        </div>

        <div class="section-title">身份证照片</div>
        <div class="upload-card">
          <div class="upload-item">
            <span class="upload-label">身份证正面</span>
            <van-uploader v-model="frontFileList" :after-read="onFrontRead" :max-count="1" />
          </div>
          <div class="upload-item">
            <span class="upload-label">身份证反面</span>
            <van-uploader v-model="backFileList" :after-read="onBackRead" :max-count="1" />
          </div>
        </div>

        <div class="section-title">人脸识别</div>
        <div class="form-card face-card">
          <div class="face-row">
            <span class="face-label">人脸识别结果</span>
            <van-uploader v-model="faceFileList" :after-read="onFaceRead" :max-count="1" />
          </div>
        </div>

        <div v-if="isDev" class="test-skip">
          <van-checkbox v-model="form.skipVerification">测试环境跳过上传和人脸识别</van-checkbox>
        </div>

        <div class="submit-actions">
          <van-button block type="primary" native-type="submit" :loading="submitting" loading-text="提交中..." round size="large">
            提交审核
          </van-button>
        </div>
      </van-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { bindIdentity } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const isDev = computed(() => import.meta.env.DEV)

const form = reactive({
  realName: '',
  idCard: '',
  gender: 1,
  email: '',
  idCardFrontUrl: '',
  idCardBackUrl: '',
  faceRecognitionUrl: '',
  skipVerification: false
})

const frontFileList = ref([])
const backFileList = ref([])
const faceFileList = ref([])
const submitting = ref(false)

const idCardRules = [
  { required: true, message: '请输入身份证号' },
  { pattern: /^\d{17}[\dXx]$/, message: '身份证号格式不正确' }
]

function onFrontRead(file: any) {
  form.idCardFrontUrl = URL.createObjectURL(file.file)
}

function onBackRead(file: any) {
  form.idCardBackUrl = URL.createObjectURL(file.file)
}

function onFaceRead(file: any) {
  form.faceRecognitionUrl = URL.createObjectURL(file.file)
}

async function onSubmit() {
  submitting.value = true
  try {
    const data = await bindIdentity({
      realName: form.realName,
      idCard: form.idCard,
      gender: form.gender,
      email: form.email || undefined,
      idCardFrontUrl: form.idCardFrontUrl || undefined,
      idCardBackUrl: form.idCardBackUrl || undefined,
      faceRecognitionUrl: form.faceRecognitionUrl || undefined,
      skipVerification: form.skipVerification
    })
    userStore.setProfile(data)
    showToast('实名认证提交成功')
    router.back()
  } catch {
    // 错误已在拦截器处理
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.bind-identity-page {
  min-height: 100vh;
  background: var(--color-bg);
  padding-bottom: 40px;
}

.content {
  padding: 16px;
}

.page-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 16px;
  padding: 0 4px;
}

.form-card {
  background: var(--color-card);
  border: 1px solid var(--color-card-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin-bottom: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text);
  margin: 20px 0 12px;
  padding: 0 4px;
}

.upload-card {
  background: var(--color-card);
  border: 1px solid var(--color-card-border);
  border-radius: var(--radius-lg);
  padding: 16px;
  margin-bottom: 16px;
}

.upload-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.upload-item:last-child {
  margin-bottom: 0;
}

.upload-label {
  font-size: 14px;
  color: var(--color-text);
}

.face-card {
  padding: 16px;
}

.face-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.face-label {
  font-size: 14px;
  color: var(--color-text);
}

.test-skip {
  margin: 16px 4px;
  padding: 12px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: var(--radius-lg);
}

.submit-actions {
  margin-top: 24px;
  padding: 0 4px;
}

.submit-actions :deep(.van-button--primary) {
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
