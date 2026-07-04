import request from './request'

export interface PatientStats {
  consultCount: number
  appointmentCount: number
  prescriptionCount: number
}

/** 获取患者首页统计数据 */
export function getPatientStats(): Promise<PatientStats> {
  return request.get('/api/v1/dashboard/stats')
}
