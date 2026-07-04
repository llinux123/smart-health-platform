import request from './request'

export interface ScheduleQuery {
  deptName?: string
  departmentId?: number
  workDate?: string
}

export interface Department {
  id: number
  name: string
  description: string
  icon?: string
  intro?: string
  sortOrder: number
  isActive: boolean
}

export function listDepartments() {
  return request.get('/api/v1/dept/list')
}

export function getScheduleDetail(id: number) {
  return request.get(`/api/v1/schedule/detail/${id}`)
}

export interface RegistrationData {
  scheduleId: number
}

export function listSchedules(params: ScheduleQuery) {
  return request.get('/api/v1/schedule/list', { params })
}

export function getDoctorDetail(id: number) {
  return request.get(`/api/v1/doctor/${id}`)
}

export function createSchedule(data: Record<string, any>) {
  return request.post('/api/v1/schedule/create', data)
}

export function seckill(data: RegistrationData) {
  return request.post('/api/v1/registration/seckill', data)
}

export function getOrderDetail(orderSn: string) {
  return request.get('/api/v1/registration/order/detail', { params: { orderSn } })
}

export function listOrders() {
  return request.get('/api/v1/registration/order/list')
}

export function cancelOrder(orderSn: string) {
  return request.post('/api/v1/registration/order/cancel', null, { params: { orderSn } })
}

export function payOrder(orderSn: string) {
  return request.post('/api/v1/registration/order/pay', null, { params: { orderSn } })
}
