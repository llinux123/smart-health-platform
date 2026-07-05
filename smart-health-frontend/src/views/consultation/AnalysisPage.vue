<template>
  <div class="analysis-page page-container">
    <van-nav-bar title="AI 分析结果" left-arrow @click-left="goHome" />

    <div class="analysis-content">
      <!-- 多文件预览 -->
      <div v-if="fileUrls.length" class="file-previews">
        <div v-for="(url, idx) in fileUrls" :key="idx" class="file-preview-item">
          <template v-if="isImageUrl(url)">
            <van-image
              width="100%"
              fit="contain"
              :src="url"
              radius="8"
            />
          </template>
          <template v-else>
            <div class="file-info">
              <van-icon name="description" size="24" />
              <span class="file-info-name">{{ getFileName(url) }}</span>
            </div>
          </template>
        </div>
      </div>

      <div class="analysis-card card">
        <h3 class="card-title">
          <van-icon name="chart-trending-o" />
          AI 分析结果
        </h3>
        <div class="symptom-draft" v-html="renderedDraft"></div>
      </div>

      <van-notice-bar
        left-icon="warning-o"
        text="以上结果仅供参考，不构成医疗诊断，请及时就医"
        color="#FAAD14"
        background="#FFFBE6"
      />

      <div class="analysis-actions">
        <van-button
          type="primary"
          block
          :loading="creating"
          @click="startConsult"
        >
          开始 AI 问诊追问
        </van-button>
        <van-button
          plain
          block
          class="mt-12"
          @click="goReUpload"
        >
          重新上传
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showLoadingToast } from 'vant'
import { createSession } from '@/api/consult'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const route = useRoute()
const router = useRouter()

const fileUrls = ref([])
const draftId = ref('')
const symptomDraft = ref('')
const creating = ref(false)

const renderedDraft = computed(() => {
  if (!symptomDraft.value) return ''
  return DOMPurify.sanitize(marked(symptomDraft.value))
})

onMounted(() => {
  const urls = route.query.fileUrls || ''
  fileUrls.value = urls ? urls.split(',').filter(Boolean) : []
  draftId.value = route.query.draftId || ''
  symptomDraft.value = route.query.symptomDraft || ''
})

function isImageUrl(url) {
  const ext = url.toLowerCase().split('.').pop()
  return ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(ext)
}

function getFileName(url) {
  const parts = url.split('/')
  return parts[parts.length - 1]
}

function goHome() {
  router.replace('/home')
}

function goReUpload() {
  // 使用 replace 导航到上传页，避免返回到当前分析结果
  router.replace('/consultation/upload')
}

async function startConsult() {
  creating.value = true
  try {
    const sessionSn = await createSession(
      draftId.value,
      symptomDraft.value,
      fileUrls.value.join(',')
    )
    // 使用 replace 导航到对话页，避免返回到分析结果页
    router.replace(`/consultation/chat/${sessionSn}`)
  } catch (err) {
    // 错误已在拦截器中处理
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.analysis-page {
  animation: fade-in 0.3s ease;
}

.analysis-content {
  padding: 16px;
}

.file-previews {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-preview-item {
  background: var(--color-card);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-caption);
}

.file-info-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.analysis-card {
  margin-bottom: 16px;
}

.analysis-card .card-title {
  font-size: var(--font-size-card-title);
  font-weight: var(--font-weight-semibold);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--color-text);
}

.symptom-draft {
  font-size: var(--font-size-body);
  line-height: var(--line-height-relaxed);
  color: var(--color-text);
}

.symptom-draft :deep(strong) {
  color: var(--color-primary);
}

.symptom-draft :deep(ol),
.symptom-draft :deep(ul) {
  padding-left: 20px;
  margin: 8px 0;
}

.analysis-actions {
  margin-top: 24px;
}

.analysis-actions :deep(.van-button--primary) {
  height: 48px;
  border-radius: var(--radius-lg);
  font-size: 16px;
  font-weight: var(--font-weight-semibold);
}

.mt-12 {
  margin-top: 12px;
}
</style>
