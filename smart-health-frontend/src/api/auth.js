import request from './request'

export function login(data) {
  return request.post('/api/v1/auth/login', data)
}

export function register(data) {
  return request.post('/api/v1/auth/register', data)
}
