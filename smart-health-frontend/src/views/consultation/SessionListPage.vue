<template>
  <div class="session-list-page page-container">
    <van-nav-bar title="问诊记录">
      <template #right>
        <van-icon name="plus" size="20" @click="createNewSession" />
      </template>
    </van-nav-bar>

    <van-loading v-if="loading" class="page-loading" />

    <template v-else>
      <EmptyState v-if="sessions.length === 0" description="暂无问诊记录" action-text="开始新问诊" @action="createNewSession" />

      <van-cell-group v-else inset class="session-list">
        <van-cell
          v-for="session in sessions"
          :key="session.sessionSn"
          :title="session.symptomDraftSummary || '新问诊'"
          :label="`${session.turnCount || 0} 轮对话 · ${formatDateTime(session.createTime)}`"
          is-link
          @click="$router.push(`/consultation/chat/${session.sessionSn}`)"
        >
          <template #right-icon>
            <van-icon name="chat-o" size="20" color="#1890FF" />
          </template>
        </van-cell>
      </van-cell-group>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showLoadingToast } from 'vant'
import { listSessions, createSession } from '@/api/consult'
import { formatDateTime } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const sessions = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    sessions.value = await listSessions()
  } catch (err) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
})

async function createNewSession() {
  try {
    const sessionSn = await createSession()
    router.push(`/consultation/chat/${sessionSn}`)
  } catch (err) {
    // 错误已在拦截器中处理
  }
}
</script>

<style scoped>
.page-loading {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.session-list {
  margin-top: 16px;
}
</style>
