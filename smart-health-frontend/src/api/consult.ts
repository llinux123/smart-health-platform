import request from './request'

// ============ 多模态分析 ============

export function multimodalAnalyze(files: File[], type: string) {
  const formData = new FormData()
  files.forEach(file => formData.append('files', file))
  formData.append('type', type)
  return request.post('/api/v1/ai/multimodal/analyze', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}

// ============ 会话管理 ============

export interface SessionListParams {
  keyword?: string
  startDate?: string
  endDate?: string
  status?: 'IN_PROGRESS' | 'COMPLETED'
  isPinned?: boolean
  page?: number
  size?: number
}

export interface SessionInfo {
  id: number
  sessionSn: string
  symptomDraftSummary: string
  symptomDraft: string
  fileUrls: string
  aiSummary: string
  turnCount: number
  status: 'IN_PROGRESS' | 'COMPLETED' | 'PENDING_DOCTOR' | 'DOCTOR_ACTIVE'
  isPinned: boolean
  createTime: string
  lastChatTime: string
  hasRating: boolean
  assignedDoctorId?: number
  assignedDoctorName?: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

/** 创建问诊会话 */
export function createSession(draftId: string, symptomDraft: string, fileUrls?: string) {
  return request.post('/api/v1/ai/sessions', null, {
    params: { draftId, symptomDraft, fileUrls }
  })
}

/** 分页查询会话列表 */
export function listSessions(params: SessionListParams = {}) {
  return request.get('/api/v1/ai/sessions', { params })
}

/** 获取会话详情 */
export function getSessionDetail(sessionSn: string) {
  return request.get(`/api/v1/ai/sessions/${sessionSn}`)
}

/** 置顶/取消置顶 */
export function togglePin(sessionSn: string) {
  return request.put(`/api/v1/ai/sessions/${sessionSn}/pin`)
}

/** 删除会话（mode: recycle | permanent） */
export function deleteSession(sessionSn: string, mode: 'recycle' | 'permanent') {
  return request.post(`/api/v1/ai/sessions/${sessionSn}/delete`, null, {
    params: { mode }
  })
}

/** 确认结束问诊 */
export function completeSession(sessionSn: string) {
  return request.post(`/api/v1/ai/sessions/${sessionSn}/complete`)
}

/** 转接真人医生 */
export function handoffSession(sessionSn: string, reason?: string) {
  return request.post(`/api/v1/ai/sessions/${sessionSn}/handoff`, null, {
    params: { reason }
  })
}

/** 评分 */
export function rateSession(sessionSn: string, rating: number, feedback?: string) {
  return request.post(`/api/v1/ai/sessions/${sessionSn}/rate`, { rating, feedback })
}

// ============ 对话轮次 ============

export interface TurnInfo {
  id: number
  turnNumber: number
  userMessage: string
  assistantMessage: string
  citations: Array<{ title: string; category: string; snippet: string }>
  senderType?: string
  createTime: string
}

/** 分页获取对话轮次（按 turn_number DESC，每次 5 轮） */
export function getSessionTurns(sessionSn: string, page: number = 1, size: number = 5) {
  return request.get(`/api/v1/ai/sessions/${sessionSn}/turns`, {
    params: { page, size }
  })
}

/** 重新生成最后一轮 AI 回复 */
export function regenerateLastTurn(sessionSn: string, turnNumber: number) {
  return request.post(`/api/v1/ai/sessions/${sessionSn}/turns/${turnNumber}/regenerate`)
}

// ============ 回收站 ============

/** 回收站列表（分页） */
export function listRecycleBin(page: number = 1, size: number = 10) {
  return request.get('/api/v1/ai/sessions/recycle-bin', {
    params: { page, size }
  })
}

/** 从回收站恢复 */
export function restoreSession(sessionSn: string) {
  return request.post(`/api/v1/ai/sessions/recycle-bin/${sessionSn}/restore`)
}

/** 回收站中彻底删除 */
export function permanentDeleteSession(sessionSn: string) {
  return request.post(`/api/v1/ai/sessions/recycle-bin/${sessionSn}/permanent`)
}

// ============ 旧接口（向后兼容） ============

/** @deprecated 使用 getSessionTurns 替代 */
export function getSessionHistory(sessionSn: string) {
  return request.get(`/api/v1/ai/sessions/${sessionSn}/history`)
}

// ============ 医生端问诊接口 ============

export interface DoctorConsultSession {
  sessionSn: string
  patientName: string
  patientGender: number
  patientAge: number
  symptomSummary: string
  fileCount: number
  turnCount: number
  status: 'PENDING_DOCTOR' | 'DOCTOR_ACTIVE'
  lastChatTime: string
}

export interface DoctorConsultDetail {
  sessionSn: string
  patientName: string
  patientGender: number
  patientAge: number
  symptomDraft: string
  aiSummary?: string
  fileUrls: string[]
  status: string
  turns: TurnInfo[]
  createTime: string
  lastChatTime: string
}

/** 医生端：待接诊列表 */
export function listDoctorPending(params: { keyword?: string; page?: number; size?: number } = {}) {
  return request.get('/api/v1/doctor/consultations/pending', { params })
}

/** 医生端：查看问诊详情 */
export function getDoctorConsultDetail(sessionSn: string) {
  return request.get(`/api/v1/doctor/consultations/${sessionSn}`)
}

/** 医生端：回复患者 */
export function doctorReply(sessionSn: string, message: string, action: 'REPLY' | 'RESOLVE' = 'REPLY') {
  return request.post(`/api/v1/doctor/consultations/${sessionSn}/reply`, { message, action })
}

/** 医生端：标记已解决 */
export function doctorResolve(sessionSn: string) {
  return request.post(`/api/v1/doctor/consultations/${sessionSn}/resolve`)
}
