import { defineStore } from 'pinia'
import { ref } from 'vue'
import { listSessions, getSessionHistory } from '@/api/consult'

export const useConsultationStore = defineStore('consultation', () => {
  const sessionList = ref([])
  const currentSessionSn = ref('')
  const chatHistory = ref([])

  async function fetchSessions() {
    sessionList.value = await listSessions()
  }

  async function fetchHistory(sessionSn) {
    currentSessionSn.value = sessionSn
    chatHistory.value = await getSessionHistory(sessionSn)
  }

  function clearCurrent() {
    currentSessionSn.value = ''
    chatHistory.value = []
  }

  return {
    sessionList,
    currentSessionSn,
    chatHistory,
    fetchSessions,
    fetchHistory,
    clearCurrent
  }
})
