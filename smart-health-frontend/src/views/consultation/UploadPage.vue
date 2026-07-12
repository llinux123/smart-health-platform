<template>
  <div class="upload-page page-container">
    <van-nav-bar title="AI 智能问诊" left-arrow @click-left="handleBack" />

    <div class="upload-content">
      <!-- 图片类型选择 -->
      <div class="type-selector">
        <van-button
          :type="imageType === '症状图片' ? 'primary' : 'default'"
          size="small"
          plain="true"
          @click="selectType('症状图片')"
          class="type-btn"
        >
          <van-icon name="photograph" /> 症状图片
        </van-button>
        <van-button
          :type="imageType === '检查报告' ? 'primary' : 'default'"
          size="small"
          plain="true"
          @click="selectType('检查报告')"
          class="type-btn"
        >
          <van-icon name="records-o" /> 检查报告
        </van-button>
      </div>

      <div class="upload-section">
        <h3 class="section-title">{{ imageType === '检查报告' ? '上传检查报告' : '上传症状照片' }}</h3>
        <p class="upload-hint">
          {{ imageType === '检查报告' ? '支持图片、PDF、Word 格式' : '支持 JPG/PNG 等图片格式' }}
        </p>
        <van-uploader
          v-model="fileList"
          :max-count="3"
          :before-read="beforeRead"
          :accept="acceptType"
          :multiple="true"
        >
          <van-button icon="photograph" type="primary" plain>{{ imageType === '检查报告' ? '选择文件' : '选择图片' }}</van-button>
        </van-uploader>
        <!-- 对于非图片文件，显示文件名 -->
        <div v-for="item in fileList" :key="item.url || item.file?.name" class="file-item">
          <template v-if="!isImageFile(item.file?.name || item.url || '')">
            <van-icon name="description" size="20" />
            <span class="file-name">{{ item.file?.name || item.url }}</span>
            <van-icon name="cross" class="file-remove" @click="removeFile(item)" />
          </template>
        </div>
      </div>

      <van-field
        v-model="symptomDesc"
        type="textarea"
        rows="3"
        label="症状描述"
        placeholder="可选：补充描述您的症状（如持续时间、疼痛程度等）"
      />

      <div class="upload-actions">
        <van-button
          type="primary"
          block
          :loading="analyzing"
          loading-text="AI 正在分析..."
          :disabled="fileList.length === 0"
          @click="startAnalyze"
        >
          开始 AI 分析
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { multimodalAnalyze } from '@/api/consult'

const router = useRouter()
const fileList = ref([])
const imageType = ref('症状图片') // 默认选择症状图片
const symptomDesc = ref('')
const analyzing = ref(false)

// 根据选择的类型决定 accept
const acceptType = computed(() => {
  if (imageType.value === '检查报告') {
    return 'image/*,.pdf,.doc,.docx'
  }
  return 'image/*'
})

function isImageFile(filename) {
  const ext = filename.toLowerCase().split('.').pop()
  return ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(ext)
}

function selectType(type) {
  imageType.value = type
  // 切换类型时清空已选文件
  fileList.value = []
}

function removeFile(item) {
  const idx = fileList.value.indexOf(item)
  if (idx !== -1) {
    fileList.value.splice(idx, 1)
  }
}

function beforeRead(file) {
  const files = Array.isArray(file) ? file : [file]
  for (const f of files) {
    if (f.size > 10 * 1000 * 1000) {
      showToast('文件大小不能超过 10MB')
      return false
    }
  }
  return true
}

function handleBack() {
  router.replace('/home')
}

async function startAnalyze() {
  if (fileList.value.length === 0) {
    showToast('请先上传文件')
    return
  }

  analyzing.value = true
  try {
    const typeValue = imageType.value === '检查报告' ? 'REPORT' : 'IMAGE'
    const rawFiles = fileList.value
      .map(item => {
        if (item instanceof File) return item
        if (item.file instanceof File) return item.file
        if (item.url && item.file) return item.file
        return null
      })
      .filter(Boolean)

    if (rawFiles.length === 0) {
      showToast('文件数据异常，请重新选择文件')
      analyzing.value = false
      return
    }

    const result = await multimodalAnalyze(rawFiles, typeValue)

    // 使用 replace 导航到分析结果页，避免返回时看到旧结果
    router.replace({
      path: '/consultation/analysis',
      query: {
        draftId: result.draftId,
        symptomDraft: result.symptomDraft,
        fileUrls: Array.isArray(result.fileUrls) ? result.fileUrls.join(',') : String(result.fileUrls || ''),
        from: 'upload'
      }
    })
  } catch (err) {
    // 错误已在拦截器中处理
  } finally {
    analyzing.value = false
  }
}
</script>

<style scoped>
.upload-page {
  animation: fade-in 0.3s ease;
}

.upload-content {
  padding: 16px;
}

.type-selector {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.type-btn {
  flex: 1;
  height: 44px;
  border-radius: var(--radius-md);
  font-size: 14px;
}

.upload-section {
  margin-bottom: 16px;
}

.upload-hint {
  font-size: var(--font-size-caption);
  color: var(--color-text-tertiary);
  margin-bottom: 12px;
}

.upload-content .section-title {
  font-size: var(--font-size-card-title);
  font-weight: var(--font-weight-semibold);
  margin-bottom: 4px;
  color: var(--color-text);
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  font-size: var(--font-size-caption);
  color: var(--color-text-secondary);
}

.file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-remove {
  color: var(--color-text-tertiary);
  cursor: pointer;
}

.upload-actions {
  margin-top: 24px;
}

.upload-actions :deep(.van-button--primary) {
  height: 48px;
  border-radius: var(--radius-lg);
  font-size: 16px;
  font-weight: var(--font-weight-semibold);
}
</style>
