<template>
  <div class="schedule-card card" @click="$emit('click')">
    <div class="schedule-header">
      <van-tag type="primary" size="medium">{{ schedule.deptName }}</van-tag>
      <span class="schedule-price">{{ formatMoney(schedule.price) }}</span>
    </div>
    <div class="schedule-info">
      <div class="info-row">
        <van-icon name="calendar-o" />
        <span>{{ formatDate(schedule.workDate) }} {{ schedule.shiftName || getShiftName(schedule.shift) }}</span>
      </div>
      <div class="info-row">
        <van-icon name="friends-o" />
        <span>
          剩余号源：
          <span :class="schedule.visibleCount > 0 ? 'count-available' : 'count-full'">
            {{ schedule.visibleCount > 0 ? schedule.visibleCount : '已约满' }}
          </span>
        </span>
      </div>
    </div>
    <div class="schedule-action">
      <van-button
        size="small"
        type="primary"
        :disabled="schedule.visibleCount <= 0"
        @click.stop="$emit('seckill')"
      >
        {{ schedule.visibleCount > 0 ? '立即抢号' : '已约满' }}
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { formatDate, formatMoney, getShiftName } from '@/utils/format'

defineProps({
  schedule: { type: Object, required: true }
})

defineEmits(['click', 'seckill'])
</script>

<style scoped>
.schedule-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.schedule-card:active {
  transform: scale(0.98);
}

.schedule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.schedule-price {
  font-size: 18px;
  font-weight: bold;
  color: #F5222D;
}

.schedule-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #666;
}

.count-available {
  color: #52C41A;
  font-weight: bold;
}

.count-full {
  color: #F5222D;
}

.schedule-action {
  display: flex;
  justify-content: flex-end;
}
</style>
