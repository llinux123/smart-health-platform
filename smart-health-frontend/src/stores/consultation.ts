import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  listSessions,
  getSessionTurns,
  togglePin,
  deleteSession,
  completeSession,
  rateSession,
  regenerateLastTurn,
  listRecycleBin,
  restoreSession,
  permanentDeleteSession,
  type SessionInfo,
  type TurnInfo,
  type SessionListParams,
  type PageResult
} from '@/api/consult'

export type { SessionInfo, TurnInfo, SessionListParams, PageResult }

export const useConsultationStore = defineStore('consultation', () => {
  // ============ 会话列表 ============
  const sessionList = ref<SessionInfo[]>([])
  const sessionTotal = ref(0)
  const sessionPage = ref(1)
  const sessionLoading = ref(false)

  // ============ 当前会话 ============
  const currentSessionSn = ref<string>('')
  const turns = ref<TurnInfo[]>([])
  const turnsTotal = ref(0)
  const turnsPage = ref(1)
  const turnsLoading = ref(false)
  const hasMoreTurns = computed(() => turns.value.length < turnsTotal.value)

  // ============ 回收站 ============
  const recycleBinList = ref<SessionInfo[]>([])
  const recycleBinTotal = ref(0)
  const recycleBinPage = ref(1)
  const recycleBinLoading = ref(false)

  // ============ 会话列表操作 ============

  async function fetchSessions(params: SessionListParams = {}, append = false): Promise<void> {
    sessionLoading.value = true
    try {
      const res = await listSessions(params)
      // 兼容后端未更新时的数组格式
      const list = Array.isArray(res) ? res : (res?.list || [])
      const total = Array.isArray(res) ? res.length : (res?.total || 0)
      if (append) {
        sessionList.value = [...sessionList.value, ...list]
      } else {
        sessionList.value = list
      }
      sessionTotal.value = total
      sessionPage.value = params.page || 1
    } finally {
      sessionLoading.value = false
    }
  }

  async function loadMoreSessions(params: SessionListParams = {}): Promise<void> {
    const nextPage = sessionPage.value + 1
    await fetchSessions({ ...params, page: nextPage }, true)
  }

  // ============ 对话轮次操作 ============

  async function fetchTurns(sessionSn: string, page = 1, append = false): Promise<void> {
    turnsLoading.value = true
    try {
      currentSessionSn.value = sessionSn
      const res = await getSessionTurns(sessionSn, page)
      // 兼容后端未更新时的数组格式
      const list = Array.isArray(res) ? res : (res?.list || [])
      const total = Array.isArray(res) ? res.length : (res?.total || 0)
      if (append) {
        turns.value = [...list, ...turns.value]
      } else {
        turns.value = list
      }
      turnsTotal.value = total
      turnsPage.value = page
    } finally {
      turnsLoading.value = false
    }
  }

  async function loadOlderTurns(sessionSn: string): Promise<void> {
    const nextPage = turnsPage.value + 1
    await fetchTurns(sessionSn, nextPage, true)
  }

  // ============ 会话管理操作 ============

  async function toggleSessionPin(sessionSn: string): Promise<void> {
    await togglePin(sessionSn)
    const session = sessionList.value.find(s => s.sessionSn === sessionSn)
    if (session) {
      session.isPinned = !session.isPinned
    }
  }

  async function deleteSessionItem(sessionSn: string, mode: 'recycle' | 'permanent'): Promise<void> {
    await deleteSession(sessionSn, mode)
    sessionList.value = sessionList.value.filter(s => s.sessionSn !== sessionSn)
    sessionTotal.value = Math.max(0, sessionTotal.value - 1)
  }

  async function completeSessionItem(sessionSn: string): Promise<void> {
    await completeSession(sessionSn)
    const session = sessionList.value.find(s => s.sessionSn === sessionSn)
    if (session) {
      session.status = 'COMPLETED'
    }
  }

  async function rateSessionItem(sessionSn: string, rating: number, feedback?: string): Promise<void> {
    await rateSession(sessionSn, rating, feedback)
    const session = sessionList.value.find(s => s.sessionSn === sessionSn)
    if (session) {
      session.hasRating = true
    }
  }

  async function regenerateTurn(sessionSn: string, turnNumber: number): Promise<TurnInfo> {
    const res = await regenerateLastTurn(sessionSn, turnNumber)
    // 更新本地 turn 数据
    const turnIndex = turns.value.findIndex(t => t.turnNumber === turnNumber)
    if (turnIndex !== -1) {
      turns.value[turnIndex] = res
    }
    return res
  }

  function clearCurrent(): void {
    currentSessionSn.value = ''
    turns.value = []
    turnsTotal.value = 0
    turnsPage.value = 1
  }

  // ============ 回收站操作 ============

  async function fetchRecycleBin(page = 1, append = false): Promise<void> {
    recycleBinLoading.value = true
    try {
      const res = await listRecycleBin(page)
      // 兼容后端未更新时的数组格式
      const list = Array.isArray(res) ? res : (res?.list || [])
      const total = Array.isArray(res) ? res.length : (res?.total || 0)
      if (append) {
        recycleBinList.value = [...recycleBinList.value, ...list]
      } else {
        recycleBinList.value = list
      }
      recycleBinTotal.value = total
      recycleBinPage.value = page
    } finally {
      recycleBinLoading.value = false
    }
  }

  async function restoreSessionItem(sessionSn: string): Promise<void> {
    await restoreSession(sessionSn)
    recycleBinList.value = recycleBinList.value.filter(s => s.sessionSn !== sessionSn)
    recycleBinTotal.value = Math.max(0, recycleBinTotal.value - 1)
  }

  async function permanentDeleteSessionItem(sessionSn: string): Promise<void> {
    await permanentDeleteSession(sessionSn)
    recycleBinList.value = recycleBinList.value.filter(s => s.sessionSn !== sessionSn)
    recycleBinTotal.value = Math.max(0, recycleBinTotal.value - 1)
  }

  return {
    // 会话列表
    sessionList,
    sessionTotal,
    sessionPage,
    sessionLoading,
    fetchSessions,
    loadMoreSessions,
    // 对话轮次
    currentSessionSn,
    turns,
    turnsTotal,
    turnsPage,
    turnsLoading,
    hasMoreTurns,
    fetchTurns,
    loadOlderTurns,
    // 会话管理
    toggleSessionPin,
    deleteSessionItem,
    completeSessionItem,
    rateSessionItem,
    regenerateTurn,
    clearCurrent,
    // 回收站
    recycleBinList,
    recycleBinTotal,
    recycleBinPage,
    recycleBinLoading,
    fetchRecycleBin,
    restoreSessionItem,
    permanentDeleteSessionItem
  }
})
