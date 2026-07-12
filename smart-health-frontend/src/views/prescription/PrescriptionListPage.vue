<template>
  <div class="prescription-list-page page-container">
    <van-nav-bar title="我的处方" left-arrow @click-left="$router.push('/home')" />

    <van-loading v-if="loading" class="page-loading" />

    <template v-else>
      <EmptyState v-if="prescriptions.length === 0" description="暂无处方记录" />

      <div v-else class="prescription-list">
        <div
          v-for="rx in prescriptions"
          :key="rx.id"
          class="rx-card card"
          @click="$router.push(`/prescriptions/${rx.id}`)"
        >
          <div class="rx-header">
            <span class="rx-sn">{{ rx.prescriptionSn }}</span>
            <van-tag :type="getAuditStatus(rx.auditStatus).color" size="medium">
              {{ getAuditStatus(rx.auditStatus).text }}
            </van-tag>
          </div>
          <div class="rx-body">
            <p class="rx-diagnosis">{{ rx.diagnosis }}</p>
            <div class="rx-meta">
              <span>{{ formatDateTime(rx.createTime) }}</span>
              <span>{{ rx.items?.length || 0 }} 种药品</span>
            </div>
          </div>
          <div class="rx-footer">
            <van-icon name="arrow" />
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listPrescriptions } from '@/api/prescription'
import { formatDateTime, getAuditStatus } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'

const prescriptions = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    prescriptions.value = await listPrescriptions()
  } catch (err) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.prescription-list-page {
  animation: fade-in 0.3s ease;
}

.prescription-list {
  padding: 16px;
}

.rx-card {
  cursor: pointer;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
  background: var(--color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 16px;
  margin-bottom: 12px;
}

.rx-card:active {
  transform: scale(0.98);
  box-shadow: var(--shadow-sm);
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
  margin-bottom: 8px;
}

.rx-diagnosis {
  font-size: 15px;
  font-weight: var(--font-weight-medium);
  color: var(--color-text);
  margin-bottom: 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.rx-meta {
  display: flex;
  justify-content: space-between;
  font-size: var(--font-size-caption);
  color: var(--color-text-tertiary);
}

.rx-footer {
  display: flex;
  justify-content: flex-end;
  color: var(--color-text-tertiary);
}
</style>
