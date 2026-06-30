import request from './request'

export function listPrescriptions(patientId) {
  return request.get('/api/v1/prescriptions', { params: { patientId } })
}

export function getPrescription(id, patientId) {
  return request.get(`/api/v1/prescriptions/${id}`, { params: { patientId } })
}

export function listPendingAudit() {
  return request.get('/api/v1/prescriptions/pending-audit')
}

export function issuePrescription(data, doctorId) {
  return request.post('/api/v1/prescription/issue', data, { params: { doctorId } })
}

export function auditPrescription(id, data, pharmacistId) {
  return request.post(`/api/v1/prescriptions/${id}/audit`, data, { params: { pharmacistId } })
}
