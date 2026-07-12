<template>
  <div class="order-list-page page-container">
    <van-nav-bar title="我的挂号订单" left-arrow @click-left="$router.push('/home')" />

    <van-tabs v-model:active="tab" sticky>
      <van-tab title="有效订单" name="active" />
      <van-tab title="已取消" name="cancelled" />
    </van-tabs>

    <van-loading v-if="loading" class="page-loading" />

    <template v-else>
      <EmptyState
        v-if="displayedOrders.length === 0"
        :description="tab === 'active' ? '暂无有效订单' : '暂无已取消订单'"
        action-text="去挂号"
        @action="$router.push('/registration/schedules')"
      />

      <div v-else class="order-list">
        <div v-for="order in displayedOrders" :key="order.orderSn" class="order-card card" @click="goToDetail(order)">
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listOrders } from '@/api/registration'
import { formatDate, formatDateTime, formatMoney, getOrderStatus } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const orders = ref([])
const loading = ref(true)
const tab = ref('active')

// 有效订单：排除已取消（status=4）；已取消 tab 仅显示 status=4
const displayedOrders = computed(() => {
  if (tab.value === 'cancelled') {
    return orders.value.filter(o => o.status === 4)
  }
  return orders.value.filter(o => o.status !== 4)
})

onMounted(async () => {
  try {
    orders.value = await listOrders()
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
.order-list-page {
  animation: fade-in 0.3s ease;
}

.order-list {
  padding: 16px;
}

.order-card {
  cursor: pointer;
  background: var(--color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 16px;
  margin-bottom: 12px;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.order-card:active {
  transform: scale(0.98);
  box-shadow: var(--shadow-sm);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.order-sn {
  font-size: var(--font-size-body);
  font-weight: var(--font-weight-medium);
  color: var(--color-text);
}

.order-body {
  margin-bottom: 12px;
}

.order-info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
  font-size: var(--font-size-body);
  color: var(--color-text);
}

.order-info-row.secondary {
  font-size: var(--font-size-caption);
  color: var(--color-text-tertiary);
}

.order-fee {
  color: var(--color-accent);
  font-weight: var(--font-weight-bold);
}

.order-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid var(--color-divider);
  padding-top: 12px;
}

.order-footer :deep(.van-button--primary) {
  height: 32px;
  padding: 0 16px;
  font-size: var(--font-size-caption);
}
</style>
