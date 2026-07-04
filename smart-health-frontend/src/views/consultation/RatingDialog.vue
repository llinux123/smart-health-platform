<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { showLoadingToast, closeToast, showToast } from 'vant'
import { useConsultationStore } from '@/stores/consultation'

const props = defineProps<{
  show: boolean
  sessionSn: string
}>()

const emit = defineEmits<{
  close: []
  complete: []
}>()

const store = useConsultationStore()

const visible = computed({
  get: () => props.show,
  set: () => emit('close')
})

const rating = ref(0)
const feedback = ref('')
const submitting = ref(false)

watch(() => props.show, (val) => {
  if (val) {
    rating.value = 0
    feedback.value = ''
  }
})

const ratingTexts = ['', '很差', '较差', '一般', '满意', '非常满意']

async function handleSubmit() {
  if (rating.value === 0) {
    showToast('请先选择评分')
    return
  }
  submitting.value = true
  showLoadingToast({ message: '提交中...', forbidClick: true })
  try {
    await store.rateSessionItem(props.sessionSn, rating.value, feedback.value || undefined)
    closeToast()
    showToast('评分已提交')
    emit('complete')
  } catch {
    closeToast()
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <van-popup
    v-model:show="visible"
    round
    position="bottom"
    :style="{ maxHeight: '70vh' }"
    @close="emit('close')"
  >
    <div class="rating-dialog">
      <div class="rating-dialog__header">
        <h3>为本次问诊评分</h3>
        <van-icon name="cross" size="20" class="close-btn" @click="emit('close')" />
      </div>

      <div class="rating-dialog__body">
        <!-- 星级评分 -->
        <div class="rating-stars">
          <van-rate
            v-model="rating"
            :count="5"
            size="32"
            color="#D4956B"
            void-color="#E8E4E0"
          />
          <span v-if="rating > 0" class="rating-text">
            {{ ratingTexts[rating] }}
          </span>
        </div>

        <!-- 文字反馈 -->
        <div class="feedback-area">
          <van-field
            v-model="feedback"
            type="textarea"
            rows="3"
            placeholder="说说您的问诊体验（选填）"
            maxlength="500"
            show-word-limit
            autosize
          />
        </div>
      </div>

      <div class="rating-dialog__footer">
        <van-button
          type="primary"
          block
          round
          :loading="submitting"
          :disabled="rating === 0"
          @click="handleSubmit"
        >
          提交评分
        </van-button>
      </div>
    </div>
  </van-popup>
</template>

<style scoped>
.rating-dialog {
  padding: var(--spacing-lg);
}

.rating-dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-lg);
}

.rating-dialog__header h3 {
  font-size: var(--font-size-title);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text);
}

.close-btn {
  color: var(--color-text-secondary);
  cursor: pointer;
  padding: 4px;
}

.rating-dialog__body {
  margin-bottom: var(--spacing-lg);
}

.rating-stars {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md) 0;
}

.rating-text {
  font-size: var(--font-size-body);
  color: var(--color-warm);
  font-weight: var(--font-weight-medium);
}

.feedback-area {
  margin-top: var(--spacing-md);
  border: 1px solid var(--color-divider);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.feedback-area :deep(.van-field) {
  padding: var(--spacing-sm);
}

.feedback-area :deep(.van-field__control) {
  font-size: var(--font-size-body);
}

.rating-dialog__footer {
  padding-top: var(--spacing-sm);
}
</style>
