<template>
  <div class="analysis-page page-container">
    <van-nav-bar title="AI 分析结果" left-arrow @click-left="$router.back()" />

    <div class="analysis-content">
      <div v-if="fileUrl" class="image-preview">
        <van-image
          width="100%"
          fit="contain"
          :src="fileUrl"
          radius="8"
        />
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
          @click="$router.push('/consultation/upload')"
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

const route = useRoute()
const router = useRouter()

const fileUrl = ref('')
const draftId = ref('')
const symptomDraft = ref('')
const creating = ref(false)

const renderedDraft = computed(() => {
  if (!symptomDraft.value) return ''
  return marked(symptomDraft.value)
})

onMounted(() => {
  fileUrl.value = route.query.fileUrl || ''
  draftId.value = route.query.draftId || ''
  symptomDraft.value = route.query.symptomDraft || ''
})

async function startConsult() {
  creating.value = true
  try {
    const sessionSn = await createSession(draftId.value, symptomDraft.value)
    router.push(`/consultation/chat/${sessionSn}`)
  } catch (err) {
    // 错误已在拦截器中处理
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.analysis-content {
  padding: 16px;
}

.image-preview {
  margin-bottom: 16px;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.analysis-card {
  margin-bottom: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.symptom-draft {
  font-size: 14px;
  line-height: 1.8;
  color: #333;
}

.symptom-draft :deep(strong) {
  color: #1890FF;
}

.symptom-draft :deep(ol),
.symptom-draft :deep(ul) {
  padding-left: 20px;
  margin: 8px 0;
}

.analysis-actions {
  margin-top: 24px;
}

.mt-12 {
  margin-top: 12px;
}
</style>
