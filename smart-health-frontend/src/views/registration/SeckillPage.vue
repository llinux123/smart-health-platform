<template>
  <div class="seckill-page page-container">
    <van-nav-bar title="确认挂号" left-arrow @click-left="$router.back()" />

    <div class="seckill-content">
      <van-loading v-if="loading" class="page-loading" />

      <template v-else>
        <div class="order-info card">
          <h3 class="card-title">挂号信息确认</h3>
          <van-cell title="医生" :value="scheduleInfo.doctorName || '未知'" />
          <van-cell title="科室" :value="scheduleInfo.deptName" />
          <van-cell title="出诊日期" :value="formatDate(scheduleInfo.workDate)" />
          <van-cell title="班次" :value="scheduleInfo.shiftName || getShiftName(scheduleInfo.shift)" />
          <van-cell title="挂号费" :value="formatMoney(scheduleInfo.price)" value-class="price-cell" />
        </div>

        <van-notice-bar
          left-icon="warning-o"
          text="挂号成功后请在 5 分钟内完成支付，超时将自动取消"
        />

        <div class="seckill-actions">
          <van-button
            type="primary"
            block
            size="large"
            :loading="submitting"
            loading-text="抢号中..."
            :disabled="submitted"
            @click="handleSeckill"
          >
            {{ submitted ? '已提交' : '确认抢号' }}
          </van-button>
        </div>

        <!-- 抢号结果 -->
        <div v-if="result" class="result-card card">
          <div class="result-icon" :class="result.success ? 'result-success' : 'result-error'">
            <van-icon :name="result.success ? 'checked' : 'cross'" size="48" />
          </div>
          <h3 class="result-title">{{ result.success ? '抢号成功' : '抢号失败' }}</h3>
          <p class="result-subtitle">{{ result.message }}</p>
          <div class="result-footer">
            <van-button v-if="result.success" type="primary" @click="goToOrder">
              查看订单
            </van-button>
            <van-button v-else plain type="primary" @click="handleSeckill">
              再试一次
            </van-button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { seckill, getScheduleDetail } from '@/api/registration'
import { formatDate, formatMoney, getShiftName } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const scheduleId = Number(route.params.scheduleId)
const submitting = ref(false)
const submitted = ref(false)
const result = ref(null)
const loading = ref(true)

const scheduleInfo = reactive({
  deptName: '',
  workDate: '',
  shift: 0,
  shiftName: '',
  price: 0,
  doctorName: ''
})

onMounted(async () => {
  try {
    const detail = await getScheduleDetail(scheduleId)
    scheduleInfo.deptName = detail.deptName || ''
    scheduleInfo.workDate = detail.workDate || ''
    scheduleInfo.shift = detail.shift || 1
    scheduleInfo.shiftName = detail.shiftName || getShiftName(detail.shift)
    scheduleInfo.price = detail.price || 0
    scheduleInfo.doctorName = detail.doctorName || ''
  } catch {
    showToast('加载排班信息失败')
  } finally {
    loading.value = false
  }
})

async function handleSeckill() {
  submitting.value = true
  result.value = null
  try {
    const response = await seckill({ scheduleId })
    submitted.value = true
    result.value = {
      success: true,
      message: `订单号：${response.orderSn}`,
      orderSn: response.orderSn
    }
  } catch (err) {
    result.value = {
      success: false,
      message: err.message || '抢号失败，请稍后重试'
    }
  } finally {
    submitting.value = false
  }
}

function goToOrder() {
  if (result.value?.orderSn) {
    router.push(`/registration/orders/${result.value.orderSn}`)
  }
}
</script>

<style scoped>
.seckill-page {
  animation: fade-in 0.3s ease;
}

.seckill-content {
  padding: 16px;
}

.seckill-content .card-title {
  font-size: var(--font-size-card-title);
  font-weight: var(--font-weight-semibold);
  margin-bottom: 12px;
  color: var(--color-text);
}

.price-cell {
  color: var(--color-accent);
  font-weight: var(--font-weight-bold);
}

.seckill-actions {
  margin: 24px 0;
}

.seckill-actions :deep(.van-button--primary) {
  height: 48px;
  border-radius: var(--radius-lg);
  font-weight: var(--font-weight-semibold);
}

.result-card {
  margin-top: 16px;
  text-align: center;
  padding: 24px 16px;
}

.result-icon {
  margin-bottom: 16px;
}

.result-success {
  color: var(--color-success);
}

.result-error {
  color: var(--color-danger);
}

.result-title {
  font-size: var(--font-size-title);
  font-weight: var(--font-weight-semibold);
  margin-bottom: 8px;
  color: var(--color-text);
}

.result-subtitle {
  font-size: var(--font-size-body);
  color: var(--color-text-secondary);
  margin-bottom: 16px;
}

.result-footer {
  margin-top: 16px;
}
</style>
