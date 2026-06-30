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

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listSchedules } from '@/api/registration'
import { formatDate } from '@/utils/format'
import ScheduleCard from '@/components/ScheduleCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const schedules = ref([])
const loading = ref(true)
const filterDept = ref('')
const filterDate = ref('')

const deptOptions = [
  { text: '全部科室', value: '' },
  { text: '皮肤科', value: '皮肤科' },
  { text: '内科', value: '内科' },
  { text: '骨科', value: '骨科' },
  { text: '心内科', value: '心内科' }
]

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

onMounted(() => loadSchedules())

async function loadSchedules() {
  loading.value = true
  try {
    const params = {}
    if (filterDept.value) params.deptName = filterDept.value
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
.filter-bar {
  background: #fff;
  margin-bottom: 12px;
}

.page-loading {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.schedule-list {
  padding: 0 16px;
}
</style>
