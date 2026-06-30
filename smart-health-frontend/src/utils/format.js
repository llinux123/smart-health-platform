/**
 * 日期格式化
 */
export function formatDate(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/**
 * 日期时间格式化
 */
export function formatDateTime(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}`
}

/**
 * 金额格式化
 */
export function formatMoney(amount) {
  if (amount == null) return '¥0.00'
  return `¥${Number(amount).toFixed(2)}`
}

/**
 * 订单状态映射
 */
const ORDER_STATUS_MAP = {
  0: { text: '排队中', color: 'primary' },
  1: { text: '待支付', color: 'warning' },
  2: { text: '已支付', color: 'success' },
  3: { text: '已就诊', color: 'default' },
  4: { text: '已退号', color: 'danger' }
}

export function getOrderStatus(status) {
  return ORDER_STATUS_MAP[status] || { text: '未知', color: 'default' }
}

/**
 * 审核状态映射
 */
const AUDIT_STATUS_MAP = {
  0: { text: '待审核', color: 'warning' },
  1: { text: '审核通过', color: 'success' },
  2: { text: '已驳回', color: 'danger' }
}

export function getAuditStatus(status) {
  return AUDIT_STATUS_MAP[status] || { text: '未知', color: 'default' }
}

/**
 * 班次名称
 */
const SHIFT_MAP = {
  1: '上午',
  2: '下午',
  3: '晚上'
}

export function getShiftName(shift) {
  return SHIFT_MAP[shift] || '未知班次'
}
