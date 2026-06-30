<template>
  <div class="prescription-detail-page page-container">
    <van-nav-bar title="处方详情" left-arrow @click-left="$router.back()" />

    <van-loading v-if="loading" class="page-loading" />

    <template v-else-if="rx">
      <!-- 处方抬头 -->
      <div class="rx-header card">
        <h2 class="rx-sn">{{ rx.prescriptionSn }}</h2>
        <van-tag :type="getAuditStatus(rx.auditStatus).color" size="large">
          {{ getAuditStatus(rx.auditStatus).text }}
        </van-tag>
        <p class="rx-date">{{ formatDateTime(rx.createTime) }}</p>
      </div>

      <!-- 诊断信息 -->
      <div class="rx-section card">
        <h3 class="section-title">诊断信息</h3>
        <p class="diagnosis-text">{{ rx.diagnosis }}</p>
      </div>

      <!-- 药品列表 -->
      <div class="rx-section card">
        <h3 class="section-title">药品明细</h3>
        <van-cell-group :border="false">
          <van-cell
            v-for="item in rx.items"
            :key="item.id"
            :title="item.medicineName"
            :label="`${item.spec} | ${item.usage}`"
          >
            <template #value>
              <div class="item-value">
                <span>×{{ item.quantity }}{{ item.unit }}</span>
                <span class="item-price">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
              </div>
            </template>
          </van-cell>
        </van-cell-group>

        <div class="total-row" v-if="rx.items?.length">
          <span>药品总计</span>
          <span class="total-price">{{ formatMoney(totalPrice) }}</span>
        </div>
      </div>

      <!-- 审核信息 -->
      <div v-if="rx.auditStatus !== 0" class="rx-section card">
        <h3 class="section-title">审核信息</h3>
        <van-cell title="审核状态" :value="getAuditStatus(rx.auditStatus).text" />
        <van-cell v-if="rx.auditComments" title="审核意见" :value="rx.auditComments" />
        <van-cell v-if="rx.auditTime" title="审核时间" :value="formatDateTime(rx.auditTime)" />
      </div>

      <!-- PDF 下载 -->
      <div v-if="rx.pdfUrl" class="rx-actions">
        <van-button type="primary" plain block icon="down" @click="downloadPdf">
          下载处方 PDF
        </van-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { showToast } from 'vant'
import { getPrescription } from '@/api/prescription'
import { useUserStore } from '@/stores/user'
import { formatDateTime, formatMoney, getAuditStatus } from '@/utils/format'

const route = useRoute()
const userStore = useUserStore()
const rxId = route.params.id
const rx = ref(null)
const loading = ref(true)

const totalPrice = computed(() => {
  if (!rx.value?.items) return 0
  return rx.value.items.reduce((sum, item) => sum + item.price * item.quantity, 0)
})

onMounted(async () => {
  try {
    rx.value = await getPrescription(rxId, userStore.patientId)
  } catch (err) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
})

function downloadPdf() {
  if (rx.value?.pdfUrl) {
    window.open(rx.value.pdfUrl, '_blank')
  } else {
    showToast('PDF 文件不可用')
  }
}
</script>

<style scoped>
.page-loading {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.rx-header {
  margin: 16px;
  text-align: center;
}

.rx-sn {
  font-size: 18px;
  font-weight: bold;
  font-family: monospace;
  margin-bottom: 8px;
}

.rx-date {
  font-size: 13px;
  color: #999;
  margin-top: 8px;
}

.rx-section {
  margin: 0 16px 16px;
}

.section-title {
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 12px;
  color: #333;
}

.diagnosis-text {
  font-size: 14px;
  line-height: 1.6;
  color: #555;
}

.item-value {
  text-align: right;
}

.item-price {
  display: block;
  color: #F5222D;
  font-weight: bold;
  font-size: 14px;
}

.total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  font-size: 14px;
}

.total-price {
  color: #F5222D;
  font-weight: bold;
  font-size: 16px;
}

.rx-actions {
  padding: 16px;
}
</style>
