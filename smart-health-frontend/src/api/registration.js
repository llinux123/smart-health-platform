import request from './request'
import { isMockEnabled } from '@/mock'
import { mockListSchedules, mockSeckill, mockGetOrderDetail, mockListOrders } from '@/mock/registration'

export function listSchedules(params) {
  if (isMockEnabled()) return mockListSchedules(params)
  return request.get('/api/v1/schedule/list', { params })
}

export function createSchedule(data) {
  return request.post('/api/v1/schedule/create', data)
}

export function seckill(data) {
  if (isMockEnabled()) return mockSeckill(data)
  return request.post('/api/v1/registration/seckill', data)
}

export function getOrderDetail(orderSn) {
  if (isMockEnabled()) return mockGetOrderDetail()
  return request.get('/api/v1/registration/order/detail', { params: { orderSn } })
}

export function listOrders(patientId) {
  if (isMockEnabled()) return mockListOrders()
  return request.get('/api/v1/registration/order/list', { params: { patientId } })
}
