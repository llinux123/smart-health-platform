<template>
  <div class="prescription-review-page page-container">
    <van-nav-bar title="处方审核" left-arrow @click-left="$router.back()" />

    <van-loading v-if="loading" class="page-loading" />

    <template v-else>
      <EmptyState v-if="pendingList.length === 0" description="暂无待审核处方" />

      <div v-else class="review-list">
        <div v-for="rx in pendingList" :key="rx.id" class="rx-card card">
          <div class="rx-header">
            <span class="rx-sn">{{ rx.prescriptionSn }}</span>
            <van-tag type="warning" size="medium">待审核</van-tag>
          </div>
          <div class="rx-body">
            <p class="rx-diagnosis">{{ rx.diagnosis }}</p>
            <div class="rx-items">
              <span v-for="item in rx.items" :key="item.id" class="item-tag">
                {{ item.medicineName }} ×{{ item.quantity }}
              </span>
            </div>
            <p class="rx-date">{{ formatDateTime(rx.createTime) }}</p>
          </div>
          <div class="rx-actions">
            <van-button size="small" type="success" @click="openAudit(rx, 'APPROVE')">
              通过
            </van-button>
            <van-button size="small" type="danger" @click="openAudit(rx, 'REJECT')">
              驳回
            </van-button>
          </div>
        </div>
      </div>
    </template>

    <!-- 审核弹窗 -->
    <van-dialog
      v-model:show="auditDialogVisible"
      :title="auditAction === 'APPROVE' ? '确认通过' : '驳回处方'"
      show-cancel-button
      @confirm="submitAudit"
    >
      <div class="audit-dialog-body">
        <p class="audit-rx-sn">{{ currentRx?.prescriptionSn }}</p>
        <van-field
          v-model="auditComments"
          type="textarea"
          rows="3"
          :placeholder="auditAction === 'REJECT' ? '请填写驳回理由' : '审核意见（选填）'"
        />
      </div>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { showSuccessToast } from 'vant'
import { listPendingAudit, auditPrescription } from '@/api/prescription'
import { formatDateTime } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'

const pendingList = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    pendingList.value = await listPendingAudit()
  } catch (err) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
})

// 审核逻辑
const auditDialogVisible = ref(false)
const currentRx = ref(null)
const auditAction = ref('')
const auditComments = ref('')

function openAudit(rx, action) {
  currentRx.value = rx
  auditAction.value = action
  auditComments.value = ''
  auditDialogVisible.value = true
}

async function submitAudit() {
  if (auditAction.value === 'REJECT' && !auditComments.value.trim()) {
    return
  }

  try {
    await auditPrescription(currentRx.value.id, {
      action: auditAction.value,
      comments: auditComments.value
    })
    showSuccessToast(auditAction.value === 'APPROVE' ? '审核通过' : '已驳回')
    // 从列表中移除
    pendingList.value = pendingList.value.filter(rx => rx.id !== currentRx.value.id)
  } catch (err) {
    // 错误已在拦截器中处理
  }
}
</script>

<style scoped>
.prescription-review-page {
  animation: fade-in 0.3s ease;
}

.review-list {
  padding: 16px;
}

.review-list .rx-card {
  margin-bottom: 12px;
  background: var(--color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 16px;
}

.rx-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.rx-sn {
  font-size: 13px;
  color: var(--color-text-secondary);
  font-family: var(--font-mono);
}

.rx-body {
  margin-bottom: 12px;
}

.rx-diagnosis {
  font-size: var(--font-size-body);
  font-weight: var(--font-weight-medium);
  color: var(--color-text);
  margin-bottom: 8px;
}

.rx-items {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.item-tag {
  font-size: var(--font-size-caption);
  color: var(--color-text-secondary);
  background: var(--color-bg);
  padding: 3px 10px;
  border-radius: var(--radius-full);
}

.rx-date {
  font-size: var(--font-size-caption);
  color: var(--color-text-tertiary);
}

.rx-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.audit-dialog-body {
  padding: 16px;
}

.audit-rx-sn {
  font-size: 13px;
  color: var(--color-text-secondary);
  font-family: var(--font-mono);
  margin-bottom: 12px;
}
</style>
