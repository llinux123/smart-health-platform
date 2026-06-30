import request from './request'
import { isMockEnabled } from '@/mock'
import { mockLogin, mockRegister } from '@/mock/auth'

export function login(data) {
  if (isMockEnabled()) return mockLogin(data)
  return request.post('/api/v1/auth/login', data)
}

export function register(data) {
  if (isMockEnabled()) return mockRegister(data)
  return request.post('/api/v1/auth/register', data)
}
