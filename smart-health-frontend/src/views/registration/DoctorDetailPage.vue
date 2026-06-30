<template>
  <div class="doctor-detail-page page-container">
    <van-nav-bar title="医生详情" left-arrow @click-left="$router.back()" />

    <div class="doctor-info card">
      <div class="doctor-header">
        <van-image round width="64" height="64" :src="doctor.avatar || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'" />
        <div class="doctor-meta">
          <h2>{{ doctor.name }}</h2>
          <van-tag type="primary">{{ doctor.title }}</van-tag>
          <p class="dept-name">{{ doctor.deptName }}</p>
        </div>
      </div>
      <div class="doctor-specialty">
        <h4>擅长领域</h4>
        <p>{{ doctor.specialty }}</p>
      </div>
      <div class="doctor-intro">
        <h4>医生简介</h4>
        <p>{{ doctor.intro }}</p>
      </div>
    </div>

    <div class="schedule-section">
      <h3 class="section-title">出诊排班</h3>
      <ScheduleCard
        v-for="s in schedules"
        :key="s.id"
        :schedule="s"
        @seckill="goToSeckill(s)"
      />
      <EmptyState v-if="schedules.length === 0" description="暂无排班信息" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listSchedules, getDoctorDetail } from '@/api/registration'
import ScheduleCard from '@/components/ScheduleCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const doctorId = Number(route.params.id)

const doctor = ref({ name: '未知医生', title: '主治医师', specialty: '', intro: '', deptName: '' })
const schedules = ref([])

onMounted(async () => {
  try {
    const [doctorData, allSchedules] = await Promise.all([
      getDoctorDetail(doctorId),
      listSchedules()
    ])
    doctor.value = { ...doctorData, deptName: '' }
    schedules.value = allSchedules.filter(s => s.doctorId === doctorId)
    if (schedules.value.length > 0) {
      doctor.value.deptName = schedules.value[0].deptName
    }
  } catch (err) {
    // 错误已在拦截器中处理
  }
})

function goToSeckill(schedule) {
  router.push(`/registration/seckill/${schedule.id}`)
}
</script>

<style scoped>
.doctor-info {
  margin: 16px;
}

.doctor-header {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
}

.doctor-meta h2 {
  font-size: 20px;
  margin-bottom: 4px;
}

.dept-name {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}

.doctor-specialty,
.doctor-intro {
  margin-top: 12px;
}

.doctor-specialty h4,
.doctor-intro h4 {
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
}

.doctor-specialty p,
.doctor-intro p {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.schedule-section {
  padding: 0 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 12px;
}
</style>
