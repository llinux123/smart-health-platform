<template>
  <div class="schedule-list-page page-container">
    <van-nav-bar title="挂号预约" left-arrow @click-left="$router.back()" />

    <!-- 筛选区 -->
    <div class="filter-bar">
      <van-dropdown-menu>
        <van-dropdown-item v-model="filterDept" :options="deptOptions" @change="loadSchedules" />
        <van-dropdown-item v-model="filterDate" :options="dateOptions" @change="loadSchedules" />
      </van-dropdown-menu>
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listSchedules, listDepartments } from '@/api/registration'
import { formatDate } from '@/utils/format'
import ScheduleCard from '@/components/ScheduleCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const schedules = ref([])
const loading = ref(true)
const filterDept = ref('')
const filterDate = ref('')

const deptOptions = ref([{ text: '全部科室', value: '' }])
const deptMap = ref<Record<string, number>>({})

// 生成未来 7 天日期选项
const dateOptions = [
  { text: '全部日期', value: '' },
  ...Array.from({ length: 7 }, (_, i) => {
    const d = new Date()
    d.setDate(d.getDate() + i)
    const dateStr = d.toISOString().slice(0, 10)
    return { text: i === 0 ? '今天' : i === 1 ? '明天' : formatDate(dateStr), value: dateStr }
  })
]

onMounted(async () => {
  try {
    const depts: Array<{ id: number; name: string }> = await listDepartments()
    deptMap.value = Object.fromEntries(depts.map(d => [d.name, d.id]))
    deptOptions.value = [
      { text: '全部科室', value: '' },
      ...depts.map(d => ({ text: d.name, value: d.name }))
    ]
  } catch {
    // fallback 静默处理
  }
  await loadSchedules()
})

async function loadSchedules() {
  loading.value = true
  try {
    const params: Record<string, any> = {}
    if (filterDept.value) {
      params.deptName = filterDept.value
      params.departmentId = deptMap.value[filterDept.value] || undefined
    }
    if (filterDate.value) params.workDate = filterDate.value
    schedules.value = await listSchedules(params)
  } catch (err) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

function goToSeckill(schedule) {
  router.push(`/registration/seckill/${schedule.id}`)
}
</script>

<style scoped>
.schedule-list-page {
  animation: fade-in 0.3s ease;
}

.filter-bar {
  background: var(--color-card);
  margin-bottom: 12px;
  border-radius: var(--radius-md);
  margin: 12px 16px 8px;
  box-shadow: var(--shadow-sm);
}

.schedule-list {
  padding: 0 16px;
}
</style>
