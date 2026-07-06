<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showLoadingToast, closeToast } from 'vant'
import { listDoctorPending, type DoctorConsultSession } from '@/api/consult'

const router = useRouter()

const sessions = ref<DoctorConsultSession[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const finished = ref(false)

function genderLabel(g: number) {
  return g === 1 ? '男' : g === 2 ? '女' : '未知'
}

function statusLabel(s: string) {
  return s === 'PENDING_DOCTOR' ? '⏳ 待接诊' : '🩺 沟通中'
}

function relativeTime(time: string) {
  if (!time) return ''
  const now = Date.now()
  const t = new Date(time).getTime()
  const diff = now - t
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return '刚刚'
  if (mins < 60) return `${mins}分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  return `${days}天前`
}

async function fetchSessions(reset = false) {
  if (loading.value) return
  if (reset) {
    page.value = 1
    sessions.value = []
    finished.value = false
  }

  loading.value = true
  try {
    const res = await listDoctorPending({ keyword: keyword.value || undefined, page: page.value, size: 10 })
    const data = res as any
    const list = Array.isArray(data) ? data : (data?.list || [])
    sessions.value = reset ? list : [...sessions.value, ...list]

    const t = Array.isArray(data) ? data.length : (data?.total || 0)
    total.value = t
    if (sessions.value.length >= t) {
      finished.value = true
    }
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function onLoad() {
  page.value++
  fetchSessions()
}

function onSearch() {
  fetchSessions(true)
}

function goToDetail(session: DoctorConsultSession) {
  router.push(`/doctor/consultations/${session.sessionSn}`)
}

onMounted(() => {
  fetchSessions(true)
})
</script>

<template>
  <div class="doctor-consult-list-page">
    <van-nav-bar title="待接诊问诊" left-arrow @click-left="$router.push('/home')" />

    <van-search v-model="keyword" placeholder="搜索症状描述..." @search="onSearch" />

    <van-pull-refresh v-model="loading" @refresh="onSearch" success-text="刷新成功">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <div v-if="sessions.length === 0 && !loading" class="empty-state">
          <van-empty description="暂无待接诊问诊" />
        </div>

        <van-cell
          v-for="s in sessions"
          :key="s.sessionSn"
          :title="`${s.patientName} | ${genderLabel(s.patientGender)}${s.patientAge != null ? ' | ' + s.patientAge + '岁' : ''}`"
          :label="s.symptomSummary || '(无描述)'"
          is-link
          @click="goToDetail(s)"
        >
          <template #value>
            <van-tag :type="s.status === 'PENDING_DOCTOR' ? 'warning' : 'primary'" size="small">
              {{ statusLabel(s.status) }}
            </van-tag>
          </template>
          <template #extra>
            <span class="meta-info">
              📎{{ s.fileCount }} · 💬{{ s.turnCount }}
            </span>
          </template>
          <template #bottom>
            <span class="time-info">{{ relativeTime(s.lastChatTime) }}</span>
          </template>
        </van-cell>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<style scoped>
.doctor-consult-list-page {
  min-height: 100vh;
  background: var(--color-bg);
}

.empty-state {
  padding-top: 40px;
}

.meta-info {
  font-size: var(--font-size-small);
  color: var(--color-text-tertiary);
}

.time-info {
  font-size: var(--font-size-small);
  color: var(--color-text-tertiary);
}
</style>
