import request from './request'

export interface LoginData {
  username: string
  password: string
  loginType: 'PATIENT' | 'STAFF'
}

export interface RegisterData {
  username: string
  password: string
  realName: string
  phone: string
}

export function login(data: LoginData) {
  return request.post('/api/v1/auth/login', data)
}

export function register(data: RegisterData) {
  return request.post('/api/v1/auth/register', data)
}

export function getProfile() {
  return request.get('/api/v1/auth/profile')
}
