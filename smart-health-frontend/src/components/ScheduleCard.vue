<template>
  <div class="schedule-card" @click="$emit('click')">
    <div class="schedule-top">
      <span class="schedule-dept">{{ schedule.deptName }}</span>
      <span class="schedule-price">{{ formatMoney(schedule.price) }}</span>
    </div>

    <!-- 医生信息 -->
    <div class="doctor-info" v-if="schedule.doctorName">
      <van-image
        v-if="schedule.doctorAvatar"
        :src="schedule.doctorAvatar"
        round
        width="40"
        height="40"
        class="doctor-avatar"
      />
      <div v-else class="doctor-avatar-placeholder">
        <van-icon name="contact" size="20" />
      </div>
      <div class="doctor-detail">
        <span class="doctor-name">{{ schedule.doctorName }}</span>
        <span class="doctor-dept-tag">{{ schedule.deptName }}</span>
      </div>
    </div>

    <div class="schedule-body">
      <div class="schedule-row">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
          <line x1="16" y1="2" x2="16" y2="6"/>
          <line x1="8" y1="2" x2="8" y2="6"/>
          <line x1="3" y1="10" x2="21" y2="10"/>
        </svg>
        <span>{{ formatDate(schedule.workDate) }} {{ schedule.shiftName || getShiftName(schedule.shift) }}</span>
      </div>
      <div class="schedule-row">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M17 21V19C17 17.9391 16.5786 16.9217 15.8284 16.1716C15.0783 15.4214 14.0609 15 13 15H11C9.93913 15 8.92172 15.4214 8.17157 16.1716C7.42143 16.9217 7 17.9391 7 19V21"/>
          <circle cx="12" cy="7" r="4"/>
        </svg>
        <span>
          号源：
          <span :class="schedule.visibleCount > 0 ? 'count-available' : 'count-full'">
            {{ schedule.visibleCount > 0 ? `剩余 ${schedule.visibleCount}` : '已约满' }}
          </span>
        </span>
      </div>
    </div>
    <div class="schedule-footer">
      <van-button
        size="small"
        round
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
  background: var(--color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.schedule-card:active {
  transform: scale(0.98);
  box-shadow: var(--shadow-sm);
}

.schedule-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.schedule-dept {
  display: inline-block;
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
  font-size: var(--font-size-caption);
  font-weight: var(--font-weight-medium);
  padding: 3px 10px;
  border-radius: var(--radius-full);
}

.doctor-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 8px 0;
  border-top: 1px solid var(--color-divider);
  border-bottom: 1px solid var(--color-divider);
}

.doctor-avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
}

.doctor-detail {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.doctor-name {
  font-size: 14px;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text);
}

.doctor-dept-tag {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.schedule-price {
  font-size: 18px;
  font-weight: var(--font-weight-bold);
  color: var(--color-accent);
}

.schedule-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
}

.schedule-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.schedule-row svg {
  flex-shrink: 0;
  opacity: 0.5;
}

.count-available {
  color: var(--color-success);
  font-weight: var(--font-weight-semibold);
}

.count-full {
  color: var(--color-danger);
  font-weight: var(--font-weight-medium);
}

.schedule-footer {
  display: flex;
  justify-content: flex-end;
}

.schedule-footer :deep(.van-button--primary) {
  height: 32px;
  padding: 0 16px;
  font-size: 12px;
}
</style>
