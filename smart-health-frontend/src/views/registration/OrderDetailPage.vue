<template>
  <div class="order-detail-page page-container">
    <van-nav-bar title="订单详情" left-arrow @click-left="$router.back()" />

    <van-loading v-if="loading" class="page-loading" />

    <template v-else-if="order">
      <!-- 状态进度 -->
      <div class="status-bar">
        <template v-if="order.status === 4">
          <van-steps :active="0" active-color="#EE0A24">
            <van-step>已退号</van-step>
          </van-steps>
        </template>
        <template v-else>
          <van-steps :active="statusStep" active-color="#1890FF">
            <van-step>排队中</van-step>
            <van-step>待支付</van-step>
            <van-step>已支付</van-step>
            <van-step>已就诊</van-step>
          </van-steps>
        </template>
      </div>

      <!-- 订单信息 -->
      <div class="order-info card">
        <h3 class="card-title">订单信息</h3>
        <van-cell title="订单号" :value="order.orderSn" />
        <van-cell title="医生" :value="`${order.doctorName || '医生'} (${order.deptName})`" />
        <van-cell title="就诊日期" :value="formatDate(order.workDate)" />
        <van-cell title="班次" :value="order.shiftName" />
        <van-cell title="挂号费" :value="formatMoney(order.fee)" />
        <van-cell title="创建时间" :value="formatDateTime(order.createTime)" />
        <van-cell v-if="order.payTime" title="支付时间" :value="formatDateTime(order.payTime)" />
      </div>

      <!-- 操作按钮 -->
      <div class="order-actions">
        <van-button
          v-if="order.status === 1"
          type="primary"
          block
          @click="handlePay"
        >
          去支付
        </van-button>
        <van-button
          v-if="order.status <= 1"
          plain
          type="danger"
          block
          class="mt-12"
          @click="handleCancel"
        >
          取消订单
        </van-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { showToast, showSuccessToast, showConfirmDialog } from 'vant'
import { getOrderDetail, cancelOrder, payOrder } from '@/api/registration'
import { formatDate, formatDateTime, formatMoney } from '@/utils/format'

const route = useRoute()
const orderSn = route.params.orderSn
const order = ref(null)
const loading = ref(true)

const statusStep = computed(() => {
  if (!order.value) return 0
  const map = { 0: 0, 1: 1, 2: 2, 3: 3 }
  return map[order.value.status] ?? 0
})

onMounted(async () => {
  try {
    order.value = await getOrderDetail(orderSn)
  } catch (err) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
})

async function handlePay() {
  try {
    showToast({ message: '支付中...', forbidClick: true, duration: 0 })
    await payOrder(orderSn)
    showSuccessToast('支付成功')
    // 重新加载订单数据
    order.value = await getOrderDetail(orderSn)
  } catch (err) {
    // 错误已在拦截器中处理
  }
}

async function handleCancel() {
  try {
    await showConfirmDialog({ title: '确认取消', message: '确定要取消该订单吗？' })
    await cancelOrder(orderSn)
    showSuccessToast('订单已取消')
    // 重新加载订单数据
    order.value = await getOrderDetail(orderSn)
  } catch (err) {
    // 用户取消弹窗 或 请求错误（拦截器已处理）
  }
}
</script>

<style scoped>
.page-loading {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.status-bar {
  background: #fff;
  padding: 16px;
  margin-bottom: 12px;
}

.order-info {
  margin: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 12px;
}

.order-actions {
  padding: 0 16px;
}

.mt-12 {
  margin-top: 12px;
}
</style>
