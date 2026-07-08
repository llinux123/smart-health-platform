<template>
  <div class="bind-identity-step2">
    <van-nav-bar title="身份核验" left-arrow @click-left="onBack" fixed placeholder />

    <!-- 桌面端拦截：展示二维码引导手机访问 -->
    <div v-if="isDesktop" class="desktop-block">
      <div class="qr-container">
        <img :src="qrCodeUrl" alt="二维码" class="qr-img" />
        <p class="qr-tip">请使用手机微信扫码访问<br />以完成身份认证</p>
      </div>
    </div>

    <!-- 移动端正常流程 -->
    <div v-else class="content">
      <div class="page-header">
        <h2 class="header-title">身份核验（步骤 2/2）</h2>
        <p class="page-desc">{{ step1Summary }}，请上传身份证照片完成核验</p>
      </div>

      <!-- 身份证照片上传 -->
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

      <!-- 提示信息 -->
      <div class="tips-card">
        <van-icon name="info-o" size="16" color="var(--color-text-secondary)" />
        <span class="tips-text">请确保照片清晰、完整、无遮挡，提交后将由人工审核。</span>
      </div>

      <!-- Dev 环境跳过 -->
      <div v-if="isDev" class="test-skip">
        <van-checkbox v-model="skipVerification">测试环境跳过上传直接提交</van-checkbox>
      </div>

      <!-- 提交按钮 -->
      <div class="submit-actions">
        <van-button block type="primary" @click="onSubmit"
                    :disabled="!canSubmit" :loading="submitting"
                    loading-text="提交中..." round size="large" class="identity-submit-btn">
          提交审核
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showDialog } from 'vant'
import type { UploaderFileListItem } from 'vant'
import { useIdentityVerificationStore } from '@/stores/identityVerification'
import { useImageCompress } from '@/composables/useImageCompress'

const router = useRouter()
const store = useIdentityVerificationStore()
const { fileToDataUrl, compressImage, ensureBase64Limit } = useImageCompress()

const isDev = computed(() => import.meta.env.DEV)

// —— 桌面端检测 ——
const isDesktop = computed(() => {
  return !/Mobile|Android|iPhone/i.test(navigator.userAgent)
})
const qrCodeUrl = computed(() => {
  const url = window.location.href
  return `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(url)}`
})

// —— Step1 数据回显（脱敏显示）——
const step1Data = computed(() => store.getStep1Data())
const step1Summary = computed(() => {
  if (!step1Data.value) return '信息缺失'
  return `${step1Data.value.realName}（${maskIdCard(step1Data.value.idCard)}）`
})
function maskIdCard(idCard: string): string {
  if (idCard.length < 8) return idCard
  return idCard.slice(0, 4) + '****' + idCard.slice(-4)
}

// —— 身份证上传 ——
const frontFileList = ref<UploaderFileListItem[]>([])
const backFileList = ref<UploaderFileListItem[]>([])

// 监听文件列表变化，删除图片时同步清空对应 URL
watch(frontFileList, (list) => {
  if (!list.length) {
    store.setIdCardResult('', store.step2Data.idCardBackUrl)
  }
})
watch(backFileList, (list) => {
  if (!list.length) {
    store.setIdCardResult(store.step2Data.idCardFrontUrl, '')
  }
})

const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp']
const MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB

/** 校验文件类型和大小 */
function validateFile(file: File): boolean {
  if (!ALLOWED_TYPES.includes(file.type)) {
    showToast('请上传图片文件')
    return false
  }
  if (file.size > MAX_FILE_SIZE) {
    showToast('图片大小不能超过 5MB')
    return false
  }
  return true
}

/** 处理身份证正面上传 */
async function onFrontRead(items: UploaderFileListItem | UploaderFileListItem[]) {
  const file = Array.isArray(items) ? items[0] : items
  if (!file?.file || !validateFile(file.file)) return

  try {
    let dataUrl = await fileToDataUrl(file.file)
    dataUrl = await compressImage(dataUrl, 800, 0.8)
    dataUrl = await ensureBase64Limit(dataUrl)
    store.setIdCardResult(dataUrl, store.step2Data.idCardBackUrl)
  } catch {
    showToast('图片读取失败')
  }
}

/** 处理身份证反面上传 */
async function onBackRead(items: UploaderFileListItem | UploaderFileListItem[]) {
  const file = Array.isArray(items) ? items[0] : items
  if (!file?.file || !validateFile(file.file)) return

  try {
    let dataUrl = await fileToDataUrl(file.file)
    dataUrl = await compressImage(dataUrl, 800, 0.8)
    dataUrl = await ensureBase64Limit(dataUrl)
    store.setIdCardResult(store.step2Data.idCardFrontUrl, dataUrl)
  } catch {
    showToast('图片读取失败')
  }
}

// —— 提交 ——
const skipVerification = ref(false)
const submitting = ref(false)
const canSubmit = computed(() => {
  if (isDev.value && skipVerification.value) return true
  return store.canSubmit
})

async function onSubmit() {
  submitting.value = true
  try {
    await store.submitIdentity(isDev.value && skipVerification.value)
    showToast('实名认证提交成功')
    router.replace('/my')
  } catch (e: any) {
    showToast(e.message || '提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

/** 返回上一步，确认后清理数据 */
function onBack() {
  showDialog({
    title: '确认返回',
    message: '返回上一步将丢失当前上传的照片，是否继续？',
    showCancelButton: true
  }).then(() => {
    store.clearAll()
    router.back()
  }).catch(() => {})
}

// —— 空状态保护：若 Step1 数据缺失则回退 ——
onMounted(() => {
  if (!step1Data.value) {
    router.replace('/account/bind-identity')
  }
})
</script>

<style scoped>
.bind-identity-step2 {
  min-height: 100vh;
  background: var(--color-bg);
  padding-bottom: 40px;
}

.content {
  padding: 16px;
}

/* 桌面端拦截 */
.desktop-block {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
  padding: 24px;
}

.qr-container {
  text-align: center;
}

.qr-img {
  width: 200px;
  height: 200px;
  border-radius: var(--radius-md);
  margin-bottom: 16px;
}

.qr-tip {
  font-size: var(--font-size-body);
  color: var(--color-text-secondary);
  line-height: var(--line-height-relaxed);
}

/* 页面头部 */
.page-header {
  text-align: center;
  margin-bottom: 24px;
  padding-top: 16px;
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

/* 分区标题 */
.section-title {
  font-size: 14px;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text);
  margin: 20px 0 12px;
  padding: 0 4px;
}

/* 上传卡片 */
.upload-card {
  background: var(--color-card);
  border: 1px solid #FDE68A;
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

/* 提示卡片 */
.tips-card {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 16px 4px;
  padding: 0 4px;
}

.tips-text {
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: var(--line-height-normal);
}

/* Dev 环境跳过 */
.test-skip {
  margin: 16px 4px;
  padding: 12px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: var(--radius-lg);
}

/* 提交按钮 */
.submit-actions {
  margin-top: 24px;
  padding: 0 4px;
}

.identity-submit-btn {
  height: 48px;
  font-size: 16px;
  font-weight: var(--font-weight-semibold);
}
</style>
