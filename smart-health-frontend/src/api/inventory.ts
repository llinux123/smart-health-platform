import request from './request'

export interface InventoryItem {
  id: number
  pharmacyId: number
  medicineId: number
  medicineName: string
  stock: number
  lockStock: number
  unit: string
  updateTime: string
}

export interface InventoryLogItem {
  id: number
  pharmacyId: number
  medicineId: number
  changeType: string
  quantityChange: number
  stockBefore: number
  stockAfter: number
  reason: string
  operatorId: number
  createTime: string
}

/** 查询药房库存列表 */
export function listInventory(pharmacyId: number = 1) {
  return request.get('/api/v1/inventory/list', { params: { pharmacyId } })
}

/** 入库 */
export function inbound(data: { pharmacyId: number; medicineId: number; quantity: number; reason?: string }) {
  return request.post('/api/v1/inventory/inbound', data)
}

/** 出库 */
export function outbound(data: { pharmacyId: number; medicineId: number; quantity: number; reason?: string }) {
  return request.post('/api/v1/inventory/outbound', data)
}

/** 盘点 */
export function reconcile(data: { pharmacyId: number; medicineId: number; actualStock: number; reason?: string }) {
  return request.post('/api/v1/inventory/reconcile', data)
}

/** 库存变动日志 */
export function listInventoryLogs(pharmacyId?: number, medicineId?: number, page: number = 1, size: number = 20) {
  return request.get('/api/v1/inventory/logs', { params: { pharmacyId, medicineId, page, size } })
}
