interface StatusInfo {
  text: string
  color: string
}

export function formatDate(dateStr?: string): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

export function formatDateTime(dateStr?: string): string {
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

export function formatMoney(amount: number | null | undefined): string {
  if (amount == null) return '¥0.00'
  const num = Number(amount)
  if (isNaN(num)) return '¥0.00'
  return `¥${num.toFixed(2)}`
}

const ORDER_STATUS_MAP: Record<number, StatusInfo> = {
  0: { text: '排队中', color: 'primary' },
  1: { text: '待支付', color: 'warning' },
  2: { text: '已支付', color: 'success' },
  3: { text: '已就诊', color: 'default' },
  4: { text: '已退号', color: 'danger' }
}

export function getOrderStatus(status: number): StatusInfo {
  return ORDER_STATUS_MAP[status] || { text: '未知', color: 'default' }
}

const AUDIT_STATUS_MAP: Record<number, StatusInfo> = {
  0: { text: '待审核', color: 'warning' },
  1: { text: '审核通过', color: 'success' },
  2: { text: '已驳回', color: 'danger' }
}

export function getAuditStatus(status: number): StatusInfo {
  return AUDIT_STATUS_MAP[status] || { text: '未知', color: 'default' }
}

const SHIFT_MAP: Record<number, string> = {
  1: '上午',
  2: '下午',
  3: '晚上'
}

export function getShiftName(shift: number): string {
  return SHIFT_MAP[shift] || '未知班次'
}

// ============ 问诊会话状态 ============

const CONSULTATION_STATUS_MAP: Record<string, StatusInfo> = {
  IN_PROGRESS: { text: '问诊中', color: 'primary' },
  PENDING_DOCTOR: { text: '待接诊', color: 'warning' },
  DOCTOR_ACTIVE: { text: '医生已接诊', color: 'primary' },
  COMPLETED: { text: '已结束', color: 'default' }
}

export function getConsultationStatus(status: string): StatusInfo {
  return CONSULTATION_STATUS_MAP[status] || { text: '未知', color: 'default' }
}

/** 格式化相对时间（如：3分钟前、2小时前、昨天） */
export function formatRelativeTime(dateStr?: string): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const now = Date.now()
  const diff = now - d.getTime()
  if (diff < 0) return formatDate(dateStr)
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  return formatDate(dateStr)
}
