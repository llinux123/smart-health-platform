import request from './request'

export function listSchedules(params) {
  return request.get('/api/v1/schedule/list', { params })
}

export function getDoctorDetail(id) {
  return request.get(`/api/v1/doctor/${id}`)
}

export function createSchedule(data) {
  return request.post('/api/v1/schedule/create', data)
}

export function seckill(data) {
  return request.post('/api/v1/registration/seckill', data)
}

export function getOrderDetail(orderSn) {
  return request.get('/api/v1/registration/order/detail', { params: { orderSn } })
}

export function listOrders(patientId) {
  return request.get('/api/v1/registration/order/list', { params: { patientId } })
}

export function cancelOrder(orderSn) {
  return request.post('/api/v1/registration/order/cancel', null, { params: { orderSn } })
}

export function payOrder(orderSn) {
  return request.post('/api/v1/registration/order/pay', null, { params: { orderSn } })
}
