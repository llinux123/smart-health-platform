<template>
  <div class="order-list-page page-container">
    <van-nav-bar title="我的挂号订单" />

    <van-loading v-if="loading" class="page-loading" />

    <template v-else>
      <EmptyState v-if="orders.length === 0" description="暂无挂号订单" action-text="去挂号" @action="$router.push('/registration/schedules')" />

      <div v-else class="order-list">
        <div v-for="order in orders" :key="order.id" class="order-card card" @click="goToDetail(order)">
          <div class="order-header">
            <span class="order-sn">{{ order.orderSn }}</span>
            <van-tag :type="getOrderStatus(order.status).color" size="medium">
              {{ getOrderStatus(order.status).text }}
            </van-tag>
          </div>
          <div class="order-body">
            <div class="order-info-row">
              <span>{{ order.doctorName || '医生' }} · {{ order.deptName }}</span>
              <span class="order-fee">{{ formatMoney(order.fee) }}</span>
            </div>
            <div class="order-info-row secondary">
              <span>{{ formatDate(order.workDate) }} {{ order.shiftName }}</span>
              <span>{{ formatDateTime(order.createTime) }}</span>
            </div>
          </div>
          <div class="order-footer">
            <van-button
              v-if="order.status === 1"
              size="small"
              type="primary"
              @click.stop="goToDetail(order)"
            >
              去支付
            </van-button>
            <van-button
              size="small"
              plain
              @click.stop="goToDetail(order)"
            >
              查看详情
            </van-button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listOrders } from '@/api/registration'
import { useUserStore } from '@/stores/user'
import { formatDate, formatDateTime, formatMoney, getOrderStatus } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const userStore = useUserStore()
const orders = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    orders.value = await listOrders(userStore.patientId)
  } catch (err) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
})

function goToDetail(order) {
  router.push(`/registration/orders/${order.orderSn}`)
}
</script>

<style scoped>
.page-loading {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.order-list {
  padding: 16px;
}

.order-card {
  cursor: pointer;
}

.order-card:active {
  transform: scale(0.98);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.order-sn {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.order-body {
  margin-bottom: 12px;
}

.order-info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
  font-size: 14px;
}

.order-info-row.secondary {
  font-size: 12px;
  color: #999;
}

.order-fee {
  color: #F5222D;
  font-weight: bold;
}

.order-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}
</style>
