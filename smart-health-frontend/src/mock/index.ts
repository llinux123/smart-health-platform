import { mockLogin, mockRegister } from './auth'
import { mockCreateSession, mockListSessions, mockGetSessionHistory, mockMultimodalAnalyze } from './consult'
import { mockListSchedules, mockSeckill, mockGetOrderDetail, mockListOrders } from './registration'
import { mockListPrescriptions, mockGetPrescription, mockListPendingAudit } from './prescription'
import { mockGetPatientStats } from './dashboard'

type MockHandler = (...args: any[]) => Promise<any>

const mockHandlers: Record<string, MockHandler> = {
  'POST:/api/v1/auth/login': mockLogin,
  'POST:/api/v1/auth/register': mockRegister,
  'POST:/api/v1/ai/sessions': mockCreateSession,
  'GET:/api/v1/ai/sessions': mockListSessions,
  'GET:/api/v1/ai/sessions/history': mockGetSessionHistory,
  'POST:/api/v1/ai/multimodal/analyze': mockMultimodalAnalyze,
  'GET:/api/v1/schedule/list': mockListSchedules,
  'POST:/api/v1/registration/seckill': mockSeckill,
  'GET:/api/v1/registration/order/detail': mockGetOrderDetail,
  'GET:/api/v1/registration/order/list': mockListOrders,
  'GET:/api/v1/prescriptions': mockListPrescriptions,
  'GET:/api/v1/prescriptions/detail': mockGetPrescription,
  'GET:/api/v1/prescriptions/pending-audit': mockListPendingAudit,
  'GET:/api/v1/dashboard/stats': mockGetPatientStats
}

export function getMockHandler(method: string, url: string): MockHandler | null {
  const cleanUrl = url.split('?')[0]
  for (const [pattern, handler] of Object.entries(mockHandlers)) {
    const [pMethod, pUrl] = pattern.split(':')
    if (pMethod === method.toUpperCase() && cleanUrl.startsWith(pUrl)) {
      return handler
    }
  }
  return null
}

export const isMockEnabled = (): boolean => import.meta.env.VITE_USE_MOCK === 'true'
