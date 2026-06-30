import request from './request'
import { isMockEnabled } from '@/mock'
import {
  mockMultimodalAnalyze,
  mockCreateSession,
  mockListSessions,
  mockGetSessionHistory
} from '@/mock/consult'

export function multimodalAnalyze(file, type) {
  if (isMockEnabled()) return mockMultimodalAnalyze()
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', type)
  return request.post('/api/v1/ai/multimodal/analyze', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}

export function createSession(draftId, symptomDraft) {
  if (isMockEnabled()) return mockCreateSession()
  return request.post('/api/v1/ai/sessions', null, {
    params: { draftId, symptomDraft }
  })
}

export function listSessions() {
  if (isMockEnabled()) return mockListSessions()
  return request.get('/api/v1/ai/sessions')
}

export function getSessionHistory(sessionSn) {
  if (isMockEnabled()) return mockGetSessionHistory()
  return request.get(`/api/v1/ai/sessions/${sessionSn}/history`)
}
