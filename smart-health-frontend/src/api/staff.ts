import request from './request'

export interface StaffVO {
  id: number
  username: string
  realName: string
  phone: string
  role: string
  doctorId: number | null
  createTime: string
}

export interface StaffRequest {
  username: string
  password?: string
  realName: string
  phone?: string
  role: 'DOCTOR' | 'PHARMACIST' | 'ADMIN'
  doctorId?: number | null
}

/** 获取员工列表 */
export function listStaff(): Promise<StaffVO[]> {
  return request.get('/api/v1/staff')
}

/** 创建员工 */
export function createStaff(data: StaffRequest): Promise<StaffVO> {
  return request.post('/api/v1/staff', data)
}

/** 更新员工信息 */
export function updateStaff(id: number, data: StaffRequest): Promise<StaffVO> {
  return request.put(`/api/v1/staff/${id}`, data)
}

/** 删除员工（软删除） */
export function deleteStaff(id: number): Promise<void> {
  return request.del(`/api/v1/staff/${id}`)
}
