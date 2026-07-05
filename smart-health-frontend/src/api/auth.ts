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

export interface LoginResponse {
  token: string
  userId: number
  username: string
  realName: string
  role: string
  patientId?: number | null
  doctorId?: number | null
  isNewUser?: boolean
  randomPassword?: string
}

export interface BindIdentityData {
  realName: string
  idCard: string
  gender: number
  email?: string
}

export interface ResetPasswordData {
  phone: string
  verifyCode: string
  newPassword: string
}

export function login(data: LoginData) {
  return request.post<LoginResponse>('/api/v1/auth/login', data)
}

export function register(data: RegisterData) {
  return request.post('/api/v1/auth/register', data)
}

export function sendSmsCode(phone: string) {
  return request.post('/api/v1/auth/send-code', { phone })
}

export function smsLogin(phone: string, code: string) {
  return request.post<LoginResponse>('/api/v1/auth/login/sms', { phone, code })
}

export function bindIdentity(data: BindIdentityData) {
  return request.post('/api/v1/auth/bind-identity', data)
}

export function resetPassword(data: ResetPasswordData) {
  return request.post('/api/v1/auth/reset-password', data)
}

export function getProfile() {
  return request.get('/api/v1/auth/profile')
}

export function logout() {
  return request.post('/api/v1/auth/logout')
}
