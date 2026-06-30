import request from './request'
import { isMockEnabled } from '@/mock'
import { mockListPrescriptions, mockGetPrescription, mockListPendingAudit } from '@/mock/prescription'

export function listPrescriptions(patientId) {
  if (isMockEnabled()) return mockListPrescriptions()
  return request.get('/api/v1/prescriptions', { params: { patientId } })
}

export function getPrescription(id, patientId) {
  if (isMockEnabled()) return mockGetPrescription()
  return request.get(`/api/v1/prescriptions/${id}`, { params: { patientId } })
}

export function listPendingAudit() {
  if (isMockEnabled()) return mockListPendingAudit()
  return request.get('/api/v1/prescriptions/pending-audit')
}

export function issuePrescription(data, doctorId) {
  return request.post('/api/v1/prescription/issue', data, { params: { doctorId } })
}

export function auditPrescription(id, data, pharmacistId) {
  return request.post(`/api/v1/prescriptions/${id}/audit`, data, { params: { pharmacistId } })
}
