import request from './request'

export function listPrescriptions() {
  return request.get('/api/v1/prescriptions')
}

export function getPrescription(id: number, patientId: number) {
  return request.get(`/api/v1/prescriptions/${id}`, { params: { patientId } })
}

export function listPendingAudit() {
  return request.get('/api/v1/prescriptions/pending-audit')
}

export function issuePrescription(data: Record<string, any>, doctorId: number) {
  return request.post('/api/v1/prescription/issue', data, { params: { doctorId } })
}

export function auditPrescription(id: number, data: Record<string, any>, pharmacistId: number) {
  return request.post(`/api/v1/prescriptions/${id}/audit`, data, { params: { pharmacistId } })
}

/** 搜索药品（前缀优先 + 模糊匹配） */
export function searchMedicine(keyword: string, limit: number = 10) {
  return request.get('/api/v1/medicine/search', { params: { keyword, limit } })
}
