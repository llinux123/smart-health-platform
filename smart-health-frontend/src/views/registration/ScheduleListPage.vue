<template>
  <div class="schedule-list-page page-container">
    <van-nav-bar title="挂号预约" left-arrow @click-left="$router.back()" />

    <div class="filter-bar">
      <div class="filter-triggers">
        <van-popover v-model:show="showDeptPopover" :actions="deptActions" placement="bottom-start" trigger="click" @select="onDeptSelect">
          <template #reference>
            <div class="filter-btn" :class="{ active: showDeptPopover }">
              <span>{{ currentDeptLabel }}</span>
              <van-icon name="arrow-down" class="arrow-icon" :class="{ rotated: showDeptPopover }" />
            </div>
          </template>
        </van-popover>

        <van-popover v-model:show="showDatePopover" :actions="dateActions" placement="bottom-start" trigger="click" @select="onDateSelect">
          <template #reference>
            <div class="filter-btn" :class="{ active: showDatePopover }">
              <span>{{ currentDateLabel }}</span>
              <van-icon name="arrow-down" class="arrow-icon" :class="{ rotated: showDatePopover }" />
            </div>
          </template>
        </van-popover>
      </div>
    </div>

    <van-loading v-if="loading" class="page-loading" />

    <template v-else>
      <EmptyState v-if="schedules.length === 0" description="暂无可预约的排班" />

      <div v-else class="schedule-list">
        <ScheduleCard
          v-for="schedule in schedules"
          :key="schedule.id"
          :schedule="schedule"
          @seckill="goToSeckill(schedule)"
        />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listSchedules, listDepartments } from '@/api/registration'
import { formatDate } from '@/utils/format'
import ScheduleCard from '@/components/ScheduleCard.vue'
import EmptyState from '@/components/EmptyState.vue'

interface Schedule {
  id: number
  deptName: string
  price: number
  doctorName?: string
  doctorAvatar?: string
  workDate: string
  shiftName?: string
  shift?: string
  visibleCount: number
}

const router = useRouter()
const schedules = ref<Schedule[]>([])
const loading = ref(true)
const showDeptPopover = ref(false)
const showDatePopover = ref(false)

const filterDept = ref('')
const filterDate = ref('')

const deptOptions = ref<{ text: string; value: string }[]>([{ text: '全部科室', value: '' }])
const deptMap = ref<Record<string, number>>({})

const today = new Date()
const todayStr = today.toISOString().slice(0, 10)

const dateOptions = [
  { text: '全部日期', value: '' },
  ...Array.from({ length: 7 }, (_, i) => {
    const d = new Date()
    d.setDate(d.getDate() + i)
    const dateStr = d.toISOString().slice(0, 10)
    return { text: i === 0 ? '今天' : i === 1 ? '明天' : formatDate(dateStr), value: dateStr }
  })
]

const deptActions = computed(() =>
  deptOptions.value.map(o => ({ text: o.text }))
)

const dateActions = computed(() =>
  dateOptions.map(o => ({ text: o.text }))
)

const currentDeptLabel = computed(() => {
  const matched = deptOptions.value.find(o => o.value === filterDept.value)
  return matched ? matched.text : '全部科室'
})

const currentDateLabel = computed(() => {
  const matched = dateOptions.find(o => o.value === filterDate.value)
  return matched ? matched.text : '全部日期'
})

onMounted(async () => {
  try {
    const depts: Array<{ id: number; name: string }> = await listDepartments()
    deptMap.value = Object.fromEntries(depts.map(d => [d.name, d.id]))
    deptOptions.value = [
      { text: '全部科室', value: '' },
      ...depts.map(d => ({ text: d.name, value: d.name }))
    ]
  } catch {
    // fallback
  }
  filterDate.value = todayStr
  await loadSchedules()
})

function onDeptSelect(action: { text: string }) {
  const dept = deptOptions.value.find(o => o.text === action.text)
  filterDept.value = dept ? dept.value : ''
  showDeptPopover.value = false
  loadSchedules()
}

function onDateSelect(action: { text: string }) {
  const date = dateOptions.find(o => o.text === action.text)
  filterDate.value = date ? date.value : ''
  showDatePopover.value = false
  loadSchedules()
}

async function loadSchedules() {
  loading.value = true
  try {
    const params: Record<string, any> = {}
    if (filterDept.value) {
      params.deptName = filterDept.value
      params.departmentId = deptMap.value[filterDept.value] || undefined
    }
    if (filterDate.value) params.workDate = filterDate.value
    let result: Schedule[] = await listSchedules(params)
    if (!filterDate.value) {
      result = result.filter(s => s.workDate >= todayStr)
    }
    schedules.value = result
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function goToSeckill(schedule: Schedule) {
  router.push(`/registration/seckill/${schedule.id}`)
}
</script>

<style scoped>
.schedule-list-page {
  animation: fade-in 0.3s ease;
}

.filter-bar {
  padding: 12px 16px 0;
}

.filter-triggers {
  display: flex;
  gap: 12px;
}

.filter-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: var(--radius-full);
  border: 1px solid var(--color-divider);
  background: var(--color-card);
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  user-select: none;
}

.filter-btn.active {
  color: var(--color-primary);
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.arrow-icon {
  font-size: 12px;
  transition: transform var(--transition-fast);
}

.arrow-icon.rotated {
  transform: rotate(180deg);
}

.schedule-list {
  padding: 12px 16px;
}
</style>
