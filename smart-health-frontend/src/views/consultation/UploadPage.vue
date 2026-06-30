<template>
  <div class="upload-page page-container">
    <van-nav-bar title="AI 智能问诊" left-arrow @click-left="$router.back()" />

    <div class="upload-content">
      <div class="upload-section">
        <h3 class="section-title">上传症状照片或检查报告</h3>
        <van-uploader
          v-model="fileList"
          :max-count="3"
          :before-read="beforeRead"
          :after-read="afterRead"
          accept="image/*"
        >
          <van-button icon="photograph" type="primary" plain>选择图片</van-button>
        </van-uploader>
      </div>

      <van-field
        v-model="imageType"
        is-link
        readonly
        label="图片类型"
        :placeholder="imageType || '请选择'"
        @click="showTypePicker = true"
      />
      <van-popup v-model:show="showTypePicker" position="bottom" round>
        <van-picker
          :columns="typeOptions"
          @confirm="onTypeConfirm"
          @cancel="showTypePicker = false"
        />
      </van-popup>

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
          :disabled="fileList.length === 0 || !imageType"
          @click="startAnalyze"
        >
          开始 AI 分析
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { multimodalAnalyze } from '@/api/consult'

const router = useRouter()
const fileList = ref([])
const imageType = ref('')
const symptomDesc = ref('')
const analyzing = ref(false)
const showTypePicker = ref(false)

const typeOptions = [
  { text: '症状图片', value: 'IMAGE' },
  { text: '检查报告', value: 'REPORT' }
]

function onTypeConfirm({ selectedOptions }) {
  imageType.value = selectedOptions[0]?.text || ''
  showTypePicker.value = false
}

function beforeRead(file) {
  const files = Array.isArray(file) ? file : [file]
  for (const f of files) {
    if (f.size > 10 * 1024 * 1024) {
      showToast('图片大小不能超过 10MB')
      return false
    }
  }
  return true
}

function afterRead(file) {
  // file 可能是对象或数组
  const files = Array.isArray(file) ? file : [file]
  files.forEach(f => {
    f.status = 'done'
    f.message = ''
  })
}

async function startAnalyze() {
  if (fileList.value.length === 0) {
    showToast('请先上传图片')
    return
  }

  analyzing.value = true
  try {
    const typeValue = typeOptions.find(t => t.text === imageType.value)?.value || 'IMAGE'
    const file = fileList.value[0].file || fileList.value[0]
    const result = await multimodalAnalyze(file, typeValue)

    // 将分析结果传递到分析结果页
    router.push({
      path: '/consultation/analysis',
      query: {
        draftId: result.draftId,
        symptomDraft: result.symptomDraft,
        fileUrl: result.fileUrl
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
.upload-content {
  padding: 16px;
}

.upload-section {
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 12px;
}

.upload-actions {
  margin-top: 24px;
}
</style>
