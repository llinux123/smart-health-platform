import request from './request'

export function multimodalAnalyze(file, type) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', type)
  return request.post('/api/v1/ai/multimodal/analyze', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}

export function createSession(draftId, symptomDraft) {
  return request.post('/api/v1/ai/sessions', null, {
    params: { draftId, symptomDraft }
  })
}

export function listSessions() {
  return request.get('/api/v1/ai/sessions')
}

export function getSessionHistory(sessionSn) {
  return request.get(`/api/v1/ai/sessions/${sessionSn}/history`)
}
